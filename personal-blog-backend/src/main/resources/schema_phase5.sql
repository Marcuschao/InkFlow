SET NAMES utf8mb4;

ALTER TABLE `article`
  ADD COLUMN `freshness_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0健康 1需更新 2严重过时' AFTER `status`,
  ADD COLUMN `freshness_checked_at` datetime DEFAULT NULL AFTER `freshness_status`,
  ADD COLUMN `seo_title` varchar(255) DEFAULT NULL AFTER `summary`,
  ADD COLUMN `seo_description` varchar(512) DEFAULT NULL AFTER `seo_title`,
  ADD KEY `idx_article_freshness` (`status`,`freshness_status`);

CREATE TABLE IF NOT EXISTS `blog_site_kv` (
  `k` varchar(64) NOT NULL,
  `v` varchar(512) NOT NULL,
  PRIMARY KEY (`k`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `article_translation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `article_id` bigint(20) NOT NULL,
  `locale` varchar(16) NOT NULL,
  `title` varchar(255) NOT NULL,
  `summary` text,
  `content` longtext NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0机翻 1已校对',
  `seo_title` varchar(255) DEFAULT NULL,
  `seo_description` varchar(512) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_locale` (`article_id`,`locale`),
  KEY `idx_at_article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
