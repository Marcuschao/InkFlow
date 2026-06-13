package com.blog.content.service.impl;

import com.blog.content.model.entity.About;
import com.blog.content.mapper.AboutMapper;
import com.blog.content.service.AboutService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AboutServiceImpl extends ServiceImpl<AboutMapper, About> implements AboutService {

    @Override
    public About getAboutContent() {
        // 通常关于我内容只有一条，这里获取第一条
        return getOne(new QueryWrapper<About>().last("LIMIT 1"));
    }

    @Override
    @Transactional
    public boolean updateAboutContent(About about) {
        About existingAbout = getAboutContent();
        if (existingAbout != null) {
            about.setId(existingAbout.getId()); // 更新现有记录
            return updateById(about);
        } else {
            return save(about); // 如果没有记录，则创建新记录
        }
    }
}
