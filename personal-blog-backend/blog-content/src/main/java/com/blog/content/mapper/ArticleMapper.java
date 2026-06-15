package com.blog.content.mapper;

import com.blog.content.model.entity.Article;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 根据ID查询文章详情，包含分类名和标签列表
     *
     * @param id 文章ID
     * @return 文章实体
     */
    @Select("SELECT * FROM article WHERE id = #{id}")
    Article selectArticleVOById(@Param("id") Long id);

    /**
     * 分页查询文章列表，包含分类名和标签列表
     *
     * @param page       分页对象
     * @param categoryId 分类ID
     * @param tagId      标签ID
     * @param keyword    关键词
     * @return 分页文章列表
     */
    IPage<Article> selectArticleVOPage(Page<Article> page,
                                       @Param("categoryId") Long categoryId,
                                       @Param("tagId") Long tagId,
                                       @Param("keyword") String keyword);

    /**
     * 根据文章ID删除文章-标签关联
     *
     * @param articleId 文章ID
     */
    @Delete("DELETE FROM article_tag WHERE article_id = #{articleId}")
    void deleteArticleTagsByArticleId(@Param("articleId") Long articleId);

    /**
     * 批量插入文章-标签关联
     *
     * @param articleId 文章ID
     * @param tagIds    标签ID列表
     */
    void insertArticleTags(@Param("articleId") Long articleId, @Param("tagIds") List<Long> tagIds);

    List<Article> searchPublishedByKeywords(@Param("keywords") List<String> keywords,
                                            @Param("excludeId") Long excludeId,
                                            @Param("limit") int limit);

    List<Article> searchPublishedByKeywordsExcludeIds(@Param("keywords") List<String> keywords,
                                                      @Param("excludeIds") List<Long> excludeIds,
                                                      @Param("limit") int limit);

    List<Article> selectPublishedExcludeIds(@Param("excludeIds") List<Long> excludeIds, @Param("limit") int limit);

    List<Article> selectArticleVOPageByCursor(@Param("lastId") Long lastId,
                                              @Param("size") Integer size,
                                              @Param("categoryId") Long categoryId,
                                              @Param("tagId") Long tagId,
                                              @Param("keyword") String keyword);

    @Update("UPDATE article SET comment_count = comment_count + 1 WHERE id = #{articleId}")
    int incrementCommentCount(@Param("articleId") Long articleId);

    @Select("SELECT t.name FROM article_tag at INNER JOIN tag t ON at.tag_id = t.id WHERE at.article_id = #{articleId} ORDER BY t.id")
    List<String> selectTagNamesByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT tag_id FROM article_tag WHERE article_id = #{articleId}")
    List<Long> selectTagIdsByArticleId(@Param("articleId") Long articleId);

    @Update("UPDATE article SET like_count = like_count + 1 WHERE id = #{articleId}")
    int incrementLikeCount(@Param("articleId") Long articleId);

    @Update("UPDATE article SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{articleId}")
    int decrementLikeCount(@Param("articleId") Long articleId);

    @Update("UPDATE article SET like_count = #{count} WHERE id = #{articleId}")
    int updateLikeCount(@Param("articleId") Long articleId, @Param("count") int count);
}
