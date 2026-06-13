package com.blog.ai.service.impl;

import com.blog.ai.agent.KeywordHelper;
import com.blog.ai.agent.langchain.BlogChatAssistant;
import com.blog.ai.agent.tools.ArticleSearchTools;
import com.blog.ai.model.dto.agent.*;
import com.blog.ai.model.entity.Article;
import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.llm.AiService;
import com.blog.ai.mapper.ArticleMapper;
import com.blog.ai.service.AgentService;
import com.blog.ai.service.ReportStorageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {

    private static final int CONTEXT_CHUNK = 1200;
    private static final int CONTEXT_CHUNK_SCOPED = 12000;
    private static final int WEEKLY_SNIPPET = 1500;
    private static final String REFUSAL = "抱歉，我只能回答博客相关的问题";

    private final AiService aiService;
    private final ArticleMapper articleMapper;
    private final ObjectMapper objectMapper;
    private final Optional<BlogChatAssistant> blogChatAssistant;
    private final ArticleSearchTools articleSearchTools;
    private final ReportStorageService reportStorageService;

    public AgentServiceImpl(AiService aiService, ArticleMapper articleMapper, ObjectMapper objectMapper,
                            @Autowired(required = false) BlogChatAssistant blogChatAssistant,
                            ArticleSearchTools articleSearchTools,
                            @Autowired(required = false) ReportStorageService reportStorageService) {
        this.aiService = aiService;
        this.articleMapper = articleMapper;
        this.objectMapper = objectMapper;
        this.blogChatAssistant = Optional.ofNullable(blogChatAssistant);
        this.articleSearchTools = articleSearchTools;
        this.reportStorageService = reportStorageService;
    }

    @Override
    public String outline(OutlineRequest request) {
        if (!StringUtils.hasText(request.getTitle()) && !StringUtils.hasText(request.getTopic())) {
            throw new ServiceException(400, "标题或主题至少填一项");
        }
        String sys = "你是博客写作助手。根据用户提供的主题生成清晰的文章大纲，使用 Markdown 多级列表，不要输出与大纲无关的话。";
        String user = "文章标题：" + nullToEmpty(request.getTitle())
                + "\n已有摘要：" + nullToEmpty(request.getSummary())
                + "\n补充主题说明：" + nullToEmpty(request.getTopic());
        return aiService.chat(sys, user);
    }

    @Override
    public String expand(ExpandRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new ServiceException(400, "标题不能为空");
        }
        if (!StringUtils.hasText(request.getContext())) {
            throw new ServiceException(400, "上下文不能为空");
        }
        String sys = "你是博客写作助手。在保持风格一致的前提下续写正文，直接输出续写内容，不要重复已有段落。";
        String user = "标题：" + request.getTitle()
                + "\n已有内容：\n" + request.getContext()
                + "\n续写方向：" + nullToEmpty(request.getDirection());
        return aiService.chat(sys, user);
    }

    @Override
    public String polish(PolishRequest request) {
        if (!StringUtils.hasText(request.getText())) {
            throw new ServiceException(400, "文本不能为空");
        }
        String sys = "你是文字编辑。润色用户给出的文本，改正语病、优化表达，保留原意与 Markdown 结构，直接输出润色后的全文。";
        return aiService.chat(sys, request.getText());
    }

    @Override
    public String editorOutline(EditorOutlineRequest request) {
        if (!StringUtils.hasText(request.getTitle())
                && !StringUtils.hasText(request.getKeywords())
                && !StringUtils.hasText(request.getContent())) {
            throw new ServiceException(400, "标题、关键词或正文至少填一项");
        }
        String sys = "你是博客写作助手。根据标题、关键词与已有草稿节选生成清晰的文章大纲，使用 Markdown 多级列表，不要输出与大纲无关的寒暄。";
        String user = "文章标题：" + nullToEmpty(request.getTitle())
                + "\n关键词与侧重点：" + nullToEmpty(request.getKeywords())
                + "\n正文节选：\n" + truncate(nullToEmpty(request.getContent()), 8000);
        return aiService.chat(sys, user);
    }

    @Override
    public String editorContinue(EditorContinueRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new ServiceException(400, "标题不能为空");
        }
        String content = nullToEmpty(request.getContent());
        int len = content.length();
        int rawStart = request.getSelectionStart() != null ? request.getSelectionStart() : len;
        int rawEnd = request.getSelectionEnd() != null ? request.getSelectionEnd() : len;
        int lo = clamp(Math.min(rawStart, rawEnd), 0, len);
        int hi = clamp(Math.max(rawStart, rawEnd), 0, len);
        int caret = lo;

        String before = truncateTail(content.substring(0, caret), 4500);
        String after = truncate(content.substring(hi), 1200);
        String ctx = before + "\n\n[[请在此处自然接续上文续写；以下为选区或光标之后的草稿节选（若有），勿重复粘贴]]\n\n" + after;

        ExpandRequest ex = new ExpandRequest();
        ex.setTitle(request.getTitle());
        ex.setContext(ctx);
        ex.setDirection(request.getKeywords());
        return expand(ex);
    }

    @Override
    public String editorPolish(EditorPolishRequest request) {
        if (!StringUtils.hasText(request.getSelectedText())) {
            throw new ServiceException(400, "待润色文本不能为空");
        }
        String sys = "你是文字编辑。润色用户给出的文本片段，改正语病、优化表达，保留原意与 Markdown 结构，直接输出润色后的全文，不要附加解释。";
        StringBuilder user = new StringBuilder();
        if (StringUtils.hasText(request.getTitle())) {
            user.append("文章标题：").append(request.getTitle()).append("\n\n");
        }
        user.append("待润色文本：\n").append(request.getSelectedText());
        return aiService.chat(sys, user.toString());
    }

    @Override
    public String summary(SummaryRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new ServiceException(400, "标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new ServiceException(400, "正文不能为空");
        }
        String sys = "你是博客编辑。根据标题与正文生成简短中文摘要（1～3 句），不要标题前缀，不要加引号。";
        String user = "标题：" + request.getTitle() + "\n正文：\n" + truncate(request.getContent(), 8000);
        return aiService.chat(sys, user);
    }

    @Override
    public List<String> tags(TagsRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new ServiceException(400, "标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new ServiceException(400, "正文不能为空");
        }
        String sys = "你是博客编辑。根据标题与正文抽取 3～8 个简短中文标签，只输出 JSON 字符串数组，不要 markdown、不要其它说明。";
        String user = "标题：" + request.getTitle() + "\n正文：\n" + truncate(request.getContent(), 6000);
        String raw = aiService.chat(sys, user);
        return parseStringList(raw);
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String q = request.resolveQuestion();
        if (!StringUtils.hasText(q)) {
            throw new ServiceException(400, "问题不能为空");
        }
        List<Article> articles = new ArrayList<>();
        Long scopedId = request.getArticleId();
        if (scopedId != null) {
            Article cur = articleMapper.selectById(scopedId);
            if (cur != null && Integer.valueOf(1).equals(cur.getStatus())) {
                articles.add(cur);
            }
        }
        if (!articles.isEmpty()) {
            return answerFromArticles(articles, q, CONTEXT_CHUNK_SCOPED);
        }
        if (blogChatAssistant.isPresent()) {
            articleSearchTools.beginSession();
            try {
                String answer = blogChatAssistant.get().respond(q);
                List<ChatSourceDto> sources = articleSearchTools.endSession();
                ChatResponse res = new ChatResponse();
                res.setAnswer(answer != null ? answer.trim() : "");
                res.setSources(sources != null ? sources : List.of());
                return res;
            } catch (Exception ex) {
                articleSearchTools.endSession();
            }
        }
        List<String> keywords = KeywordHelper.fromText(q);
        if (keywords.isEmpty()) {
            return refusalResponse();
        }
        articles = articleMapper.searchPublishedByKeywords(keywords, null, 3);
        if (articles == null || articles.isEmpty()) {
            articles = articleMapper.selectList(new QueryWrapper<Article>()
                    .eq("status", 1)
                    .orderByDesc("create_time")
                    .last("LIMIT 3"));
        }
        if (articles == null || articles.isEmpty()) {
            return refusalResponse();
        }
        return answerFromArticles(articles, q, CONTEXT_CHUNK);
    }

    private ChatResponse answerFromArticles(List<Article> articles, String q, int chunk) {
        String sys = "你是博客问答助手。回答必须结合下面「参考资料」中的实际内容。"
                + "若用户问你能做什么、如何提问等元问题，请根据参考资料中的文章主题，简要说明可以围绕这些主题回答哪些类型的问题。"
                + "若参考资料与用户问题完全无关、或资料为空，请只回复：" + REFUSAL
                + "。不要编造参考资料中不存在的事实，表述需简洁。"
                + (articles.size() == 1 ? "当前用户正在阅读参考资料中的唯一一篇文章，请紧扣该文内容作答。" : "");
        StringBuilder ctx = new StringBuilder();
        List<ChatSourceDto> sources = new ArrayList<>();
        for (Article a : articles) {
            sources.add(new ChatSourceDto(a.getId(), a.getTitle()));
            ctx.append("---\n标题：").append(a.getTitle()).append("\n摘要：")
                    .append(nullToEmpty(a.getSummary())).append("\n正文片段：\n")
                    .append(truncate(nullToEmpty(a.getContent()), chunk)).append("\n");
        }
        String user = "参考资料：\n" + ctx + "\n用户问题：" + q;
        String answer = aiService.chat(sys, user);
        ChatResponse res = new ChatResponse();
        res.setAnswer(answer);
        res.setSources(sources);
        return res;
    }

    @Override
    public List<RecommendArticleDto> recommend(Long articleId) {
        if (articleId == null) {
            throw new ServiceException(400, "articleId 不能为空");
        }
        Article current = articleMapper.selectById(articleId);
        if (current == null) {
            throw new ServiceException(404, "文章不存在");
        }
        String blob = nullToEmpty(current.getTitle()) + " "
                + nullToEmpty(current.getSummary()) + " "
                + truncate(nullToEmpty(current.getContent()), 800);
        List<String> keywords = KeywordHelper.fromText(blob);
        if (keywords.isEmpty()) {
            return List.of();
        }
        List<Article> found = articleMapper.searchPublishedByKeywords(keywords, articleId, 3);
        if (found == null) {
            return List.of();
        }
        return found.stream().map(a -> toRecommendDto(a, "与当前阅读主题相近")).collect(Collectors.toList());
    }

    @Override
    public List<RecommendArticleDto> recommendWithContext(Long articleId, List<Long> recentArticleIds) {
        Article current = articleId != null ? articleMapper.selectById(articleId) : null;
        if (articleId != null && current == null) {
            throw new ServiceException(404, "文章不存在");
        }
        List<Long> recentIds = recentArticleIds != null ? recentArticleIds : List.of();
        List<Article> recentArticles = loadRecentArticlesPublished(recentIds);
        List<Long> exclude = mergeExcludeIds(articleId, recentIds);
        String blob = keywordBlobFrom(current, recentArticles);
        List<String> keywords = KeywordHelper.fromText(blob);
        List<Article> found = resolveCandidates(keywords, exclude, 9);
        return takeTopWithReasons(found, recentArticles, current);
    }

    @Override
    public List<RecommendArticleDto> recommendHome(List<Long> recentArticleIds) {
        List<Long> recentIds = recentArticleIds != null ? recentArticleIds : List.of();
        List<Article> recentArticles = loadRecentArticlesPublished(recentIds);
        List<Long> exclude = mergeExcludeIds(null, recentIds);
        String blob = keywordBlobFrom(null, recentArticles);
        List<String> keywords = KeywordHelper.fromText(blob);
        List<Article> found = resolveCandidates(keywords, exclude, 9);
        return takeTopWithReasons(found, recentArticles, null);
    }

    private List<Long> mergeExcludeIds(Long articleId, List<Long> recentIds) {
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        if (articleId != null) {
            set.add(articleId);
        }
        if (recentIds != null) {
            for (Long id : recentIds) {
                if (id != null) {
                    set.add(id);
                }
            }
        }
        return new ArrayList<>(set);
    }

    private List<Article> loadRecentArticlesPublished(List<Long> idsOrdered) {
        if (idsOrdered == null || idsOrdered.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniq = new LinkedHashSet<>();
        for (Long id : idsOrdered) {
            if (id != null && uniq.size() < 16) {
                uniq.add(id);
            }
        }
        if (uniq.isEmpty()) {
            return List.of();
        }
        Collection<Article> rows = articleMapper.selectBatchIds(uniq);
        Map<Long, Article> map = rows.stream()
                .collect(
                        Collectors.toMap(
                                Article::getId,
                                a -> a,
                                (x, y) -> x //遇到ID冲突保留第一个
                        )
                );
        List<Article> out = new ArrayList<>();
        for (Long id : uniq) {
            Article a = map.get(id);
            if (a != null && Integer.valueOf(1).equals(a.getStatus())) {
                out.add(a);
            }
        }
        return out;
    }

    private String keywordBlobFrom(Article current, List<Article> recentArticles) {
        StringBuilder sb = new StringBuilder();
        if (current != null) {
            sb.append(nullToEmpty(current.getTitle())).append(' ')
                    .append(nullToEmpty(current.getSummary())).append(' ')
                    .append(truncate(nullToEmpty(current.getContent()), 900)).append(' ');
        }
        for (Article r : recentArticles) {
            sb.append(nullToEmpty(r.getTitle())).append(' ')
                    .append(nullToEmpty(r.getSummary())).append(' ')
                    .append(truncate(nullToEmpty(r.getContent()), 500)).append(' ');
        }
        return sb.toString();
    }

    private List<Article> resolveCandidates(List<String> keywords, List<Long> excludeIds, int limit) {
        List<Article> found;
        if (keywords == null || keywords.isEmpty()) {
            found = articleMapper.selectPublishedExcludeIds(excludeIds, limit);
        } else {
            found = articleMapper.searchPublishedByKeywordsExcludeIds(keywords, excludeIds, limit);
        }
        return found != null ? found : List.of();
    }

    private List<RecommendArticleDto> takeTopWithReasons(List<Article> found,
                                                         List<Article> recentOrdered,
                                                         Article current) {
        List<RecommendArticleDto> out = new ArrayList<>();
        int n = Math.min(3, found.size());
        for (int i = 0; i < n; i++) {
            Article a = found.get(i);
            String reason = buildReason(a, recentOrdered, current);
            out.add(toRecommendDto(a, reason));
        }
        return out;
    }

    private String buildReason(Article candidate, List<Article> recentOrdered, Article current) {
        for (Article r : recentOrdered) {
            if (shareKeywords(candidate, r)) {
                return "与您最近阅读的《" + nullToEmpty(r.getTitle()) + "》主题相近";
            }
        }
        if (current != null && shareKeywords(candidate, current)) {
            return "与当前阅读的《" + nullToEmpty(current.getTitle()) + "》主题相近";
        }
        return "猜你喜欢";
    }

    private boolean shareKeywords(Article a, Article b) {
        List<String> ka = KeywordHelper.fromText(compactBlob(a));
        List<String> kb = KeywordHelper.fromText(compactBlob(b));
        if (ka.isEmpty() || kb.isEmpty()) {
            return false;
        }
        HashSet<String> sa = new HashSet<>(ka);
        for (String x : kb) {
            if (sa.contains(x)) {
                return true;
            }
        }
        return false;
    }

    private String compactBlob(Article a) {
        return nullToEmpty(a.getTitle()) + " " + nullToEmpty(a.getSummary()) + " "
                + truncate(nullToEmpty(a.getContent()), 400);
    }

    private RecommendArticleDto toRecommendDto(Article a, String reason) {
        RecommendArticleDto d = new RecommendArticleDto();
        d.setId(a.getId());
        d.setTitle(a.getTitle());
        d.setSummary(a.getSummary());
        d.setCover(a.getCover());
        d.setCreateTime(a.getCreateTime());
        d.setReason(reason);
        return d;
    }

    @Override
    public String weeklyReport(WeeklyReportRequest request) {
        if (request == null) {
            request = new WeeklyReportRequest();
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDateTime from = weekStart.atStartOfDay();

        List<Article> articles = articleMapper.selectList(new QueryWrapper<Article>()
                .eq("status", 1)
                .ge("create_time", from)
                .orderByDesc("create_time")
                .last("LIMIT 25"));

        if (articles == null || articles.isEmpty()) {
            articles = articleMapper.selectList(new QueryWrapper<Article>()
                    .eq("status", 1)
                    .orderByDesc("create_time")
                    .last("LIMIT 15"));
        }

        if (articles == null || articles.isEmpty()) {
            return "暂无已发布文章，无法生成周报。";
        }

        StringBuilder ctx = new StringBuilder();
        for (Article a : articles) {
            ctx.append("---\n标题：").append(nullToEmpty(a.getTitle()))
                    .append("\n摘要：").append(nullToEmpty(a.getSummary()))
                    .append("\n正文节选：\n")
                    .append(truncate(nullToEmpty(a.getContent()), WEEKLY_SNIPPET))
                    .append("\n发布时间：")
                    .append(a.getCreateTime() != null ? a.getCreateTime().toString() : "")
                    .append("\n");
        }

        String sys = "你是博客主编。根据给出的文章列表撰写周报，使用 Markdown。"
                + "结构建议：本周综述、主要更新（可按主题归纳）、简要数据观感（如篇数、主题分布）、下周写作建议。"
                + "只依据列表中的真实标题与摘要/节选内容归纳，不要编造不存在的文章。";

        String user = "以下为候选文章（优先自然周周一至当前已发布的稿件；若本周无稿则为站点近期已发布文章）：\n"
                + ctx;
        if (StringUtils.hasText(request.getFocus())) {
            user = user + "\n编排侧重点：" + request.getFocus().trim();
        }

        String result = aiService.chat(sys, user);
        persistWeeklyReport(result, weekStart);
        return result;
    }

    private void persistWeeklyReport(String markdown, LocalDate weekStart) {
        if (reportStorageService == null || !StringUtils.hasText(markdown)) {
            return;
        }
        if (markdown.startsWith("暂无")) {
            return;
        }
        try {
            reportStorageService.saveWeeklyReport("周报 " + weekStart, markdown);
        } catch (Exception ignored) {
        }
    }

    private ChatResponse refusalResponse() {
        ChatResponse r = new ChatResponse();
        r.setAnswer(REFUSAL);
        r.setSources(List.of());
        return r;
    }

    private List<String> parseStringList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String s = raw.trim();
        int l = s.indexOf('[');
        int r = s.lastIndexOf(']');
        if (l >= 0 && r > l) {
            s = s.substring(l, r + 1);
        }
        try {
            List<String> list = objectMapper.readValue(s, new TypeReference<List<String>>() {
            });
            if (list == null) {
                return List.of();
            }
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (String item : list) {
                if (StringUtils.hasText(item)) {
                    seen.add(item.trim());
                }
            }
            return new ArrayList<>(seen);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 将 null 转为 ""，保持非 null 字符串原样返回
     *
     * @param s
     * @return
     */
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * 截断字符串到指定长度，优先保留前半部分
     *
     * @param s
     * @param max
     * @return
     */
    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String truncateTail(String s, int max) {
        if (s == null || s.length() <= max) {
            return s == null ? "" : s;
        }
        return s.substring(s.length() - max);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
