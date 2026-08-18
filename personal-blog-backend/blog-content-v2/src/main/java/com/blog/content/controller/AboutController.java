package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.model.entity.About;
import com.blog.content.service.AboutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/about")
public class AboutController {

    @Autowired
    private AboutService aboutService;

    /**
     * 获取关于我内容
     * @return 统一响应格式的关于我内容
     */
    @GetMapping
    public Result<About> getAboutContent() {
        About about = aboutService.getAboutContent();
        if (about == null) {
            return Result.fail(404, "关于我内容不存在");
        }
        return Result.success(about);
    }

    /**
     * 更新关于我内容
     * @param about 关于我实体
     * @return 统一响应格式
     */
    @PutMapping
    public Result<String> updateAboutContent(@RequestBody About about) {
        boolean success = aboutService.updateAboutContent(about);
        if (success) {
            return Result.success("关于我内容更新成功");
        } else {
            return Result.fail("关于我内容更新失败");
        }
    }
}
