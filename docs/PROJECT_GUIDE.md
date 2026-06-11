SUH-PROJECT-UTILITY 프로젝트 가이드

프로젝트 소개

SUH-PROJECT-UTILITY는 SUHSAECHAN LAB이라는 이름으로 운영되는 개인 프로젝트 통합 관리 웹 애플리케이션입니다. 이 프로젝트는 개발자의 다양한 프로젝트와 도구를 하나의 대시보드에서 관리하고 접근할 수 있도록 만들어진 서비스입니다. 운영 URL은 https://lab.suhsaechan.kr/ 이며 현재 버전은 v2.5.10입니다. GitHub 저장소는 https://github.com/Cassiiopeia/suh-project-utility 입니다.

프로젝트의 핵심 목적은 개발 과정에서 반복적으로 필요한 작업들을 자동화하고, 여러 프로젝트의 상태를 한눈에 파악할 수 있는 통합 대시보드를 제공하는 것입니다. GitHub 이슈 관리, Docker 컨테이너 모니터링, 스터디 노트, 번역, 챗봇, Task 관리 등 다양한 기능을 제공합니다.

개발자 정보

개발자는 서새찬 Suh Saechan입니다. GitHub 프로필은 https://github.com/Cassiiopeia 입니다. 세종대학교 소속이며 개인 프로젝트를 꾸준히 개발하고 운영하고 있습니다.

기술 스택

백엔드는 자바 Java 17과 스프링 부트 Spring Boot 3.4.2를 사용합니다. 데이터베이스는 PostgreSQL을 메인 데이터베이스로, Redis를 캐싱과 세션 관리에 사용합니다. 벡터 데이터베이스로 Qdrant를 사용하여 RAG 챗봇의 문서 검색을 지원합니다.

프론트엔드는 Thymeleaf 템플릿 엔진을 사용하고 TailwindCSS와 DaisyUI 5로 스타일링합니다. 아이콘은 Font Awesome 6.5.1을 사용합니다. JavaScript 라이브러리로는 jQuery 3.7.1, CryptoJS, Marked.js를 사용합니다.

빌드 도구는 Gradle 8.12.1을 사용합니다. 배포는 Docker 컨테이너 기반이며 GitHub Actions를 통한 CI/CD 파이프라인이 구성되어 있습니다. 운영 서버는 Synology NAS 기반입니다.

AI 관련 기술로는 SUH-AIDER Engine을 사용합니다. 이 엔진은 로컬 LLM 서버와 연동하여 번역, 챗봇 의도 분류, 응답 생성 등을 수행합니다. 임베딩과 벡터 검색에는 Qdrant gRPC 클라이언트 1.16.1을 사용합니다.

프로젝트 구조

이 프로젝트는 멀티 모듈 구조로 되어 있습니다. 총 15개의 모듈로 구성되어 있습니다.

Suh-Common 모듈은 공통 유틸리티, 기본 엔티티, 설정, 예외 처리를 담당합니다. 모든 외부 라이브러리 의존성은 이 모듈에서 선언합니다.

Suh-Web 모듈은 웹 컨트롤러, 뷰 라우팅, 설정 클래스를 담당합니다. 모든 Configuration 클래스가 이 모듈에 위치합니다.

Suh-Application 모듈은 대시보드 통계 집계 등 애플리케이션 레벨 서비스를 담당합니다.

도메인 모듈은 기능별로 분리되어 있습니다. Suh-Domain-Docker는 Docker 컨테이너 모니터링, Suh-Domain-Github는 GitHub 이슈 헬퍼, Suh-Domain-Notice는 공지사항 관리, Suh-Domain-Study는 스터디 노트, Suh-Domain-Module은 모듈 버전 관리, Suh-Domain-Task는 할일 Task 관리, Suh-Domain-Chatbot은 RAG 기반 챗봇, Suh-Domain-AiServer는 AI 서버 관리, Suh-Domain-GrassPlanter는 GitHub 자동 커밋 관리, Suh-Domain-Statistics는 방문자 통계, Suh-Domain-Somansa-Bus는 버스 예약 시스템을 담당합니다.

Suh-Module-Translate 모듈은 AI 기반 번역 서비스를 담당합니다.

로그인 및 대시보드

로그인은 Spring Security 기반 폼 로그인 방식입니다. 로그인 페이지에서 사용자명과 비밀번호를 입력하여 인증합니다.

로그인 후 대시보드에 접속하면 상단에 서버 통계 요약 정보가 표시됩니다. 총 방문자 수, 오늘 방문자 수, 챗봇 메시지 수, 토큰 사용량, 페이지 조회 수, 프로필 조회 수, 세종대 인증 횟수, SUH 랜덤 사용 횟수가 카드 형태로 표시됩니다.

대시보드 중앙에는 공지사항이 캐러셀 형태로 스크롤되며 표시됩니다. 각 공지사항을 클릭하면 상세 내용을 확인할 수 있습니다.

대시보드에는 Task D-Day 섹션이 있습니다. 현재 진행 중인 Task 목표들이 D-Day 카드로 표시되며 목표일까지 남은 날짜, 진행률, 목표량이 표시됩니다. 전체보기를 클릭하면 Task 트래커 페이지로 이동합니다.

대시보드 하단에는 내부 도구, 외부 서비스, AI 어시스턴트, 서버, 서비스, 프로젝트 등의 섹션이 카드 형태로 나열됩니다.

기능별 가이드

GitHub 이슈 도우미 Issue Helper

사용 방법: 로그인 후 대시보드에서 이슈 도우미 카드를 클릭하거나 상단 메뉴에서 깃헙 이슈를 클릭합니다. GitHub 이슈 URL을 입력하면 자동으로 브랜치명과 커밋 메시지를 생성합니다.

기능 목적: GitHub 이슈를 기반으로 브랜치명과 커밋 메시지를 표준화된 형식으로 자동 생성합니다. 개발 워크플로우에서 반복적인 이슈 번호 확인, 브랜치 생성, 커밋 메시지 작성 작업을 자동화합니다.

기술 구현: OkHttp와 Jsoup을 사용하여 GitHub 이슈 페이지를 크롤링합니다. 이슈 제목에서 태그와 본문을 파싱하여 브랜치명 형식 YYYYMMDD_이슈번호_제목과 커밋 메시지 형식 제목 : feat : 설명 : URL을 생성합니다.

내부 동작: IssueHelperService가 이슈 URL에서 HTML을 가져와 title 태그를 파싱합니다. 제목에서 대괄호 태그를 분리하고 특수문자를 제거한 후 언더스코어로 공백을 치환하여 브랜치명을 만듭니다. GithubService가 요청 이력을 데이터베이스에 저장하고 리포지토리 접근 권한을 관리합니다.

DevOps 템플릿

사용 방법: 대시보드에서 DevOps 템플릿 카드를 클릭합니다. GitHub Actions CI/CD 자동화 템플릿을 확인하고 사용할 수 있습니다.

기능 목적: GitHub Actions 기반의 CI/CD 파이프라인 템플릿을 제공합니다. 자주 사용하는 워크플로우를 표준화하여 새 프로젝트에 빠르게 적용할 수 있습니다.

외부 프로젝트: GitHub 저장소는 https://github.com/Cassiiopeia/suh-devops-template 입니다.

Docker 컨테이너 로그

사용 방법: 대시보드에서 컨테이너 로그 카드를 클릭합니다. 실행 중인 Docker 컨테이너 목록이 표시됩니다. 컨테이너를 선택하면 로그를 실시간으로 스트리밍하여 볼 수 있습니다.

기능 목적: 원격 서버에서 실행 중인 Docker 컨테이너의 상태를 모니터링하고 로그를 실시간으로 확인합니다. 서버에 직접 접속하지 않고도 컨테이너 상태를 파악할 수 있습니다.

기술 구현: JSch 라이브러리를 사용하여 SSH로 원격 서버에 접속합니다. docker ps 명령어로 컨테이너 목록을 가져오고 docker logs 명령어로 로그를 조회합니다. 실시간 스트리밍은 Server-Sent Events SSE를 사용하여 구현합니다.

내부 동작: DockerLogService가 SSH 세션을 열어 원격 서버에서 docker 명령어를 실행합니다. 컨테이너 정보는 ID, Name, Image, Status 형식으로 파싱됩니다. 스트리밍 모드에서는 docker logs -f 명령어로 실시간 로그를 SSE Emitter를 통해 클라이언트에 전달합니다.

AI 서버 관리

사용 방법: 대시보드에서 AI 서버 관리 카드를 클릭합니다. AI 서버의 연결 상태와 사용 가능한 모델을 확인할 수 있습니다.

기능 목적: Ollama 기반 로컬 LLM 서버의 상태를 모니터링하고 관리합니다.

기술 구현: SUH-AIDER Engine을 통해 Ollama 서버와 통신합니다.

AI 번역기 Translator

사용 방법: 대시보드에서 AI 번역기 카드를 클릭합니다. 번역할 텍스트를 입력하면 AI가 자동으로 언어를 감지하고 번역합니다.

기능 목적: 커스텀 LLM 기반 번역 서비스를 제공합니다. 외부 번역 API에 의존하지 않고 자체 AI 모델로 번역을 수행합니다.

기술 구현: SUH-AIDER Engine의 경량 LLM 모델 기본값 gemma3:1b을 사용합니다. Structured Output 기능으로 번역 결과, 감지된 언어, 신뢰도 점수를 JSON 형식으로 받습니다.

내부 동작: TranslateService가 LLM에 번역 프롬프트를 전달합니다. 프롬프트는 번역 결과를 translatedText, detectedLanguage, confidence 필드를 가진 JSON으로 반환하도록 요청합니다. 응답을 파싱하여 TranslationDto로 변환한 후 TranslationResponse로 반환합니다.

스터디 플랜 Study Management

사용 방법: 대시보드에서 스터디 플랜 카드를 클릭합니다. 카테고리를 선택하여 스터디 노트를 열람하거나 새 노트를 작성할 수 있습니다. 마크다운 형식으로 내용을 작성하고 파일을 첨부할 수 있습니다.

기능 목적: 마크다운 기반의 스터디 노트를 관리합니다. 계층형 카테고리로 노트를 분류하고 태그, 조회수, 파일 첨부 기능을 제공합니다.

기술 구현: CommonMark 0.17.0 라이브러리로 마크다운을 렌더링합니다. 파일 업로드는 Synology NAS에 SMB 프로토콜로 저장합니다. 페이지네이션과 정렬 기능을 지원합니다.

내부 동작: StudyPostService가 게시글 CRUD를 담당합니다. StudyCategoryService가 계층형 카테고리 관리를 담당하며 카테고리 삭제 시 하위 카테고리나 게시글이 있으면 삭제를 거부합니다. StudyFileService가 파일 업로드와 삭제를 처리하며 파일명을 UUID로 변환하여 충돌을 방지합니다. 태그는 쉼표로 구분된 문자열로 저장됩니다.

세종대학교 인증 Sejong Auth

사용 방법: 대시보드에서 세종대 인증 카드를 클릭합니다. 세종대학교 포털 인증 테스트를 수행할 수 있습니다.

기능 목적: 세종대학교 포털 시스템 인증 라이브러리의 테스트 페이지입니다.

외부 프로젝트: GitHub 저장소는 https://github.com/Cassiiopeia/SUH-sejong-univ-auth 입니다.

Grass Planter

사용 방법: 대시보드에서 Grass Planter 카드를 클릭합니다. GitHub 자동 커밋 프로필을 관리할 수 있습니다.

기능 목적: GitHub 잔디 Grass를 자동으로 심는 스케줄러를 관리합니다. 자동 커밋 설정과 상태를 확인할 수 있습니다.

외부 프로젝트: GitHub 저장소는 https://github.com/Cassiiopeia/suh-grass-planter 입니다.

SUH 랜덤 엔진 SUH Random Engine

사용 방법: 대시보드에서 SUH 랜덤 카드를 클릭합니다. 랜덤 닉네임 생성과 가챠 시스템 테스트를 수행할 수 있습니다.

기능 목적: 랜덤 닉네임 생성과 가챠 확률 시스템을 제공하는 라이브러리의 테스트 페이지입니다.

외부 프로젝트: GitHub 저장소는 https://github.com/Cassiiopeia/suh-random-engine 입니다.

공지사항 관리 Notice Management

사용 방법: 대시보드에서 공지사항 관리 카드를 클릭합니다. 공지사항을 생성, 수정, 삭제할 수 있습니다. 중요 공지 설정과 기간별 활성화가 가능합니다. 각 공지에 댓글을 달 수 있습니다.

기능 목적: 서비스 공지사항을 관리합니다. 대시보드 상단에 캐러셀로 표시되는 공지를 관리할 수 있습니다.

기술 구현: PostgreSQL에 공지사항 데이터를 저장합니다. 시간 기반 활성화 startDate, endDate를 지원합니다.

내부 동작: NoticeService가 공지사항 CRUD와 활성 상태 관리를 담당합니다. 조회 시 조회수가 자동으로 증가합니다. NoticeCommentService가 댓글 관리를 담당하며 익명 댓글을 지원하고 IP를 익명화하여 저장합니다.

Task 트래커

사용 방법: 대시보드에서 Task D-Day 섹션의 전체보기를 클릭하거나 상단 메뉴에서 Task 트래커를 클릭합니다. Task 추가 버튼을 클릭하여 새 목표를 생성합니다. 제목, 설명, 목표일, 총 목표량, 단위, 아이콘, 색상을 설정합니다. 생성된 Task 카드에서 진행 기록 추가 버튼을 클릭하여 일별 진행 상황을 기록합니다. 시작 페이지와 끝 페이지를 듀얼 레인지 슬라이더로 입력합니다.

기능 목적: 문제집 풀기 등 개인 목표를 Task 단위로 관리하고 일별 진행률을 추적합니다. 대시보드에 D-Day 카드로 마감까지 남은 날짜와 진행률을 한눈에 확인할 수 있습니다.

기술 구현: PostgreSQL에 TaskGoal과 TaskProgress 엔티티를 저장합니다. TaskProgress는 TaskGoal에 대한 ManyToOne 관계로 연결됩니다. 진행률은 모든 Progress 기록에서 최대 endAmount를 조회하여 totalAmount 대비 백분율로 계산합니다.

내부 동작: TaskGoalService가 목표 CRUD, 완료, 취소 상태 전환을 담당합니다. TaskProgressService가 일별 진행 기록을 관리하며 동일 목표의 동일 날짜에 기록이 이미 존재하면 업데이트하고 없으면 새로 생성합니다. 목표 삭제 시 연관된 모든 Progress 기록을 먼저 삭제한 후 Goal을 삭제합니다. D-Day 계산은 현재 날짜와 목표일 사이의 일수로 산출하며 7일 이내는 빨강, 30일 이내는 노랑, 그 이상은 초록, 초과 시는 회색으로 표시됩니다. 단위 기본값은 페이지이며 아이콘 10종과 색상 8종을 선택할 수 있습니다.

버스 예약 Somansa Bus Reservation

사용 방법: 대시보드에서 버스 자동 예약 카드를 클릭하거나 상단 메뉴에서 버스 예약을 클릭합니다. 버스 노선 정보를 조회하고 예약을 관리할 수 있습니다.

기능 목적: 통근 버스 자동 예약 시스템입니다. 회원별 예약 관리와 노선 정보 조회를 제공합니다.

외부 프로젝트: GitHub 저장소는 https://github.com/Cassiiopeia/auto-bus-reservation 입니다.

RAG 챗봇 Chatbot

사용 방법: 페이지 하단에 챗봇 아이콘이 표시됩니다. 클릭하면 채팅 창이 열리고 질문을 입력할 수 있습니다. 챗봇이 프로젝트에 대한 질문에 답변합니다.

기능 목적: RAG Retrieval-Augmented Generation 기반 챗봇으로 프로젝트에 대한 사용자 질문에 자동으로 답변합니다. 등록된 문서를 벡터화하여 검색하고 관련 정보를 기반으로 응답을 생성합니다.

기술 구현: SUH-AIDER Engine의 LLM 모델을 사용합니다. Qdrant 벡터 데이터베이스에 문서 임베딩을 저장합니다. 3단계 파이프라인으로 처리합니다. 1단계는 경량 LLM으로 의도를 분류하고 2단계는 Qdrant에서 관련 문서를 검색하며 3단계는 고품질 LLM으로 응답을 생성합니다.

내부 동작: ChatbotService가 멀티 스텝 에이전트 처리를 수행합니다. 1단계 의도 분류에서 질문 유형을 KNOWLEDGE_QUERY, GREETING, CHITCHAT, CLARIFICATION으로 분류하고 RAG 검색 필요 여부를 결정합니다. 신뢰도가 0.7 미만이면 재시도합니다. 2단계에서 RAG 검색이 필요한 경우 질문을 벡터로 변환하여 Qdrant에서 유사 문서 청크를 검색합니다. 3단계에서 검색 결과와 대화 이력을 포함한 프롬프트로 최종 응답을 생성합니다. 스트리밍 모드에서는 Thinking 이벤트를 실시간으로 전달합니다.

DocumentService가 챗봇 문서 관리를 담당합니다. 문서를 토큰 수 기준으로 청크 단위로 분할하며 한국어는 글자당 약 0.5 토큰, 영어는 약 0.25 토큰으로 추정합니다. 문장 경계를 인식하여 분할하고 청크 간 오버랩으로 문맥을 보존합니다. PostgreSQL에 메타데이터를, Qdrant에 벡터를 저장합니다.

챗봇 문서 관리 Chatbot Document Management

사용 방법: 대시보드에서 챗봇 문서 관리 카드를 클릭합니다. RAG 챗봇이 참조하는 지식 문서를 관리할 수 있습니다. 문서를 추가, 수정, 삭제하고 카테고리별로 분류할 수 있습니다.

기능 목적: RAG 챗봇이 답변에 사용하는 지식 문서를 관리합니다. 문서를 추가하면 자동으로 벡터화되어 Qdrant에 저장됩니다.

대시보드 외부 서비스

원격 크롬: 두 개의 원격 크롬 접속 서비스를 제공합니다. JJ 서버는 https://web.suhsaechan.kr/ 이고 House 서버는 https://chrome.suhsaechan.kr/ 입니다.

AI 어시스턴트 GPT

대시보드에는 커스텀 GPT 링크들이 등록되어 있습니다.

프로젝트 GPT로는 Swagger 생성기, TypeScript API 변환기, 변수명 전문가가 있습니다. Swagger 생성기는 Controller, Service, Repository, DTO를 분석하여 Swagger 문서를 생성합니다. TypeScript API 변환기는 Spring 코드를 TypeScript로 변환합니다. 변수명 전문가는 적절한 변수명을 추천합니다.

Somansa GPT로는 CM Tag Creator, CM Branch Creator, Issue PPT Guide, SpringBooster가 있습니다.

인프라 서비스

Nexus는 https://nexus.suhsaechan.kr 에서 접근하며 Maven과 Docker 레포지토리를 관리합니다.

Qdrant는 https://qdrant.suhsaechan.kr 에서 접근하며 벡터 데이터베이스 대시보드입니다.

RabbitMQ는 https://rabbitmq.suhsaechan.kr 에서 접근하며 메시지 큐 대시보드입니다.

Traefik은 https://traefik.suhsaechan.kr 에서 접근하며 리버스 프록시 대시보드입니다.

연관 프로젝트

Mapsy 프로젝트: 메인 페이지는 https://mapsy.suhsaechan.kr 이고 AI 서비스는 https://ai.mapsy.suhsaechan.kr 이며 API 문서는 https://api.mapsy.suhsaechan.kr 입니다.

세종말싸미 Sejong-Malsami 프로젝트: 메인 페이지는 https://test.sejong-malsami.co.kr 이고 Swagger 문서는 https://api.sejong-malsami.co.kr/docs/swagger-ui/index.html 이며 관리자 페이지는 https://api.sejong-malsami.co.kr 입니다.

Plane Accident Finder 프로젝트: APK 다운로드는 http://suh-project.synology.me/plane-accident-finder/ 입니다.

Rom Rom 프로젝트: APK 다운로드는 https://suh-project.synology.me/romrom 이고 API 문서는 https://api.romrom.xyz/docs/swagger-ui/index.html 입니다.

Re: Wave 프로젝트: 선택의 순간, 그녀의 운명이 갈린다. 프론트엔드는 https://bagel.suhsaechan.kr/ 이고 API 문서는 https://api.bagel.suhsaechan.kr/docs/swagger 입니다.

아키텍처 및 배포

전체 시스템은 멀티 모듈 Spring Boot 애플리케이션입니다. Suh-Common이 모든 모듈의 기반이 되며 외부 라이브러리 의존성을 관리합니다. 각 도메인 모듈은 entity, repository, service, dto 패키지 구조를 가지며 최대 2단계 깊이까지만 허용합니다.

JPA 관계 매핑은 ManyToOne만 사용합니다. OneToMany와 OneToOne은 사용하지 않으며 자식 엔티티 조회는 Repository에서 직접 수행합니다.

API 설계는 모든 엔드포인트가 POST 메서드를 사용하고 MediaType.MULTIPART_FORM_DATA_VALUE를 Content-Type으로 사용합니다. 각 도메인은 하나의 Request와 하나의 Response 클래스만 사용합니다. 응답에 isSuccess나 message 필드를 포함하지 않으며 성공 시 데이터만 반환하고 실패 시 CustomException을 발생시킵니다.

데이터베이스는 PostgreSQL을 메인으로 사용하며 BasePostgresEntity를 상속한 엔티티들이 createdDate와 updatedDate를 자동 관리합니다. Redis는 캐싱과 세션 관리에 사용되며 대시보드 통계는 5분간 캐싱됩니다. Qdrant는 챗봇 문서의 벡터 임베딩을 저장하고 검색합니다.

배포는 GitHub Actions CI/CD 파이프라인을 통해 자동화되어 있습니다. Docker 이미지를 빌드하여 배포하며 개발 dev과 운영 prod 프로필로 환경을 분리합니다. 운영 서버는 Synology NAS 기반이며 Traefik을 리버스 프록시로 사용합니다.

파일 업로드는 Synology NAS에 SMB 프로토콜로 저장합니다. 최대 파일 크기는 200MB이며 확장자 제한이 적용됩니다.

페이지 라우팅

메인 페이지와 대시보드는 루트 경로 / 와 /dashboard에서 접근합니다. 로그인은 /login, 프로필은 /profile에서 접근합니다.

기능 페이지는 다음과 같습니다. GitHub 이슈 헬퍼는 /issue-helper, DevOps 템플릿은 /suh-devops-template, 번역기는 /translator, 세종대 인증은 /sejong-auth, SUH 랜덤은 /suh-random, 공지사항 관리는 /notice-management, Docker 로그는 /docker-logs, 스터디 관리는 /study-management, Grass Planter는 /grass, Task 트래커는 /task-tracker, AI 서버는 /ai-server, 챗봇 문서 관리는 /chatbot-management, 버스 예약은 /somansa/bus, 버스 회원 상세는 /somansa/bus/member/회원ID에서 접근합니다.
