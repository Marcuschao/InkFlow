package com.blog.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.ai.model.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * Security invariant: ownership transfer is a single database compare-and-set operation.
     * Never replace this with select-then-unconditional-update, and never rely on a Redis lock
     * as the consistency boundary. A zero row count means the token did not own the session or
     * another principal already claimed it; callers must deny access.
     */
    @Update("UPDATE ai_chat_session SET user_id=#{userId}, guest_token_hash=NULL, update_time=NOW() " +
            "WHERE id=#{sessionId} AND guest_token_hash=#{guestTokenHash} AND user_id IS NULL")
    int claimOne(@Param("sessionId") Long sessionId,
                 @Param("userId") Long userId,
                 @Param("guestTokenHash") String guestTokenHash);

    /** Same atomic ownership rule as {@link #claimOne(Long, Long, String)}. */
    @Update("UPDATE ai_chat_session SET user_id=#{userId}, guest_token_hash=NULL, update_time=NOW() " +
            "WHERE guest_token_hash=#{guestTokenHash} AND user_id IS NULL")
    int claimAll(@Param("userId") Long userId,
                 @Param("guestTokenHash") String guestTokenHash);
}
