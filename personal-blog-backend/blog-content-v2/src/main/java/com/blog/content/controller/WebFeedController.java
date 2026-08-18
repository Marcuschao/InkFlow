package com.blog.content.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.model.entity.Article;
import com.blog.content.service.ArticleService;
import com.blog.content.service.BlogSiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@RestController
public class WebFeedController {

    private static final ZoneId GMT = ZoneId.of("GMT");
    private static final DateTimeFormatter RFC822 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).withZone(GMT);

    @Autowired
    private ArticleService articleService;
    @Autowired
    private BlogSiteService blogSiteService;

    private static String xmlEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String baseUrl() {
        String u = blogSiteService.getSiteUrl();
        if (u.endsWith("/")) {
            return u.substring(0, u.length() - 1);
        }
        return u;
    }

    @GetMapping(value = "/rss.xml", produces = "application/rss+xml;charset=UTF-8")
    public ResponseEntity<String> rss() {
        List<Article> items = articleService.list(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getCreateTime)
                .last("LIMIT 20"));
        String title = xmlEscape(blogSiteService.getSiteTitle());
        String desc = xmlEscape(blogSiteService.getSiteDescription());
        String link = xmlEscape(baseUrl());
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\"><channel>");
        sb.append("<title>").append(title).append("</title>");
        sb.append("<link>").append(link).append("</link>");
        sb.append("<description>").append(desc).append("</description>");
        sb.append("<language>zh-cn</language>");
        for (Article a : items) {
            String url = baseUrl() + "/article/" + a.getId();
            sb.append("<item>");
            sb.append("<title>").append(xmlEscape(a.getTitle())).append("</title>");
            sb.append("<link>").append(xmlEscape(url)).append("</link>");
            sb.append("<guid>").append(xmlEscape(url)).append("</guid>");
            ZonedDateTime zdt = null;
            if (a.getCreateTime() != null) {
                zdt = a.getCreateTime().atZone(ZoneId.systemDefault()).withZoneSameInstant(GMT);
            }
            if (zdt != null) {
                sb.append("<pubDate>").append(RFC822.format(zdt)).append("</pubDate>");
            }
            String sum = a.getSummary();
            if (sum != null && !sum.isEmpty()) {
                sb.append("<description>").append(xmlEscape(sum)).append("</description>");
            }
            sb.append("</item>");
        }
        sb.append("</channel></rss>");
        return ResponseEntity.ok(sb.toString());
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> sitemap() {
        List<Article> items = articleService.list(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getUpdateTime));
        String base = baseUrl();
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        sb.append("<url><loc>").append(xmlEscape(base + "/")).append("</loc><changefreq>daily</changefreq></url>");
        for (Article a : items) {
            String loc = base + "/article/" + a.getId();
            sb.append("<url><loc>").append(xmlEscape(loc)).append("</loc>");
            Instant lm = null;
            if (a.getUpdateTime() != null) {
                lm = a.getUpdateTime().atZone(ZoneId.systemDefault()).toInstant();
            } else if (a.getCreateTime() != null) {
                lm = a.getCreateTime().atZone(ZoneId.systemDefault()).toInstant();
            }
            if (lm != null) {
                sb.append("<lastmod>").append(lm.toString()).append("</lastmod>");
            }
            sb.append("</url>");
        }
        sb.append("</urlset>");
        return ResponseEntity.ok(sb.toString());
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> robots() {
        String base = baseUrl();
        String body = "User-agent: *\nAllow: /\nSitemap: " + base + "/sitemap.xml\n";
        return ResponseEntity.ok(body);
    }
}
