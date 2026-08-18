package com.blog.content.mapper;

import com.blog.content.knowledge.model.TagArticleCountDto;
import com.blog.content.knowledge.model.TagCooccurrenceDto;
import com.blog.content.knowledge.model.TagWeightRowDto;
import com.blog.content.model.vo.knowledge.RelatedAuthorVo;
import com.blog.content.model.vo.knowledge.RelatedTagVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface KnowledgeQueryMapper {

    List<TagArticleCountDto> selectTagArticleCounts();

    List<TagCooccurrenceDto> selectTagCooccurrences();

    List<RelatedTagVo> selectRelatedTags(@Param("tagId") Long tagId, @Param("limit") int limit);

    List<RelatedAuthorVo> selectHotAuthorsByTag(@Param("tagId") Long tagId, @Param("limit") int limit);

    List<TagWeightRowDto> selectWriteTagWeights(@Param("userId") Long userId);

    List<TagWeightRowDto> selectFavoriteTagWeights(@Param("userId") Long userId);

    List<TagWeightRowDto> selectReadTagWeights(@Param("articleIds") List<Long> articleIds);

    List<TagArticleCountDto> selectTopArticles(@Param("limit") int limit);

    List<TagArticleCountDto> selectTopAuthors(@Param("limit") int limit);

    List<TagWeightRowDto> selectTagViewScores(@Param("since") LocalDateTime since);
}
