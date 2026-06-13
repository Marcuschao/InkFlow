package com.blog.content.model.dto.site;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicSiteConfigDto {
    private String chatbotVisibility;
    private String siteTitle;
    private String siteDescription;
    private String siteUrl;
    private LocalDateTime launchTime;
}
