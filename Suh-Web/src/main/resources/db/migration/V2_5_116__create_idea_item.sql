CREATE TABLE IF NOT EXISTS idea_item (
  idea_item_id UUID PRIMARY KEY,
  keyword VARCHAR(200) NOT NULL,
  memo TEXT,
  idea_status VARCHAR(20) NOT NULL,
  created_date TIMESTAMP NOT NULL,
  updated_date TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_idea_item_created_date
  ON idea_item (created_date DESC);
