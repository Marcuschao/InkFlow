package com.blog.content.controller.internal;

import com.blog.content.common.support.Result;
import com.blog.content.knowledge.service.KnowledgeGraphService;
import com.blog.content.model.vo.knowledge.KnowledgeGraphVo;
import com.blog.content.model.vo.knowledge.TagNodeDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/knowledge")
public class InternalKnowledgeController {

    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    @GetMapping("/graph")
    public Result<KnowledgeGraphVo> graph() {
        return Result.success(knowledgeGraphService.getGraph());
    }

    @GetMapping("/node")
    public Result<TagNodeDetailVo> node(@RequestParam Long tagId) {
        return Result.success(knowledgeGraphService.getNodeDetail(tagId, 10, null));
    }
}
