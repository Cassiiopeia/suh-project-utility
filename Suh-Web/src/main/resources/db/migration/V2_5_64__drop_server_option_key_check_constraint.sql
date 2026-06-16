-- server_option.option_key 의 CHECK 제약(enum 값 화이트리스트) 제거
-- @Enumerated(EnumType.STRING) 으로 Hibernate가 생성한 CHECK 제약이 ServerOptionKey enum에
-- 새 값(CHATBOT_RAG_MIN_SCORE, CHATBOT_RAG_TOP_K)을 추가해도 갱신되지 않아 INSERT가 거부되던 문제 해결.
-- 애플리케이션 레벨에서 enum 타입이 값을 강제하므로 DB CHECK 제약은 제거한다(enum 값 추가 시마다 제약을 고칠 필요 제거).

-- 완전 초기화 상태(테이블 없음)도 커버: 테이블이 있을 때만 제약 제거.
-- 제약 이름이 환경마다 다를 수 있어 information_schema로 option_key 관련 CHECK 제약을 동적으로 찾아 DROP.
DO $$
DECLARE
  constraint_rec RECORD;
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_name = 'server_option'
  ) THEN
    FOR constraint_rec IN
      SELECT con.conname
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      WHERE rel.relname = 'server_option'
        AND con.contype = 'c'
        AND pg_get_constraintdef(con.oid) LIKE '%option_key%'
    LOOP
      EXECUTE 'ALTER TABLE server_option DROP CONSTRAINT ' || quote_ident(constraint_rec.conname);
    END LOOP;
  END IF;
END $$;
