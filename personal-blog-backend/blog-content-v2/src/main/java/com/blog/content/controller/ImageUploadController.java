package com.blog.content.controller;

import com.blog.content.config.security.CurrentUserService;
import com.blog.content.service.FileStorageService;
import com.blog.content.common.support.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class ImageUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private CurrentUserService currentUserService;

    @PostMapping("/image")
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        currentUserService.requireUserId();
        String url = fileStorageService.saveDiaryImage(file);
        return Result.success(url);
    }
}
