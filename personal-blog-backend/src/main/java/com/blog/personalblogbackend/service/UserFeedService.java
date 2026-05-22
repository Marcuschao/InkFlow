package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.common.support.PageResult;
import com.blog.personalblogbackend.model.vo.ArticleVO;

public interface UserFeedService {

    PageResult<ArticleVO> feed(Long userId, int page, int size);
}
