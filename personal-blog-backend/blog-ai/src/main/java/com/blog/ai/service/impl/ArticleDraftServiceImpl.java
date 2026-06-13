package com.blog.ai.service.impl;

import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.mapper.ArticleMapper;
import com.blog.ai.mapper.TagMapper;
import com.blog.ai.model.entity.Article;
import com.blog.ai.model.entity.Tag;
import com.blog.ai.service.ArticleDraftService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArticleDraftServiceImpl implements ArticleDraftService {

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private TagMapper tagMapper;

    @Override
    @Transactional
    public Long duplicateArticleAsDraft(Long sourceArticleId) {
        Article src = articleMapper.selectById(sourceArticleId);
        if (src == null) {
            throw new ServiceException(404, "文章不存在");
        }
        List<String> tags = articleMapper.selectTagNamesByArticleId(sourceArticleId);
        Article dup = new Article();
        dup.setTitle(src.getTitle() + " (副本)");
        dup.setSummary(src.getSummary());
        dup.setContent(src.getContent());
        dup.setCover(src.getCover());
        dup.setCategoryId(src.getCategoryId());
        dup.setAuthorId(src.getAuthorId());
        dup.setStatus(0);
        dup.setViewCount(0);
        dup.setFreshnessStatus(0);
        dup.setFreshnessCheckedAt(null);
        dup.setSeoTitle(src.getSeoTitle());
        dup.setSeoDescription(src.getSeoDescription());
        articleMapper.insert(dup);
        handleArticleTags(dup.getId(), tags);
        return dup.getId();
    }

    private void handleArticleTags(Long articleId, List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return;
        }
        List<Long> tagIdsToInsert = new ArrayList<>();
        for (String tagName : tagNames) {
            if (!StringUtils.hasText(tagName)) {
                continue;
            }
            Tag tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("name", tagName));
            if (tag == null) {
                tag = new Tag();
                tag.setName(tagName);
                tagMapper.insert(tag);
            }
            tagIdsToInsert.add(tag.getId());
        }
        if (!CollectionUtils.isEmpty(tagIdsToInsert)) {
            articleMapper.insertArticleTags(articleId, tagIdsToInsert);
        }
    }
}
