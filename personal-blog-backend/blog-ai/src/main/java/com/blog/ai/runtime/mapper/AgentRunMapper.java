package com.blog.ai.runtime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.ai.runtime.model.AgentRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentRunMapper extends BaseMapper<AgentRun> {
    @Update("UPDATE ai_agent_run SET status=#{next}, step_count=step_count+1, version=version+1, updated_at=NOW() " +
            "WHERE run_id=#{runId} AND status=#{expected} AND version=#{version}")
    int transition(@Param("runId") String runId, @Param("expected") String expected,
                   @Param("next") String next, @Param("version") int version);

    @Update("UPDATE ai_agent_run SET status=#{status}, result_json=#{resultJson}, model=#{model}, " +
            "input_tokens=#{inputTokens}, output_tokens=#{outputTokens}, cost=#{cost}, error_code=#{errorCode}, " +
            "error_message=#{errorMessage}, finished_at=NOW(), updated_at=NOW(), version=version+1 " +
            "WHERE run_id=#{runId} AND status NOT IN ('COMPLETED','FAILED','CANCELLED','TIMEOUT','BUDGET_EXCEEDED','SAFETY_BLOCKED')")
    int finish(@Param("runId") String runId, @Param("status") String status,
               @Param("resultJson") String resultJson, @Param("model") String model,
               @Param("inputTokens") int inputTokens, @Param("outputTokens") int outputTokens,
               @Param("cost") double cost, @Param("errorCode") String errorCode,
               @Param("errorMessage") String errorMessage);
}
