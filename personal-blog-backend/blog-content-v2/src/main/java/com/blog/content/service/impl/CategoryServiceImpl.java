package com.blog.content.service.impl;

import com.blog.content.cache.MetaCacheService;
import com.blog.content.model.entity.Category;
import com.blog.content.mapper.CategoryMapper;
import com.blog.content.service.CategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final MetaCacheService metaCacheService;

    public CategoryServiceImpl(MetaCacheService metaCacheService) {
        this.metaCacheService = metaCacheService;
    }

    @Override
    public List<Category> getAllCategories() {
        List<Category> cached = metaCacheService.getCategories();
        if (cached != null) {
            return cached;
        }
        List<Category> categories = list();
        metaCacheService.putCategories(categories);
        return categories;
    }
}
