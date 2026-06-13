package com.blog.content.service.impl;

import com.blog.content.mapper.UserMapper;
import com.blog.content.mapper.UserProfileMapper;
import com.blog.content.model.entity.Article;
import com.blog.content.model.entity.User;
import com.blog.content.model.entity.UserProfile;
import com.blog.content.model.vo.ArticleVO;
import com.blog.content.service.ArticleFavoriteService;
import com.blog.content.service.ArticleInteractionEnricher;
import com.blog.content.service.ArticleLikeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArticleInteractionEnricherImpl implements ArticleInteractionEnricher {

    @Autowired
    private ArticleLikeService articleLikeService;
    @Autowired
    @Lazy
    private ArticleFavoriteService articleFavoriteService;
    @Autowired
    private UserProfileMapper userProfileMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public void enrich(ArticleVO vo, Long viewerId) {
        if (vo == null || vo.getId() == null) {
            return;
        }
        if (vo.getLikeCount() == null) {
            vo.setLikeCount(0);
        }
        if (viewerId != null) {
            vo.setLiked(articleLikeService.isLiked(viewerId, vo.getId()));
            vo.setFavorited(articleFavoriteService.isFavorited(viewerId, vo.getId()));
        }
        if (vo.getAuthorId() != null) {
            UserProfile profile = userProfileMapper.selectById(vo.getAuthorId());
            User user = userMapper.selectById(vo.getAuthorId());
            if (profile != null && StringUtils.hasText(profile.getNickname())) {
                vo.setAuthorNickname(profile.getNickname());
                vo.setAuthorAvatar(profile.getAvatar());
            } else if (user != null) {
                vo.setAuthorNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
        }
    }

    @Override
    public ArticleVO toSummaryVo(Article article) {
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(article, vo);
        vo.setContent(null);
        if (vo.getLikeCount() == null) {
            vo.setLikeCount(0);
        }
        return vo;
    }
}
