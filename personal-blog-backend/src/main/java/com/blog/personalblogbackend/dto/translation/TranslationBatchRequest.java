package com.blog.personalblogbackend.dto.translation;

import lombok.Data;

import java.util.List;

@Data
public class TranslationBatchRequest {
    private List<Long> articleIds;
    private List<String> locales;
}
