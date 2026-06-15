package com.blog.content.mapper;

import com.blog.content.model.entity.ArticleTagScore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleTagScoreMapper extends BaseMapper<ArticleTagScore> {

    @Delete("DELETE FROM article_tag_score WHERE article_id = #{articleId}")
    void deleteByArticleId(@Param("articleId") Long articleId);
}
