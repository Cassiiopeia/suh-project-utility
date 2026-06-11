🗒️ 설명
---

GrassPlanter(깃허브 잔디 자동 커밋) 기능의 UI를 DaisyUI native 기반으로 전면 재설계하고, 프로필별 설정을 화면에서 관리할 수 있게 개선한다. 동시에 현재 화면의 동작 버그를 수정한다.

**현재 상태 (진단 완료)**

- 백엔드(서비스·API 7종)·라우팅(`/grass`)·메뉴 링크·스케줄러는 모두 정상 존재.
- 그러나 `grass_profile` 테이블에 등록된 프로필이 0개 → 자동커밋이 동작할 대상 자체가 없음.
- 프로필을 등록·관리할 UI가 부실하고 버그가 있어 실질적으로 사용 불가 상태.
- 원본 독립 레포(`suh-grass-planter`)는 자체 DB로 매일 정상 동작 중이며, 본 프로젝트는 그 기능을 흡수한 상태(백엔드는 오히려 더 발전 — 스케줄/통계/동적 IV 암호화 보유).

**개편 목표**

1. UI를 DaisyUI native 컴포넌트로 전면 재설계 (card/stat/badge/modal/table/tabs)
2. 프로필 등록·수정·삭제·수동커밋·로그 조회 흐름을 사용하기 쉽게
3. 프로필별 설정(커밋 메시지 템플릿, 목표 잔디 레벨, 대상 레포, 자동커밋 on/off, 일일 목표)을 화면에서 관리
4. 현재 버그 수정

🔄 재현 방법
---

1. `/grass` 페이지 접속 → 등록된 프로필 없음.
2. 프로필 추가 후 수정 시도 → 폼에 기존 값이 채워지지 않음.
3. 커밋 로그의 상태/레벨 표시가 실제 데이터와 불일치.

📸 참고 자료
---

- 페이지: `Suh-Web/src/main/resources/templates/pages/grassPlanter.html`
- 컨트롤러(뷰): `Suh-Web/.../controller/view/PageController.java` (`/grass`)
- 컨트롤러(API): `Suh-Web/.../controller/api/GrassController.java` (생성/수정/삭제/목록/커밋/로그/기여도)
- 서비스: `Suh-Domain-GrassPlanter/.../service/GrassService.java`
- 엔티티: `GrassProfile`, `GrassSchedule`, `GrassCommitLog`

**수정 대상 버그**
- 프로필 수정 시 기존 데이터 미표시 (서버에서 프로필 조회해 폼 채우기 필요)
- 커밋 로그 상태 판정: `httpStatus` → `isSuccess` 사용
- 커밋 레벨 표시: `targetCommitLevel` → `commitLevel` 사용

✅ 예상 동작
---

- `/grass`에서 프로필 추가 → DB에 저장되고 카드로 표시
- 프로필 수정 → 기존 값이 폼에 채워지고 저장됨
- 수동 커밋 실행 → 실제 GitHub 커밋 + 로그에 성공 기록
- 기여도 레벨(0~4) 색상 시각화, 다크/라이트 모드 정상
- 프로필별 설정을 화면에서 변경 가능

⚙️ 환경 정보
---

- **실행 환경**: Synology NAS Docker (`suh-project-utility-green`)
- **런타임**: Spring Boot 3.4.2 / Java 17
- **UI**: Thymeleaf + Tailwind + DaisyUI 5 + jQuery
- **모듈**: `Suh-Domain-GrassPlanter`, `Suh-Web`

🛠️ 해결 방향 (예정)
---

1. `grassPlanter.html`을 DaisyUI native 컴포넌트로 재구성 (인라인 스타일 금지, 표준 Tailwind 클래스)
2. 프로필 수정 데이터 채우기, 상태/레벨 표시 버그 수정
3. 프로필별 설정 입력 폼 정비
4. 실제 PAT로 등록→수정→커밋 전 과정 검증

> 범위 제외(YAGNI): 스케줄 관리 UI, 인증 강화, Redis 캐싱은 추후 별도 진행.

🙋‍♂️ 담당자
---

- **백엔드/프론트엔드**: Cassiiopeia
