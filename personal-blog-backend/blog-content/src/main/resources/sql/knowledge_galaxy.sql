CREATE TABLE IF NOT EXISTS tag_relationship (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tag_id_1 BIGINT NOT NULL,
  tag_id_2 BIGINT NOT NULL,
  weight DOUBLE NOT NULL DEFAULT 0,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_pair (tag_id_1, tag_id_2)
);

CREATE TABLE IF NOT EXISTS article_tag_score (
  article_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  score DOUBLE NOT NULL DEFAULT 1.0,
  update_time DATETIME NOT NULL,
  PRIMARY KEY (article_id, tag_id)
);

CREATE TABLE IF NOT EXISTS user_tag_subscription (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL,
  UNIQUE KEY uk_user_tag (user_id, tag_id)
);

CREATE TABLE IF NOT EXISTS learning_path (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  steps_json JSON NOT NULL,
  create_user BIGINT,
  create_time DATETIME NOT NULL
);
