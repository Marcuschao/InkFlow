package com.blog.content.gamification.reward.service.impl;

import com.blog.content.common.exception.ServiceException;
import com.blog.content.gamification.points.service.PointsService;
import com.blog.content.gamification.reward.mapper.ArticleRewardMapper;
import com.blog.content.gamification.reward.model.entity.ArticleReward;
import com.blog.content.gamification.reward.model.vo.ArticleRewardVo;
import com.blog.content.gamification.reward.service.ArticleRewardService;
import com.blog.content.model.entity.Article;
import com.blog.content.model.enums.ArticleStatus;
import com.blog.content.notification.NotificationProducer;
import com.blog.content.service.ArticleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ArticleRewardServiceImpl implements ArticleRewardService {

    private static final Set<Integer> ALLOWED_POINTS = Set.of(5, 10, 20, 50);

    private final ArticleRewardMapper articleRewardMapper;
    private final ArticleService articleService;
    private final PointsService pointsService;
    private final NotificationProducer notificationProducer;

    public ArticleRewardServiceImpl(ArticleRewardMapper articleRewardMapper,
                                    ArticleService articleService,
                                    PointsService pointsService,
                                    NotificationProducer notificationProducer) {
        this.articleRewardMapper = articleRewardMapper;
        this.articleService = articleService;
        this.pointsService = pointsService;
        this.notificationProducer = notificationProducer;
    }

    @Override
    @Transactional
    public ArticleRewardVo reward(Long articleId, Long fromUserId, int points) {
        if (!ALLOWED_POINTS.contains(points)) {
            throw new ServiceException(400, "不支持的打赏档位");
        }
        Article article = articleService.getById(articleId);
        if (article == null || !ArticleStatus.isPublished(article.getStatus())) {
            throw new ServiceException(404, "文章不存在");
        }
        Long toUserId = article.getAuthorId();
        if (toUserId == null) {
            throw new ServiceException(400, "文章作者不存在");
        }
        pointsService.transferPoints(fromUserId, toUserId, points,
                "打赏文章《" + article.getTitle() + "》",
                "收到文章《" + article.getTitle() + "》打赏");

        ArticleReward reward = new ArticleReward();
        reward.setArticleId(articleId);
        reward.setFromUserId(fromUserId);
        reward.setToUserId(toUserId);
        reward.setPoints(points);
        reward.setCreateTime(LocalDateTime.now());
        articleRewardMapper.insert(reward);
        notificationProducer.notifyReward(fromUserId, article, points);
        return articleRewardMapper.selectByArticleId(articleId, 1).stream()
                .filter(r -> reward.getId().equals(r.getId()))
                .findFirst()
                .orElseGet(() -> {
                    ArticleRewardVo vo = new ArticleRewardVo();
                    vo.setId(reward.getId());
                    vo.setArticleId(articleId);
                    vo.setFromUserId(fromUserId);
                    vo.setToUserId(toUserId);
                    vo.setPoints(points);
                    vo.setCreateTime(reward.getCreateTime());
                    return vo;
                });
    }

    @Override
    public List<ArticleRewardVo> listArticleRewards(Long articleId) {
        return articleRewardMapper.selectByArticleId(articleId, 50);
    }

    @Override
    public List<ArticleRewardVo> listUserHistory(Long userId) {
        return articleRewardMapper.selectUserHistory(userId, 100);
    }
}
