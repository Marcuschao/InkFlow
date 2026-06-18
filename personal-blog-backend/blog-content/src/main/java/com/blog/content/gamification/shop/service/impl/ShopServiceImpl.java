package com.blog.content.gamification.shop.service.impl;

import com.blog.content.common.exception.ServiceException;
import com.blog.content.gamification.points.service.PointsService;
import com.blog.content.gamification.shop.mapper.ItemMapper;
import com.blog.content.gamification.shop.mapper.UserItemMapper;
import com.blog.content.gamification.shop.model.entity.Item;
import com.blog.content.gamification.shop.model.entity.UserItem;
import com.blog.content.gamification.shop.model.vo.EquippedItemVo;
import com.blog.content.gamification.shop.model.vo.ShopItemVo;
import com.blog.content.gamification.shop.model.vo.UserItemVo;
import com.blog.content.gamification.shop.service.ShopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShopServiceImpl implements ShopService {

    private static final String ITEM_ON_SALE = "ON_SALE";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_UNUSED = "UNUSED";
    private static final String STATUS_EXPIRED = "EXPIRED";

    private final ItemMapper itemMapper;
    private final UserItemMapper userItemMapper;
    private final PointsService pointsService;

    public ShopServiceImpl(ItemMapper itemMapper,
                           UserItemMapper userItemMapper,
                           PointsService pointsService) {
        this.itemMapper = itemMapper;
        this.userItemMapper = userItemMapper;
        this.pointsService = pointsService;
    }

    @Override
    public List<ShopItemVo> listItems(Long userId) {
        int points = pointsService.getTotalPoints(userId);
        List<ShopItemVo> items = itemMapper.selectShopItems(userId);
        items.forEach(item -> item.setAffordable(points >= safePrice(item.getPrice())));
        return items;
    }

    @Override
    @Transactional
    public UserItemVo buy(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || !ITEM_ON_SALE.equals(item.getStatus())) {
            throw new ServiceException(404, "道具不存在");
        }
        int price = safePrice(item.getPrice());
        if (!pointsService.deductPoints(userId, price, "兑换道具：" + item.getName())) {
            throw new ServiceException(400, "积分不足");
        }
        LocalDateTime now = LocalDateTime.now();
        UserItem userItem = new UserItem();
        userItem.setUserId(userId);
        userItem.setItemId(itemId);
        userItem.setStatus(STATUS_UNUSED);
        userItem.setObtainTime(now);
        if (item.getDurationDays() != null && item.getDurationDays() > 0) {
            userItem.setExpireTime(now.plusDays(item.getDurationDays()));
        }
        userItemMapper.insert(userItem);
        return findOwned(userId, userItem.getId());
    }

    @Override
    public List<UserItemVo> listUserItems(Long userId) {
        return userItemMapper.selectUserItems(userId);
    }

    @Override
    @Transactional
    public UserItemVo equip(Long userId, Long userItemId) {
        UserItemVo current = findOwned(userId, userItemId);
        if (STATUS_EXPIRED.equals(current.getStatus()) || isExpired(current.getExpireTime())) {
            throw new ServiceException(400, "道具已过期");
        }
        if (STATUS_ACTIVE.equals(current.getStatus())) {
            return current;
        }
        userItemMapper.unequipActiveByType(userId, current.getType());
        int updated = userItemMapper.equipById(userItemId, userId);
        if (updated <= 0) {
            throw new ServiceException(400, "道具无法装备");
        }
        return findOwned(userId, userItemId);
    }

    @Override
    @Transactional
    public UserItemVo unequip(Long userId, Long userItemId) {
        findOwned(userId, userItemId);
        userItemMapper.unequipById(userItemId, userId);
        return findOwned(userId, userItemId);
    }

    @Override
    public List<EquippedItemVo> listEquippedItems(Long userId) {
        return userItemMapper.selectEquippedItems(userId);
    }

    @Override
    public int markExpiredItems() {
        return userItemMapper.markExpired();
    }

    private UserItemVo findOwned(Long userId, Long userItemId) {
        return userItemMapper.selectUserItems(userId).stream()
                .filter(item -> userItemId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(404, "道具不存在"));
    }

    private boolean isExpired(LocalDateTime expireTime) {
        return expireTime != null && !expireTime.isAfter(LocalDateTime.now());
    }

    private int safePrice(Integer price) {
        return price != null ? price : 0;
    }
}
