package com.blog.content.event;

import com.blog.content.model.entity.Article;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

// 设计模式：观察者模式 - 文章生命周期变更事件，供监听器响应副作用
@Getter
public class ArticleLifecycleEvent extends ApplicationEvent {

    private final Article previous;
    private final Article fresh;

    public ArticleLifecycleEvent(Object source, Article previous, Article fresh) {
        super(source);
        this.previous = previous;
        this.fresh = fresh;
    }
}
