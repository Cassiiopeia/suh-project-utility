CREATE TABLE IF NOT EXISTS clipboard_item (
  clipboard_item_id UUID PRIMARY KEY,
  item_type VARCHAR(20) NOT NULL,
  text_content TEXT,
  original_file_name VARCHAR(255),
  stored_file_name VARCHAR(255),
  content_type VARCHAR(255),
  file_size_bytes BIGINT,
  created_date TIMESTAMP NOT NULL,
  updated_date TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_clipboard_item_created_date
  ON clipboard_item (created_date DESC);
