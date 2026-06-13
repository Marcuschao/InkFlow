package com.blog.content.revision.handler;

import com.blog.content.common.exception.ServiceException;
import com.blog.content.common.revision.RevisionTargetType;
import com.blog.content.event.ArticleLifecycleEventPublisher;
import com.blog.content.mapper.ArticleMapper;
import com.blog.content.mapper.ContentRevisionMapper;
import com.blog.content.mapper.TagMapper;
import com.blog.content.model.entity.Article;
import com.blog.content.model.entity.ContentRevision;
import com.blog.content.model.entity.Tag;
import com.blog.content.model.vo.revision.ContentRevisionDetailVo;
import com.blog.content.model.vo.revision.ContentRevisionListItemVo;
import com.blog.content.model.vo.revision.RevisionDiffResponseVo;
import com.blog.content.service.RevisionContentStorage;
import com.blog.content.service.RevisionDiffService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ArticleRevisionHandler extends AbstractRevisionTargetHandler {

    private final ArticleMapper articleMapper;
    private final TagMapper tagMapper;
    private final ArticleLifecycleEventPublisher articleLifecycleEventPublisher;

    public ArticleRevisionHandler(ContentRevisionMapper contentRevisionMapper,
                                  RevisionContentStorage revisionContentStorage,
                                  RevisionDiffService revisionDiffService,
                                  ArticleMapper articleMapper,
                                  TagMapper tagMapper,
                                  ArticleLifecycleEventPublisher articleLifecycleEventPublisher) {
        super(contentRevisionMapper, revisionContentStorage, revisionDiffService);
        this.articleMapper = articleMapper;
        this.tagMapper = tagMapper;
        this.articleLifecycleEventPublisher = articleLifecycleEventPublisher;
    }

    @Override
    public RevisionTargetType type() {
        return RevisionTargetType.ARTICLE;
    }

    public void snapshot(Article article, String articleTagsCsv, String remark) {
        if (article == null || article.getId() == null) {
            return;
        }
        ContentRevision rev = newRevision(article.getId());
        rev.setTitle(article.getTitle());
        rev.setSummary(article.getSummary());
        rev.setSeoTitle(article.getSeoTitle());
        rev.setSeoDescription(article.getSeoDescription());
        String content = nullToEmpty(article.getContent());
        rev.setContent(content);
        rev.setArticleTags(StringUtils.hasText(articleTagsCsv) ? articleTagsCsv.trim() : "");
        rev.setArticleCategoryId(article.getCategoryId());
        rev.setArticleStatus(article.getStatus());
        rev.setArticleCover(article.getCover());
        rev.setRemark(remark);
        persistSnapshot(rev, content);
    }

    @Override
    public List<ContentRevisionListItemVo> listRevisions(Long targetId, Long userId) {
        return listRevisionsInternal(targetId);
    }

    @Override
    public ContentRevisionDetailVo getRevision(Long targetId, Long revisionId, Long userId) {
        return toDetailVo(requireRevision(revisionId, targetId));
    }

    @Override
    @Transactional
    public void restore(Long articleId, Long revisionId, Long userId) {
        Article cur = articleMapper.selectById(articleId);
        if (cur == null) {
            throw new ServiceException(404, "文章不存在");
        }
        ContentRevision target = requireRevision(revisionId, articleId);
        Article previous = new Article();
        BeanUtils.copyProperties(cur, previous);
        List<String> curTags = articleMapper.selectTagNamesByArticleId(articleId);
        snapshot(cur, String.join(",", curTags), "回退前自动存档");
        applyRevisionToArticle(cur, target);
        articleMapper.updateById(cur);
        syncArticleTagsFromCsv(articleId, target.getArticleTags());
        Article fresh = articleMapper.selectById(articleId);
        List<String> freshTags = articleMapper.selectTagNamesByArticleId(articleId);
        snapshot(fresh, String.join(",", freshTags), "从修订 #" + target.getRevisionNo() + " 恢复");
        articleLifecycleEventPublisher.publish(previous, fresh);
    }

    @Override
    public RevisionDiffResponseVo diff(Long targetId, Long userId, Long leftRevisionId, Long rightRevisionId) {
        return diffInternal(leftRevisionId, rightRevisionId, targetId);
    }

    private void applyRevisionToArticle(Article cur, ContentRevision target) {
        cur.setTitle(target.getTitle());
        cur.setSummary(target.getSummary());
        cur.setSeoTitle(target.getSeoTitle());
        cur.setSeoDescription(target.getSeoDescription());
        cur.setContent(revisionContentStorage.loadContent(target));
        cur.setCategoryId(target.getArticleCategoryId());
        cur.setStatus(target.getArticleStatus());
        cur.setCover(target.getArticleCover());
    }

    private void syncArticleTagsFromCsv(Long articleId, String csv) {
        articleMapper.deleteArticleTagsByArticleId(articleId);
        if (!StringUtils.hasText(csv)) {
            return;
        }
        List<String> tagNames = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tagNames)) {
            return;
        }
        List<Long> tagIdsToInsert = new ArrayList<>();
        for (String tagName : tagNames) {
            Tag tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("name", tagName));
            if (tag == null) {
                tag = new Tag();
                tag.setName(tagName);
                tagMapper.insert(tag);
            }
            tagIdsToInsert.add(tag.getId());
        }
        articleMapper.insertArticleTags(articleId, tagIdsToInsert);
    }
}
