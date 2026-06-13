package com.blog.content.service;

import com.blog.content.common.exception.ServiceException;
import com.blog.content.config.properties.AppProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;

    private final Path uploadRoot;
    private final ObjectProvider<MinioStorageService> minioStorageProvider;

    public FileStorageService(AppProperties appProperties, ObjectProvider<MinioStorageService> minioStorageProvider) {
        this.uploadRoot = Path.of(appProperties.getUploadDir()).toAbsolutePath().normalize();
        this.minioStorageProvider = minioStorageProvider;
    }

    public String saveDiaryImage(MultipartFile file) {
        validateImage(file);
        String ext = extensionOf(file.getOriginalFilename());
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String objectKey = "diary/" + year + "/" + month + "/" + name;

        MinioStorageService minio = minioStorageProvider.getIfAvailable();
        if (minio != null) {
            try {
                byte[] bytes = file.getBytes();
                minio.putObject(minio.bucketDiary(), objectKey, bytes, contentType(ext));
                return minio.buildPublicUrl(minio.bucketDiary(), objectKey);
            } catch (IOException e) {
                throw new ServiceException(500, "图片读取失败");
            }
        }
        return saveDiaryImageLocal(file, year, month, name);
    }

    public String saveAvatar(Long userId, MultipartFile file) {
        validateImage(file);
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ServiceException(400, "头像不能超过 5MB");
        }
        MinioStorageService minio = minioStorageProvider.getIfAvailable();
        if (minio == null) {
            throw new ServiceException(503, "对象存储未启用");
        }
        String ext = extensionOf(file.getOriginalFilename());
        String objectKey = "avatars/" + userId + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            byte[] bytes = file.getBytes();
            minio.putObject(minio.bucketAvatars(), objectKey, bytes, contentType(ext));
            return minio.buildPublicUrl(minio.bucketAvatars(), objectKey);
        } catch (IOException e) {
            throw new ServiceException(500, "头像读取失败");
        }
    }

    private String saveDiaryImageLocal(MultipartFile file, String year, String month, String name) {
        Path dir = uploadRoot.resolve("diary").resolve(year).resolve(month);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            file.transferTo(target);
        } catch (IOException e) {
            throw new ServiceException(500, "图片保存失败");
        }
        return "/upload/diary/" + year + "/" + month + "/" + name;
    }

    private static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(400, "请选择图片文件");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ServiceException(400, "仅支持 jpg、jpeg、png、gif、webp");
        }
    }

    private static String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).trim().toLowerCase(Locale.ROOT);
    }

    private static String contentType(String ext) {
        return switch (ext) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}
