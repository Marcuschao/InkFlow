package com.blog.personalblogbackend.dto.revision;

import lombok.Data;

import java.util.List;

@Data
public class RevisionDiffResponseVo {

    private Long leftRevisionId;
    private Long rightRevisionId;
    private List<DiffLineVo> lines;
}
