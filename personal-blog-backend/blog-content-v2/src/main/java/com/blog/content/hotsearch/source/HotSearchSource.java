package com.blog.content.hotsearch.source;

import com.blog.content.hotsearch.model.HotItem;

import java.util.List;

public interface HotSearchSource {
    String getId();

    String getName();

    List<HotItem> fetchHotList();

    default boolean isFallbackData(List<HotItem> items) {
        return false;
    }
}
