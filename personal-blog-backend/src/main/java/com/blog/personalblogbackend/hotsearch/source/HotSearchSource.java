package com.blog.personalblogbackend.hotsearch.source;

import com.blog.personalblogbackend.hotsearch.model.HotItem;

import java.util.List;

public interface HotSearchSource {
    String getId();

    String getName();

    List<HotItem> fetchHotList();
}
