package com.blog.content.mapper;

import com.blog.content.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT IFNULL(points, 0) FROM user WHERE id = #{userId}")
    Integer selectPointsById(@Param("userId") Long userId);

    @Update("UPDATE user SET points = IFNULL(points, 0) + #{delta} WHERE id = #{userId}")
    int addPointsById(@Param("userId") Long userId, @Param("delta") int delta);
}
