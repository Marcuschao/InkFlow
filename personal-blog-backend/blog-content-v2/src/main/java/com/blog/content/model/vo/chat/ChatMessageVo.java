package com.blog.content.model.vo.chat;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import com.blog.content.gamification.shop.model.vo.EquippedItemVo;

@Data
public class ChatMessageVo {
    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private List<EquippedItemVo> equippedItems;
    private String content;
    private Boolean admin;
    private Boolean recalled;
    private String clientMsgId;
    private LocalDateTime createTime;
}
