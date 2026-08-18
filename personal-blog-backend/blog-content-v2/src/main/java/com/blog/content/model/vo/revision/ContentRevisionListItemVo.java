package com.blog.content.model.vo.revision;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentRevisionListItemVo {

    private Long id;
    private Integer revisionNo;
    private String title;
    private String remark;
    private LocalDateTime createdAt;
}
