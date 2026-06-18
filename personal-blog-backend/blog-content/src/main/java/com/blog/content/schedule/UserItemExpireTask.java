package com.blog.content.schedule;

import com.blog.content.gamification.shop.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserItemExpireTask {

    private final ShopService shopService;

    public UserItemExpireTask(ShopService shopService) {
        this.shopService = shopService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void markExpiredItems() {
        int count = shopService.markExpiredItems();
        if (count > 0) {
            log.info("expired user items count={}", count);
        }
    }
}
