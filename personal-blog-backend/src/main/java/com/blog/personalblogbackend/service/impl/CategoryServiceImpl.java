package com.blog.personalblogbackend.service.impl;

import com.blog.personalblogbackend.cache.MetaCacheService;
import com.blog.personalblogbackend.model.entity.Category;
import com.blog.personalblogbackend.mapper.CategoryMapper;
import com.blog.personalblogbackend.service.CategoryService;
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
