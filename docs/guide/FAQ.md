# FAQ (자주 묻는 질문)

> **lab.suhsaechan.kr** 사용 중 궁금한 점이 있으신가요?  
> 여기에서 자주 묻는 질문들을 확인해보세요!

---

## 🔐 로그인 & 계정 관련

### Q1. 회원가입은 어떻게 하나요?

**A:** 이 사이트는 **회원가입이 없습니다**. 

- 관리자가 미리 설정한 계정으로만 로그인할 수 있습니다
- 팀원이라면 관리자에게 계정을 요청하세요
- 일부 기능(번역, 챗봇, 이슈 헬퍼)은 **로그인 없이도 사용 가능**합니다

---

### Q2. 로그인 계정을 받고 싶어요!

**A:** 계정이 필요하시면 관리자에게 문의하세요.

- **Email**: chan4760@gmail.com
- **GitHub Issues**: 프로젝트 저장소에 Issue 등록

계정은 선별적으로 제공됩니다.

---

### Q3. 사용자 권한은 어떻게 나뉘어 있나요?

**A:** 3단계 권한 시스템을 사용합니다.

| 권한 | 설명 | 접근 가능 기능 |
|------|------|-------------|
| **비로그인** | 계정 없이 사용 | 번역, 챗봇, 이슈 헬퍼, 프로필 페이지 |
| **USER** | 일반 사용자 | 모든 도구 + Docker 정보, 모듈 버전 조회 |
| **ADMIN** | 관리자 | USER 권한 + 챗봇 관리, 통계 관리 |
| **SUPER_ADMIN** | 슈퍼 관리자 | 모든 권한 + 공지사항 관리 |

---

### Q4. 로그인 후 어디로 이동하나요?

**A:** 로그인 성공 시 자동으로 `/dashboard` 페이지로 이동합니다.

---

## 🛠️ 기능 사용 관련

### Q5. 로그인 없이 사용할 수 있는 기능은 무엇인가요?

**A:** 다음 기능들은 **인증 없이** 사용 가능합니다:

- ✅ **AI Translator** (`/translator`) - AI 번역
- ✅ **Chatbot** - 대화 및 이력 조회 (스트리밍 포함)
- ✅ **Issue Helper** (`/issue-helper`) - GitHub 이슈 헬퍼
- ✅ **Profile** (`/profile`) - 프로필 페이지

---

### Q6. 각 페이지는 어떻게 접근하나요?

**A:** 주요 페이지 목록입니다:

| 페이지 | URL | 로그인 필요 | 설명 |
|--------|-----|-----------|------|
| **대시보드** | `/dashboard` | ✅ | 메인 화면, 통계 및 공지사항 |
| **프로필** | `/profile` | ❌ | 사용자 프로필 |
| **번역기** | `/translator` | ❌ | AI 번역 도구 |
| **이슈 헬퍼** | `/issue-helper` | ❌ | GitHub 이슈 도우미 |
| **OpenAI 채팅** | `/openai-chat` | ✅ | OpenAI 채팅 |
| **잔디 심기** | `/grass` | ✅ | GitHub Grass Planter |
| **도커 로그** | `/docker-logs` | ✅ | 컨테이너 로그 모니터링 |
| **학습 관리** | `/study-management` | ✅ | 학습 노트 관리 |
| **버스 예약** | `/somansa/bus-reservation` | ✅ | 소만사 버스 예약 |
| **AI 서버** | `/ai-server` | ✅ | AI 서버 관리 |
| **챗봇 관리** | `/chatbot-management` | ✅ ADMIN | 챗봇 문서 관리 |
| **공지사항 관리** | `/notice-management` | ✅ SUPER_ADMIN | 공지사항 관리 |

---

### Q7. Grass Planter는 어떻게 사용하나요?

**A:** GitHub 잔디를 자동으로 채우는 도구입니다.

1. `/grass` 페이지로 이동 (로그인 필요)
2. **프로필 생성**: GitHub 계정, Private Key 설정
3. **자동 커밋 활성화**: 스케줄러가 자동으로 커밋 생성
4. **수동 커밋**: 원할 때 즉시 커밋 가능

⚠️ **주의**: Private Key는 안전하게 관리하세요!

---

### Q8. 챗봇은 어떤 질문에 답할 수 있나요?

**A:** 이 사이트의 문서를 기반으로 답변합니다.

- 사이트 기능 설명
- 사용 방법 안내
- 트러블슈팅 도움
- 기술 스택 정보

**좋은 질문 예시**:
- "번역 기능은 어떻게 사용하나요?"
- "Grass Planter의 Private Key는 어디서 얻나요?"
- "로그인 없이 사용할 수 있는 기능은 뭐가 있나요?"

---

### Q9. Docker Logs는 어떤 컨테이너를 모니터링하나요?

**A:** 실행 중인 모든 Docker 컨테이너를 모니터링할 수 있습니다.

- 기본적으로 `sejong-malsami-back` 컨테이너 로그를 표시
- 컨테이너 목록에서 다른 컨테이너 선택 가능
- 실시간 폴링 방식으로 로그 업데이트

---

## 📡 API 사용 관련

### Q10. API 문서는 어디서 볼 수 있나요?

**A:** Swagger UI에서 확인할 수 있습니다.

- **URL**: `https://lab.suhsaechan.kr/swagger-ui.html`
- 모든 API 엔드포인트와 스키마 확인 가능
- 직접 테스트도 가능

---

### Q11. 외부에서 API를 호출할 수 있나요?

**A:** 네, 가능합니다!

**인증 불필요 API** (누구나 호출 가능):
```bash
# 번역 API
POST https://lab.suhsaechan.kr/api/translate

# 챗봇 API (비스트리밍)
POST https://lab.suhsaechan.kr/api/chatbot/chat

# 챗봇 API (스트리밍)
GET https://lab.suhsaechan.kr/api/chatbot/chat/stream
```

**인증 필요 API**: 
- 로그인 후 세션 쿠키를 포함하여 요청해야 합니다
- CSRF 토큰이 필요한 경우도 있습니다

---

### Q12. API 요청 시 `consumes = MediaType.MULTIPART_FORM_DATA_VALUE`는 무슨 뜻인가요?

**A:** 대부분의 POST API는 `multipart/form-data` 형식을 사용합니다.

```bash
# cURL 예시
curl -X POST https://lab.suhsaechan.kr/api/translate \
  -F "text=Hello World" \
  -F "sourceLang=en" \
  -F "targetLang=ko"
```

---

## 🔧 트러블슈팅

### Q13. 로그인이 안 돼요!

**A:** 다음을 확인해보세요:

1. **계정 정보 확인**: 관리자에게 받은 계정 정보가 맞는지 확인
2. **비밀번호 확인**: 오타가 없는지 확인
3. **브라우저 캐시 삭제**: 쿠키/캐시를 삭제 후 재시도
4. **네트워크 확인**: 서버 연결 상태 확인

그래도 안 되면 관리자에게 문의하세요.

---

### Q14. "403 Forbidden" 에러가 발생해요!

**A:** 권한이 없는 페이지에 접근하려고 할 때 발생합니다.

**해결 방법**:
- 로그인이 필요한 페이지인지 확인
- 현재 계정의 권한 레벨 확인
- 슈퍼 관리자만 접근 가능한 페이지인지 확인

---

### Q15. 챗봇 응답이 느려요.

**A:** 챗봇은 OpenAI API를 사용하기 때문에 응답 시간이 걸릴 수 있습니다.

**개선 방법**:
- **스트리밍 모드 사용**: 실시간으로 응답을 받을 수 있습니다
- **간단한 질문**: 복잡한 질문보다 구체적이고 간단한 질문이 빠릅니다

---

### Q16. 파일 업로드가 안 돼요!

**A:** 파일 크기 제한을 확인해보세요.

- 최대 업로드 크기는 서버 설정에 따라 다릅니다
- 파일 형식이 허용된 형식인지 확인
- 브라우저 콘솔에서 오류 메시지 확인

---

## 🤝 협업 & 공유 관련

### Q17. 팀원들과 어떻게 협업하나요?

**A:** 각 팀원에게 계정을 발급하여 협업합니다.

- **공통 계정 사용 가능**: USER 권한 계정을 여러 명이 공유할 수 있습니다
- **개인별 작업 추적**: 세션별로 IP와 해시값을 저장하여 추적 가능
- **역할 분담**: 관리자 권한을 통해 역할을 나눌 수 있습니다

---

### Q18. 내가 만든 데이터를 팀원이 볼 수 있나요?

**A:** 네, 대부분의 데이터는 공유됩니다.

- **공지사항**: 모든 사용자에게 표시
- **학습 노트**: 로그인한 사용자 모두 조회 가능
- **챗봇 대화 이력**: 세션별로 분리되어 개인 전용
- **Grass Planter 프로필**: 공유되지 않음 (개인 GitHub 계정)

---

### Q19. Somansa Bus 예약은 뭐예요?

**A:** 회사 또는 조직 내부의 버스 예약 시스템입니다.

- 사용자 등록 후 노선별로 예약 가능
- 스케줄 관리 및 자동 예약 기능
- 예약 이력 조회

소만사(SOMANSA) 내부에서 사용하는 기능입니다.

---

## 🧑‍💻 기술적인 질문

### Q20. 이 사이트는 어떤 기술로 만들어졌나요?

**A:** 다음 기술 스택을 사용합니다:

```
Backend    → Spring Boot 3.4 + Java 17
Database   → PostgreSQL + Redis
Frontend   → Thymeleaf + TailwindCSS + DaisyUI
Deploy     → Docker + GitHub Actions + Synology NAS
AI         → OpenAI API (GPT 모델)
Vector DB  → Qdrant (챗봇 문서 검색용)
```

---

### Q21. 소스 코드를 볼 수 있나요?

**A:** GitHub에서 공개되어 있습니다!

- **Repository**: [https://github.com/Cassiiopeia](https://github.com/Cassiiopeia)
- 프로젝트를 Fork하거나 참고할 수 있습니다
- 기여(Contribution)는 환영합니다!

---

### Q22. 로컬에서 실행할 수 있나요?

**A:** 네! Docker를 사용하면 쉽게 실행할 수 있습니다.

```bash
# 클론
git clone [repository-url]

# Gradle 빌드
./gradlew clean build

# Spring Boot 실행
./gradlew bootRun

# 또는 Docker 사용
docker build -t suh-project-utility .
docker run -p 8080:8080 suh-project-utility
```

환경 변수 설정이 필요할 수 있습니다 (Database, OpenAI API Key 등).

---

### Q23. CSRF 토큰은 언제 필요한가요?

**A:** 대부분의 폼 제출에서 필요합니다.

**CSRF 검사 제외되는 API**:
- `/api/issue-helper/create/commit-branch/github-workflow`
- `/api/grass/**` (GrassPlanter 관련 모든 API)

**CSRF 토큰이 필요한 경우**:
- Thymeleaf 템플릿에서 자동으로 포함됩니다: `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>`

---

### Q24. Redis는 어디에 사용되나요?

**A:** 캐싱과 세션 관리에 사용됩니다.

- **대시보드 통계**: 5분 캐싱으로 성능 최적화
- **세션 저장**: 로그인 세션 관리
- **임시 데이터 저장**: 챗봇 대화 이력 등

---

### Q25. 벡터 데이터베이스는 왜 사용하나요?

**A:** 챗봇의 문서 검색(RAG)을 위해 사용합니다.

1. **문서 청크 분할**: 긴 문서를 작은 청크로 분할
2. **벡터 임베딩**: 각 청크를 벡터로 변환
3. **유사도 검색**: 사용자 질문과 유사한 문서 찾기
4. **LLM 응답 생성**: 검색된 문서를 기반으로 답변 생성

---

## 📞 기타

### Q26. 버그를 발견했어요!

**A:** 제보 감사합니다!

**버그 리포트 방법**:
1. GitHub Issues에 등록 (권장)
2. 이메일 전송: chan4760@gmail.com

**포함할 정보**:
- 발생한 페이지/기능
- 재현 방법
- 예상 동작 vs 실제 동작
- 브라우저 정보
- 스크린샷 (가능하면)

---

### Q27. 새로운 기능을 제안하고 싶어요!

**A:** 제안 환영합니다!

- GitHub Issues에 `enhancement` 라벨로 등록
- 이메일로 제안서 전송

**좋은 제안 형식**:
- 왜 필요한가?
- 어떤 문제를 해결하는가?
- 어떻게 동작해야 하는가?

---

### Q28. 이 프로젝트에 기여할 수 있나요?

**A:** 물론입니다!

**기여 방법**:
1. Fork & Clone
2. Feature Branch 생성
3. 코드 작성 & 테스트
4. Pull Request 제출

**기여 가능한 분야**:
- 버그 수정
- 새로운 기능 추가
- 문서 개선
- 테스트 코드 작성
- UI/UX 개선

---

### Q29. 데이터는 안전한가요?

**A:** 네, 다음과 같은 보안 조치를 취하고 있습니다:

- **HTTPS 사용**: 모든 통신 암호화
- **세션 관리**: Spring Security 기반 인증
- **IP 추적**: 비정상 접근 감지
- **CSRF 보호**: 폼 제출 보안
- **환경 변수 분리**: 민감 정보 Git에 노출 방지

다만, GitHub Private Key 등 민감 정보는 **사용자가 직접 관리**해야 합니다.

---

### Q30. 서비스가 중단될 수도 있나요?

**A:** 개인 프로젝트이기 때문에 언제든 중단될 수 있습니다.

> **"Do whatever you want. Just don't blame me if it breaks."**  
> — 프로젝트 라이선스

- 실험적인 프로젝트입니다
- 업무용으로 사용 시 백업을 권장합니다
- 중요한 데이터는 로컬에 저장하세요

---

## 🎉 여전히 궁금한 점이 있으신가요?

더 많은 질문이 있으시면:

- **Email**: chan4760@gmail.com
- **GitHub**: [Issues 탭](https://github.com/Cassiiopeia)
- **Chatbot**: 사이트의 챗봇에게 물어보세요!

---

**Happy Coding! 🚀**

