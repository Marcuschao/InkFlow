package com.blog.content.profile.mapper;

import com.blog.content.profile.model.vo.TimelineItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TimelineMapper {

    List<TimelineItemVo> selectInteractionTimeline(@Param("userId") Long userId, @Param("limit") int limit);
}
