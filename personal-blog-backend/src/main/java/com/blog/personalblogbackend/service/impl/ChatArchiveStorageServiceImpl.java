package com.blog.personalblogbackend.service.impl;

import com.blog.personalblogbackend.config.properties.MinioProperties;
import com.blog.personalblogbackend.config.storage.MinioBucketSupport;
import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;
import com.blog.personalblogbackend.service.ChatArchiveStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
@ConditionalOnBean(MinioClient.class)
@RequiredArgsConstructor
public class ChatArchiveStorageServiceImpl implements ChatArchiveStorageService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final MinioBucketSupport minioBucketSupport;
    private final ObjectMapper objectMapper;

    @Override
    public void uploadDayShard(LocalDate day, int shardIndex, List<ChatMessageVo> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        minioBucketSupport.ensureBucket(minioProperties.getBucket());
        String objectName = objectName(day, shardIndex);
        try {
            byte[] payload = gzipJson(messages);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectName)
                    .stream(new ByteArrayInputStream(payload), payload.length, -1)
                    .contentType("application/gzip")
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("上传聊天归档失败: " + objectName, e);
        }
    }

    @Override
    public List<ChatMessageVo> readMessagesBefore(Long cursorId, LocalDateTime anchorTime, int limit) {
        if (cursorId == null || limit <= 0) {
            return Collections.emptyList();
        }
        LocalDate startDay = anchorTime != null
                ? anchorTime.toLocalDate()
                : LocalDate.now(ZONE).minusDays(7);
        List<ChatMessageVo> collected = new ArrayList<>();
        LocalDate day = startDay;
        LocalDate minDay = startDay.minusYears(2);
        while (collected.size() < limit && !day.isBefore(minDay)) {
            collected.addAll(readDayBefore(day, cursorId, limit - collected.size()));
            day = day.minusDays(1);
        }
        collected.sort(Comparator.comparing(ChatMessageVo::getId).reversed());
        if (collected.size() > limit) {
            return new ArrayList<>(collected.subList(0, limit));
        }
        return collected;
    }

    public int nextShardIndex(LocalDate day) {
        String prefix = "chat/" + day.format(DAY_FMT) + "/messages_";
        int max = -1;
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .prefix(prefix)
                    .recursive(true)
                    .build());
            for (Result<Item> result : results) {
                Item item = result.get();
                String name = item.objectName();
                if (!name.endsWith(".json.gz")) {
                    continue;
                }
                String file = name.substring(name.lastIndexOf('/') + 1);
                String idxPart = file.substring("messages_".length(), file.length() - ".json.gz".length());
                max = Math.max(max, Integer.parseInt(idxPart));
            }
        } catch (Exception e) {
            log.warn("读取 MinIO 分片索引失败 day={}", day, e);
        }
        return max + 1;
    }

    private List<ChatMessageVo> readDayBefore(LocalDate day, Long cursorId, int limit) {
        String prefix = "chat/" + day.format(DAY_FMT) + "/messages_";
        List<ChatMessageVo> rows = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .prefix(prefix)
                    .recursive(true)
                    .build());
            List<String> objectNames = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.objectName().endsWith(".json.gz")) {
                    objectNames.add(item.objectName());
                }
            }
            objectNames.sort(Comparator.reverseOrder());
            for (String objectName : objectNames) {
                if (rows.size() >= limit) {
                    break;
                }
                rows.addAll(readObjectBefore(objectName, cursorId, limit - rows.size()));
            }
        } catch (Exception e) {
            log.warn("读取 MinIO 归档失败 day={}", day, e);
        }
        return rows;
    }

    private List<ChatMessageVo> readObjectBefore(String objectName, Long cursorId, int limit) {
        try (InputStream raw = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(objectName)
                .build());
             GZIPInputStream gzip = new GZIPInputStream(raw)) {
            List<ChatMessageVo> all = objectMapper.readValue(gzip, new TypeReference<List<ChatMessageVo>>() {});
            List<ChatMessageVo> filtered = new ArrayList<>();
            for (ChatMessageVo vo : all) {
                if (vo.getId() != null && vo.getId() < cursorId) {
                    filtered.add(vo);
                }
            }
            filtered.sort(Comparator.comparing(ChatMessageVo::getId).reversed());
            if (filtered.size() > limit) {
                return new ArrayList<>(filtered.subList(0, limit));
            }
            return filtered;
        } catch (Exception e) {
            log.warn("解析 MinIO 对象失败 object={}", objectName, e);
            return Collections.emptyList();
        }
    }

    private byte[] gzipJson(List<ChatMessageVo> messages) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(objectMapper.writeValueAsString(messages).getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    private static String objectName(LocalDate day, int shardIndex) {
        return "chat/" + day.format(DAY_FMT) + "/messages_" + shardIndex + ".json.gz";
    }
}
