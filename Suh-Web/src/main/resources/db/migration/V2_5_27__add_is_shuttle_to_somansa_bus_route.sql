-- somansa_bus_route 테이블이 없는 경우 초기 생성
CREATE TABLE IF NOT EXISTS somansa_bus_route (
    somansa_bus_route_id UUID NOT NULL,
    created_date         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_date         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    description          VARCHAR(255) NOT NULL,
    disptid              INTEGER NOT NULL,
    caralias             VARCHAR(255) NOT NULL,
    departure_time       VARCHAR(255),
    station              VARCHAR(255),
    bus_number           INTEGER,
    is_shuttle           BOOLEAN NOT NULL DEFAULT false,
    is_active            BOOLEAN NOT NULL,
    CONSTRAINT pk_somansa_bus_route PRIMARY KEY (somansa_bus_route_id)
);

-- 테이블이 이미 존재하는 경우 is_shuttle 컬럼만 추가
ALTER TABLE somansa_bus_route ADD COLUMN IF NOT EXISTS is_shuttle BOOLEAN NOT NULL DEFAULT false;
