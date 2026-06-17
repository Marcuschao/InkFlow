package com.blog.content.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.content.profile.model.entity.ProfileVisitor;
import com.blog.content.profile.model.vo.VisitorVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProfileVisitorMapper extends BaseMapper<ProfileVisitor> {

    @Select("""
            SELECT v.visitor_user_id AS userId,
                   COALESCE(p.nickname, u.nickname, u.username) AS nickname,
                   COALESCE(p.avatar, u.avatar) AS avatar,
                   MAX(v.visit_time) AS visitTime
            FROM profile_visitor v
            LEFT JOIN user_profile p ON p.user_id = v.visitor_user_id
            LEFT JOIN user u ON u.id = v.visitor_user_id
            WHERE v.profile_owner_id = #{ownerId}
              AND v.visit_time >= #{since}
            GROUP BY v.visitor_user_id, p.nickname, u.nickname, u.username, p.avatar, u.avatar
            ORDER BY visitTime DESC
            LIMIT #{limit}
            """)
    List<VisitorVo> selectRecentVisitors(@Param("ownerId") Long ownerId,
                                       @Param("since") LocalDateTime since,
                                       @Param("limit") int limit);
}
