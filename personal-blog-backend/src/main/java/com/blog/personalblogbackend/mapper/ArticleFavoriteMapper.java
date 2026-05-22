package com.blog.personalblogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.personalblogbackend.model.entity.ArticleFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleFavoriteMapper extends BaseMapper<ArticleFavorite> {

    @Select("""
            SELECT a.* FROM article a
            INNER JOIN article_favorite f ON f.article_id = a.id
            WHERE f.user_id = #{userId} AND a.status = 1
            ORDER BY f.create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<com.blog.personalblogbackend.model.entity.Article> selectFavoriteArticles(
            @Param("userId") Long userId,
            @Param("offset") long offset,
            @Param("size") long size);

    @Select("""
            SELECT COUNT(*) FROM article_favorite f
            INNER JOIN article a ON a.id = f.article_id
            WHERE f.user_id = #{userId} AND a.status = 1
            """)
    long countFavoriteArticles(@Param("userId") Long userId);
}
