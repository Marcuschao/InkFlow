package com.blog.content.model.dto.agent;

import lombok.Data;

@Data
public class EditorPolishRequest {

    private String title;
    private String content;
    private String selectedText;
    private Integer selectionStart;
    private Integer selectionEnd;
}
