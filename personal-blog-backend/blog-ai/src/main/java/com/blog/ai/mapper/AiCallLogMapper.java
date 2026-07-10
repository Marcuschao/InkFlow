package com.blog.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.ai.model.entity.AiCallLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AiCallLogMapper extends BaseMapper<AiCallLog> {

    @Select("SELECT COUNT(*) FROM ai_call_log")
    Long countTotal();

    @Select("SELECT feature AS feat, COUNT(*) AS cnt FROM ai_call_log WHERE created_at >= #{start} AND created_at < #{end} GROUP BY feature ORDER BY cnt DESC")
    List<Map<String, Object>> aggregateByFeature(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) AS cnt, " +
            "SUM(CASE WHEN success = 1 OR status = 'success' THEN 1 ELSE 0 END) AS ok_cnt, " +
            "AVG(COALESCE(latency_ms, duration_ms)) AS avg_latency, " +
            "COALESCE(SUM(cost), 0) AS total_cost " +
            "FROM ai_call_log WHERE created_at >= #{start} AND created_at < #{end}")
    Map<String, Object> overview(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT DATE(created_at) AS stat_day, COUNT(*) AS cnt, COALESCE(SUM(cost), 0) AS total_cost, " +
            "SUM(CASE WHEN success = 1 OR status = 'success' THEN 1 ELSE 0 END) AS ok_cnt " +
            "FROM ai_call_log WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY DATE(created_at) ORDER BY stat_day")
    List<Map<String, Object>> trendByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT model, COUNT(*) AS cnt FROM ai_call_log WHERE created_at >= #{start} AND created_at < #{end} " +
            "AND model IS NOT NULL GROUP BY model ORDER BY cnt DESC")
    List<Map<String, Object>> byModel(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT user_id, username, COUNT(*) AS cnt FROM ai_call_log WHERE created_at >= #{start} AND created_at < #{end} " +
            "AND user_id IS NOT NULL GROUP BY user_id, username ORDER BY cnt DESC LIMIT #{limit}")
    List<Map<String, Object>> byUser(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("limit") int limit);
}
