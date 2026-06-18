package com.blog.content.config.storage;

import com.blog.content.config.properties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class MinioBucketSupport {

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final Set<String> ready = ConcurrentHashMap.newKeySet();

    public void ensureConfiguredBuckets() {
        MinioProperties.Buckets buckets = properties.getBuckets();
        ensureBucket(buckets.getChat(), false);
        ensureBucket(buckets.getAvatars(), true);
        ensureBucket(buckets.getDiary(), true);
        ensureBucket(buckets.getVersions(), false);
        ensureBucket(buckets.getLogs(), false);
        ensureBucket(buckets.getReports(), false);
        ensureBucket(buckets.getBackups(), false);
        ensureBucket(buckets.getAssets(), true);
    }

    public void ensureBucket(String bucket) {
        ensureBucket(bucket, isPublicReadBucket(bucket));
    }

    public void ensureBucket(String bucket, boolean publicRead) {
        if (!StringUtils.hasText(bucket)) {
            return;
        }
        String cacheKey = bucket + "|" + publicRead;
        if (ready.contains(cacheKey)) {
            return;
        }
        synchronized (("minio-bucket-" + bucket).intern()) {
            if (ready.contains(cacheKey)) {
                return;
            }
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Created MinIO bucket: {}", bucket);
                }
                if (publicRead) {
                    applyPublicReadPolicy(bucket);
                }
                ready.add(cacheKey);
            } catch (Exception e) {
                throw new IllegalStateException("MinIO bucket 初始化失败: " + bucket + " — " + rootMessage(e), e);
            }
        }
    }

    private boolean isPublicReadBucket(String bucket) {
        MinioProperties.Buckets buckets = properties.getBuckets();
        return bucket.equals(buckets.getAvatars())
                || bucket.equals(buckets.getDiary())
                || bucket.equals(buckets.getAssets());
    }

    private void applyPublicReadPolicy(String bucket) throws Exception {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::%s/*"]
                  }]
                }
                """.formatted(bucket);
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(bucket)
                .config(policy)
                .build());
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}
