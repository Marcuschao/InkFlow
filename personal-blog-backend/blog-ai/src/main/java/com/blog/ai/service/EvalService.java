package com.blog.ai.service;
import com.blog.ai.common.support.PageResult;
import com.blog.ai.model.dto.eval.*;
import com.blog.ai.model.entity.*;
import java.util.Map;
public interface EvalService {
    PageResult<AiEvalDataset> datasets(long page,long size);
    AiEvalDataset saveDataset(Long id, EvalDatasetRequest req);
    void deleteDataset(Long id);
    PageResult<AiEvalCase> cases(Long datasetId,long page,long size);
    AiEvalCase saveCase(Long datasetId, Long id, EvalCaseRequest req);
    void deleteCase(Long id);
    Map<String,Object> importJsonl(Long datasetId,String text);
    AiEvalRun startRun(Long datasetId,Integer topK);
    PageResult<AiEvalRun> runs(long page,long size);
    AiEvalRun run(Long id);
    PageResult<AiEvalResult> results(Long runId,long page,long size);
    AiAnswerFeedback feedback(Long userId, FeedbackRequest req);
    AiAnswerFeedback feedback(Long userId, Long messageId);
    Map<String,Long> feedbackStats();
}
