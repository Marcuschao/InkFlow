package com.blog.personalblogbackend.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogVo {
    private Long id;
    private String username;
    private String action;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;
}
