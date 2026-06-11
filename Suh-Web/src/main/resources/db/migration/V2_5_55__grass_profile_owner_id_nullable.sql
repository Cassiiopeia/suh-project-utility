-- grass_profile.owner_id 를 nullable 로 완화
-- 시스템/개인 도구 특성상 소유자 ID가 필수가 아니며, NOT NULL 제약 때문에 프로필 생성이 실패하던 문제 해결

-- 완전 초기화 상태(테이블 없음)도 커버: 테이블이 있을 때만 제약 변경
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'grass_profile' AND column_name = 'owner_id'
  ) THEN
    ALTER TABLE grass_profile ALTER COLUMN owner_id DROP NOT NULL;
  END IF;
END $$;
