package com.blog.content.service;

import com.blog.content.model.dto.stat.PageViewRequest;

public interface StatIngestService {

    void recordPageView(PageViewRequest request);
}
