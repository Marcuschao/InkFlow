package com.blog.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.common.revision.RevisionTargetType;
import com.blog.content.mapper.ContentRevisionMapper;
import com.blog.content.model.entity.ContentRevision;
import com.blog.content.service.MinioStorageService;
import com.blog.content.service.RevisionContentStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevisionContentStorageImpl implements RevisionContentStorage {

    private final ContentRevisionMapper contentRevisionMapper;
    private final ObjectProvider<MinioStorageService> minioStorageProvider;

    @Override
    public void persistAfterInsert(ContentRevision revision, RevisionTargetType type, String content) {
        String body = content == null ? "" : content;
        MinioStorageService minio = minioStorageProvider.getIfAvailable();
        if (minio == null) {
            revision.setContent(body);
            contentRevisionMapper.updateById(revision);
            return;
        }
        String objectKey = objectKey(type, revision.getTargetId(), revision.getId());
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        minio.putObject(minio.bucketVersions(), objectKey, bytes, "text/markdown; charset=utf-8");
        revision.setContentStorageKey(objectKey);
        revision.setContentMd5(md5(bytes));
        revision.setContent(null);
        contentRevisionMapper.updateById(revision);
    }

    @Override
    public String loadContent(ContentRevision revision) {
        if (revision == null) {
            return "";
        }
        if (StringUtils.hasText(revision.getContent())) {
            return revision.getContent();
        }
        if (!StringUtils.hasText(revision.getContentStorageKey())) {
            return "";
        }
        MinioStorageService minio = minioStorageProvider.getIfAvailable();
        if (minio == null) {
            return "";
        }
        try (InputStream in = minio.getObject(minio.bucketVersions(), revision.getContentStorageKey())) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int migrateLegacyBatch(int batchSize) {
        List<ContentRevision> rows = contentRevisionMapper.selectList(new LambdaQueryWrapper<ContentRevision>()
                .isNotNull(ContentRevision::getContent)
                .isNull(ContentRevision::getContentStorageKey)
                .last("LIMIT " + Math.max(1, batchSize)));
        int count = 0;
        for (ContentRevision row : rows) {
            RevisionTargetType type = RevisionTargetType.valueOf(row.getTargetType());
            String content = row.getContent();
            persistAfterInsert(row, type, content);
            count++;
        }
        return count;
    }

    private static String objectKey(RevisionTargetType type, Long targetId, Long revisionId) {
        if (type == RevisionTargetType.ARTICLE) {
            return "versions/article/" + targetId + "/" + revisionId + ".md";
        }
        return "versions/diary/" + targetId + "/" + revisionId + ".md";
    }

    private static String md5(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            return "";
        }
    }
}
