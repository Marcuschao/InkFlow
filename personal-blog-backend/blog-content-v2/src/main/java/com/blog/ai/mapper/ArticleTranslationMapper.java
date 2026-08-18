package com.blog.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.ai.model.entity.ArticleTranslation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleTranslationMapper extends BaseMapper<ArticleTranslation> {
}
