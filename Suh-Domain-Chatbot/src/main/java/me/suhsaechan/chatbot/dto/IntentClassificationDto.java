package me.suhsaechan.chatbot.dto;

import kr.suhsaechan.ai.annotation.AiClass;
import kr.suhsaechan.ai.annotation.AiSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent-LLM Step 1: 사용자 의도 분류 결과
 *
 * <p>SUH-AIDER의 Structured Output 기능을 활용하여
 * LLM이 JSON 형식으로 의도 분류 결과를 반환하도록 합니다.</p>
 *
 * <p>Agent-LLM 동작 흐름:</p>
 * <pre>
 * 1. [의도 분류] 사용자 질문 → 경량 LLM → IntentClassificationDto
 * 2. [RAG 검색] needsRagSearch=true인 경우에만 벡터 검색 수행
 * 3. [응답 생성] 분류 결과 + RAG 결과 → 고품질 LLM → 최종 응답
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AiClass(
    title = "사용자 의도 분류 결과",
    description = "사용자 질문을 분석하여 질문 유형을 분류하고, RAG 벡터 검색의 필요 여부를 판단한 결과입니다. " +
                  "이 분석은 불필요한 벡터 검색을 방지하고 응답 속도를 개선하는 Agent-LLM의 첫 번째 단계입니다.",
    example = "{\"intentType\":\"KNOWLEDGE_QUERY\",\"needsRagSearch\":true,\"confidence\":0.95," +
              "\"reason\":\"Docker 로그 조회 방법을 묻는 구체적인 기능 질문이므로 문서 검색이 필요합니다.\",\"summary\":\"Docker 로그 조회 방법 문의\"}"
)
public class IntentClassificationDto {

    /**
     * 질문 유형 분류
     */
    @AiSchema(
        description = "질문 유형을 다음 4가지 중 하나로 분류합니다:\n" +
                      "- KNOWLEDGE_QUERY: 특정 정보, 기능, 사용법을 묻는 질문 (문서 검색 필요)\n" +
                      "  예: 'Docker 로그는 어디서 볼 수 있나요?', '스터디 노트 사용법 알려줘'\n" +
                      "- GREETING: 인사, 감사 표현 (문서 검색 불필요)\n" +
                      "  예: '안녕하세요', '고마워요', '잘 부탁드립니다'\n" +
                      "- CHITCHAT: 일반적인 잡담, 감정 표현 (문서 검색 불필요)\n" +
                      "  예: '오늘 날씨 어때?', '심심해', '기분 좋아'\n" +
                      "- CLARIFICATION: 이전 답변에 대한 추가 질문 (컨텍스트에 따라 선택적 검색)\n" +
                      "  예: '그럼 그건 어떻게 해?', '좀 더 자세히 알려줘'",
        required = true,
        allowableValues = {"KNOWLEDGE_QUERY", "GREETING", "CHITCHAT", "CLARIFICATION"},
        example = "KNOWLEDGE_QUERY"
    )
    private String intentType;

    /**
     * RAG 검색 필요 여부
     */
    @AiSchema(
        description = "RAG 벡터 검색의 필요 여부를 판단한 결과입니다.\n" +
                      "- true: 문서 검색이 필요함 (KNOWLEDGE_QUERY 또는 컨텍스트 기반 CLARIFICATION)\n" +
                      "- false: 문서 검색 불필요 (GREETING, CHITCHAT, 또는 이전 답변만으로 충분한 경우)",
        required = true,
        example = "true"
    )
    private Boolean needsRagSearch;

    /**
     * 분류 신뢰도 (0.0 ~ 1.0)
     */
    @AiSchema(
        description = "이 분류 결과에 대한 LLM의 신뢰도입니다.\n" +
                      "- 0.9 이상: 매우 확실함\n" +
                      "- 0.7 ~ 0.9: 확실함\n" +
                      "- 0.5 ~ 0.7: 보통\n" +
                      "- 0.5 미만: 불확실함 (시스템은 안전을 위해 RAG 검색 수행)",
        required = true,
        minimum = "0.0",
        maximum = "1.0",
        example = "0.95"
    )
    private Float confidence;

    /**
     * 분류 이유 설명
     */
    @AiSchema(
        description = "왜 이렇게 분류했는지에 대한 구체적인 이유입니다. " +
                      "디버깅 및 Agent 로깅에 사용됩니다. " +
                      "예: '사용자가 Docker 컨테이너의 로그를 조회하는 구체적인 방법을 질문하고 있으므로, " +
                      "관련 문서를 검색하여 정확한 사용법을 제공해야 합니다.'",
        required = false,
        maxLength = 200,
        example = "Docker 로그 조회 방법을 묻는 구체적인 기능 질문이므로 관련 문서 검색이 필요합니다"
    )
    private String reason;

    /**
     * 질문 요약 (다음 Agent 스텝에 전달할 핵심 내용)
     */
    @AiSchema(
        description = "사용자 질문의 핵심 내용을 간결하게 요약한 문장입니다. " +
                      "이 요약은 RAG 검색 쿼리 최적화 및 Agent 로깅에 사용됩니다. " +
                      "예: '사용자는 Docker 컨테이너의 로그를 웹 UI에서 조회하는 방법을 알고 싶어함'",
        required = false,
        maxLength = 100,
        example = "Docker 로그 조회 방법 문의"
    )
    private String summary;
}
