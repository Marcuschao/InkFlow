package com.blog.content.service;

import com.blog.content.common.support.PageResult;
import com.blog.content.model.vo.audit.AuditLogVo;

public interface AuditLogQueryService {

    PageResult<AuditLogVo> page(long current, long size);

    void record(String username, String action, String detail, String ip);
}
