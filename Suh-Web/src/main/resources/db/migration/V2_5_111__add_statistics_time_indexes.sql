CREATE TABLE IF NOT EXISTS page_visit_log (
  page_visit_log_id UUID PRIMARY KEY,
  page_path VARCHAR(255) NOT NULL,
  client_hash VARCHAR(255),
  user_ip VARCHAR(255),
  user_agent VARCHAR(255),
  referrer VARCHAR(255),
  visited_at TIMESTAMP NOT NULL,
  is_bot BOOLEAN NOT NULL,
  created_date TIMESTAMP NOT NULL,
  updated_date TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS feature_usage_log (
  feature_usage_log_id UUID PRIMARY KEY,
  feature_name VARCHAR(255) NOT NULL,
  client_hash VARCHAR(255),
  used_at TIMESTAMP NOT NULL,
  additional_info TEXT,
  created_date TIMESTAMP NOT NULL,
  updated_date TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_page_visit_log_visited_at
  ON page_visit_log (visited_at);

CREATE INDEX IF NOT EXISTS idx_feature_usage_log_used_at
  ON feature_usage_log (used_at);
