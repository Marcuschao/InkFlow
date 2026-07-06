package com.blog.ai.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.config.properties.RagProperties;
import com.blog.ai.mapper.KnowledgeDocumentMapper;
import com.blog.ai.model.entity.KnowledgeDocument;
import com.blog.ai.rag.messaging.RagMessageProducer;
import com.blog.ai.service.MinioStorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class KnowledgeDocumentService {

    private static final int MAX_TITLE_LENGTH = 255;

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final MinioStorageService minioStorageService;
    private final MinioClient minioClient;
    private final RagProperties ragProperties;
    private final RagMessageProducer ragMessageProducer;
    private final TransactionTemplate insertTxTemplate;

    public KnowledgeDocumentService(KnowledgeDocumentMapper knowledgeDocumentMapper,
                                    MinioStorageService minioStorageService,
                                    MinioClient minioClient,
                                    RagProperties ragProperties,
                                    RagMessageProducer ragMessageProducer,
                                    PlatformTransactionManager transactionManager) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.minioStorageService = minioStorageService;
        this.minioClient = minioClient;
        this.ragProperties = ragProperties;
        this.ragMessageProducer = ragMessageProducer;
        this.insertTxTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 上传知识文档：校验 → 短事务落库 → MinIO（不占 DB 连接）→ 投递解析 MQ。
     * MinIO 失败会将记录标为 FAILED；MQ 失败同样标 FAILED。
     */
    public KnowledgeDocument upload(MultipartFile file) {
        // 检验文件
        validateUploadFile(file);

        String originalName = file.getOriginalFilename().trim();
        String fileType = extractExtension(originalName);
        String bucket = minioStorageService.bucketKnowledge();
        String objectKey = buildObjectKey(originalName);

        // 仅 insert 在短事务内，提交后立即释放连接
        KnowledgeDocument doc = insertPendingDocumentInTx(originalName, fileType, objectKey);
        Long docId = doc.getId();

        // MinIO 上传，失败标记为 FAILED
        try {
            uploadToMinio(file, bucket, objectKey);
        } catch (ServiceException e) {
            markUploadFailed(docId, e.getMessage());
            throw e;
        } catch (Exception e) {
            String msg = "MinIO 上传失败: " + e.getMessage();
            markUploadFailed(docId, msg);
            throw new ServiceException(500, msg);
        }

        // MQ投递解析任务
        dispatchParseTask(docId);
        return doc;
    }

    private KnowledgeDocument insertPendingDocumentInTx(String originalName, String fileType, String objectKey) {
        // 短事务落库，插入文档状态记录
        KnowledgeDocument inserted = insertTxTemplate.execute(status -> {
            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setTitle(truncateTitle(originalName));
            doc.setFileType(fileType);
            doc.setMinioPath(objectKey);
            doc.setStatus("PENDING");
            doc.setChunkCount(0L);
            doc.setCreateTime(LocalDateTime.now());
            doc.setUpdateTime(LocalDateTime.now());
            knowledgeDocumentMapper.insert(doc);
            return doc;
        });
        if (inserted == null || inserted.getId() == null) {
            throw new ServiceException(500, "创建知识文档记录失败");
        }
        return inserted;
    }

    private void markUploadFailed(Long docId, String errorMsg) {
        try {
            updateStatus(docId, "FAILED", errorMsg, null);
        } catch (Exception e) {
            log.error("[rag] mark upload failed docId={}: {}", docId, e.getMessage(), e);
        }
    }

    /**
     * 校验空文件、大小、扩展名等，全部在写库/上传前完成
     */
    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(400, "文件不能为空");
        }

        long size = file.getSize();
        if (size < 0) {
            throw new ServiceException(400, "无法获取文件大小，请重试");
        }
        long maxSize = ragProperties.getMaxFileSize();
        if (size > maxSize) {
            throw new ServiceException(400, "文件大小超过限制（最大 " + (maxSize / 1024 / 1024) + " MB）");
        }
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            throw new ServiceException(400, "文件名不能为空");
        }
        String fileType = extractExtension(originalName.trim());
        if (!ragProperties.getAllowFileType().contains(fileType)) {
            throw new ServiceException(400, "不支持的文件类型: " + fileType);
        }
    }

    /**
     * 流式写入 MinIO，避免大文件 OOM
     */
    private void uploadToMinio(MultipartFile file, String bucket, String objectKey) {
        try (InputStream inputStream = file.getInputStream()) {
            minioStorageService.putObject(
                    bucket, objectKey, inputStream, file.getSize(), file.getContentType());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(500, "MinIO 上传失败: " + e.getMessage());
        }
    }

    private void dispatchParseTask(Long docId) {
        if (ragMessageProducer.sendParseTask(docId)) {
            return;
        }
        log.error("[rag] parse task dispatch failed docId={}", docId);
        markDispatchFailed(docId, "解析任务投递失败，请稍后重试或联系管理员");
    }

    private void markDispatchFailed(Long docId, String errorMsg) {
        try {
            updateStatus(docId, "FAILED", errorMsg, null);
        } catch (Exception e) {
            log.error("[rag] mark dispatch failed docId={}: {}", docId, e.getMessage(), e);
        }
    }

    private String buildObjectKey(String originalName) {
        return "docs/" + UUID.randomUUID() + "/" + sanitizeFileName(originalName);
    }

    private static String truncateTitle(String name) {
        if (name == null) {
            return "file";
        }
        return name.length() <= MAX_TITLE_LENGTH ? name : name.substring(0, MAX_TITLE_LENGTH);
    }

    /**
     * 去掉路径分隔符等，避免 objectKey 异常
     */
    private static String sanitizeFileName(String name) {
        if (!StringUtils.hasText(name)) {
            return "file";
        }
        String s = name.trim()
                .replaceAll("[\\\\/]+", "_")
                .replaceAll("\\.\\.", "_")
                .replaceAll("[\\x00-\\x1f]", "");
        if (s.length() > 200) {
            s = s.substring(0, 200);
        }
        return s.isEmpty() ? "file" : s;
    }

    public InputStream openObject(KnowledgeDocument doc) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioStorageService.bucketKnowledge())
                    .object(doc.getMinioPath())
                    .build());
        } catch (Exception e) {
            throw new ServiceException(500, "MinIO 读取失败: " + e.getMessage());
        }
    }

    public IPage<KnowledgeDocument> page(long page, long size, String status) {
        QueryWrapper<KnowledgeDocument> qw = new QueryWrapper<>();
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        qw.orderByDesc("create_time");
        return knowledgeDocumentMapper.selectPage(new Page<>(page, size), qw);
    }

    public KnowledgeDocument get(Long id) {
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(id);
        if (doc == null) {
            throw new ServiceException(404, "知识文档不存在");
        }
        return doc;
    }

    public void delete(Long id) {
        KnowledgeDocument doc = get(id);
        try {
            minioStorageService.deleteObject(minioStorageService.bucketKnowledge(), doc.getMinioPath());
        } catch (Exception ignored) {
        }
        knowledgeDocumentMapper.deleteById(id);
    }

    public void updateStatus(Long id, String status, String errorMsg, Long chunkCount) {
        KnowledgeDocument update = new KnowledgeDocument();
        update.setId(id);
        update.setStatus(status);
        update.setErrorMsg(errorMsg);
        update.setChunkCount(chunkCount);
        update.setUpdateTime(LocalDateTime.now());
        knowledgeDocumentMapper.updateById(update);
    }

    private String extractExtension(String name) {
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx + 1).toLowerCase() : "unknown";
    }
}
