# 버스 노선 외부 API 동기화 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 정적 하드코딩 노선 데이터를 외부 buseezy API(`csios.busin.co.kr/driving/drivelist.aspx`) 응답으로부터 동기화하는 방식으로 변경.

**Architecture:** Spring Boot 멀티모듈. `Suh-Domain-Somansa-Bus` 도메인 모듈에 sync 로직 + Jsoup HTML 파싱. 트리거 회원 정보는 `ServerOptionKey` enum + `ServerOption` entity 로 관리 (시스템 동적 설정 패턴). 충돌 처리는 `disptid` 기준 upsert + soft delete (`isActive=false`). UI 는 `somansaBusDashboard.html` 에 sync 버튼 + 마지막 동기화 시각 표시.

**Tech Stack:** Java 17, Spring Boot 3.4.2, Spring Data JPA, OkHttp 4.x, Jsoup 1.15.4, Lombok, Thymeleaf.

**관련 이슈:** [#188](https://github.com/Cassiiopeia/suh-project-utility/issues/188)
**Spec:** `docs/superpowers/specs/2026-05-08-bus-route-api-sync-design.md`

---

## File Structure

### 신규 파일
| 경로 | 책임 |
|------|------|
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/RouteData.java` | 외부 HTML 파싱 결과 DTO (1개 노선 분량) |
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/RouteLabelParts.java` | 라벨 텍스트 파싱 결과 DTO (departureTime/station/busNumber) |
| `Suh-Domain-Somansa-Bus/src/test/java/me/suhsaechan/somansabus/service/SomansaBusApiServiceParseTest.java` | HTML 파싱 + 라벨 파싱 단위 테스트 (Spring 의존성 없음, 순수 Jsoup) |

### 수정 파일
| 경로 | 변경 |
|------|------|
| `Suh-Common/src/main/java/me/suhsaechan/common/constant/ServerOptionKey.java` | `SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID` enum 키 추가 |
| `Suh-Common/src/main/java/me/suhsaechan/common/exception/ErrorCode.java` | `SOMANSA_BUS_API_FETCH_FAILED` 추가 (login/session 은 기존 재사용) |
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/entity/SomansaBusRoute.java` | `Boolean isShuttle` 필드 추가 |
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/SomansaBusResponse.java` | sync 결과 6개 필드 + `lastSyncedAt` 1개 추가 |
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusApiService.java` | 도메인 `cs.android.busin.co.kr` → `csios.busin.co.kr` 일괄 갱신 (URL 4개 + Referer 2개) + `fetchRouteList()`/`parseRouteList()`/`parseLabel()` 추가 |
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusRouteService.java` | `syncRoutes()` 추가, `initializeRoutes()` (dead code) 제거, `getAllRoutes()` 에 `lastSyncedAt` 추가 |
| `Suh-Web/src/main/java/me/suhsaechan/web/controller/api/SomansaBusController.java` | `POST /api/somansa-bus/route/sync` endpoint 추가 |
| `Suh-Web/src/main/resources/templates/pages/somansaBusDashboard.html` | sync 버튼 + 마지막 동기화 시각 + JS 핸들러 |

### 제거 파일
| 경로 | 이유 |
|------|------|
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/config/SomansaBusDataInitializer.java` | 외부 API 동기화로 대체. 하드코딩 시드 제거 |

---

## Task 의존성 그래프

```
T1 (ServerOptionKey)  ─┐
T2 (ErrorCode)         ├─→ T6 (RouteService.syncRoutes) ──→ T7 (Controller endpoint) ──→ T8 (Dashboard UI)
T3 (Entity isShuttle)  ┤                                                                       
T4 (Response DTO)      ┤                                                                       
T5 (RouteData / RouteLabelParts DTO + ApiService 파싱) ─┘                                       
                                                                                               
T9 (SomansaBusDataInitializer 제거)  ← 독립, 마지막에                                            
```

**병렬 가능**: T1, T2, T3, T4, T5 (서로 독립). T6 은 T1~T5 모두 완료 후. T7 은 T6 후. T8 은 T7 후. T9 는 언제든 가능 (마지막 안전).

---

## Task 1: `ServerOptionKey` 신규 키 추가

**Files:**
- Modify: `Suh-Common/src/main/java/me/suhsaechan/common/constant/ServerOptionKey.java`

- [ ] **Step 1: 현재 enum 읽기**

```bash
cat Suh-Common/src/main/java/me/suhsaechan/common/constant/ServerOptionKey.java
```

확인: 현재 `CHATBOT_*` 4개 enum 항목 존재.

- [ ] **Step 2: 신규 키 추가**

기존 마지막 항목 `CHATBOT_RESPONSE_GENERATOR_MODEL("챗봇 응답 생성 모델", "rnj-1:8b");` 의 세미콜론을 콤마로 바꾸고 새 항목 추가:

```java
@Getter
@RequiredArgsConstructor
public enum ServerOptionKey {
  CHATBOT_CHUNK_SIZE("챗봇 청크 크기 (토큰 수)", "500"),
  CHATBOT_CHUNK_OVERLAP("챗봇 청크 중첩 크기 (토큰 수)", "100"),
  CHATBOT_INTENT_CLASSIFIER_MODEL("챗봇 의도 분류 모델", "gemma3:1b"),
  CHATBOT_RESPONSE_GENERATOR_MODEL("챗봇 응답 생성 모델", "rnj-1:8b"),
  SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID("소만사 버스 노선 동기화 트리거 회원 loginId", "chan4760@somansa.com");

  private final String description;
  private final String defaultValue;
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :Suh-Common:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋 (사용자 승인 후만)**

⚠️ Git 커밋 절대 금지 규칙 — 사용자 명시 승인 받은 후에만 진행.

```bash
git add Suh-Common/src/main/java/me/suhsaechan/common/constant/ServerOptionKey.java
git commit -m "정적 노선 데이터를 외부 API 동기화 방식으로 변경 : feat : ServerOptionKey 에 SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID 추가 https://github.com/Cassiiopeia/suh-project-utility/issues/188"
```

---

## Task 2: `ErrorCode` 신규 항목 추가

**Files:**
- Modify: `Suh-Common/src/main/java/me/suhsaechan/common/exception/ErrorCode.java`

기존 `SOMANSA_BUS_LOGIN_FAILED`, `SOMANSA_BUS_SESSION_FAILED` 는 재사용. fetch 실패만 신규 추가.

- [ ] **Step 1: 기존 SOMANSA_BUS 섹션 위치 확인**

```bash
grep -n "SOMANSA_BUS" Suh-Common/src/main/java/me/suhsaechan/common/exception/ErrorCode.java
```

Expected:
```
83:  SOMANSA_BUS_MEMBER_NOT_FOUND...
84:  SOMANSA_BUS_MEMBER_ALREADY_EXISTS...
...
89:  SOMANSA_BUS_LOGIN_FAILED...
90:  SOMANSA_BUS_SESSION_FAILED...
```

- [ ] **Step 2: `SOMANSA_BUS_API_FETCH_FAILED` 추가**

`SOMANSA_BUS_SESSION_FAILED` 다음 줄에 추가:

```java
  SOMANSA_BUS_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "버스 예약 시스템 로그인에 실패했습니다."),
  SOMANSA_BUS_SESSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "버스 예약 세션 생성에 실패했습니다."),
  SOMANSA_BUS_API_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "버스인 노선 조회 API 호출에 실패했습니다."),
```

(콤마/세미콜론 위치는 ErrorCode 파일 마지막 라인이 어떤 enum 인지 보고 맞춤. 본 항목이 마지막이 아니라면 콤마, 마지막이라면 세미콜론)

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :Suh-Common:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋 (사용자 승인 후만)**

```bash
git add Suh-Common/src/main/java/me/suhsaechan/common/exception/ErrorCode.java
git commit -m "정적 노선 데이터를 외부 API 동기화 방식으로 변경 : feat : ErrorCode 에 SOMANSA_BUS_API_FETCH_FAILED 추가 https://github.com/Cassiiopeia/suh-project-utility/issues/188"
```

---

## Task 3: `SomansaBusRoute` Entity 에 `isShuttle` 필드 추가

**Files:**
- Modify: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/entity/SomansaBusRoute.java`

- [ ] **Step 1: 현재 entity 확인**

```bash
cat Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/entity/SomansaBusRoute.java
```

확인: 현재 `isActive` 가 마지막 필드 (라인 42 근처).

- [ ] **Step 2: `isShuttle` 필드 추가**

기존 `isActive` 필드 직전에 추가 (또는 직후, 자연스러운 곳):

```java
  @Column(nullable = false)
  @Builder.Default
  private Boolean isShuttle = false;

  @Column(nullable = false)
  private Boolean isActive;
```

> `@Builder.Default` 필요 — `@SuperBuilder` + 기본값 조합. 안 붙이면 빌더 사용 시 `false` 가 안 들어가고 `null` 됨.

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :Suh-Domain-Somansa-Bus:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋 (사용자 승인 후만)**

```bash
git add Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/entity/SomansaBusRoute.java
git commit -m "정적 노선 데이터를 외부 API 동기화 방식으로 변경 : feat : SomansaBusRoute 에 isShuttle 필드 추가 https://github.com/Cassiiopeia/suh-project-utility/issues/188"
```

---

## Task 4: `SomansaBusResponse` 에 sync 결과 필드 추가

**Files:**
- Modify: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/SomansaBusResponse.java`

- [ ] **Step 1: 현재 Response 확인**

```bash
cat Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/SomansaBusResponse.java
```

- [ ] **Step 2: import 추가 + 필드 추가**

파일 상단 import 추가:

```java
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
```

마지막 필드(`thisWeekFailedReservations`) 뒤에 추가:

```java
    private Integer thisWeekFailedReservations;

    // 노선 동기화 결과
    private Integer syncCreatedCount;
    private Integer syncUpdatedCount;
    private Integer syncDeactivatedCount;
    private Integer syncTotalCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime syncedAt;

    private String syncTriggerLoginId;

    // 노선 list 조회 시 마지막 동기화 시각
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSyncedAt;
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :Suh-Domain-Somansa-Bus:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋 (사용자 승인 후만)**

```bash
git add Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/SomansaBusResponse.java
git commit -m "정적 노선 데이터를 외부 API 동기화 방식으로 변경 : feat : SomansaBusResponse 에 sync 결과 필드 7개 추가 https://github.com/Cassiiopeia/suh-project-utility/issues/188"
```

---

## Task 5: 신규 DTO + ApiService 파싱 로직 + 도메인 일괄 갱신

**Files:**
- Create: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/RouteData.java`
- Create: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/RouteLabelParts.java`
- Modify: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusApiService.java`
- Test: `Suh-Domain-Somansa-Bus/src/test/java/me/suhsaechan/somansabus/service/SomansaBusApiServiceParseTest.java`

### Step 1: `RouteData` DTO 생성

- [ ] **Step 1: 새 파일 작성**

`Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/RouteData.java`:

```java
package me.suhsaechan.somansabus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteData {
  private Integer disptid;
  private String caralias;
  private Boolean isShuttle;
  private String description;
  private String departureTime;
  private String station;
  private Integer busNumber;
}
```

### Step 2: `RouteLabelParts` DTO 생성

- [ ] **Step 2: 새 파일 작성**

`Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/RouteLabelParts.java`:

```java
package me.suhsaechan.somansabus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteLabelParts {
  private String departureTime;
  private String station;
  private Integer busNumber;

  public static RouteLabelParts empty() {
    return RouteLabelParts.builder().build();
  }
}
```

### Step 3: 단위 테스트 먼저 작성 (TDD)

- [ ] **Step 3: 파싱 단위 테스트 작성**

`Suh-Domain-Somansa-Bus/src/test/java/me/suhsaechan/somansabus/service/SomansaBusApiServiceParseTest.java`:

```java
package me.suhsaechan.somansabus.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import me.suhsaechan.somansabus.dto.RouteData;
import me.suhsaechan.somansabus.dto.RouteLabelParts;
import org.junit.jupiter.api.Test;

class SomansaBusApiServiceParseTest {

  private final SomansaBusApiService service = new SomansaBusApiService();

  @Test
  void parseLabel_출근_정상() {
    RouteLabelParts parts = service.parseLabelForTest("06:55 당산역 1호 - 출근");
    assertAll(
        () -> assertEquals("06:55", parts.getDepartureTime()),
        () -> assertEquals("당산역", parts.getStation()),
        () -> assertEquals(1, parts.getBusNumber())
    );
  }

  @Test
  void parseLabel_셔틀_busNumber_없음() {
    RouteLabelParts parts = service.parseLabelForTest("07:30 판교역 - 셔틀");
    assertAll(
        () -> assertEquals("07:30", parts.getDepartureTime()),
        () -> assertEquals("판교역", parts.getStation()),
        () -> assertNull(parts.getBusNumber())
    );
  }

  @Test
  void parseLabel_텍스트_앞뒤_공백_제거() {
    RouteLabelParts parts = service.parseLabelForTest("   17:45  당산역  1호 -  퇴근   ");
    assertEquals("17:45", parts.getDepartureTime());
    assertEquals("당산역", parts.getStation());
    assertEquals(1, parts.getBusNumber());
  }

  @Test
  void parseLabel_빈문자열_empty_반환() {
    RouteLabelParts parts = service.parseLabelForTest("");
    assertAll(
        () -> assertNull(parts.getDepartureTime()),
        () -> assertNull(parts.getStation()),
        () -> assertNull(parts.getBusNumber())
    );
  }

  @Test
  void parseRouteList_HTML_파싱() {
    String html = """
        <html><body>
        <div class="line-list">
          <ul>
            <li>
              <a href="#n" onclick="goPage(47040,  '출근' ,  'False')" class="cont">
                <span class="workshift forward">출근</span>
                <span class="text">06:55 당산역 1호 - 출근</span>
              </a>
            </li>
            <li>
              <a href="#n" onclick="goPage(46124,  '출근' ,  'True')" class="cont">
                <span class="workshift forward">셔틀</span>
                <span class="text">07:30 판교역 - 셔틀</span>
              </a>
            </li>
          </ul>
        </div>
        </body></html>
        """;

    List<RouteData> routes = service.parseRouteListForTest(html);

    assertEquals(2, routes.size());

    RouteData first = routes.get(0);
    assertAll(
        () -> assertEquals(47040, first.getDisptid()),
        () -> assertEquals("출근", first.getCaralias()),
        () -> assertFalse(first.getIsShuttle()),
        () -> assertEquals("06:55 당산역 1호 - 출근", first.getDescription()),
        () -> assertEquals("06:55", first.getDepartureTime()),
        () -> assertEquals("당산역", first.getStation()),
        () -> assertEquals(1, first.getBusNumber())
    );

    RouteData shuttle = routes.get(1);
    assertAll(
        () -> assertEquals(46124, shuttle.getDisptid()),
        () -> assertTrue(shuttle.getIsShuttle()),
        () -> assertNull(shuttle.getBusNumber())
    );
  }
}
```

> **주의**: `service.parseLabelForTest(...)`, `service.parseRouteListForTest(...)` 는 다음 step 에서 ApiService 에 package-private 으로 추가할 메서드. private 메서드를 직접 테스트하기 위한 thin wrapper.

### Step 4: 테스트 실행 → 실패 확인

- [ ] **Step 4: FAIL 확인**

```bash
./gradlew :Suh-Domain-Somansa-Bus:test --tests "me.suhsaechan.somansabus.service.SomansaBusApiServiceParseTest"
```

Expected: FAIL — `parseLabelForTest`, `parseRouteListForTest` 메서드 없음

### Step 5: ApiService 도메인 일괄 갱신 + 파싱 로직 추가

- [ ] **Step 5: 현재 ApiService 확인**

```bash
cat Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusApiService.java
```

확인: 라인 29~38 의 4개 `@Value`, 라인 122 (createSession Referer), 라인 154 (makeReservation Referer) 의 `cs.android.busin.co.kr` 값.

- [ ] **Step 6: 도메인 갱신 + 파싱 메서드 추가**

`SomansaBusApiService.java` 전체 교체:

```java
package me.suhsaechan.somansabus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.somansabus.config.SomansaBusHttpClient;
import me.suhsaechan.somansabus.dto.RouteData;
import me.suhsaechan.somansabus.dto.RouteLabelParts;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusApiService {

  @Value("${somansa.busin.url.login-page:https://csios.busin.co.kr/Login.aspx?device=}")
  private String loginPageUrl;

  @Value("${somansa.busin.url.login-api:https://csios.busin.co.kr/Login.aspx/LoginCheck}")
  private String loginUrl;

  @Value("${somansa.busin.url.create-session:https://csios.busin.co.kr/Default.aspx/CreateSession}")
  private String createSessionUrl;

  @Value("${somansa.busin.url.reservation:https://csios.busin.co.kr/driving/ride_on.aspx/Reserv}")
  private String reservationUrl;

  @Value("${somansa.busin.url.drivelist:https://csios.busin.co.kr/driving/drivelist.aspx}")
  private String drivelistUrl;

  private static final String PUSH_ID = "pc";
  private static final String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final Pattern GO_PAGE_PATTERN = Pattern.compile("goPage\\((\\d+),\\s*'([^']+)'\\s*,\\s*'(True|False)'\\)");
  private static final Pattern LABEL_PATTERN = Pattern.compile("^\\s*(\\d{2}:\\d{2})\\s+(.+?)(?:\\s+(\\d+)호)?\\s*-.*$");

  private final OkHttpClient client = SomansaBusHttpClient.getClient();

  public int login(String loginId) {
    log.info("버스 예약 로그인 시작: {}", loginId);

    Request getRequest = new Request.Builder()
        .url(loginPageUrl)
        .get()
        .addHeader("User-Agent", USER_AGENT)
        .build();

    try (Response getResponse = client.newCall(getRequest).execute()) {
      if (!getResponse.isSuccessful()) {
        log.error("로그인 페이지 GET 실패, 코드: {}", getResponse.code());
        return -1;
      }
      log.debug("로그인 페이지 GET 성공, 세션 쿠키 획득");
    } catch (IOException e) {
      log.error("로그인 페이지 GET 중 예외 발생", e);
      return -1;
    }

    String payloadData = String.format("{ \"data\": \"%s,%s\" }", loginId, PUSH_ID);
    log.debug("로그인 페이로드: {}", payloadData);

    MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    RequestBody body = RequestBody.create(payloadData, mediaType);

    Request postRequest = new Request.Builder()
        .url(loginUrl)
        .post(body)
        .addHeader("Content-Type", "application/json; charset=UTF-8")
        .addHeader("User-Agent", USER_AGENT)
        .addHeader("Referer", loginPageUrl)
        .build();

    try (Response response = client.newCall(postRequest).execute()) {
      if (!response.isSuccessful()) {
        log.error("로그인 POST 실패, 코드: {}", response.code());
        return -1;
      }

      String responseBody = response.body().string();
      log.debug("로그인 응답: {}", responseBody);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(responseBody);
      int passengerId = rootNode.path("d").asInt();

      if (passengerId > 0) {
        log.info("로그인 성공: {}, 승객ID: {}", loginId, passengerId);
      } else {
        log.warn("로그인 실패: {}, 응답 값: {}", loginId, passengerId);
      }

      return passengerId;
    } catch (IOException e) {
      log.error("로그인 POST 중 예외 발생", e);
      return -1;
    }
  }

  public boolean createSession(String rideId, int passengerId) {
    log.info("세션 생성 시작: rideId={}, passengerId={}", rideId, passengerId);

    String data = String.format("%s,%d,,%s", rideId, passengerId, PUSH_ID);
    String payload = String.format("{ \"data\": \"%s\" }", data);
    log.debug("세션 생성 페이로드: {}", payload);

    MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    RequestBody body = RequestBody.create(payload, mediaType);

    Request request = new Request.Builder()
        .url(createSessionUrl)
        .post(body)
        .addHeader("Content-Type", "application/json; charset=UTF-8")
        .addHeader("User-Agent", USER_AGENT)
        .addHeader("Referer", "https://csios.busin.co.kr/default.aspx?device=")
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error("세션 생성 실패, 코드: {}", response.code());
        return false;
      }
      log.info("세션 생성 성공: rideId={}, passengerId={}", rideId, passengerId);
      return true;
    } catch (IOException e) {
      log.error("세션 생성 중 예외 발생", e);
      return false;
    }
  }

  public boolean makeReservation(int passengerId, SomansaBusRoute route, LocalDate reservationDate) {
    String formattedDate = reservationDate.format(DATE_FORMATTER);
    log.info("예약 시작 - 승객ID: {}, 버스: {}, 예약일: {}", passengerId, route.getDescription(), formattedDate);

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode payload = mapper.createObjectNode();
    payload.put("passengerid", passengerId);
    payload.put("disptid", route.getDisptid());
    payload.put("caralias", route.getCaralias());
    payload.put("clientid", "busman");
    payload.put("createdate", formattedDate);

    String payloadJson = payload.toString();
    log.debug("예약 페이로드: {}", payloadJson);

    String encodedShuttleType = URLEncoder.encode(route.getCaralias(), StandardCharsets.UTF_8);
    String refererUrl = "https://csios.busin.co.kr/driving/ride_on.aspx?rt=r&disptid=" + route.getDisptid() +
        "&shuttletype=" + encodedShuttleType + "&scr=0&isshuttle=0";
    log.debug("Referer URL: {}", refererUrl);

    MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    RequestBody body = RequestBody.create(payloadJson, mediaType);

    Request request = new Request.Builder()
        .url(reservationUrl)
        .post(body)
        .addHeader("Content-Type", "application/json; charset=UTF-8")
        .addHeader("User-Agent", USER_AGENT)
        .addHeader("Referer", refererUrl)
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        String responseBody = response.body() != null ? response.body().string() : "응답 없음";
        log.error("예약 실패 - HTTP 상태 코드: {}, 응답: {}", response.code(), responseBody);
        return false;
      }

      String responseBody = response.body() != null ? response.body().string() : "";
      log.debug("예약 응답: {}", responseBody);
      log.info("예약 성공 - 버스: {}, 예약일: {}", route.getDescription(), formattedDate);

      return true;
    } catch (IOException e) {
      log.error("예약 중 예외 발생", e);
      return false;
    }
  }

  public List<RouteData> fetchRouteList() {
    log.info("노선 목록 조회 시작");

    Request request = new Request.Builder()
        .url(drivelistUrl)
        .get()
        .addHeader("User-Agent", USER_AGENT)
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error("노선 목록 조회 실패, 코드: {}", response.code());
        throw new CustomException(ErrorCode.SOMANSA_BUS_API_FETCH_FAILED);
      }
      String html = response.body() != null ? response.body().string() : "";
      List<RouteData> result = parseRouteList(html);
      log.info("노선 목록 조회 완료: {}개", result.size());
      return result;
    } catch (IOException e) {
      log.error("노선 목록 조회 중 예외 발생", e);
      throw new CustomException(ErrorCode.SOMANSA_BUS_API_FETCH_FAILED);
    }
  }

  List<RouteData> parseRouteList(String html) {
    Document doc = Jsoup.parse(html);
    List<RouteData> result = new ArrayList<>();

    for (Element li : doc.select(".line-list li")) {
      Element a = li.selectFirst("a.cont");
      if (a == null) continue;

      String onclick = a.attr("onclick");
      Matcher m = GO_PAGE_PATTERN.matcher(onclick);
      if (!m.find()) continue;

      int disptid = Integer.parseInt(m.group(1));
      String caralias = m.group(2);
      boolean isShuttle = "True".equals(m.group(3));

      Element textSpan = a.selectFirst("span.text");
      String description = textSpan != null ? textSpan.text().trim() : "";

      RouteLabelParts parts = parseLabel(description);

      result.add(RouteData.builder()
          .disptid(disptid)
          .caralias(caralias)
          .isShuttle(isShuttle)
          .description(description)
          .departureTime(parts.getDepartureTime())
          .station(parts.getStation())
          .busNumber(parts.getBusNumber())
          .build());
    }
    return result;
  }

  RouteLabelParts parseLabel(String description) {
    if (description == null) return RouteLabelParts.empty();
    Matcher m = LABEL_PATTERN.matcher(description);
    if (!m.find()) return RouteLabelParts.empty();
    return RouteLabelParts.builder()
        .departureTime(m.group(1))
        .station(m.group(2).trim())
        .busNumber(m.group(3) != null ? Integer.parseInt(m.group(3)) : null)
        .build();
  }

  // package-private test wrapper (private 메서드 테스트용)
  RouteLabelParts parseLabelForTest(String description) {
    return parseLabel(description);
  }

  List<RouteData> parseRouteListForTest(String html) {
    return parseRouteList(html);
  }
}
```

> 주의:
> - `parseLabel` / `parseRouteList` 는 package-private (`final` 없음, 접근 제어자 없음). 같은 패키지 테스트 가능.
> - `parseLabelForTest` / `parseRouteListForTest` 는 명시적 wrapper. 단순 위임이라 중복 같지만 테스트 가독성 위해 둠.
> - `OkHttpClient client = SomansaBusHttpClient.getClient()` 는 `final` 안 붙어있음 (기존 코드 그대로 유지)
> - `LABEL_PATTERN` 의 `(.+?)` 는 lazy quantifier. busNumber 가 있으면 station 까지 매칭, 없으면 station 이 다음 ` -` 까지 캡처. `.trim()` 으로 끝의 공백 제거.

### Step 7: 테스트 다시 실행 → PASS 확인

- [ ] **Step 7: PASS 확인**

```bash
./gradlew :Suh-Domain-Somansa-Bus:test --tests "me.suhsaechan.somansabus.service.SomansaBusApiServiceParseTest"
```

Expected: BUILD SUCCESSFUL — 5 tests passed

### Step 8: 커밋 (사용자 승인 후만)

- [ ] **Step 8**

```bash
git add Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/RouteData.java
git add Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/RouteLabelParts.java
git add Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusApiService.java
git add Suh-Domain-Somansa-Bus/src/test/java/me/suhsaechan/somansabus/service/SomansaBusApiServiceParseTest.java
git commit -m "정적 노선 데이터를 외부 API 동기화 방식으로 변경 : feat : SomansaBusApiService 도메인 csios.busin.co.kr 일괄 갱신 및 fetchRouteList HTML 파싱 추가 https://github.com/Cassiiopeia/suh-project-utility/issues/188"
```

---

## Task 6: `SomansaBusRouteService.syncRoutes()` 추가 + dead code 제거 + `getAllRoutes()` 에 `lastSyncedAt` 추가

**Files:**
- Modify: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusRouteService.java`

**의존**: T1, T2, T3, T4, T5 모두 완료 후 진행.

- [ ] **Step 1: 현재 RouteService 확인**

```bash
cat Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusRouteService.java
```

확인:
- `getAllRoutes()`, `getRoutesByType(request)`, `getRouteById(id)` 메서드 존재
- `initializeRoutes()` (호출 없는 dead code, 50줄 가량)

- [ ] **Step 2: RouteService 전체 교체**

```java
package me.suhsaechan.somansabus.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.entity.ServerOption;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.somansabus.dto.RouteData;
import me.suhsaechan.somansabus.dto.SomansaBusRequest;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import me.suhsaechan.somansabus.repository.SomansaBusRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusRouteService {

  private final SomansaBusRouteRepository routeRepository;
  private final SomansaBusApiService apiService;
  private final ServerOptionService serverOptionService;

  @Transactional(readOnly = true)
  public SomansaBusResponse getAllRoutes() {
    log.info("전체 버스 노선 조회");
    List<SomansaBusRoute> routes = routeRepository.findByIsActiveTrueOrderByDepartureTimeAsc();
    LocalDateTime lastSyncedAt = routes.stream()
        .map(SomansaBusRoute::getUpdatedDate)
        .filter(d -> d != null)
        .max(LocalDateTime::compareTo)
        .orElse(null);
    return SomansaBusResponse.builder()
        .routes(routes)
        .totalCount((long) routes.size())
        .lastSyncedAt(lastSyncedAt)
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getRoutesByType(SomansaBusRequest request) {
    log.info("버스 노선 타입별 조회: {}", request.getCaralias());
    List<SomansaBusRoute> routes = routeRepository.findByCaraliasAndIsActiveTrue(request.getCaralias());
    return SomansaBusResponse.builder()
        .routes(routes)
        .totalCount((long) routes.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getRouteById(UUID routeId) {
    log.info("버스 노선 상세 조회: {}", routeId);
    SomansaBusRoute route = routeRepository.findById(routeId)
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_ROUTE_NOT_FOUND));
    return SomansaBusResponse.builder()
        .route(route)
        .build();
  }

  @Transactional
  public SomansaBusResponse syncRoutes() {
    String triggerLoginId = resolveTriggerLoginId();
    log.info("버스 노선 동기화 시작 - 트리거 회원: {}", triggerLoginId);

    int passengerId = apiService.login(triggerLoginId);
    if (passengerId <= 0) {
      log.error("동기화 로그인 실패: {}", triggerLoginId);
      throw new CustomException(ErrorCode.SOMANSA_BUS_LOGIN_FAILED);
    }

    boolean sessionCreated = apiService.createSession(triggerLoginId, passengerId);
    if (!sessionCreated) {
      log.error("동기화 세션 생성 실패: {}", triggerLoginId);
      throw new CustomException(ErrorCode.SOMANSA_BUS_SESSION_FAILED);
    }

    List<RouteData> remoteRoutes = apiService.fetchRouteList();
    log.info("외부 노선 응답 수신: {}개", remoteRoutes.size());

    Set<Integer> remoteDisptids = new HashSet<>();
    int created = 0;
    int updated = 0;
    int deactivated = 0;

    // upsert
    for (RouteData remote : remoteRoutes) {
      remoteDisptids.add(remote.getDisptid());

      Optional<SomansaBusRoute> existingOpt = routeRepository.findByDisptid(remote.getDisptid());
      if (existingOpt.isPresent()) {
        SomansaBusRoute existing = existingOpt.get();
        boolean changed = applyRemoteToEntity(existing, remote);
        boolean reactivated = false;
        if (Boolean.FALSE.equals(existing.getIsActive())) {
          existing.setIsActive(true);
          reactivated = true;
        }
        if (changed || reactivated) {
          routeRepository.save(existing);
          updated++;
        }
      } else {
        SomansaBusRoute newRoute = SomansaBusRoute.builder()
            .disptid(remote.getDisptid())
            .description(remote.getDescription())
            .caralias(remote.getCaralias())
            .departureTime(remote.getDepartureTime())
            .station(remote.getStation())
            .busNumber(remote.getBusNumber())
            .isShuttle(Boolean.TRUE.equals(remote.getIsShuttle()))
            .isActive(true)
            .build();
        routeRepository.save(newRoute);
        created++;
      }
    }

    // soft delete
    List<SomansaBusRoute> activeRoutes = routeRepository.findByIsActiveTrue();
    for (SomansaBusRoute existing : activeRoutes) {
      if (!remoteDisptids.contains(existing.getDisptid())) {
        existing.setIsActive(false);
        routeRepository.save(existing);
        deactivated++;
      }
    }

    long totalCount = routeRepository.count();
    log.info("버스 노선 동기화 완료 - 신규: {}, 변경: {}, 비활성: {}, 총: {}",
        created, updated, deactivated, totalCount);

    return SomansaBusResponse.builder()
        .syncCreatedCount(created)
        .syncUpdatedCount(updated)
        .syncDeactivatedCount(deactivated)
        .syncTotalCount((int) totalCount)
        .syncedAt(LocalDateTime.now())
        .syncTriggerLoginId(triggerLoginId)
        .build();
  }

  private String resolveTriggerLoginId() {
    ServerOptionKey key = ServerOptionKey.SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID;
    ServerOption option = serverOptionService.getOption(key);
    if (option != null && option.getOptionValue() != null && !option.getOptionValue().isBlank()) {
      return option.getOptionValue();
    }
    return key.getDefaultValue();
  }

  private boolean applyRemoteToEntity(SomansaBusRoute entity, RouteData remote) {
    boolean changed = false;
    if (!equalsNullable(entity.getDescription(), remote.getDescription())) {
      entity.setDescription(remote.getDescription());
      changed = true;
    }
    if (!equalsNullable(entity.getCaralias(), remote.getCaralias())) {
      entity.setCaralias(remote.getCaralias());
      changed = true;
    }
    if (!equalsNullable(entity.getDepartureTime(), remote.getDepartureTime())) {
      entity.setDepartureTime(remote.getDepartureTime());
      changed = true;
    }
    if (!equalsNullable(entity.getStation(), remote.getStation())) {
      entity.setStation(remote.getStation());
      changed = true;
    }
    if (!equalsNullable(entity.getBusNumber(), remote.getBusNumber())) {
      entity.setBusNumber(remote.getBusNumber());
      changed = true;
    }
    boolean remoteIsShuttle = Boolean.TRUE.equals(remote.getIsShuttle());
    boolean entityIsShuttle = Boolean.TRUE.equals(entity.getIsShuttle());
    if (entityIsShuttle != remoteIsShuttle) {
      entity.setIsShuttle(remoteIsShuttle);
      changed = true;
    }
    return changed;
  }

  private boolean equalsNullable(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    return a.equals(b);
  }
}
```

> 변경 사항:
> - `initializeRoutes()` 메서드 (50줄, dead code) **완전 제거**
> - `apiService`, `serverOptionService` 의존성 추가
> - `getAllRoutes()` 응답에 `lastSyncedAt` (max `updatedDate`) 추가
> - `syncRoutes()` 신규 메서드 — login → createSession → fetchRouteList → upsert + soft delete

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :Suh-Domain-Somansa-Bus:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 기존 테스트(있으면) 회귀 확인**

```bash
./gradlew :Suh-Domain-Somansa-Bus:test
```

Expected: 모든 기존 테스트 + Task 5 의 파싱 테스트 PASS

- [ ] **Step 5: 커밋 (사용자 승인 후만)**

```bash
git add Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusRouteService.java
git commit -m "정적 노선 데이터를 외부 API 동기화 방식으로 변경 : feat : SomansaBusRouteService.syncRoutes 추가 및 initializeRoutes dead code 제거 https://github.com/Cassiiopeia/suh-project-utility/issues/188"
```

---

## Task 7: Controller 에 `/route/sync` endpoint 추가

**Files:**
- Modify: `Suh-Web/src/main/java/me/suhsaechan/web/controller/api/SomansaBusController.java`

**의존**: T6 완료 후.

- [ ] **Step 1: 현재 Controller 확인**

```bash
grep -n "route" Suh-Web/src/main/java/me/suhsaechan/web/controller/api/SomansaBusController.java
```

Expected:
```
68:  @PostMapping(value = "/route/list", ...)
74:  @PostMapping(value = "/route/list/by-type", ...)
80:  @PostMapping(value = "/route/detail", ...)
```

- [ ] **Step 2: `/route/sync` endpoint 추가**

`/route/detail` 메서드 직후에 추가:

```java
  @PostMapping(value = "/route/detail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getRouteDetail(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(routeService.getRouteById(request.getSomansaBusRouteId()));
  }

  @PostMapping(value = "/route/sync", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> syncRoutes() {
    return ResponseEntity.ok(routeService.syncRoutes());
  }
```

> 정확한 들여쓰기/스타일은 기존 컨트롤러 따름.

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :Suh-Web:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋 (사용자 승인 후만)**

```bash
git add Suh-Web/src/main/java/me/suhsaechan/web/controller/api/SomansaBusController.java
git commit -m "정적 노선 데이터를 외부 API 동기화 방식으로 변경 : feat : SomansaBusController 에 /route/sync endpoint 추가 https://github.com/Cassiiopeia/suh-project-utility/issues/188"
```

---

## Task 8: Dashboard UI — sync 버튼 + 마지막 동기화 시각

**Files:**
- Modify: `Suh-Web/src/main/resources/templates/pages/somansaBusDashboard.html`

**의존**: T7 완료 후.

- [ ] **Step 1: 현재 dashboard HTML 의 노선 섹션 확인**

```bash
grep -n "route\|노선\|/api/somansa-bus" Suh-Web/src/main/resources/templates/pages/somansaBusDashboard.html | head -40
```

기존에 노선 카드/섹션이 어디에 있는지 확인. 보통 "버스 노선" 헤더 + `/api/somansa-bus/route/list` 호출하는 JS 함수 존재.

- [ ] **Step 2: HTML — 노선 섹션 헤더 영역 변경**

기존 노선 헤더(예시 — 실제 코드에 맞게 조정):

```html
<div class="route-section-header">
  <h3>버스 노선</h3>
</div>
```

위를 다음으로 변경:

```html
<div class="route-section-header" style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px;">
  <h3 style="margin: 0;">버스 노선 (<span id="routeTotalCount">0</span>건)</h3>
  <button id="btnSyncRoutes" class="btn btn-primary" style="padding: 6px 12px; font-size: 13px;">
    🔄 노선 동기화
  </button>
  <span class="last-synced" style="margin-left: auto; font-size: 12px; color: #666;">
    마지막 동기화: <span id="lastSyncedAt">-</span>
  </span>
</div>
```

> 정확한 클래스/스타일은 기존 dashboard.html 컨벤션에 맞춰 조정. CLAUDE.md "Tailwind 하드코딩 금지" 준수 — 가능한 표준 클래스 사용.

- [ ] **Step 3: JS — `/route/list` 응답에서 lastSyncedAt 표시**

기존 `sendFormRequest('/api/somansa-bus/route/list', ...)` 콜백 안에서:

```javascript
sendFormRequest('/api/somansa-bus/route/list', {}, function(data) {
  document.getElementById('routeTotalCount').textContent = data.totalCount || 0;
  document.getElementById('lastSyncedAt').textContent = data.lastSyncedAt
    ? formatDateTime(data.lastSyncedAt)
    : '없음';
  renderRoutes(data.routes);  // 기존 렌더 함수
});
```

> `formatDateTime` 은 dashboard 에 이미 있으면 재사용. 없으면 ISO 문자열 그대로 표시.

- [ ] **Step 4: JS — 동기화 버튼 핸들러 추가**

`<script>` 안에 추가:

```javascript
document.getElementById('btnSyncRoutes').addEventListener('click', function() {
  if (!confirm('노선 동기화를 시작합니다. 외부 시스템(buseezy)에 로그인 후 노선 정보를 가져옵니다. 진행할까요?')) {
    return;
  }

  const btn = this;
  btn.disabled = true;
  btn.textContent = '🔄 동기화 중...';

  sendFormRequest('/api/somansa-bus/route/sync', {}, function(data) {
    btn.disabled = false;
    btn.textContent = '🔄 노선 동기화';
    alert('✅ 동기화 완료\n'
      + '신규: +' + (data.syncCreatedCount || 0) + '\n'
      + '변경: ' + (data.syncUpdatedCount || 0) + '\n'
      + '비활성: ' + (data.syncDeactivatedCount || 0) + '\n'
      + '총: ' + (data.syncTotalCount || 0) + '건\n'
      + '트리거: ' + (data.syncTriggerLoginId || ''));
    // 노선 목록 재조회
    if (typeof loadRoutes === 'function') {
      loadRoutes();
    } else {
      location.reload();
    }
  }, function(err) {
    btn.disabled = false;
    btn.textContent = '🔄 노선 동기화';
    alert('❌ 동기화 실패: ' + (err && err.message ? err.message : '알 수 없는 오류'));
  });
});
```

> `sendFormRequest` 의 정확한 시그니처는 기존 `dashboard.html` 의 패턴 따름. 두 번째 콜백이 에러 핸들러가 아닐 수 있음 — 그 경우 `try/catch` 또는 응답 코드 체크로 변경.

- [ ] **Step 5: 정적 페이지 띄워서 수동 검증 (가능한 경우)**

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

브라우저: `http://localhost:8080/somansa/bus/dashboard` (실제 라우트 확인)
- 페이지 로드 → "마지막 동기화" 표시 확인
- "🔄 노선 동기화" 버튼 클릭 → 확인 모달 → 동기화 → alert 결과
- 노선 목록 갱신 확인

> ⚠️ 외부 buseezy 호출이라 dev 환경에서 실제 호출됨. 트리거 회원이 실제 buseezy 계정인지 사전 확인.

- [ ] **Step 6: 커밋 (사용자 승인 후만)**

```bash
git add Suh-Web/src/main/resources/templates/pages/somansaBusDashboard.html
git commit -m "정적 노선 데이터를 외부 API 동기화 방식으로 변경 : feat : somansaBusDashboard 에 노선 동기화 버튼 및 마지막 동기화 시각 추가 https://github.com/Cassiiopeia/suh-project-utility/issues/188"
```

---

## Task 9: `SomansaBusDataInitializer` 제거

**Files:**
- Delete: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/config/SomansaBusDataInitializer.java`

**의존**: 다른 task 와 독립. 안전하게 마지막에 진행 (혹시 부팅 시 자동 시드가 누군가에게 필요하면 그때 발견).

- [ ] **Step 1: 호출/참조 없는지 다시 확인**

```bash
grep -rn "SomansaBusDataInitializer" --include="*.java" .
```

Expected: 파일 자체 외에는 다른 참조 없음.

- [ ] **Step 2: 파일 삭제**

```bash
rm Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/config/SomansaBusDataInitializer.java
```

- [ ] **Step 3: 빌드 검증**

```bash
./gradlew :Suh-Domain-Somansa-Bus:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋 (사용자 승인 후만)**

```bash
git add Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/config/SomansaBusDataInitializer.java
git commit -m "정적 노선 데이터를 외부 API 동기화 방식으로 변경 : refactor : SomansaBusDataInitializer 제거 (외부 API 동기화로 대체) https://github.com/Cassiiopeia/suh-project-utility/issues/188"
```

---

## Task 10: CLAUDE.md ServerOptionKey 컨벤션 — commit (worktree 에 이미 적용됨)

**Files:**
- Already modified (uncommitted): `CLAUDE.md`

이미 brainstorming 단계에서 worktree 에 적용된 변경분을 본 이슈 PR 에 묶어 commit.

- [ ] **Step 1: 변경분 확인**

```bash
git diff --stat CLAUDE.md
```

Expected: `+49 -0` 정도 (정확한 라인 수는 환경별 차이 가능)

- [ ] **Step 2: 커밋 (사용자 승인 후만)**

```bash
git add CLAUDE.md
git commit -m "정적 노선 데이터를 외부 API 동기화 방식으로 변경 : docs : CLAUDE.md 에 시스템 동적 설정값 ServerOptionKey 컨벤션 섹션 추가 https://github.com/Cassiiopeia/suh-project-utility/issues/188"
```

---

## 빌드/테스트 최종 검증

모든 task 완료 후:

- [ ] **전체 빌드**

```bash
./gradlew clean build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **전체 테스트**

```bash
./gradlew test
```

Expected: 본 이슈 단위 테스트 PASS, 기존 회귀 PASS

- [ ] **수동 검증 시나리오**

| # | 시나리오 | 기대 결과 |
|---|---------|----------|
| 1 | dev 부팅 후 첫 페이지 진입 | 노선 0건 (DataInitializer 제거됨) |
| 2 | 동기화 버튼 클릭 → 정상 응답 | created/updated/deactivated 결과 alert |
| 3 | 동기화 후 페이지 새로고침 | 노선 목록 + lastSyncedAt 표시 |
| 4 | trigger member loginId 가 buseezy 미등록 → 동기화 호출 | `SOMANSA_BUS_LOGIN_FAILED` 에러 응답 |
| 5 | ServerOption admin 페이지에서 `SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID` 변경 | 다음 sync 호출 시 변경된 회원으로 로그인 |

---

## Self-Review (작성자가 사전 확인)

### Spec coverage
- spec §2 도메인 변경 → Task 5 Step 6 (4개 URL + Referer 일괄 갱신) ✅
- spec §3-1 entity isShuttle → Task 3 ✅
- spec §3-2 ServerOptionKey → Task 1 ✅
- spec §3-3 syncRoutes → Task 6 ✅
- spec §3-3-1 logout 호출 안 함 → 코드에 logout 호출 없음, 자연 만료 ✅
- spec §3-4 fetchRouteList/parseRouteList/parseLabel → Task 5 ✅
- spec §3-5 controller endpoint → Task 7 ✅
- spec §3-6 Response sync 필드 → Task 4 ✅
- spec §3-7 ErrorCode → Task 2 (단, 기존 `SOMANSA_BUS_LOGIN_FAILED`/`SOMANSA_BUS_SESSION_FAILED` 재사용으로 spec 보다 추가 항목 적음) ✅
- spec §3-8 dashboard UI → Task 8 ✅
- spec §4 CLAUDE.md → Task 10 ✅
- spec §5 SomansaBusDataInitializer 제거 → Task 9 ✅

### Placeholder scan
- "TBD"/"TODO" 없음 ✅
- "appropriate error handling" 같은 막연한 지시 없음 ✅
- 모든 코드 step 에 실제 코드 포함 ✅

### Type consistency
- `RouteData` 필드 (`disptid`, `caralias`, `isShuttle`, `description`, `departureTime`, `station`, `busNumber`) — Task 5 정의 ↔ Task 6 사용 일치 ✅
- `RouteLabelParts` (`departureTime`, `station`, `busNumber`) — Task 5 정의 ↔ Task 5 ApiService 사용 일치 ✅
- `SomansaBusResponse` 신규 필드 (`syncCreatedCount`, `syncUpdatedCount`, `syncDeactivatedCount`, `syncTotalCount`, `syncedAt`, `syncTriggerLoginId`, `lastSyncedAt`) — Task 4 정의 ↔ Task 6 builder ↔ Task 8 JS 사용 일치 ✅
- `ServerOptionKey.SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID` — Task 1 정의 ↔ Task 6 사용 일치 ✅
- `ErrorCode.SOMANSA_BUS_API_FETCH_FAILED` — Task 2 추가 ↔ Task 5 사용 일치 ✅
- `ErrorCode.SOMANSA_BUS_LOGIN_FAILED` / `SOMANSA_BUS_SESSION_FAILED` — 기존 enum, Task 6 사용 ✅

### 기타
- Git 커밋 절대 금지 규칙 — 모든 commit step 앞에 ⚠️ 경고 명시 ✅
- 의존성 추가 없음 (Jsoup 1.15.4 는 `Suh-Common` 에 이미 있음) ✅
- @OneToMany/@OneToOne 사용 없음 ✅
- Boolean 필드 `is` 접두사 (`isShuttle`, `isActive`) ✅
- LocalDateTime `@JsonFormat` 적용 (`syncedAt`, `lastSyncedAt`) ✅
