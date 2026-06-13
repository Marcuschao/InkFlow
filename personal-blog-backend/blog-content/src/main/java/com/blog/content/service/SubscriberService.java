package com.blog.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.content.model.dto.subscribe.SubscribeRequest;
import com.blog.content.model.entity.Subscriber;

import java.util.List;

public interface SubscriberService extends IService<Subscriber> {

    int STATUS_PENDING = 0;
    int STATUS_ACTIVE = 1;

    void subscribe(SubscribeRequest req);

    boolean confirmSubscribe(String token);

    List<String> listActiveEmails();
}
