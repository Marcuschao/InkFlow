package com.blog.personalblogbackend.event;

import com.blog.personalblogbackend.model.entity.Article;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class ArticleLifecycleEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public ArticleLifecycleEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(Article previous, Article fresh) {
        applicationEventPublisher.publishEvent(new ArticleLifecycleEvent(this, previous, fresh));
    }
}
