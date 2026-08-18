package com.blog.content.knowledge.service;

import com.blog.content.model.dto.knowledge.UserLandscapeRequest;
import com.blog.content.model.vo.knowledge.HotTagVo;
import com.blog.content.model.vo.knowledge.KnowledgeGraphVo;
import com.blog.content.model.vo.knowledge.TagNodeDetailVo;
import com.blog.content.model.vo.knowledge.UserLandscapeVo;

import java.util.List;

public interface KnowledgeGraphService {

    KnowledgeGraphVo getGraph();

    TagNodeDetailVo getNodeDetail(Long tagId, int articleLimit, Long viewerUserId);

    KnowledgeGraphVo getSubgraph(Long articleId);

    List<HotTagVo> getHotTags(int limit);

    UserLandscapeVo getUserLandscape(Long userId, UserLandscapeRequest request);

    void rebuildGraph();

    void refreshHotTags();
}
