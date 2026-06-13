package com.blog.content.service;

import java.util.Optional;

public interface SiteKvService {

    Optional<String> get(String key);

    void put(String key, String value);
}
