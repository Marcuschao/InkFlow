package com.blog.content.controller;

import com.blog.content.common.exception.ServiceException;
import com.blog.content.common.support.Result;
import com.blog.content.gamification.badge.mapper.BadgeMapper;
import com.blog.content.gamification.badge.model.entity.Badge;
import com.blog.content.gamification.shop.mapper.ItemMapper;
import com.blog.content.gamification.shop.model.entity.Item;
import com.blog.content.service.FileStorageService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class AdminAssetIconController {

    private final FileStorageService fileStorageService;
    private final BadgeMapper badgeMapper;
    private final ItemMapper itemMapper;

    public AdminAssetIconController(FileStorageService fileStorageService,
                                    BadgeMapper badgeMapper,
                                    ItemMapper itemMapper) {
        this.fileStorageService = fileStorageService;
        this.badgeMapper = badgeMapper;
        this.itemMapper = itemMapper;
    }

    @PostMapping("/badges/{id}/icon")
    public Result<String> uploadBadgeIcon(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        Badge badge = badgeMapper.selectById(id);
        if (badge == null) {
            throw new ServiceException(404, "徽章不存在");
        }
        String url = fileStorageService.saveAssetImage("badges", file);
        badge.setIconUrl(url);
        badgeMapper.updateById(badge);
        return Result.success(url);
    }

    @PostMapping("/items/{id}/icon")
    public Result<String> uploadItemIcon(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new ServiceException(404, "道具不存在");
        }
        String url = fileStorageService.saveAssetImage("items", file);
        item.setIconUrl(url);
        itemMapper.updateById(item);
        return Result.success(url);
    }
}
