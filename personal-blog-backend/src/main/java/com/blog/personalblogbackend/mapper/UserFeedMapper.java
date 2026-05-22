package com.blog.personalblogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.personalblogbackend.model.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserFeedMapper {

    @Select("""
            SELECT a.* FROM article a
            INNER JOIN user_follow uf ON uf.followee_id = a.author_id
            WHERE uf.follower_id = #{userId}
              AND a.status = 1
              AND a.create_time >= #{since}
            ORDER BY a.create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<Article> selectFeedArticles(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            @Param("offset") long offset,
            @Param("size") long size);

    @Select("""
            SELECT COUNT(*) FROM article a
            INNER JOIN user_follow uf ON uf.followee_id = a.author_id
            WHERE uf.follower_id = #{userId}
              AND a.status = 1
              AND a.create_time >= #{since}
            """)
    long countFeedArticles(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
