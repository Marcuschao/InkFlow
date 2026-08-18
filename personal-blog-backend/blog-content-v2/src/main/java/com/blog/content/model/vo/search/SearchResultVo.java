package com.blog.content.model.vo.search;

import com.blog.content.common.support.PageResult;
import com.blog.content.model.vo.knowledge.RelatedTagVo;
import lombok.Data;

import java.util.List;

@Data
public class SearchResultVo {
    private PageResult<ArticleSearchHitVo> page;
    private List<RelatedTagVo> relatedTags;
}
