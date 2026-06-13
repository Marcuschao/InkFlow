package com.blog.ai.agent.tools;

import com.blog.ai.agent.KeywordHelper;
import com.blog.ai.model.dto.agent.ChatSourceDto;
import com.blog.ai.model.entity.Article;
import com.blog.ai.mapper.ArticleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ArticleSearchTools {

    private static final int CONTEXT_CHUNK = 1200;

    private final ArticleMapper articleMapper;
    private final ThreadLocal<List<ChatSourceDto>> sources = new ThreadLocal<>();

    public ArticleSearchTools(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public void beginSession() {
        sources.set(new ArrayList<>());
    }

    public List<ChatSourceDto> endSession() {
        List<ChatSourceDto> list = sources.get();
        sources.remove();
        return list != null ? List.copyOf(list) : List.of();
    }

    @Tool("检索站内已发布的博客文章。参数 searchQuery 为用户问题或关键词；返回匹配文章的标题与正文片段供回答问题引用。")
    public String searchBlogArticles(String searchQuery) {
        List<String> keywords = KeywordHelper.fromText(searchQuery);
        if (keywords.isEmpty()) {
            return "未提取到有效检索词。";
        }
        List<Article> articles = articleMapper.searchPublishedByKeywords(keywords, null, 5);
        if (articles == null || articles.isEmpty()) {
            articles = articleMapper.selectList(new QueryWrapper<Article>()
                    .eq("status", 1)
                    .orderByDesc("create_time")
                    .last("LIMIT 3"));
        }
        if (articles == null || articles.isEmpty()) {
            return "暂无可用文章资料。";
        }
        List<ChatSourceDto> acc = sources.get();
        if (acc == null) {
            acc = new ArrayList<>();
            sources.set(acc);
        }
        StringBuilder sb = new StringBuilder();
        for (Article a : articles) {
            acc.add(new ChatSourceDto(a.getId(), a.getTitle()));
            sb.append("---\n标题：").append(a.getTitle()).append("\n摘要：")
                    .append(a.getSummary() == null ? "" : a.getSummary()).append("\n正文片段：\n")
                    .append(truncate(a.getContent(), CONTEXT_CHUNK)).append("\n");
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s == null ? "" : s;
        }
        return s.substring(0, max);
    }
}
