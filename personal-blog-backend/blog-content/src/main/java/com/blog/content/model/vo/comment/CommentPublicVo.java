package com.blog.content.model.vo.comment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentPublicVo {
    private Long id;
    private Long parentId;
    private Long userId;
    private String author;
    private String nickname;
    private String avatar;
    private String content;
    private LocalDateTime createTime;
}
