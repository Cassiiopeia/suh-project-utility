# RAG 검색 결함 수술적 수정 + 진단 가시성 (Phase 1)

- 작성일: 2026-06-16
- 대상 모듈: `Suh-Domain-Chatbot`, `Suh-Common`
- 관련 증상: 챗봇이 "너는 누구야?" 등의 질문에 "관련 문서 없음"으로 답함. 문서는 admin에 9개·청크 49개가 등록되어 있음.

## 배경 (왜 이 작업을 하는가)

사용자는 검색 품질이 너무 나빠 "코드를 Java→Python으로 전면 전환"하는 것을 반복적으로 제안했다. 검토 결과 이는 **불가능하고, 무엇보다 문제를 해결하지 못한다**:

- Spring(JVM) 안에서 Java 코드를 Python으로 "바꿔 실행"하는 것은 불가능하다. Python 코드는 별도 프로세스가 필요한데, 사용자는 "별도 서버를 띄울 수 없다"는 제약을 명시했다.
- **결정적 이유: 지금 안 되는 원인은 언어가 아니라 설계 결함이다.** 잘못된 설계를 Python으로 옮기면 결함도 그대로 복제된다. LangChain 등 Python 도구도 동일한 임베딩 모델로 동일한 (잘못된) 쿼리를 검색할 뿐이다. 동작하는 ~1000줄을 버리고 재작성하면 새 버그만 추가될 위험이 크다.

### 코드·로그로 확정된 진짜 원인

운영 로그(2026-06-16 09:19):

```
[Agent Step 1/3] 의도 분류 완료 - type: KNOWLEDGE_QUERY, needsRAG: true, confidence: 0.95,
                 searchQuery: SUH Project Utility 소개 목적 주요 기능 모듈
[Agent Step 2/3] RAG 검색 시작 - 쿼리: SUH Project Utility 소개 목적 주요 기능 모듈
[Agent Step 2/3] RAG 검색 완료 - 결과 수: 0
```

질문은 **"너는 누구야?"** 였는데, 검색에 사용된 쿼리는 **"SUH Project Utility 소개 목적 주요 기능 모듈"** 이다. 사용자의 진짜 질문이 검색에서 사라졌다.

이건 LLM의 실수가 아니라 **설계가 시킨 대로 한 것**이다. `ChatbotService.classifyUserIntentInternal`의 프롬프트(520-529줄)가 의도 분류 LLM에게 다음을 강제한다:

```
## searchQuery 생성 규칙 (매우 중요!):
1. **반드시 'SUH Project Utility' 키워드 포함**
예시:
   - '이 사이트 뭐하는 곳이야?' → 'SUH Project Utility 소개 목적 주요 기능 모듈'
   - '개발자 누구야?' → 'SUH Project Utility 서새찬 suhsaechan 개발자 프로필 소개'
```

즉 설계가 **사용자의 원본 질문을 버리고 인공 키워드 나열로 치환**해서 검색하도록 만들어져 있다. "너는 누구야?"는 526줄 예시 `'이 사이트 뭐하는 곳이야?'`에 가장 가까워, LLM이 그 예시 출력을 거의 그대로 베껴 엉뚱한 쿼리를 만들었다.

**이것이 RAG가 망가지는 핵심 메커니즘이다.** 벡터 검색은 사용자 질문 임베딩과 문서 청크 임베딩의 유사도로 동작하는데, 질문을 인공 키워드로 치환하면 정작 `서새찬 개발자` 문서의 실제 문장들과 멀어진다.

### 검색 쿼리 선택 로직

`ChatbotService.getSearchQuery(intent, message)`(228줄에서 호출)는 검색 쿼리를 다음 우선순위로 고른다:

```
searchQuery(LLM 생성) > summary > 원본 메시지
```

LLM 생성 쿼리가 있으면 **원본 질문을 버린다.** 작은 모델(gemma3:1b)이 질문을 잘못 해석하면 사용자 의도가 통째로 사라지는 구조다.

## 결정된 방향: 결함만 정조준하는 수술적 수정

설계를 Python으로 갈아엎거나 파이프라인 전체를 재구성하지 않는다. **잘못된 한 가지(원본 질문을 인공 쿼리로 치환)를 제거**하고, 동시에 "결과 0건"의 잔여 원인을 측정으로 확정할 진단 로그를 추가한다. 의도 분류·Agent 3단계 구조·재시도 로직은 **유지**한다 (이번 증상의 직접 원인이 아니므로 surgical scope 밖).

## 목표 (Phase 1 범위)

1. **검색 쿼리를 원본 질문 기반으로 전환** — searchQuery 치환을 제거하고, RAG 벡터 검색은 사용자의 원본 질문 임베딩을 사용한다.
2. **진단 로그 강화** — "결과 0건"이 minScore 필터 탓(A)인지 Qdrant 원본부터 0건(B)인지 로그만으로 판별 가능하게 한다.
3. **`minScore` / `topK` 운영화** — 하드코딩된 두 값을 `ServerOptionKey`로 승격해 재배포 없이 admin UI에서 조정 가능하게 한다.

### 전제 원칙 (모든 Phase 공통)

> **서버 설정값은 UI(admin)에서 전부 조정 가능해야 한다.**

운영 중 의미를 갖는 챗봇/RAG 튜닝 파라미터는 코드·`application.yml`·`ChatbotProperties` 하드코딩이 아니라 `ServerOptionKey`로 관리해, 재배포 없이 admin UI에서 조정 가능해야 한다. (인프라 시크릿·빌드타임 값은 예외 — `@Value` 유지.) 현재 UI로 조정 불가능한 튜닝값:

| 파라미터 | 현재 위치 | UI 조정 | Phase |
|---|---|---|---|
| `CHATBOT_CHUNK_SIZE` / `CHATBOT_CHUNK_OVERLAP` | ServerOption | O (이미 됨) | - |
| `CHATBOT_INTENT_CLASSIFIER_MODEL` / `CHATBOT_RESPONSE_GENERATOR_MODEL` | ServerOption | O (이미 됨) | - |
| **`minScore`** | `ChatbotProperties:69` 하드코딩 | X | **Phase 1** |
| **`topK`** | `ChatbotProperties:64` 하드코딩 | X | **Phase 1** |
| `CONFIDENCE_THRESHOLD = 0.7f` (재분류 트리거) | `ChatbotService` 상수 | X | Phase 2 후보 |
| `history.maxMessages` (참고 대화 이력 수) | `ChatbotProperties` | X | Phase 2 후보 |
| SSE 타임아웃 `120000ms` | `ChatbotController` 상수 | X | Phase 2 후보 |

### 명시적 비범위 (YAGNI)

다음은 이번에 **건드리지 않는다.** Phase 2에서 진단 결과를 보고 결정한다:

- 의도 분류 단계 제거 / Agent 3단계 → 2단계 축소
- confidence 재시도 로직 단순화
- re-ranking, hybrid search, query expansion
- 임베딩 모델(`embeddinggemma`) / 응답·의도 분류 모델 교체
- `CHATBOT_CHUNK_OVERLAP` 값 조정
- 잔여 하드코딩 튜닝값(CONFIDENCE_THRESHOLD, maxMessages, SSE timeout) 승격

## 현재 코드 구조 (확인된 사실)

### 검색 쿼리 선택

- `ChatbotService.java:228` (스트리밍) — `String searchQuery = getSearchQuery(intent, message);`
- `ChatbotService.java:123-125` (비스트리밍) — `intent.getSummary()`가 있으면 그것, 없으면 `request.getMessage()`
- `getSearchQuery(intent, message)` — `searchQuery > summary > message` 우선순위 (LLM 생성값이 원본을 대체)

### 검색 파라미터 결정

- `ChatbotProperties.java:64` — `topK = 3` (하드코딩)
- `ChatbotProperties.java:69` — `minScore = 0.5f` (하드코딩)
- `ChatbotService.java:119-120` (비스트리밍) / `233-234` (스트리밍) — 요청값 우선, 없으면 `chatbotProperties.getAgent().getRag()` 값
- `ChatbotService.java:804-812` — `searchRelevantDocuments(query, topK, minScore)` → 임베딩 → `vectorStoreService.search(...)`

### 필터 위치

- `QdrantVectorStoreService.java:176-191` — `search(collectionName, queryVector, topK, minScore)`
  - line 186: `qdrantClient.searchAsync(searchRequest).get()` → 원본 `List<ScoredPoint>`
  - line 188-191: `.filter(sp -> sp.getScore() >= minScore)` 후 반환 ← **필터 전 원본이 로그에 남지 않는 지점**
- `QdrantVectorStoreService.java:222+` — `countPoints(collectionName)` 이미 존재 (컬렉션 포인트 수 조회 가능)

### ServerOption 사용 패턴 (참고)

- 키 정의: `Suh-Common/src/main/java/me/suhsaechan/common/constant/ServerOptionKey.java`
- 조회: `serverOptionService.getOptionValue(ServerOptionKey.XXX)` (`CHATBOT_CHUNK_SIZE` 등에서 사용 중)
- admin 화면은 enum을 자동 노출 → 키 추가만으로 편집 UI에 나타남

## 설계

### 1. 검색 쿼리를 원본 질문 기반으로 전환

**핵심: RAG 벡터 검색은 사용자의 원본 질문을 사용한다.** 의도 분류 LLM이 만든 인공 searchQuery로 원본을 **대체하지 않는다.**

- `getSearchQuery(intent, message)`의 우선순위를 바꿔 **원본 메시지(`message`)를 검색 쿼리로 사용**한다.
- 비스트리밍 경로(`ChatbotService.java:123-125`)도 동일하게 원본 메시지를 사용하도록 맞춘다(현재 summary 우선 → message 우선).
- 의도 분류 결과(`intentType`, `needsRagSearch`, `responseFormat`)는 **그대로 사용**한다. 즉 "검색을 할지 말지", "응답을 어떤 형식으로 낼지"는 의도 분류가 계속 담당하되, **"무엇으로 검색할지"는 원본 질문**이 담당한다.

> 의도 분류 프롬프트의 searchQuery 생성 지시(520-529줄)는 이번에 **삭제하지 않는다.** `IntentClassificationDto.searchQuery` 필드를 검색에 안 쓰게만 바꾸는 것이 최소 변경이다. 프롬프트에서 해당 지시를 들어내는 것은 의도 분류 품질에 영향을 줄 수 있으므로 Phase 2에서 측정 후 정리한다. (단, 구현 시 searchQuery 미사용으로 인해 죽는 코드가 생기면 그 부분만 정리)

### 2. 진단 로그 강화

`QdrantVectorStoreService.search(...)`(176줄)에 필터링 **전** Qdrant 원본을 노출한다:

- 검색 직후(필터 전) `scoredPoints`의 **원본 건수**를 INFO로 출력.
- 각 원본 포인트의 **score** + 식별 가능한 payload 일부(문서 제목/청크 앞부분) + `minScore` 통과 여부.
- 컬렉션 이름과 `countPoints(collectionName)` 결과(컬렉션 내 실제 포인트 수)를 함께 출력 → admin의 49청크가 Qdrant에 실재하는지 대조.

기대 출력 (시나리오 A — 필터 탓):

```
[RAG-Search] collection=chatbot-docs, 컬렉션 포인트 수=49, minScore=0.5, topK=3
[RAG-Search] Qdrant 원본 3건:
  - score=0.42 PASS=N payload="서새찬은 ..."
  - score=0.39 PASS=N payload="새찬 서버 실험실 ..."
[RAG-Search] 필터 후 0건 (원본 3건 중 minScore=0.5 미달로 전부 제외)
```

기대 출력 (시나리오 B — 원본부터 0):

```
[RAG-Search] collection=chatbot-docs, 컬렉션 포인트 수=0, minScore=0.5, topK=3
[RAG-Search] Qdrant 원본 0건 → 컬렉션이 비었거나 차원/컬렉션 불일치 의심
```

로그 레벨은 **INFO**(운영에서 보여야 함), payload는 앞부분만 잘라 출력. **반환값·시그니처·필터 동작은 변경하지 않는다** — 관측만 추가.

### 3. `minScore` / `topK` ServerOption 승격

#### 3-1. 키 추가 (`ServerOptionKey.java`)

| 키 | description | defaultValue |
|---|---|---|
| `CHATBOT_RAG_MIN_SCORE` | RAG 벡터 검색 최소 유사도 점수 (0.0~1.0) | `"0.5"` |
| `CHATBOT_RAG_TOP_K` | RAG 벡터 검색 시 반환할 최대 문서 수 | `"3"` |

기본값은 현재 하드코딩 값과 동일(0.5, 3). 값 조정은 사용자가 배포 후 admin에서 직접 수행(예: 0.5→0.3).

#### 3-2. 조회 지점 변경 (`ChatbotService`)

`minScore`/`topK` 폴백 출처를 `chatbotProperties` → `ServerOption`으로 변경. 우선순위(기존 의미 유지):

```
요청에 명시된 값  >  ServerOption 값  >  (방어적) 기본값
```

- 비스트리밍(119-120줄), 스트리밍(233-234줄) 모두 폴백을 `serverOptionService.getOptionValue(...)` 파싱값으로.
- 파싱(`Integer.parseInt`/`Float.parseFloat`) 실패 시 현재 하드코딩 기본값(3/0.5f)으로 폴백해 검색이 죽지 않게 한다.

#### 3-3. `ChatbotProperties`의 기존 필드 처리

`rag.topK`/`rag.minScore` 필드는 **삭제하지 않는다.** ServerOption 파싱 실패 시의 방어적 기본값 출처로 남겨 surgical scope를 유지한다.

## 영향 범위 (파일 단위)

| 파일 | 변경 |
|---|---|
| `Suh-Common/.../constant/ServerOptionKey.java` | `CHATBOT_RAG_MIN_SCORE`, `CHATBOT_RAG_TOP_K` 추가 |
| `Suh-Domain-Chatbot/.../service/ChatbotService.java` | (1) `getSearchQuery`/비스트리밍 쿼리 선택을 원본 메시지 기반으로 변경 (2) `minScore`/`topK` 폴백 출처를 ServerOption으로 변경 (2개 지점) |
| `Suh-Domain-Chatbot/.../service/QdrantVectorStoreService.java` | `search(...)`에 필터 전 원본/score/컬렉션 포인트 수 INFO 로그 추가 |

- DB 마이그레이션(Flyway): **불필요.** ServerOptionKey 추가는 테이블 스키마를 바꾸지 않는다(런타임 기본값 주입). 구현 시 ServerOption 초기화 방식을 재확인한다.

## 검증 (Definition of Done)

1. **빌드/기동**: `Suh-Common`, `Suh-Domain-Chatbot` 컴파일·기동 성공.
2. **원본 질문 검색**: "너는 누구야?" 질문 시 로그의 RAG 검색 쿼리가 더 이상 `SUH Project Utility 소개 ...`가 아니라 **원본 질문("너는 누구야?")** 으로 찍힌다.
3. **진단 로그**: 같은 질문에서 컬렉션 포인트 수 / Qdrant 원본 건수(필터 전) / 각 원본 score+통과여부가 모두 출력되어, 시나리오 **A**(필터 후 0)인지 **B**(원본부터 0)인지 판별 가능.
4. **검색 성공 확인**: 원본 질문 검색으로 `서새찬 개발자` 등 관련 청크가 결과에 포함되는지 확인. (포함되면 결함 수정 성공. 여전히 0건이면 로그로 A/B 확정 → Phase 2)
5. **운영 조정**: admin에서 `CHATBOT_RAG_MIN_SCORE`를 0.3으로 낮춘 뒤 같은 질문 → (시나리오 A인 경우) 이전에 걸러지던 청크가 결과에 포함됨을 확인.
6. **ServerOption 노출**: admin에 `CHATBOT_RAG_MIN_SCORE`, `CHATBOT_RAG_TOP_K`가 나타나고 편집 가능.

## 다음 단계 (Phase 2, 본 spec 범위 밖)

Phase 1 결과(진단 로그 + 원본 질문 검색)를 보고 별도 spec 작성:

- **시나리오 A 확정 시**: minScore 적정값 튜닝, 짧은 질문 대응, 의도 분류 프롬프트의 searchQuery 지시 정리.
- **시나리오 B 확정 시**: Qdrant 컬렉션 재색인, 임베딩 차원/컬렉션 정합성 점검, 색인 파이프라인 결함 수정.
- **구조 단순화 검토**: 원본 질문 검색만으로 충분하면, 과복잡한 의도 분류(7필드·confidence 재시도)를 간소화할지 측정 기반으로 판단.
- **전제 원칙 후속**: `CONFIDENCE_THRESHOLD`, `history.maxMessages`, SSE 타임아웃 등 잔여 하드코딩값 순차 승격.
