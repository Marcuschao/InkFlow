package com.blog.content.mapper;

import com.blog.content.model.entity.UserTagSubscription;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserTagSubscriptionMapper extends BaseMapper<UserTagSubscription> {

    @Select("SELECT tag_id FROM user_tag_subscription WHERE user_id = #{userId}")
    List<Long> selectTagIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT user_id FROM user_tag_subscription WHERE tag_id = #{tagId}")
    List<Long> selectUserIdsByTagId(@Param("tagId") Long tagId);
}
