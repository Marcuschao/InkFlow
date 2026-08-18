package com.blog.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.common.support.PageResult;
import com.blog.content.config.SensitiveWordConfig;
import com.blog.content.mapper.SensitiveWordMapper;
import com.blog.content.model.entity.SensitiveWord;
import com.blog.content.service.SensitiveWordService;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final SensitiveWordMapper sensitiveWordMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile SensitiveWordBs bs;

    public SensitiveWordServiceImpl(SensitiveWordMapper sensitiveWordMapper, SensitiveWordBs sensitiveWordBs) {
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.bs = sensitiveWordBs;
    }

    @Override
    public boolean contains(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        lock.readLock().lock();
        try {
            return bs.contains(text);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String replace(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        lock.readLock().lock();
        try {
            return bs.replace(text);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<String> findAll(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        lock.readLock().lock();
        try {
            return bs.findAll(text);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public PageResult<SensitiveWord> page(long page, long size) {
        long pg = Math.max(page, 1);
        long sz = Math.min(Math.max(size, 1), 100);
        Page<SensitiveWord> result = sensitiveWordMapper.selectPage(
                new Page<>(pg, sz),
                new LambdaQueryWrapper<SensitiveWord>().orderByDesc(SensitiveWord::getId));
        return PageResult.build(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public SensitiveWord add(String word) {
        if (!StringUtils.hasText(word)) {
            throw new ServiceException(400, "敏感词不能为空");
        }
        String trimmed = word.trim();
        Long exists = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<SensitiveWord>()
                .eq(SensitiveWord::getWord, trimmed));
        if (exists != null && exists > 0) {
            throw new ServiceException(400, "敏感词已存在");
        }
        SensitiveWord entity = new SensitiveWord();
        entity.setWord(trimmed);
        entity.setCreateTime(LocalDateTime.now(ZONE));
        sensitiveWordMapper.insert(entity);
        lock.writeLock().lock();
        try {
            bs.addWord(trimmed);
        } finally {
            lock.writeLock().unlock();
        }
        return entity;
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new ServiceException(400, "ID 无效");
        }
        SensitiveWord entity = sensitiveWordMapper.selectById(id);
        if (entity == null) {
            return;
        }
        sensitiveWordMapper.deleteById(id);
        if (StringUtils.hasText(entity.getWord())) {
            lock.writeLock().lock();
            try {
                bs.removeWord(entity.getWord().trim());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void refresh() {
        SensitiveWordBs next = SensitiveWordConfig.build(sensitiveWordMapper);
        lock.writeLock().lock();
        try {
            bs = next;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
