package com.blog.content.gamification.reward.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.content.gamification.reward.model.entity.ArticleReward;
import com.blog.content.gamification.reward.model.vo.ArticleRewardVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleRewardMapper extends BaseMapper<ArticleReward> {

    @Select("""
            SELECT ar.*, a.title AS article_title,
                   COALESCE(fp.nickname, fu.nickname, fu.username) AS from_nickname,
                   COALESCE(fp.avatar, fu.avatar) AS from_avatar,
                   COALESCE(tp.nickname, tu.nickname, tu.username) AS to_nickname,
                   COALESCE(tp.avatar, tu.avatar) AS to_avatar
            FROM article_reward ar
            LEFT JOIN article a ON a.id = ar.article_id
            LEFT JOIN user fu ON fu.id = ar.from_user_id
            LEFT JOIN user_profile fp ON fp.user_id = ar.from_user_id
            LEFT JOIN user tu ON tu.id = ar.to_user_id
            LEFT JOIN user_profile tp ON tp.user_id = ar.to_user_id
            WHERE ar.article_id = #{articleId}
            ORDER BY ar.create_time DESC
            LIMIT #{limit}
            """)
    List<ArticleRewardVo> selectByArticleId(@Param("articleId") Long articleId, @Param("limit") int limit);

    @Select("""
            SELECT ar.*, a.title AS article_title,
                   COALESCE(fp.nickname, fu.nickname, fu.username) AS from_nickname,
                   COALESCE(fp.avatar, fu.avatar) AS from_avatar,
                   COALESCE(tp.nickname, tu.nickname, tu.username) AS to_nickname,
                   COALESCE(tp.avatar, tu.avatar) AS to_avatar,
                   CASE WHEN ar.to_user_id = #{userId} THEN TRUE ELSE FALSE END AS received
            FROM article_reward ar
            LEFT JOIN article a ON a.id = ar.article_id
            LEFT JOIN user fu ON fu.id = ar.from_user_id
            LEFT JOIN user_profile fp ON fp.user_id = ar.from_user_id
            LEFT JOIN user tu ON tu.id = ar.to_user_id
            LEFT JOIN user_profile tp ON tp.user_id = ar.to_user_id
            WHERE ar.from_user_id = #{userId} OR ar.to_user_id = #{userId}
            ORDER BY ar.create_time DESC
            LIMIT #{limit}
            """)
    List<ArticleRewardVo> selectUserHistory(@Param("userId") Long userId, @Param("limit") int limit);
}
