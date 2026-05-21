CREATE TABLE IF NOT EXISTS somansa_bus_scheduler_state (
  somansa_bus_scheduler_state_id UUID PRIMARY KEY,
  next_fire_at TIMESTAMP NOT NULL,
  last_fired_at TIMESTAMP,
  created_date TIMESTAMP NOT NULL,
  updated_date TIMESTAMP NOT NULL
);
