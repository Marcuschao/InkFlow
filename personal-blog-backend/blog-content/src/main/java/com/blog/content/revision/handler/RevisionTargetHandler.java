package com.blog.content.revision.handler;

import com.blog.content.common.revision.RevisionTargetType;
import com.blog.content.model.vo.revision.ContentRevisionDetailVo;
import com.blog.content.model.vo.revision.ContentRevisionListItemVo;
import com.blog.content.model.vo.revision.RevisionDiffResponseVo;

import java.util.List;

// 设计模式：策略模式 - 按修订目标类型（文章/分享）分发快照、恢复与查询逻辑
public interface RevisionTargetHandler {

    RevisionTargetType type();

    List<ContentRevisionListItemVo> listRevisions(Long targetId, Long userId);

    ContentRevisionDetailVo getRevision(Long targetId, Long revisionId, Long userId);

    void restore(Long targetId, Long revisionId, Long userId);

    RevisionDiffResponseVo diff(Long targetId, Long userId, Long leftRevisionId, Long rightRevisionId);
}
