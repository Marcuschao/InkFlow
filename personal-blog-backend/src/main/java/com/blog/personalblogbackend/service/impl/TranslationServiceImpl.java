package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.model.dto.translation.TranslationJobDto;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.ArticleTranslation;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.llm.AiService;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.mapper.ArticleTranslationMapper;
import com.blog.personalblogbackend.service.TranslationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class TranslationServiceImpl implements TranslationService {

    private static final Set<String> ALLOWED_LOCALES = Set.of("en", "ja", "ko");

    private final ConcurrentHashMap<String, TranslationJobDto> jobs = new ConcurrentHashMap<>();

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private ArticleTranslationMapper articleTranslationMapper;
    @Autowired
    private AiService aiService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    @Qualifier("translationBatchExecutor")
    private Executor translationBatchExecutor;

    @Override
    public ArticleTranslation getTranslation(Long articleId, String locale) {
        String loc = normalizeLocale(locale);
        validateLocale(loc);
        return articleTranslationMapper.selectOne(new LambdaQueryWrapper<ArticleTranslation>()
                .eq(ArticleTranslation::getArticleId, articleId)
                .eq(ArticleTranslation::getLocale, loc));
    }

    @Override
    @Transactional
    public void saveTranslation(ArticleTranslation tr) {
        if (tr.getArticleId() == null) {
            throw new ServiceException(400, "articleId 不能为空");
        }
        String loc = normalizeLocale(tr.getLocale());
        validateLocale(loc);
        tr.setLocale(loc);
        ArticleTranslation existing = articleTranslationMapper.selectOne(new LambdaQueryWrapper<ArticleTranslation>()
                .eq(ArticleTranslation::getArticleId, tr.getArticleId())
                .eq(ArticleTranslation::getLocale, loc));
        if (existing == null) {
            if (!StringUtils.hasText(tr.getTitle()) || !StringUtils.hasText(tr.getContent())) {
                throw new ServiceException(400, "标题与正文不能为空");
            }
            if (tr.getStatus() == null) {
                tr.setStatus(1);
            }
            articleTranslationMapper.insert(tr);
        } else {
            tr.setId(existing.getId());
            articleTranslationMapper.updateById(tr);
        }
    }

    @Override
    @Transactional
    public void machineTranslate(Long articleId, String locale) {
        String loc = normalizeLocale(locale);
        validateLocale(loc);
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException(404, "文章不存在");
        }
        String sys = "你是专业译者。将下列中文博客翻译为「" + localeLabel(loc)
                + "」，忠实原意，保留 Markdown 结构与代码块 fenced code。"
                + "仅输出一行 JSON，键为 title、summary、content（字符串），不要 markdown 围栏或其它文字。";
        String body = article.getContent() == null ? "" : article.getContent();
        if (body.length() > 11000) {
            body = body.substring(0, 11000);
        }
        String user = "标题：" + nz(article.getTitle())
                + "\n摘要：" + nz(article.getSummary())
                + "\n正文：\n" + body;
        String raw = aiService.chat(sys, user);
        JsonNode node = parseJsonObject(raw);
        ArticleTranslation tr = new ArticleTranslation();
        tr.setArticleId(articleId);
        tr.setLocale(loc);
        tr.setTitle(textReq(node, "title"));
        tr.setSummary(textOpt(node, "summary"));
        tr.setContent(textReq(node, "content"));
        tr.setStatus(0);
        ArticleTranslation existing = articleTranslationMapper.selectOne(new LambdaQueryWrapper<ArticleTranslation>()
                .eq(ArticleTranslation::getArticleId, articleId)
                .eq(ArticleTranslation::getLocale, loc));
        if (existing == null) {
            articleTranslationMapper.insert(tr);
        } else {
            tr.setId(existing.getId());
            articleTranslationMapper.updateById(tr);
        }
    }

    @Override
    @Transactional
    public void generateTranslationSeo(Long articleId, String locale) {
        String loc = normalizeLocale(locale);
        validateLocale(loc);
        ArticleTranslation tr = getTranslation(articleId, loc);
        if (tr == null) {
            throw new ServiceException(404, "该语种译文不存在");
        }
        String sys = "你是 SEO 编辑。根据下列「" + localeLabel(loc)
                + "」博文标题与节选正文，输出严格一行 JSON：{\"seoTitle\":\"...\",\"seoDescription\":\"...\"}。"
                + "seoTitle 简洁；seoDescription 不超过两条句子。";
        String user = "标题：" + nz(tr.getTitle())
                + "\n摘要：" + nz(tr.getSummary())
                + "\n正文节选：\n" + trunc(tr.getContent(), 5000);
        String raw = aiService.chat(sys, user);
        JsonNode node = parseJsonObject(raw);
        ArticleTranslation patch = new ArticleTranslation();
        patch.setId(tr.getId());
        patch.setSeoTitle(textOpt(node, "seoTitle"));
        patch.setSeoDescription(textOpt(node, "seoDescription"));
        articleTranslationMapper.updateById(patch);
    }

    @Override
    public String startBatch(List<Long> articleIds, List<String> locales) {
        if (CollectionUtils.isEmpty(articleIds) || CollectionUtils.isEmpty(locales)) {
            throw new ServiceException(400, "articleIds 与 locales 不能为空");
        }
        List<String> locNorm = locales.stream().map(TranslationServiceImpl::normalizeLocale).toList();
        for (String loc : locNorm) {
            validateLocale(loc);
        }
        String jobId = UUID.randomUUID().toString();
        TranslationJobDto job = new TranslationJobDto();
        job.setJobId(jobId);
        job.setState("RUNNING");
        job.setTotal(articleIds.size() * locNorm.size());
        job.setProcessed(0);
        jobs.put(jobId, job);
        translationBatchExecutor.execute(() -> runBatchJob(jobId, articleIds, locNorm));  //异步处理
        return jobId;
    }

    private void runBatchJob(String jobId, List<Long> articleIds, List<String> locales) {
        TranslationJobDto job = jobs.get(jobId);
        if (job == null) {
            return;
        }
        try {
            int done = 0;
            for (Long aid : articleIds) {
                for (String loc : locales) {
                    machineTranslate(aid, loc);
                    done++;
                    job.setProcessed(done);
                }
            }
            job.setState("DONE");
        } catch (Exception e) {
            job.setState("FAILED");
            job.setErrorMessage(e.getMessage() != null ? e.getMessage() : "batch failed");
        }
    }

    @Override
    public TranslationJobDto getJob(String jobId) {
        TranslationJobDto j = jobs.get(jobId);
        if (j == null) {
            throw new ServiceException(404, "任务不存在");
        }
        return j;
    }

    private static String normalizeLocale(String locale) {
        if (!StringUtils.hasText(locale)) {
            throw new ServiceException(400, "locale 不能为空");
        }
        return locale.trim().toLowerCase();
    }

    private void validateLocale(String loc) {
        if ("zh".equals(loc)) {
            throw new ServiceException(400, "请使用 en / ja / ko 作为译文语种");
        }
        if (!ALLOWED_LOCALES.contains(loc)) {
            throw new ServiceException(400, "暂不支持的 locale: " + loc);
        }
    }

    private static String localeLabel(String loc) {
        return switch (loc) {
            case "en" -> "英文";
            case "ja" -> "日文";
            case "ko" -> "韩文";
            default -> loc;
        };
    }

    private JsonNode parseJsonObject(String raw) {
        try {
            String s = raw.trim();
            int l = s.indexOf('{');
            int r = s.lastIndexOf('}');
            if (l >= 0 && r > l) {
                s = s.substring(l, r + 1);
            }
            return objectMapper.readTree(s);
        } catch (Exception e) {
            throw new ServiceException(502, "JSON 解析失败");
        }
    }

    private static String textReq(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || !n.isTextual() || !StringUtils.hasText(n.asText())) {
            throw new ServiceException(502, "缺少字段 " + field);
        }
        return n.asText().trim();
    }

    private static String textOpt(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || !n.isTextual()) {
            return null;
        }
        String t = n.asText().trim();
        return StringUtils.hasText(t) ? t : null;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String trunc(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
