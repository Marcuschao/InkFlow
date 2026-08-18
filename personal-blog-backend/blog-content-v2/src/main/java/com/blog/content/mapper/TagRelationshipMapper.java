package com.blog.content.mapper;

import com.blog.content.model.entity.TagRelationship;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagRelationshipMapper extends BaseMapper<TagRelationship> {

    @Delete("DELETE FROM tag_relationship")
    void deleteAll();
}
