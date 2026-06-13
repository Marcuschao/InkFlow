package com.blog.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.content.model.entity.Diary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiaryMapper extends BaseMapper<Diary> {
}
