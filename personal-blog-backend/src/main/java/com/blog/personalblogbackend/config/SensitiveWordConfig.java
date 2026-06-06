package com.blog.personalblogbackend.config;

import com.blog.personalblogbackend.mapper.SensitiveWordMapper;
import com.blog.personalblogbackend.model.entity.SensitiveWord;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

import java.util.List;

@Configuration
public class SensitiveWordConfig {

    public static SensitiveWordBs build(SensitiveWordMapper mapper) {
        SensitiveWordBs bs = SensitiveWordBs.newInstance().init();
        List<SensitiveWord> words = mapper.selectList(null);
        if (words != null) {
            for (SensitiveWord item : words) {
                if (item != null && StringUtils.hasText(item.getWord())) {
                    bs.addWord(item.getWord().trim());
                }
            }
        }
        return bs;
    }

    @Bean
    @Lazy
    public SensitiveWordBs sensitiveWordBs(SensitiveWordMapper sensitiveWordMapper) {
        return build(sensitiveWordMapper);
    }
}
