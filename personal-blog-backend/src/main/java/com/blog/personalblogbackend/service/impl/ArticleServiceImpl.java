package com.blog.personalblogbackend.service.impl;

import com.blog.personalblogbackend.model.dto.ArticlePageQuery;
import com.blog.personalblogbackend.model.vo.ArticleVO;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.ArticleTranslation;
import com.blog.personalblogbackend.model.entity.Tag;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.llm.AiService;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.mapper.ArticleTranslationMapper;
import com.blog.personalblogbackend.mapper.TagMapper;
import com.blog.personalblogbackend.common.revision.RevisionTargetType;
import com.blog.personalblogbackend.service.ArticleService;
import com.blog.personalblogbackend.service.ContentRevisionService;
import com.blog.personalblogbackend.stream.DomainEvent;
import com.blog.personalblogbackend.stream.EventPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private ArticleTranslationMapper articleTranslationMapper;
    @Autowired
    private AiService aiService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private ContentRevisionService contentRevisionService;

    private static boolean isPublished(Integer status) {
        return status != null && status == 1;
    }

    private void publishArticleEvents(Article previous, Article fresh) {
        if (fresh == null || !isPublished(fresh.getStatus())) {
            return;
        }
        if (previous == null || !isPublished(previous.getStatus())) {
            eventPublisher.publishAfterCommit(DomainEvent.articlePublished(fresh));
        } else {
            eventPublisher.publishAfterCommit(DomainEvent.articleUpdated(fresh));
        }
    }

    @Override
    public IPage<Article> getArticlePage(ArticlePageQuery query) {
        Page<Article> page = new Page<>(query.getPage(), query.getSize());
        return articleMapper.selectArticleVOPage(page, query.getCategoryId(), query.getTagId(), query.getKeyword());
    }

    @Override
    public Article getArticleDetail(Long id) {
        return articleMapper.selectArticleVOById(id);
    }

    @Override
    public ArticleVO getArticleVo(Long id, String lang) {
        Article article = articleMapper.selectArticleVOById(id);
        if (article == null) {
            return null;
        }
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(article, vo);
        String loc = normalizeLocale(lang);
        vo.setViewingLocale(loc);
        vo.setTranslationActive(Boolean.FALSE);
        if (loc != null && !"zh".equals(loc)) {
            ArticleTranslation tr = articleTranslationMapper.selectOne(new LambdaQueryWrapper<ArticleTranslation>()
                    .eq(ArticleTranslation::getArticleId, id)
                    .eq(ArticleTranslation::getLocale, loc));
            if (tr != null) {
                vo.setTitle(tr.getTitle());
                vo.setSummary(tr.getSummary());
                vo.setContent(tr.getContent());
                if (StringUtils.hasText(tr.getSeoTitle())) {
                    vo.setSeoTitle(tr.getSeoTitle());
                }
                if (StringUtils.hasText(tr.getSeoDescription())) {
                    vo.setSeoDescription(tr.getSeoDescription());
                }
                vo.setTranslationActive(Boolean.TRUE);
            }
        }
        return vo;
    }

    private static String normalizeLocale(String lang) {
        if (!StringUtils.hasText(lang)) {
            return "zh";
        }
        return lang.trim().toLowerCase();
    }

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
        createArticle(dup, tags);
        return dup.getId();
    }

    @Override
    @Transactional
    public void generateSeoByAi(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException(404, "文章不存在");
        }
        String sys = "你是 SEO 编辑。根据下列中文博客标题与正文节选，输出严格一行 JSON，键为 seoTitle、seoDescription。"
                + "seoTitle 简洁不超过30字符；seoDescription 不超过120字符；不要 markdown，不要其它文字。";
        String user = "标题：" + nullToEmpty(article.getTitle())
                + "\n摘要：" + nullToEmpty(article.getSummary())
                + "\n正文节选：\n" + truncate(nullToEmpty(article.getContent()), 6000);
        String raw = aiService.chat(sys, user);
        applySeoJsonToArticle(articleId, raw);
    }

    private void applySeoJsonToArticle(Long articleId, String raw) {
        try {
            String s = raw.trim();
            int l = s.indexOf('{');
            int r = s.lastIndexOf('}');
            if (l >= 0 && r > l) {
                s = s.substring(l, r + 1);
            }
            JsonNode node = objectMapper.readTree(s);
            Article patch = new Article();
            patch.setId(articleId);
            patch.setSeoTitle(textOrNull(node.get("seoTitle")));
            patch.setSeoDescription(textOrNull(node.get("seoDescription")));
            articleMapper.updateById(patch);
        } catch (Exception e) {
            throw new ServiceException(502, "SEO JSON 解析失败");
        }
    }

    private static String textOrNull(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        String t = n.asText().trim();
        return StringUtils.hasText(t) ? t : null;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s == null ? "" : s;
        }
        return s.substring(0, max);
    }

    @Override
    @Transactional
    public boolean createArticle(Article article, List<String> tagNames) {
        // 1. 插入文章
        articleMapper.insert(article);

        // 2. 处理标签
        handleArticleTags(article.getId(), tagNames);
        Article fresh = articleMapper.selectById(article.getId());
        publishArticleEvents(null, fresh);
        List<String> tagNamesAfter = articleMapper.selectTagNamesByArticleId(fresh.getId());
        contentRevisionService.snapshotArticle(fresh, String.join(",", tagNamesAfter), "创建");
        return true;
    }

    @Override
    @Transactional
    public boolean updateArticle(Article article, List<String> tagNames) {
        Article previous = articleMapper.selectById(article.getId());
        if (previous == null) {
            throw new ServiceException(404, "文章不存在");
        }
        List<String> prevTagNames = articleMapper.selectTagNamesByArticleId(previous.getId());
        contentRevisionService.snapshotArticle(previous, String.join(",", prevTagNames), "保存");
        // 1. 更新文章
        articleMapper.updateById(article);

        // 2. 处理标签 (先删除旧的，再插入新的)
        articleMapper.deleteArticleTagsByArticleId(article.getId());
        handleArticleTags(article.getId(), tagNames);
        Article fresh = articleMapper.selectById(article.getId());
        publishArticleEvents(previous, fresh);
        return true;
    }

    @Override
    @Transactional
    public boolean deleteArticle(Long id) {
        contentRevisionService.deleteByTarget(RevisionTargetType.ARTICLE, id);
        // 1. 删除文章-标签关联
        articleMapper.deleteArticleTagsByArticleId(id);
        // 2. 删除文章
        articleMapper.deleteById(id);
        return true;
    }

    private void handleArticleTags(Long articleId, List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return;
        }

        List<Long> tagIdsToInsert = new ArrayList<>();
        for (String tagName : tagNames) {
            if (!StringUtils.hasText(tagName)) continue;

            Tag tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("name", tagName));
            if (tag == null) {
                // 标签不存在，则创建新标签
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
