package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("blog_site_kv")
public class BlogSiteKv {
    @TableId
    private String k;
    private String v;
}
