package com.blog.personalblogbackend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_daily")
public class StatDaily {

    @TableId
    private LocalDate statDate;
    private Long pvCount;
    private Long uvCount;
    private LocalDateTime updatedAt;
}
