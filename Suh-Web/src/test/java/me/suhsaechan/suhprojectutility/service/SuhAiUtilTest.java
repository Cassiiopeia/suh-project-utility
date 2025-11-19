package me.suhsaechan.suhprojectutility.service;

import static me.suhsaechan.suhlogger.util.SuhLogger.lineLog;
import static me.suhsaechan.suhlogger.util.SuhLogger.timeLog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.suhsaechan.ai.model.JsonSchema;
import kr.suhsaechan.ai.model.SuhAiderRequest;
import kr.suhsaechan.ai.model.SuhAiderResponse;
import kr.suhsaechan.ai.service.SuhAiderEngine;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.dto.ExampleDto;
import me.suhsaechan.common.util.SuhAiUtil;
import me.suhsaechan.web.SuhProjectUtilityApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SuhProjectUtilityApplication.class)
@ActiveProfiles("dev")
@Slf4j
class SuhAiUtilTest {

  @Autowired
  SuhAiUtil suhAiUtil;

  @Autowired(required = false)
  SuhAiderEngine suhAiderEngine;

  @Test
  public void mainTest() {
//    timeLog(this::test);
    timeLog(this::testStructuredResponse);
  }

  void test() {
    lineLog("=== SuhAiUtil 테스트 시작 ===");

    // 1. SuhAiUtil Bean 주입 확인
    lineLog("1. SuhAiUtil Bean 주입 확인");
    if (suhAiUtil == null) {
      lineLog("❌ SuhAiUtil이 주입되지 않았습니다.");
      return;
    }
    lineLog("✅ SuhAiUtil 주입 성공");

    // 2. SuhAiderEngine Bean 주입 확인
    lineLog("2. SuhAiderEngine Bean 주입 확인");
    if (suhAiderEngine == null) {
      lineLog("❌ SuhAiderEngine이 주입되지 않았습니다. Auto-Configuration이 활성화되지 않았을 수 있습니다.");
    } else {
      lineLog("✅ SuhAiderEngine 주입 성공");
    }

    // 3. 간단한 프롬프트 테스트 (실제 AI 서버 호출)
    lineLog("3. AI 응답 생성 테스트");
    try {
      String prompt = "안녕하세요. 간단히 인사해주세요.";
      lineLog("프롬프트: " + prompt);
      
      String response = suhAiUtil.generateResponse(prompt);
      lineLog("응답: " + response);
      lineLog("✅ AI 응답 생성 성공");
    } catch (Exception e) {
      lineLog("❌ AI 응답 생성 실패: " + e.getMessage());
    }

    lineLog("=== SuhAiUtil 테스트 완료 ===");
  }

  /**
   * 구조화된 응답 테스트 (ExampleDto 기반)
   * JsonSchema.fromClass()를 사용하여 AI가 지정된 구조로 응답하도록 합니다.
   */
  void testStructuredResponse() {
    lineLog("=== 구조화된 응답 테스트 시작 ===");

    // 1. SuhAiderEngine Bean 확인
    if (suhAiderEngine == null) {
      lineLog("❌ SuhAiderEngine이 주입되지 않았습니다. 테스트를 건너뜁니다.");
      return;
    }

    // 2. JsonSchema 생성
    lineLog("1. ExampleDto 기반 JsonSchema 생성");
    JsonSchema responseSchema = JsonSchema.fromClass(ExampleDto.class);
    lineLog("✅ 스키마 생성 완료 - 필드 개수: " + responseSchema.getProperties().size());

    // 3. 프롬프트 작성 (영어로)
    String prompt = """
        Please create information about the following topic in JSON format:
        Topic: "Spring Boot Performance Optimization"

        Requirements:
        - title: A concise title
        - content: Core content in 3-5 sentences
        - category: One of Technology, Business, Lifestyle, or Other
        - priority: A number between 1 and 5
        - tags: At least 3 relevant keywords
        """;

    // 4. SuhAiderRequest 생성 (responseSchema 포함)
    lineLog("2. AI 요청 생성 (responseSchema 포함)");
    SuhAiderRequest request = SuhAiderRequest.builder()
        .model("gemma3:4b")
        .prompt(prompt)
        .stream(false)
        .responseSchema(responseSchema)
        .build();

    // 5. AI 호출
    try {
      lineLog("3. AI 호출 시작...");
      SuhAiderResponse response = suhAiderEngine.generate(request);

      lineLog("4. AI 응답 받음 - 처리 시간: " +
          (response.getTotalDuration() != null ? (response.getTotalDuration() / 1_000_000) + "ms" : "N/A"));
      lineLog("원본 응답: " + response.getResponse());

      // 6. JSON → ExampleDto 역직렬화
      lineLog("5. JSON을 ExampleDto로 변환");
      ObjectMapper objectMapper = new ObjectMapper();

      // AI가 JSON Schema 구조로 반환한 경우 properties 필드 추출
      String jsonResponse = response.getResponse();
      JsonNode rootNode = objectMapper.readTree(jsonResponse);

      // "properties" 필드가 있으면 그것을 사용, 없으면 전체 노드 사용
      JsonNode dataNode = rootNode.has("properties") ? rootNode.get("properties") : rootNode;

      ExampleDto exampleDto = objectMapper.treeToValue(dataNode, ExampleDto.class);

      // 7. 검증
      lineLog("6. 응답 검증");
      if (exampleDto.getTitle() == null || exampleDto.getTitle().isEmpty()) {
        lineLog("❌ 검증 실패: title이 비어있습니다.");
        return;
      }
      if (exampleDto.getContent() == null || exampleDto.getContent().length() < 10) {
        lineLog("❌ 검증 실패: content가 너무 짧습니다.");
        return;
      }
      if (exampleDto.getPriority() == null ||
          exampleDto.getPriority() < 1 ||
          exampleDto.getPriority() > 5) {
        lineLog("❌ 검증 실패: priority가 범위를 벗어났습니다.");
        return;
      }

      // 8. 결과 출력
      lineLog("✅ 검증 성공!");
      lineLog("- Title: " + exampleDto.getTitle());
      lineLog("- Content: " + exampleDto.getContent());
      lineLog("- Category: " + exampleDto.getCategory());
      lineLog("- Priority: " + exampleDto.getPriority());
      lineLog("- Tags: " + (exampleDto.getTags() != null ? String.join(", ", exampleDto.getTags()) : "없음"));

    } catch (JsonProcessingException e) {
      lineLog("❌ JSON 파싱 실패: " + e.getMessage());
      log.error("JSON 파싱 오류", e);
    } catch (Exception e) {
      lineLog("❌ AI 호출 실패: " + e.getMessage());
      log.error("AI 호출 오류", e);
    }

    lineLog("=== 구조화된 응답 테스트 완료 ===");
  }
}