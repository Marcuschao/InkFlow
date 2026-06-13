package com.blog.ai.service.impl;

import com.blog.ai.model.entity.BlogSiteKv;
import com.blog.ai.mapper.BlogSiteKvMapper;
import com.blog.ai.service.SiteKvService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SiteKvServiceImpl implements SiteKvService {

    @Autowired
    private BlogSiteKvMapper blogSiteKvMapper;

    @Override
    public Optional<String> get(String key) {
        BlogSiteKv row = blogSiteKvMapper.selectById(key);
        return row == null ? Optional.empty() : Optional.ofNullable(row.getV());
    }

    @Override
    public void put(String key, String value) {
        BlogSiteKv row = blogSiteKvMapper.selectById(key);
        if (row == null) {
            BlogSiteKv n = new BlogSiteKv();
            n.setK(key);
            n.setV(value != null ? value : "");
            blogSiteKvMapper.insert(n);
        } else {
            row.setV(value != null ? value : "");
            blogSiteKvMapper.updateById(row);
        }
    }
}
