-- =====================================================
-- 챗봇 RAG 문서 초기 데이터 삽입
-- 실행 방법: psql -d suh_project_utility -f init_documents.sql
-- 또는 DBeaver/pgAdmin에서 직접 실행
-- =====================================================

-- 기존 데이터 삭제 (선택적)
-- TRUNCATE TABLE chat_document_chunk CASCADE;
-- TRUNCATE TABLE chat_document CASCADE;

-- =====================================================
-- 1. 사이트 소개 문서
-- =====================================================
INSERT INTO chat_document (
    chat_document_id,
    title,
    category,
    content,
    description,
    is_active,
    is_processed,
    chunk_count,
    order_index,
    created_date,
    updated_date
) VALUES (
    gen_random_uuid(),
    '새찬 서버 실험실 소개',
    'site-info',
    '# 새찬 서버 실험실 (SAECHAN LAB)



## 주요 기능

### Dev Tools (개발 도구)
- **GitHub 이슈 도우미**: GitHub 이슈 기반 브랜치명, 커밋 메시지 자동 생성
- **Docker 컨테이너 로그**: 실시간 로그 스트리밍 (SSE 기반)
- **AI 서버 관리**: Ollama 기반 LLM 서버 상태 모니터링
- **AI 번역기**: 커스텀 API 기반 다국어 번역 서비스
- **스터디 플랜**: 마크다운 지원 스터디 노트 관리
- **Grass Planter**: GitHub 자동 커밋 스케줄러
- **챗봇 문서 관리**: RAG 챗봇 지식 문서 관리 (Qdrant 벡터DB)

### AI Assistants (커스텀 GPT)
- Swagger 문서 생성기
- TypeScript API 변환기
- 변수명 전문가
- CM Tag Creator (소만사)
- CM Branch Creator (소만사)

## 기술 스택
- Backend: Spring Boot 3.4.2, Java 17
- Database: PostgreSQL, Redis
- Vector DB: Qdrant
- AI: Ollama (embedding-gemma, granite4:1b-h)
- Frontend: Thymeleaf, Tailwind CSS, DaisyUI

## 접속 정보
- URL: https://suh.suhsaechan.kr
- AI 서버: https://ai.suhsaechan.kr',
    '새찬 서버 실험실 사이트 전체 소개',
    true,
    false,
    0,
    1,
    NOW(),
    NOW()
);

-- =====================================================
-- 2. FAQ 문서
-- =====================================================
INSERT INTO chat_document (
    chat_document_id,
    title,
    category,
    content,
    description,
    is_active,
    is_processed,
    chunk_count,
    order_index,
    created_date,
    updated_date
) VALUES (
    gen_random_uuid(),
    '자주 묻는 질문 (FAQ)',
    'faq',
    '# 자주 묻는 질문 (FAQ)

## 로그인 관련

### Q: 로그인은 어떻게 하나요?
A: 메인 페이지 우측 상단의 로그인 버튼을 클릭하여 관리자 계정으로 로그인할 수 있습니다.

### Q: 비밀번호를 잊어버렸어요.
A: 관리자에게 문의하여 비밀번호 초기화를 요청하세요.

## 기능 관련

### Q: GitHub 이슈 도우미는 어떤 기능인가요?
A: GitHub 저장소의 이슈 URL을 입력하면, 해당 이슈 정보를 파싱하여 브랜치명과 커밋 메시지를 자동으로 생성해주는 도구입니다. 소만사 컨벤션에 맞게 형식을 지정할 수 있습니다.

### Q: Docker 로그는 실시간인가요?
A: 네, SSE(Server-Sent Events) 기반으로 실시간 로그 스트리밍을 지원합니다. 원격 서버에 SSH로 연결하여 Docker 로그를 가져옵니다.

### Q: AI 번역기는 어떤 모델을 사용하나요?
A: 커스텀 AI 서버(ai.suhsaechan.kr)의 Ollama 기반 LLM을 사용합니다.

### Q: 스터디 플랜에서 마크다운을 지원하나요?
A: 네, GitHub Flavored Markdown을 완벽하게 지원합니다. 코드 하이라이팅, 테이블, 이미지 등 모든 마크다운 문법을 사용할 수 있습니다.

## 트러블슈팅

### Q: AI 서버 상태가 비정상으로 표시되면?
A: AI 서버(ai.suhsaechan.kr)의 Ollama 서비스 상태를 확인해주세요. 서버가 재시작되었거나 네트워크 문제일 수 있습니다.

### Q: 챗봇이 답변을 못하면?
A: 챗봇은 등록된 문서를 기반으로 답변합니다. 관련 문서가 없거나, 문서가 벡터화되지 않았을 수 있습니다. 챗봇 문서 관리 페이지에서 문서를 추가하거나 재처리해주세요.

### Q: 페이지 로딩이 느려요.
A: 브라우저 캐시를 삭제하고 다시 시도해보세요. 지속적인 문제는 관리자에게 문의하세요.',
    '사이트 사용 관련 자주 묻는 질문',
    true,
    false,
    0,
    2,
    NOW(),
    NOW()
);

-- =====================================================
-- 3. 프로젝트 정보 문서
-- =====================================================
INSERT INTO chat_document (
    chat_document_id,
    title,
    category,
    content,
    description,
    is_active,
    is_processed,
    chunk_count,
    order_index,
    created_date,
    updated_date
) VALUES (
    gen_random_uuid(),
    '진행 중인 프로젝트',
    'project',
    '# 진행 중인 프로젝트

## 1. 세종말싸미 (SEJONG-MALSAMI)
세종대학교 관련 커뮤니티 및 학습 플랫폼 프로젝트입니다.

### 링크
- 메인 페이지: https://test.sejong-malsami.co.kr
- API 문서 (Swagger): https://api.sejong-malsami.co.kr/docs/swagger-ui/index.html
- 관리자 페이지: https://api.sejong-malsami.co.kr

### 기술 스택
- Frontend: React, TypeScript
- Backend: Spring Boot
- Database: PostgreSQL

## 2. 롬롬 (ROM ROM)
중고 물품 거래 모바일 앱 프로젝트입니다.

### 링크
- APK 다운로드: http://suh-project.synology.me/romrom
- HTTPS API 문서: https://api.romrom.xyz/docs/swagger-ui/index.html
- HTTP API 문서: http://suh-project.synology.me:8085/docs/swagger-ui/index.html
- Figma 디자인: https://www.figma.com/design/dq4HUUqmTlVkmWr4cd2RE5

### 기술 스택
- Mobile: Flutter
- Backend: Spring Boot
- Database: PostgreSQL

## 3. PLANE ACCIDENT FINDER
비행기 사고 정보 조회 모바일 앱입니다.

### 링크
- APK 다운로드: http://suh-project.synology.me/plane-accident-finder/
- HTTPS API 문서: https://api.romrom.xyz/docs/swagger-ui/index.html
- HTTP API 문서: http://suh-project.synology.me:8082/docs/swagger-ui/index.html

### 기술 스택
- Mobile: Flutter
- Backend: Spring Boot',
    '현재 진행 중인 프로젝트 목록 및 링크',
    true,
    false,
    0,
    3,
    NOW(),
    NOW()
);

-- =====================================================
-- 4. 기능 안내 - GitHub 이슈 도우미
-- =====================================================
INSERT INTO chat_document (
    chat_document_id,
    title,
    category,
    content,
    description,
    is_active,
    is_processed,
    chunk_count,
    order_index,
    created_date,
    updated_date
) VALUES (
    gen_random_uuid(),
    'GitHub 이슈 도우미 사용 가이드',
    'features',
    '# GitHub 이슈 도우미 사용 가이드

GitHub 이슈 도우미는 이슈 URL을 입력하면 브랜치명과 커밋 메시지를 자동 생성해주는 도구입니다.

## 사용 방법

### 1단계: 이슈 URL 입력
GitHub 이슈 페이지의 URL을 복사하여 입력창에 붙여넣습니다.
예: `https://github.com/Cassiiopeia/suh-project-utility/issues/107`

### 2단계: 파싱 버튼 클릭
"파싱" 버튼을 클릭하면 이슈 정보를 자동으로 가져옵니다.

### 3단계: 결과 확인 및 복사
- 브랜치명: `feature/SUH-107-rag-chatbot-integration`
- 커밋 메시지: `SUH-Project-Utility 내부 RAG 기반 챗봇 도우미 기능 개발 필요`

## 지원 형식

### 브랜치명 형식
- feature/{이슈번호}-{간략한설명}
- fix/{이슈번호}-{간략한설명}
- hotfix/{이슈번호}-{간략한설명}

### 커밋 메시지 형식
소만사 컨벤션에 맞춘 형식으로 자동 생성됩니다.

## 팁
- 이슈 제목이 한글이어도 자동으로 영문 변환됩니다.
- 긴 제목은 적절히 축약됩니다.
- 생성된 내용은 클릭 한 번으로 클립보드에 복사됩니다.',
    'GitHub 이슈 도우미 기능 사용법',
    true,
    false,
    0,
    4,
    NOW(),
    NOW()
);

-- =====================================================
-- 5. 기능 안내 - 챗봇 사용법
-- =====================================================
INSERT INTO chat_document (
    chat_document_id,
    title,
    category,
    content,
    description,
    is_active,
    is_processed,
    chunk_count,
    order_index,
    created_date,
    updated_date
) VALUES (
    gen_random_uuid(),
    '챗봇 사용 가이드',
    'guide',
    '# RAG 챗봇 사용 가이드

이 챗봇은 새찬 서버 실험실에 대한 정보를 제공하는 AI 어시스턴트입니다.

## 챗봇 특징
- RAG(Retrieval-Augmented Generation) 기반
- 등록된 문서를 검색하여 정확한 정보 제공
- 실시간 스트리밍 응답

## 사용 방법

### 챗봇 열기
화면 우측 하단의 챗봇 아이콘(로봇 모양)을 클릭합니다.

### 질문하기
- 입력창에 질문을 입력하고 전송 버튼을 클릭합니다.
- Enter 키로도 전송 가능합니다.

### 질문 예시
- "이 사이트는 뭐야?"
- "GitHub 이슈 도우미 어떻게 써?"
- "진행 중인 프로젝트 알려줘"
- "AI 서버 상태 확인 방법은?"

## 제한사항
- 등록된 문서 범위 내에서만 답변합니다.
- 일반적인 대화나 사이트와 무관한 질문에는 답변하지 못할 수 있습니다.
- 최신 정보가 필요한 경우 문서 업데이트가 필요합니다.

## 피드백
답변이 도움이 되었다면 좋아요 버튼을, 그렇지 않다면 싫어요 버튼을 눌러주세요. 피드백은 챗봇 개선에 사용됩니다.',
    'RAG 챗봇 사용 방법 안내',
    true,
    false,
    0,
    5,
    NOW(),
    NOW()
);

-- =====================================================
-- 6. 대시보드 URL 모음 문서
-- =====================================================
INSERT INTO chat_document (
    chat_document_id,
    title,
    category,
    content,
    description,
    is_active,
    is_processed,
    chunk_count,
    order_index,
    created_date,
    updated_date
) VALUES (
    gen_random_uuid(),
    '대시보드 URL 모음',
    'urls',
    '# 새찬 서버 실험실 URL 모음

이 문서는 새찬 서버 실험실 대시보드에 연결된 모든 URL 정보를 정리한 문서입니다. 챗봇이 URL 관련 질문에 답변할 때 참고하는 문서입니다.

## Dev Tools (개발 도구)

### Internal Tools (내부 도구)

#### 이슈 도우미
- **경로**: `/issue-helper`
- **설명**: GitHub 이슈 URL을 입력하면 브랜치명과 커밋 메시지를 자동 생성해주는 도구입니다.
- **기능**: 커밋, 브랜치 자동 생성
- **기술**: GitHub API, Automation
- **GitHub**: https://github.com/Cassiiopeia/suh-project-utility

#### 컨테이너 로그
- **경로**: `/docker-logs`
- **설명**: 실시간 Docker 컨테이너 로그를 스트리밍으로 확인할 수 있는 도구입니다.
- **기능**: 실시간 로그 스트리밍
- **기술**: Docker, SSE (Server-Sent Events)

#### AI 서버 관리
- **경로**: `/ai-server`
- **설명**: Ollama 기반 LLM 서버의 상태를 모니터링하고 관리하는 페이지입니다.
- **기능**: AI 서버 상태 확인 및 관리
- **기술**: Ollama, LLM
- **AI 서버 URL**: https://ai.suhsaechan.kr
- **GitHub**: https://github.com/Cassiiopeia/suh-project-control

#### AI 번역기
- **경로**: `/translator`
- **설명**: 커스텀 AI API를 사용한 다국어 번역 서비스입니다.
- **기능**: 커스텀 API 번역 서비스
- **기술**: OpenAI, Translation

#### 스터디 플랜
- **경로**: `/study-management`
- **설명**: 마크다운을 지원하는 스터디 노트 관리 시스템입니다.
- **기능**: 마크다운 지원 스터디 노트
- **기술**: Markdown, Notes

#### Grass Planter
- **경로**: `/grass`
- **설명**: GitHub 기여 그래프를 자동으로 관리하는 커밋 스케줄러입니다.
- **기능**: GitHub 자동 커밋 관리
- **기술**: GitHub API, Scheduler
- **GitHub**: https://github.com/Cassiiopeia/suh-grass-planter

#### 챗봇 문서 관리
- **경로**: `/chatbot-management`
- **설명**: RAG 챗봇이 참조할 지식 문서를 관리하는 페이지입니다.
- **기능**: RAG 챗봇 지식 문서 관리
- **기술**: Qdrant, Vector DB

#### 버스 자동 예약
- **설명**: 자동화된 버스 예약 시스템입니다.
- **기능**: 버스 자동 예약 시스템
- **기술**: Automation, Reservation
- **GitHub**: https://github.com/Cassiiopeia/auto-bus-reservation

### External Services (외부 서비스)

#### 원격 크롬 (JJ)
- **URL**: https://web.suhsaechan.kr/
- **설명**: 외부 크롬 원격 접속 서비스 (JJ 서버)
- **기술**: Chrome, Remote Desktop

#### 원격 크롬 (House)
- **URL**: https://chrome.suhsaechan.kr/
- **설명**: 외부 크롬 원격 접속 서비스 (House 서버)
- **기술**: Chrome, Remote Desktop

## AI Assistants (커스텀 GPT)

### PROJECT GPT

#### Swagger 생성기
- **URL**: https://chatgpt.com/g/g-67a7a4cd31b48191a2396587469393d1-swaggerdocmaster
- **설명**: Controller, Service, Repository, DTO를 자동으로 생성해주는 ChatGPT 커스텀 GPT입니다.
- **기능**: Swagger 문서 생성
- **기술**: Swagger, Documentation

#### TypeScript API 변환기
- **URL**: https://chatgpt.com/g/g-67da95f653408191ac02737509ae51c7-taibseukeuribteu-api-taib-byeonhwangi
- **설명**: Spring Boot 코드를 TypeScript API로 변환해주는 ChatGPT 커스텀 GPT입니다.
- **기능**: Spring > TypeScript 변환
- **기술**: TypeScript, Spring

#### 변수명 전문가
- **URL**: https://chatgpt.com/g/g-67ee500ea598819188408e9e78a48c35-byeonsumyeong-co-jeonmunga
- **설명**: 변수명을 확실하게 알려주는 ChatGPT 커스텀 GPT입니다.
- **기능**: 변수명 확실하게 알려줌
- **기술**: Naming, Convention

### SOMANSA GPT

#### CM Tag Creator
- **URL**: https://chatgpt.com/g/g-67318f071e688190a355c62acf5033de-cm-taegeu-saengseonggi
- **설명**: 다국어 Tag Coder를 생성해주는 ChatGPT 커스텀 GPT입니다.
- **기능**: 다국어 Tag Coder
- **기술**: Tag, i18n

#### CM Branch Creator
- **URL**: https://chatgpt.com/g/g-6732dd6bcb7481908383eadad53fef7a-cm-beuraencimyeong-saengseonggi
- **설명**: 일감 Info를 기반으로 브랜치명과 커밋 메시지를 생성해주는 ChatGPT 커스텀 GPT입니다.
- **기능**: 일감 Info > 브랜치, 커밋
- **기술**: Branch, Commit

#### Issue PPT Guide
- **URL**: https://chatgpt.com/g/g-675a4ac37ae081918b3f3239c0432672-somansa-pptgyehoegseo-saengseonggi
- **설명**: 일감 Info를 기반으로 PPT 템플릿을 생성해주는 ChatGPT 커스텀 GPT입니다.
- **기능**: 일감 Info > PPT Template
- **기술**: PPT, Template

#### SpringBooster
- **URL**: https://chatgpt.com/g/g-67ce2f8e22808191a917c325d33b5e91-springbooster
- **설명**: PPT를 기반으로 Spring 코드를 개선해주는 ChatGPT 커스텀 GPT입니다.
- **기능**: Spring Improve By PPT
- **기술**: Spring, Improvement

## Projects (프로젝트)

### 세종말싸미 (SEJONG-MALSAMI)

세종대학교 관련 커뮤니티 및 학습 플랫폼 프로젝트입니다.

#### 메인 페이지
- **URL**: https://test.sejong-malsami.co.kr
- **설명**: 세종말싸미 메인 페이지
- **타입**: Frontend

#### Swagger Docs
- **URL**: https://api.sejong-malsami.co.kr/docs/swagger-ui/index.html#/
- **설명**: 세종말싸미 API 문서
- **타입**: Backend API

#### 관리자 페이지
- **URL**: https://api.sejong-malsami.co.kr
- **설명**: 세종말싸미 관리자 페이지
- **타입**: Admin

#### Figma (Test)
- **URL**: https://www.figma.com/design/g3TWHt8CvADqj2fXcf9uIA/%EC%84%B8%EC%A2%85%EB%B0%9C%EC%8B%B8%EB%AF%B9-%EB%94%94%EC%9E%90%EC%9D%B8?node-id=0-1&p=f&t=LubXbhTDNNRuyQK6-0
- **설명**: 세종말싸미 테스트 디자인
- **타입**: Design

#### 기술 스택
- Frontend: React, TypeScript
- Backend: Spring Boot
- Database: PostgreSQL

### ROM ROM

중고 물품 거래 모바일 앱 프로젝트입니다.

#### APK 다운로드
- **URL**: http://suh-project.synology.me/romrom
- **설명**: ROM ROM APK 다운로드 페이지
- **타입**: Mobile App

#### HTTPS Swagger
- **URL**: https://api.romrom.xyz/docs/swagger-ui/index.html
- **설명**: ROM ROM HTTPS API 문서
- **타입**: Backend API

#### HTTP Swagger
- **URL**: http://suh-project.synology.me:8085/docs/swagger-ui/index.html#/
- **설명**: ROM ROM HTTP API 문서
- **타입**: Backend API

#### Figma (Test)
- **URL**: https://www.figma.com/design/qXvwG0b5GYk9xkzgw9fdhb/%EB%A1%AC%EB%A1%AC-%ED%9A%8C%EC%9D%98?node-id=2049-5172&t=TFPO78n7GC2JipNk-0
- **설명**: 롬롬 테스트 디자인
- **타입**: Design

#### Figma (Prod)
- **URL**: https://www.figma.com/design/dq4HUUqmTlVkmWr4cd2RE5/%F0%9F%94%84-romrom?node-id=1344-2435&t=lbULaVU0rbuntb79-0
- **설명**: 롬롬 프로덕션 디자인
- **타입**: Design

#### 기술 스택
- Mobile: Flutter
- Backend: Spring Boot
- Database: PostgreSQL

### PLANE ACCIDENT FINDER

비행기 사고 정보 조회 모바일 앱입니다.

#### APK 다운로드
- **URL**: http://suh-project.synology.me/plane-accident-finder/
- **설명**: PLANE ACCIDENT FINDER APK 다운로드 페이지
- **타입**: Mobile App

#### HTTPS Swagger
- **URL**: https://api.romrom.xyz/docs/swagger-ui/index.html
- **설명**: PLANE ACCIDENT FINDER HTTPS API 문서
- **타입**: Backend API

#### HTTP Swagger
- **URL**: http://suh-project.synology.me:8082/docs/swagger-ui/index.html#/
- **설명**: PLANE ACCIDENT FINDER HTTP API 문서
- **타입**: Backend API

#### 기술 스택
- Mobile: Flutter
- Backend: Spring Boot

## GitHub 리포지토리

### 주요 프로젝트
- **suh-project-utility**: https://github.com/Cassiiopeia/suh-project-utility
  - 새찬 서버 실험실 메인 프로젝트
- **suh-project-control**: https://github.com/Cassiiopeia/suh-project-control
  - AI 서버 관리 프로젝트
- **suh-grass-planter**: https://github.com/Cassiiopeia/suh-grass-planter
  - GitHub 자동 커밋 스케줄러
- **auto-bus-reservation**: https://github.com/Cassiiopeia/auto-bus-reservation
  - 버스 자동 예약 시스템

## 사이트 접속 정보

- **메인 사이트**: https://lab.suhsaechan.kr
- **AI 서버**: https://ai.suhsaechan.kr

## 사용 예시

사용자가 다음과 같은 질문을 할 수 있습니다:
- "이슈 도우미 URL 알려줘" → `/issue-helper`
- "세종말싸미 Swagger 문서 링크 줘" → https://api.sejong-malsami.co.kr/docs/swagger-ui/index.html#/
- "ROM ROM 프로젝트 링크 알려줘" → 여러 링크 제공
- "AI 서버 관리 페이지 URL은?" → `/ai-server`
- "원격 크롬 접속 링크 줘" → https://web.suhsaechan.kr/ 또는 https://chrome.suhsaechan.kr/',
    '대시보드에 연결된 모든 URL 정보 모음',
    true,
    false,
    0,
    6,
    NOW(),
    NOW()
);

-- =====================================================
-- 확인 쿼리
-- =====================================================
SELECT
    chat_document_id,
    title,
    category,
    is_active,
    is_processed,
    chunk_count,
    created_date
FROM chat_document
ORDER BY order_index;

-- =====================================================
-- 주의사항
-- =====================================================
-- 1. 이 SQL로 삽입된 문서는 is_processed = false 상태입니다.
-- 2. 챗봇이 검색하려면 벡터화가 필요합니다.
-- 3. /chatbot-management 페이지에서 각 문서의 "재처리" 버튼을 클릭하거나,
--    DocumentService.processDocument()를 호출해야 합니다.
-- =====================================================
