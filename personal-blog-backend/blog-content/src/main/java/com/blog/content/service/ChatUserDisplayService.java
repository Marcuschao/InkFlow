package com.blog.content.service;

import com.blog.content.model.vo.chat.ChatMessageVo;
import com.blog.content.model.vo.chat.ChatUserDisplayVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ChatUserDisplayService {

    /**
     * 根据用户ID列表获取用户展示信息的映射
     *
     * @param userIds
     * @return
     */
    Map<Long, ChatUserDisplayVo> mapDisplayByUserIds(Collection<Long> userIds);

    /**
     * 根据用户ID列表获取用户展示信息的列表
     *
     * @param messages
     */
    void enrichMessages(List<ChatMessageVo> messages);

}
