package com.blog.content.notification;

import com.blog.content.model.vo.notification.NotificationVo;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void pushToUser(Long userId, NotificationVo vo) {
        if (userId == null || vo == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", vo);
    }
}
