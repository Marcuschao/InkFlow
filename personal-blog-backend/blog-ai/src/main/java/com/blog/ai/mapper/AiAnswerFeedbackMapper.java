package com.blog.ai.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.ai.model.entity.AiAnswerFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;
@Mapper public interface AiAnswerFeedbackMapper extends BaseMapper<AiAnswerFeedback> {
    @Select("SELECT vote, COUNT(*) AS count FROM ai_answer_feedback GROUP BY vote")
    List<Map<String,Object>> countByVote();
}
