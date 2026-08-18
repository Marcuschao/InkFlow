package com.blog.content.service;

import com.blog.content.common.support.PageResult;
import com.blog.content.model.vo.ArticleVO;

public interface UserFeedService {

    PageResult<ArticleVO> feed(Long userId, int page, int size);
}
