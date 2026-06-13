package com.blog.content.service.impl;

import com.blog.content.cache.MetaCacheService;
import com.blog.content.model.entity.Tag;
import com.blog.content.mapper.TagMapper;
import com.blog.content.service.TagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    private final MetaCacheService metaCacheService;

    public TagServiceImpl(MetaCacheService metaCacheService) {
        this.metaCacheService = metaCacheService;
    }

    @Override
    public List<Tag> getAllTags() {
        List<Tag> cached = metaCacheService.getTags();
        if (cached != null) {
            return cached;
        }
        List<Tag> tags = list();
        metaCacheService.putTags(tags);
        return tags;
    }
}
