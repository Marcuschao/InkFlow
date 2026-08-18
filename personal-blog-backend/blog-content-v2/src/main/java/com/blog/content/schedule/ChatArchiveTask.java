package com.blog.content.schedule;

import com.blog.content.service.ChatArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatArchiveTask {

    private final ChatArchiveService chatArchiveService;

    @Scheduled(cron = "${blog.chat.archive-cron:0 0 1 * * ?}", zone = "Asia/Shanghai")
    public void runArchive() {
        chatArchiveService.archiveExpiredMessages();
    }
}
