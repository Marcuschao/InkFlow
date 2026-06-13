package com.blog.auth.service.impl;

import com.blog.auth.common.exception.ServiceException;
import com.blog.auth.config.properties.MinioProperties;
import com.blog.auth.config.storage.MinioBucketSupport;
import com.blog.auth.service.MinioStorageService;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnBean(MinioClient.class)
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final MinioBucketSupport minioBucketSupport;

    @Override
    public String putObject(String bucket, String objectKey, byte[] data, String contentType) {
        if (data == null) {
            throw new ServiceException(400, "上传内容为空");
        }
        return putObject(bucket, objectKey, new ByteArrayInputStream(data), data.length, contentType);
    }

    @Override
    public String putObject(String bucket, String objectKey, InputStream stream, long size, String contentType) {
        requireKey(objectKey);
        try {
            minioBucketSupport.ensureBucket(bucket);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(stream, size, -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build());
            return objectKey;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("API port")) {
                throw new ServiceException(500, "MinIO endpoint 指向了 Console 端口，请改为 S3 API 端口");
            }
            throw new ServiceException(500, "MinIO 上传失败: " + msg);
        }
    }

    @Override
    public InputStream getObject(String bucket, String objectKey) {
        requireKey(objectKey);
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new ServiceException(500, "MinIO 读取失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteObject(String bucket, String objectKey) {
        requireKey(objectKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new ServiceException(500, "MinIO 删除失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> listObjectKeys(String bucket, String prefix, int maxKeys) {
        List<String> keys = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefix != null ? prefix : "")
                    .recursive(true)
                    .build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir()) {
                    continue;
                }
                keys.add(item.objectName());
                if (keys.size() >= maxKeys) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new ServiceException(500, "MinIO 列表失败: " + e.getMessage());
        }
        return keys;
    }

    @Override
    public String buildPublicUrl(String bucket, String objectKey) {
        String base = properties.getPublicBaseUrl().replaceAll("/$", "");
        String key = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        return base + "/" + bucket + "/" + key;
    }

    @Override
    public String presignedGetUrl(String bucket, String objectKey, Duration expiry) {
        requireKey(objectKey);
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry((int) expiry.getSeconds())
                    .build());
        } catch (Exception e) {
            throw new ServiceException(500, "MinIO 签名 URL 失败: " + e.getMessage());
        }
    }

    @Override
    public String bucketAvatars() {
        return properties.getBuckets().getAvatars();
    }

    @Override
    public String bucketDiary() {
        return properties.getBuckets().getDiary();
    }

    @Override
    public String bucketVersions() {
        return properties.getBuckets().getVersions();
    }

    @Override
    public String bucketLogs() {
        return properties.getBuckets().getLogs();
    }

    @Override
    public String bucketReports() {
        return properties.getBuckets().getReports();
    }

    @Override
    public String bucketBackups() {
        return properties.getBuckets().getBackups();
    }

    private static void requireKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new ServiceException(400, "objectKey 不能为空");
        }
    }
}
