package com.blog.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.content.model.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    @Select("""
            SELECT follower_id FROM user_follow
            WHERE followee_id = #{userId}
            ORDER BY create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<Long> selectFollowerIds(@Param("userId") Long userId, @Param("offset") long offset, @Param("size") long size);

    @Select("SELECT COUNT(*) FROM user_follow WHERE followee_id = #{userId}")
    long countFollowers(@Param("userId") Long userId);

    @Select("""
            SELECT followee_id FROM user_follow
            WHERE follower_id = #{userId}
            ORDER BY create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<Long> selectFollowingIds(@Param("userId") Long userId, @Param("offset") long offset, @Param("size") long size);

    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{userId}")
    long countFollowing(@Param("userId") Long userId);
}
