package com.blog.content.gamification.shop.service;

import com.blog.content.gamification.shop.model.vo.EquippedItemVo;
import com.blog.content.gamification.shop.model.vo.ShopItemVo;
import com.blog.content.gamification.shop.model.vo.UserItemVo;

import java.util.List;

public interface ShopService {
    List<ShopItemVo> listItems(Long userId);

    UserItemVo buy(Long userId, Long itemId);

    List<UserItemVo> listUserItems(Long userId);

    UserItemVo equip(Long userId, Long userItemId);

    UserItemVo unequip(Long userId, Long userItemId);

    List<EquippedItemVo> listEquippedItems(Long userId);

    int markExpiredItems();
}
