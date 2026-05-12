package com.blog.personalblogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.personalblogbackend.entity.PageViewEvent;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PageViewEventMapper extends BaseMapper<PageViewEvent> {

    @Select("SELECT COUNT(*) FROM page_view_event")
    Long countTotalPv();

    @Select("SELECT COUNT(DISTINCT visitor_id) FROM page_view_event")
    Long countDistinctVisitors();

    @Select("SELECT DATE(created_at) AS stat_day, COUNT(*) AS pv_cnt FROM page_view_event WHERE created_at >= #{start} GROUP BY DATE(created_at) ORDER BY stat_day")
    List<java.util.Map<String, Object>> aggregatePvByDay(@Param("start") LocalDateTime start);

    @Select("SELECT COUNT(*) FROM page_view_event WHERE created_at >= #{start} AND created_at < #{end}")
    Long countPvBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(DISTINCT visitor_id) FROM page_view_event WHERE created_at >= #{start} AND created_at < #{end}")
    Long countUvBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Delete("DELETE FROM page_view_event WHERE created_at < #{before}")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
