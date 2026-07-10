package com.blog.ai.config.guard;

import com.blog.ai.mapper.SensitiveWordMapper;
import com.blog.ai.model.entity.SensitiveWord;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SensitiveWordConfig {

    private SensitiveWordConfig() {
    }

    public static SensitiveWordBs build(SensitiveWordMapper mapper) {
        List<String> words = loadWords(mapper);
        IWordDeny deny = () -> words;
        return SensitiveWordBs.newInstance()
                .wordDeny(deny)
                .enableNumCheck(false)
                .enableEmailCheck(false)
                .enableUrlCheck(false)
                .enableIpv4Check(false)
                .init();
    }

    public static SensitiveWordBs empty() {
        return SensitiveWordBs.newInstance()
                .wordDeny((IWordDeny) Collections::emptyList)
                .enableNumCheck(false)
                .enableEmailCheck(false)
                .enableUrlCheck(false)
                .enableIpv4Check(false)
                .init();
    }

    private static List<String> loadWords(SensitiveWordMapper mapper) {
        List<String> words = new ArrayList<>();
        if (mapper == null) {
            return words;
        }
        List<SensitiveWord> rows = mapper.selectList(null);
        if (rows == null) {
            return words;
        }
        for (SensitiveWord item : rows) {
            if (item != null && StringUtils.hasText(item.getWord())) {
                words.add(item.getWord().trim());
            }
        }
        return words;
    }
}
