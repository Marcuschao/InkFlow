package com.blog.ai.rag.parse;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.Normalizer;

@Service
public class DocumentParserService {

    private final Tika tika = new Tika();

    public String parse(InputStream stream) {
        try {
            String parsed = tika.parseToString(stream);
            return Normalizer.normalize(parsed, Normalizer.Form.NFKC)
                    .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                    .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                    .replaceAll("[\\u200B-\\u200F\\u2060\\uFEFF]", "")
                    .replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]", " ");
        } catch (Exception e) {
            throw new IllegalStateException("文档解析失败: " + e.getMessage(), e);
        }
    }
}
