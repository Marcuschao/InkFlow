package com.blog.ai.service;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

public interface MinioStorageService {

    String putObject(String bucket, String objectKey, byte[] data, String contentType);

    String putObject(String bucket, String objectKey, InputStream stream, long size, String contentType);

    InputStream getObject(String bucket, String objectKey);

    void deleteObject(String bucket, String objectKey);

    List<String> listObjectKeys(String bucket, String prefix, int maxKeys);

    String buildPublicUrl(String bucket, String objectKey);

    String presignedGetUrl(String bucket, String objectKey, Duration expiry);

    String bucketAvatars();

    String bucketDiary();

    String bucketVersions();

    String bucketLogs();

    String bucketReports();

    String bucketBackups();
}
