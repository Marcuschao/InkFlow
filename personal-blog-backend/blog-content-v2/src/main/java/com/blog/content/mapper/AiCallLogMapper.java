package com.blog.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.content.model.entity.AiCallLog;
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
}
