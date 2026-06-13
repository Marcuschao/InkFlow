package com.blog.content.model.dto.agent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatResponse {
    private String answer;
    private List<ChatSourceDto> sources = new ArrayList<>();
}
