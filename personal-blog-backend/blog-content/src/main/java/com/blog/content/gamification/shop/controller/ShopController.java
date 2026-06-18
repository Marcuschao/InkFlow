package com.blog.content.gamification.shop.controller;

import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.gamification.shop.model.vo.ShopItemVo;
import com.blog.content.gamification.shop.model.vo.UserItemVo;
import com.blog.content.gamification.shop.service.ShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ShopController {

    private final ShopService shopService;
    private final CurrentUserService currentUserService;

    public ShopController(ShopService shopService, CurrentUserService currentUserService) {
        this.shopService = shopService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/shop/items")
    public Result<List<ShopItemVo>> items() {
        return Result.success(shopService.listItems(currentUserService.requireUserId()));
    }

    @PostMapping("/shop/buy/{itemId}")
    public Result<UserItemVo> buy(@PathVariable Long itemId) {
        return Result.success(shopService.buy(currentUserService.requireUserId(), itemId));
    }

    @GetMapping("/user/items")
    public Result<List<UserItemVo>> userItems() {
        return Result.success(shopService.listUserItems(currentUserService.requireUserId()));
    }

    @PostMapping("/user/items/{id}/equip")
    public Result<UserItemVo> equip(@PathVariable Long id) {
        return Result.success(shopService.equip(currentUserService.requireUserId(), id));
    }

    @PostMapping("/user/items/{id}/unequip")
    public Result<UserItemVo> unequip(@PathVariable Long id) {
        return Result.success(shopService.unequip(currentUserService.requireUserId(), id));
    }
}
