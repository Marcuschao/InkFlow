ALTER TABLE user ADD COLUMN points INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS badge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(256) NOT NULL,
    icon_url VARCHAR(512) DEFAULT NULL,
    trigger_condition VARCHAR(256) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_badge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    badge_id BIGINT NOT NULL,
    awarded_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_badge (user_id, badge_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS profile_visitor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    profile_owner_id BIGINT NOT NULL,
    visitor_user_id BIGINT NOT NULL,
    visit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_owner_time (profile_owner_id, visit_time),
    KEY idx_visitor (visitor_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS points_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    points_change INT NOT NULL,
    reason VARCHAR(128) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO badge (name, description, icon_url, trigger_condition) VALUES
('新手作家', '发布第一篇文章', '/icons/badge-first-article.svg', '{"type":"FIRST_ARTICLE","threshold":1}'),
('笔耕不辍', '累计发布 10 篇文章', '/icons/badge-article-10.svg', '{"type":"ARTICLE_COUNT","threshold":10}'),
('人气之星', '单篇文章阅读量破 1000', '/icons/badge-views-1000.svg', '{"type":"SINGLE_ARTICLE_VIEWS","threshold":1000}'),
('社交达人', '获得 50 个粉丝', '/icons/badge-followers-50.svg', '{"type":"FOLLOWER_COUNT","threshold":50}'),
('火眼金睛', '累计评论 100 次', '/icons/badge-comment-100.svg', '{"type":"COMMENT_COUNT","threshold":100}'),
('签到达人', '连续签到 7 天', '/icons/badge-sign-streak-7.svg', '{"type":"SIGN_STREAK","threshold":7}'),
('签到老手', '累计签到 30 天', '/icons/badge-sign-total-30.svg', '{"type":"SIGN_TOTAL","threshold":30}'),
('积分大户', '积分突破 1000', '/icons/badge-points-1000.svg', '{"type":"POINTS_TOTAL","threshold":1000}');
