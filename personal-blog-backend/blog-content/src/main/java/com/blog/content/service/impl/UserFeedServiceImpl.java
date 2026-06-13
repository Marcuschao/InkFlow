package com.blog.content.service.impl;

import com.blog.content.common.support.PageResult;
import com.blog.content.mapper.UserFeedMapper;
import com.blog.content.model.entity.Article;
import com.blog.content.model.vo.ArticleVO;
import com.blog.content.service.ArticleInteractionEnricher;
import com.blog.content.service.UserFeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserFeedServiceImpl implements UserFeedService {

    private static final int FEED_DAYS = 7;

    @Autowired
    private UserFeedMapper userFeedMapper;
    @Autowired
    private ArticleInteractionEnricher articleInteractionEnricher;

    @Override
    public PageResult<ArticleVO> feed(Long userId, int page, int size) {
        LocalDateTime since = LocalDateTime.now().minusDays(FEED_DAYS);
        long offset = (long) (page - 1) * size;
        List<Article> list = userFeedMapper.selectFeedArticles(userId, since, offset, size);
        long total = userFeedMapper.countFeedArticles(userId, since);
        List<ArticleVO> vos = list.stream()
                .map(articleInteractionEnricher::toSummaryVo)
                .peek(vo -> articleInteractionEnricher.enrich(vo, userId))
                .collect(Collectors.toList());
        return PageResult.build(vos, total, size, page);
    }
}
