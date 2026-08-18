package com.blog.content.knowledge.service.impl;

import com.blog.content.cache.MetaCacheService;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.config.properties.KnowledgeProperties;
import com.blog.content.knowledge.cache.KnowledgeGraphCacheService;
import com.blog.content.knowledge.model.TagArticleCountDto;
import com.blog.content.knowledge.model.TagCooccurrenceDto;
import com.blog.content.knowledge.model.TagWeightRowDto;
import com.blog.content.knowledge.service.KnowledgeGraphService;
import com.blog.content.mapper.ArticleMapper;
import com.blog.content.mapper.KnowledgeQueryMapper;
import com.blog.content.mapper.TagMapper;
import com.blog.content.mapper.TagRelationshipMapper;
import com.blog.content.mapper.UserTagSubscriptionMapper;
import com.blog.content.model.dto.ArticlePageQuery;
import com.blog.content.model.dto.knowledge.UserLandscapeRequest;
import com.blog.content.model.entity.Article;
import com.blog.content.model.entity.Tag;
import com.blog.content.model.entity.TagRelationship;
import com.blog.content.model.vo.knowledge.HotTagVo;
import com.blog.content.model.vo.knowledge.KnowledgeEdgeVo;
import com.blog.content.model.vo.knowledge.KnowledgeGraphVo;
import com.blog.content.model.vo.knowledge.KnowledgeNodeVo;
import com.blog.content.model.vo.knowledge.LandscapeTagVo;
import com.blog.content.model.vo.knowledge.RelatedTagVo;
import com.blog.content.model.vo.knowledge.TagNodeDetailVo;
import com.blog.content.model.vo.knowledge.UserLandscapeVo;
import com.blog.content.service.ArticleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphServiceImpl.class);

    private final KnowledgeQueryMapper knowledgeQueryMapper;
    private final TagRelationshipMapper tagRelationshipMapper;
    private final TagMapper tagMapper;
    private final ArticleMapper articleMapper;
    private final ArticleService articleService;
    private final UserTagSubscriptionMapper subscriptionMapper;
    private final KnowledgeGraphCacheService cacheService;
    private final KnowledgeProperties properties;
    private final MetaCacheService metaCacheService;

    public KnowledgeGraphServiceImpl(KnowledgeQueryMapper knowledgeQueryMapper,
                                     TagRelationshipMapper tagRelationshipMapper,
                                     TagMapper tagMapper,
                                     ArticleMapper articleMapper,
                                     ArticleService articleService,
                                     UserTagSubscriptionMapper subscriptionMapper,
                                     KnowledgeGraphCacheService cacheService,
                                     KnowledgeProperties properties,
                                     MetaCacheService metaCacheService) {
        this.knowledgeQueryMapper = knowledgeQueryMapper;
        this.tagRelationshipMapper = tagRelationshipMapper;
        this.tagMapper = tagMapper;
        this.articleMapper = articleMapper;
        this.articleService = articleService;
        this.subscriptionMapper = subscriptionMapper;
        this.cacheService = cacheService;
        this.properties = properties;
        this.metaCacheService = metaCacheService;
    }

    @Override
    public KnowledgeGraphVo getGraph() {
        KnowledgeGraphVo cached = cacheService.getGraph();
        if (cached != null && !CollectionUtils.isEmpty(cached.getNodes())) {
            return cached;
        }
        rebuildGraph();
        cached = cacheService.getGraph();
        if (cached != null) {
            return cached;
        }
        return buildGraphFromDb(false);
    }

    @Override
    public TagNodeDetailVo getNodeDetail(Long tagId, int articleLimit, Long viewerUserId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new ServiceException(404, "标签不存在");
        }
        TagNodeDetailVo vo = new TagNodeDetailVo();
        vo.setTagId(tagId);
        vo.setName(tag.getName());
        ArticlePageQuery q = new ArticlePageQuery();
        q.setTagId(tagId);
        q.setPage(1);
        q.setSize(Math.max(1, Math.min(articleLimit, 20)));
        IPage<Article> page = articleService.getArticlePage(q);
        vo.setArticles(page.getRecords());
        vo.setArticleCount((int) page.getTotal());
        vo.setRelatedTags(knowledgeQueryMapper.selectRelatedTags(tagId, 5));
        vo.setHotAuthors(knowledgeQueryMapper.selectHotAuthorsByTag(tagId, 5));
        if (viewerUserId != null) {
            Long cnt = subscriptionMapper.selectCount(new QueryWrapper<com.blog.content.model.entity.UserTagSubscription>()
                    .eq("user_id", viewerUserId).eq("tag_id", tagId));
            vo.setSubscribed(cnt != null && cnt > 0);
        } else {
            vo.setSubscribed(false);
        }
        return vo;
    }

    @Override
    public KnowledgeGraphVo getSubgraph(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException(404, "文章不存在");
        }
        List<Long> tagIds = articleMapper.selectTagIdsByArticleId(articleId);
        if (CollectionUtils.isEmpty(tagIds)) {
            return emptyGraph();
        }
        KnowledgeGraphVo full = getGraph();
        Set<String> nodeIds = new HashSet<>();
        List<KnowledgeNodeVo> nodes = new ArrayList<>();
        List<KnowledgeEdgeVo> edges = new ArrayList<>();

        String articleNodeId = nodeId("article", articleId);
        KnowledgeNodeVo articleNode = new KnowledgeNodeVo();
        articleNode.setId(articleNodeId);
        articleNode.setType("article");
        articleNode.setLabel(article.getTitle());
        articleNode.setRefId(articleId);
        nodes.add(articleNode);
        nodeIds.add(articleNodeId);

        if (article.getAuthorId() != null) {
            String authorNodeId = nodeId("author", article.getAuthorId());
            if (!nodeIds.contains(authorNodeId)) {
                KnowledgeNodeVo authorNode = new KnowledgeNodeVo();
                authorNode.setId(authorNodeId);
                authorNode.setType("author");
                authorNode.setLabel("作者" + article.getAuthorId());
                authorNode.setRefId(article.getAuthorId());
                nodes.add(authorNode);
                nodeIds.add(authorNodeId);
            }
            KnowledgeEdgeVo ae = new KnowledgeEdgeVo();
            ae.setSource(articleNodeId);
            ae.setTarget(nodeId("author", article.getAuthorId()));
            ae.setWeight(1.0);
            ae.setType("article-author");
            edges.add(ae);
        }

        Set<Long> expandedTags = new HashSet<>(tagIds);
        for (RelatedTagVo rt : knowledgeQueryMapper.selectRelatedTags(tagIds.get(0), 3)) {
            expandedTags.add(rt.getTagId());
        }

        if (full.getNodes() != null) {
            for (KnowledgeNodeVo n : full.getNodes()) {
                if (n.getRefId() != null && expandedTags.contains(n.getRefId()) && "tag".equals(n.getType())) {
                    if (!nodeIds.contains(n.getId())) {
                        nodes.add(n);
                        nodeIds.add(n.getId());
                    }
                }
            }
        } else {
            for (Long tid : expandedTags) {
                Tag t = tagMapper.selectById(tid);
                if (t == null) {
                    continue;
                }
                String nid = nodeId("tag", tid);
                KnowledgeNodeVo nv = new KnowledgeNodeVo();
                nv.setId(nid);
                nv.setType("tag");
                nv.setLabel(t.getName());
                nv.setRefId(tid);
                nodes.add(nv);
                nodeIds.add(nid);
            }
        }

        for (Long tid : tagIds) {
            String tidStr = nodeId("tag", tid);
            if (nodeIds.contains(tidStr)) {
                KnowledgeEdgeVo e = new KnowledgeEdgeVo();
                e.setSource(articleNodeId);
                e.setTarget(tidStr);
                e.setWeight(1.0);
                e.setType("article-tag");
                edges.add(e);
            }
        }

        if (full.getEdges() != null) {
            for (KnowledgeEdgeVo e : full.getEdges()) {
                if (nodeIds.contains(e.getSource()) && nodeIds.contains(e.getTarget())) {
                    edges.add(e);
                }
            }
        }

        if (nodes.size() > properties.getSubgraphMaxNodes()) {
            nodes = nodes.subList(0, properties.getSubgraphMaxNodes());
            Set<String> kept = nodes.stream().map(KnowledgeNodeVo::getId).collect(Collectors.toSet());
            edges = edges.stream()
                    .filter(e -> kept.contains(e.getSource()) && kept.contains(e.getTarget()))
                    .collect(Collectors.toList());
        }

        KnowledgeGraphVo result = new KnowledgeGraphVo();
        result.setNodes(nodes);
        result.setEdges(edges);
        return result;
    }

    @Override
    public List<HotTagVo> getHotTags(int limit) {
        int lim = Math.max(1, Math.min(limit, 50));
        List<Long> hotIds = cacheService.getHotTagIds(lim);
        if (hotIds.isEmpty()) {
            refreshHotTags();
            hotIds = cacheService.getHotTagIds(lim);
        }
        if (hotIds.isEmpty()) {
            return knowledgeQueryMapper.selectTagArticleCounts().stream().limit(lim).map(t -> {
                HotTagVo vo = new HotTagVo();
                vo.setTagId(t.getTagId());
                vo.setName(t.getName());
                vo.setScore(t.getArticleCount() != null ? t.getArticleCount().doubleValue() : 0.0);
                return vo;
            }).collect(Collectors.toList());
        }
        List<HotTagVo> result = new ArrayList<>();
        for (Long tagId : hotIds) {
            Tag tag = tagMapper.selectById(tagId);
            if (tag == null) {
                continue;
            }
            HotTagVo vo = new HotTagVo();
            vo.setTagId(tagId);
            vo.setName(tag.getName());
            result.add(vo);
        }
        return result;
    }

    @Override
    public UserLandscapeVo getUserLandscape(Long userId, UserLandscapeRequest request) {
        Map<Long, LandscapeTagVo> merged = new LinkedHashMap<>();
        mergeLandscape(merged, knowledgeQueryMapper.selectWriteTagWeights(userId), "write");
        mergeLandscape(merged, knowledgeQueryMapper.selectFavoriteTagWeights(userId), "favorite");
        if (request != null && !CollectionUtils.isEmpty(request.getRecentArticleIds())) {
            mergeLandscape(merged, knowledgeQueryMapper.selectReadTagWeights(request.getRecentArticleIds()), "read");
        }
        UserLandscapeVo vo = new UserLandscapeVo();
        List<LandscapeTagVo> list = new ArrayList<>(merged.values());
        list.sort(Comparator.comparing(LandscapeTagVo::getWeight).reversed());
        vo.setNodes(list);
        vo.setTotalArticles(list.size());
        return vo;
    }

    @Override
    public void rebuildGraph() {
        try {
            List<TagCooccurrenceDto> pairs = knowledgeQueryMapper.selectTagCooccurrences();
            try {
                tagRelationshipMapper.deleteAll();
            } catch (Exception ex) {
                log.warn("[knowledge] tag_relationship table unavailable, skip persist: {}", ex.getMessage());
            }
            LocalDateTime now = LocalDateTime.now();
            double lambda = properties.getDecayLambda();
            for (TagCooccurrenceDto p : pairs) {
                double days = p.getAvgDays() != null ? p.getAvgDays() : 0;
                double count = p.getCooccurCount() != null ? p.getCooccurCount() : 0;
                double weight = count * Math.exp(-lambda * days);
                if (weight <= 0) {
                    continue;
                }
                try {
                    TagRelationship tr = new TagRelationship();
                    tr.setTagId1(p.getTagId1());
                    tr.setTagId2(p.getTagId2());
                    tr.setWeight(weight);
                    tr.setUpdateTime(now);
                    tagRelationshipMapper.insert(tr);
                } catch (Exception ex) {
                    log.debug("[knowledge] skip relationship insert: {}", ex.getMessage());
                    break;
                }
            }
            KnowledgeGraphVo graph = buildGraphFromDb(true);
            cacheService.putGraph(graph);
            metaCacheService.evictTags();
            log.info("[knowledge] graph rebuilt nodes={} edges={}",
                    graph.getNodes() != null ? graph.getNodes().size() : 0,
                    graph.getEdges() != null ? graph.getEdges().size() : 0);
        } catch (Exception ex) {
            log.error("[knowledge] rebuildGraph failed: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void refreshHotTags() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Map<Long, Double> scores = new HashMap<>();
        for (TagWeightRowDto row : knowledgeQueryMapper.selectTagViewScores(since)) {
            if (row.getTagId() != null) {
                scores.merge(row.getTagId(), row.getWeight() != null ? row.getWeight() * 0.6 : 0, Double::sum);
            }
        }
        List<TagArticleCountDto> tags = knowledgeQueryMapper.selectTagArticleCounts();
        for (TagArticleCountDto t : tags) {
            double search = cacheService.getSearchTagScore(t.getTagId());
            scores.merge(t.getTagId(), search * 0.4, Double::sum);
        }
        List<ZSetOperations.TypedTuple<String>> tuples = new ArrayList<>();
        for (Map.Entry<Long, Double> e : scores.entrySet()) {
            tuples.add(ZSetOperations.TypedTuple.of(String.valueOf(e.getKey()), e.getValue()));
        }
        cacheService.replaceHotTags(tuples);
    }

    private KnowledgeGraphVo buildGraphFromDb(boolean includeArticleAuthor) {
        List<TagArticleCountDto> tagCounts = knowledgeQueryMapper.selectTagArticleCounts();
        List<KnowledgeNodeVo> nodes = new ArrayList<>();
        int maxCount = tagCounts.stream()
                .map(TagArticleCountDto::getArticleCount)
                .filter(c -> c != null && c > 0)
                .max(Integer::compareTo)
                .orElse(1);

        for (TagArticleCountDto t : tagCounts) {
            KnowledgeNodeVo n = new KnowledgeNodeVo();
            n.setId(nodeId("tag", t.getTagId()));
            n.setType("tag");
            n.setLabel(t.getName());
            n.setRefId(t.getTagId());
            n.setArticleCount(t.getArticleCount());
            n.setWeight(t.getArticleCount() != null ? (double) t.getArticleCount() / maxCount : 0.1);
            nodes.add(n);
        }

        if (includeArticleAuthor) {
            for (TagArticleCountDto a : knowledgeQueryMapper.selectTopArticles(properties.getTopArticleNodes())) {
                KnowledgeNodeVo n = new KnowledgeNodeVo();
                n.setId(nodeId("article", a.getTagId()));
                n.setType("article");
                n.setLabel(a.getName());
                n.setRefId(a.getTagId());
                n.setArticleCount(a.getArticleCount());
                n.setWeight(0.5);
                nodes.add(n);
            }
            for (TagArticleCountDto au : knowledgeQueryMapper.selectTopAuthors(properties.getTopAuthorNodes())) {
                KnowledgeNodeVo n = new KnowledgeNodeVo();
                n.setId(nodeId("author", au.getTagId()));
                n.setType("author");
                n.setLabel("作者" + au.getTagId());
                n.setRefId(au.getTagId());
                n.setArticleCount(au.getArticleCount());
                n.setWeight(0.5);
                nodes.add(n);
            }
        }

        List<TagRelationship> rels;
        try {
            rels = tagRelationshipMapper.selectList(
                    new QueryWrapper<TagRelationship>().orderByDesc("weight").last("LIMIT " + properties.getMaxEdges()));
        } catch (Exception ex) {
            log.warn("[knowledge] load relationships failed: {}", ex.getMessage());
            rels = List.of();
        }
        List<KnowledgeEdgeVo> edges = new ArrayList<>();
        for (TagRelationship tr : rels) {
            KnowledgeEdgeVo e = new KnowledgeEdgeVo();
            e.setSource(nodeId("tag", tr.getTagId1()));
            e.setTarget(nodeId("tag", tr.getTagId2()));
            e.setWeight(tr.getWeight());
            e.setType("tag-tag");
            edges.add(e);
        }

        KnowledgeGraphVo graph = new KnowledgeGraphVo();
        graph.setNodes(nodes);
        graph.setEdges(edges);
        return graph;
    }

    private void mergeLandscape(Map<Long, LandscapeTagVo> merged, List<TagWeightRowDto> rows, String source) {
        if (rows == null) {
            return;
        }
        for (TagWeightRowDto row : rows) {
            LandscapeTagVo vo = merged.computeIfAbsent(row.getTagId(), id -> {
                LandscapeTagVo t = new LandscapeTagVo();
                t.setTagId(row.getTagId());
                t.setName(row.getName());
                t.setWeight(0.0);
                t.setSource(source);
                return t;
            });
            vo.setWeight(vo.getWeight() + (row.getWeight() != null ? row.getWeight() : 0));
            if (!source.equals(vo.getSource())) {
                vo.setSource("mixed");
            }
        }
    }

    private static String nodeId(String type, Long id) {
        return type + ":" + id;
    }

    private static KnowledgeGraphVo emptyGraph() {
        KnowledgeGraphVo g = new KnowledgeGraphVo();
        g.setNodes(List.of());
        g.setEdges(List.of());
        return g;
    }
}
