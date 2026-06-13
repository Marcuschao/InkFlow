package com.blog.content.service;

import java.time.LocalDateTime;

public interface UserMuteService {
    boolean isMuted(Long userId);

    LocalDateTime getMuteUntil(Long userId);

    void mute(Long userId, int minutes);
}
