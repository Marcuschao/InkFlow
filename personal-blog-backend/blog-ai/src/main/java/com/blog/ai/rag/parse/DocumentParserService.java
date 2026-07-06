package com.blog.ai.rag.parse;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class DocumentParserService {

    private final Tika tika = new Tika();

    public String parse(InputStream stream) {
        try {
            return tika.parseToString(stream);
        } catch (Exception e) {
            throw new IllegalStateException("文档解析失败: " + e.getMessage(), e);
        }
    }
}
