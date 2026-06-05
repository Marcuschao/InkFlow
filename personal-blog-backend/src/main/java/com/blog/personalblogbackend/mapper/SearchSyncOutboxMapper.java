package com.blog.personalblogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.personalblogbackend.model.entity.SearchSyncOutbox;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SearchSyncOutboxMapper extends BaseMapper<SearchSyncOutbox> {

    @Insert("""
            INSERT INTO search_sync_outbox (article_id, event_type, article_updated_at, status, retry_count)
            VALUES (#{articleId}, #{eventType}, #{articleUpdatedAt}, 'PENDING', 0)
            ON DUPLICATE KEY UPDATE
              event_type = VALUES(event_type),
              article_updated_at = VALUES(article_updated_at),
              status = 'PENDING',
              retry_count = 0,
              last_error = NULL,
              updated_at = CURRENT_TIMESTAMP
            """)
    int upsertPending(@Param("articleId") Long articleId,
                      @Param("eventType") String eventType,
                      @Param("articleUpdatedAt") LocalDateTime articleUpdatedAt);
}
