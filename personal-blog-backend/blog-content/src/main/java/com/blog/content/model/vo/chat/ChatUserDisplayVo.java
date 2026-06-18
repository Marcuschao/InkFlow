package com.blog.content.model.vo.chat;

import lombok.Data;

import java.util.List;
import com.blog.content.gamification.shop.model.vo.EquippedItemVo;

@Data
public class ChatUserDisplayVo {
    private Long userId;
    private String username;
    private String avatar;
    private List<EquippedItemVo> equippedItems;
}
