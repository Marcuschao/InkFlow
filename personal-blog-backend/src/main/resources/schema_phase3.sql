SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `page_view_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `page_type` varchar(32) NOT NULL,
  `article_id` bigint(20) DEFAULT NULL,
  `visitor_id` varchar(64) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pve_created` (`created_at`),
  KEY `idx_pve_visitor_created` (`visitor_id`,`created_at`),
  KEY `idx_pve_article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ai_call_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `feature` varchar(64) NOT NULL,
  `success` tinyint(4) NOT NULL,
  `duration_ms` bigint(20) NOT NULL,
  `username` varchar(128) DEFAULT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_acl_created` (`created_at`),
  KEY `idx_acl_feature` (`feature`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `stat_daily` (
  `stat_date` date NOT NULL,
  `pv_count` bigint(20) NOT NULL DEFAULT '0',
  `uv_count` bigint(20) NOT NULL DEFAULT '0',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
