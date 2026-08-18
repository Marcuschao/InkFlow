package com.blog.content.service;

import com.blog.content.model.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /**
     * 获取所有分类
     * @return 分类列表
     */
    List<Category> getAllCategories();
}
