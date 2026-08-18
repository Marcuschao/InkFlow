package com.blog.ai.controller;

import com.blog.ai.common.support.PageResult;
import com.blog.ai.common.support.Result;
import com.blog.ai.model.dto.eval.*;
import com.blog.ai.model.entity.*;
import com.blog.ai.service.EvalService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai/eval")
public class AdminEvalController {
    private final EvalService service;
    public AdminEvalController(EvalService service){this.service=service;}
    @GetMapping("/datasets") public Result<PageResult<AiEvalDataset>> datasets(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long size){return Result.success(service.datasets(page,size));}
    @PostMapping("/datasets") public Result<AiEvalDataset> create(@Valid @RequestBody EvalDatasetRequest r){return Result.success(service.saveDataset(null,r));}
    @PutMapping("/datasets/{id}") public Result<AiEvalDataset> update(@PathVariable Long id,@Valid @RequestBody EvalDatasetRequest r){return Result.success(service.saveDataset(id,r));}
    @DeleteMapping("/datasets/{id}") public Result<Void> delete(@PathVariable Long id){service.deleteDataset(id);return Result.success();}
    @GetMapping("/datasets/{id}/cases") public Result<PageResult<AiEvalCase>> cases(@PathVariable Long id,@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long size){return Result.success(service.cases(id,page,size));}
    @PostMapping("/datasets/{id}/cases") public Result<AiEvalCase> createCase(@PathVariable Long id,@Valid @RequestBody EvalCaseRequest r){return Result.success(service.saveCase(id,null,r));}
    @PutMapping("/cases/{id}") public Result<AiEvalCase> updateCase(@PathVariable Long id,@RequestBody EvalCaseRequest r){return Result.success(service.saveCase(r.getDatasetId(),id,r));}
    @DeleteMapping("/cases/{id}") public Result<Void> deleteCase(@PathVariable Long id){service.deleteCase(id);return Result.success();}
    @PostMapping(value="/datasets/{id}/import-jsonl", consumes=MediaType.TEXT_PLAIN_VALUE) public Result<Map<String,Object>> importJsonl(@PathVariable Long id,@RequestBody String text){return Result.success(service.importJsonl(id,text));}
    @PostMapping("/runs") public Result<AiEvalRun> start(@Valid @RequestBody EvalRunRequest r){return Result.success(service.startRun(r.getDatasetId(),r.getTopK()));}
    @GetMapping("/runs") public Result<PageResult<AiEvalRun>> runs(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long size){return Result.success(service.runs(page,size));}
    @GetMapping("/runs/{id}") public Result<AiEvalRun> run(@PathVariable Long id){return Result.success(service.run(id));}
    @GetMapping("/runs/{id}/results") public Result<PageResult<AiEvalResult>> results(@PathVariable Long id,@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long size){return Result.success(service.results(id,page,size));}
    @GetMapping("/feedback/stats") public Result<Map<String,Long>> feedbackStats(){return Result.success(service.feedbackStats());}
}
