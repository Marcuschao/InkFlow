package com.blog.content.model.vo.comment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import com.blog.content.gamification.shop.model.vo.EquippedItemVo;

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
    private List<EquippedItemVo> equippedItems;
    private String content;
    private LocalDateTime createTime;
}
