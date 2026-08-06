package com.blog.ai.model.dto.agent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatResponse {
    private String answer;
    private List<ChatSourceDto> sources = new ArrayList<>();
    private Long sessionId;
    private Long messageId;
    private boolean grounded;
    private double confidence;
    private String refusalReason;
    private boolean degraded;
}
