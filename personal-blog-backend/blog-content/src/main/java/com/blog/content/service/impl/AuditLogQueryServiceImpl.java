package com.blog.content.service.impl;

import com.blog.content.common.support.PageResult;
import com.blog.content.model.vo.audit.AuditLogVo;
import com.blog.content.model.entity.AuditLog;
import com.blog.content.mapper.AuditLogMapper;
import com.blog.content.service.AuditLogQueryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogQueryServiceImpl implements AuditLogQueryService {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Override
    public PageResult<AuditLogVo> page(long current, long size) {
        Page<AuditLog> mp = new Page<>(current, size);
        QueryWrapper<AuditLog> qw = new QueryWrapper<>();
        qw.orderByDesc("created_at");
        IPage<AuditLog> raw = auditLogMapper.selectPage(mp, qw);
        List<AuditLogVo> vos = raw.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.build(vos, raw.getTotal(), raw.getSize(), raw.getCurrent());
    }

    @Override
    public void record(String username, String action, String detail, String ip) {
        AuditLog row = new AuditLog();
        row.setUsername(username != null ? username : "");
        row.setAction(action);
        row.setDetail(detail);
        row.setIp(ip);
        row.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(row);
    }

    private AuditLogVo toVo(AuditLog e) {
        AuditLogVo v = new AuditLogVo();
        BeanUtils.copyProperties(e, v);
        return v;
    }
}
