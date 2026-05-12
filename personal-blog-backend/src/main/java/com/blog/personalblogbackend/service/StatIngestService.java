package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.dto.stat.PageViewRequest;

public interface StatIngestService {

    void recordPageView(PageViewRequest request);
}
