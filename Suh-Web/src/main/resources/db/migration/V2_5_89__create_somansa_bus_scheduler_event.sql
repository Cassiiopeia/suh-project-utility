CREATE TABLE IF NOT EXISTS somansa_bus_scheduler_event (
  somansa_bus_scheduler_event_id UUID PRIMARY KEY,
  event_type VARCHAR(50) NOT NULL,
  target_date DATE,
  message TEXT,
  occurred_at TIMESTAMP NOT NULL,
  created_date TIMESTAMP NOT NULL,
  updated_date TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_somansa_bus_scheduler_event_occurred_at
  ON somansa_bus_scheduler_event (occurred_at DESC);
