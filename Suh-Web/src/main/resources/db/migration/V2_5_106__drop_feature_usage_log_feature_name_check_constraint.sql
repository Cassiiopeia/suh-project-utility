-- feature_usage_log.feature_name 의 CHECK 제약(enum 값 화이트리스트) 제거
-- Hibernate가 테이블 생성 시점 enum 값으로 만든 제약이라 FeatureType 값 추가(PROFILE_VIEW, SEJONG_AUTH, SUH_RANDOM 등) 후 INSERT 거부
-- INSERT 실패는 StatisticsService @Async catch 에 묻혀 통계만 조용히 누락됨. 값 강제는 애플리케이션 enum 이 담당 (V2_5_64 와 같은 방식)

-- 완전 초기화 상태(테이블 없음)도 커버: 테이블이 있을 때만 제약 제거, 제약 이름은 환경별로 동적 조회
DO $$
DECLARE
  constraint_rec RECORD;
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_name = 'feature_usage_log'
  ) THEN
    FOR constraint_rec IN
      SELECT con.conname
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      WHERE rel.relname = 'feature_usage_log'
        AND con.contype = 'c'
        AND pg_get_constraintdef(con.oid) LIKE '%feature_name%'
    LOOP
      EXECUTE 'ALTER TABLE feature_usage_log DROP CONSTRAINT ' || quote_ident(constraint_rec.conname);
    END LOOP;
  END IF;
END $$;
