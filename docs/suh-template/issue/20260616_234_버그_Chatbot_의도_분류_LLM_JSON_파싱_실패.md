🗒️ 설명
---

챗봇 스트리밍 채팅에서 의도 분류 단계의 LLM 응답 JSON 파싱이 자주 실패한다.

- 경량 모델(gemma3:1b)이 강제 JSON 스키마 응답을 안정적으로 생성하지 못해, 의도 분류 결과 파싱이 실패하는 경우가 있다.
- 파싱 실패 시 기본값(`KNOWLEDGE_QUERY`, `needsRagSearch=true`, `confidence=0.5`)으로 폴백되어 검색 자체는 동작하지만, 의도 분류가 부정확해질 수 있다(인사/잡담을 지식 질문으로 처리하는 등). 응답 형식(`responseFormat`)·요약(`summary`) 등 부가 정보도 누락된다.

🔄 재현 방법
---

1. 챗봇 페이지로 이동
2. "서새찬이 누구냐고" 같은 질문 입력
3. 서버 로그에서 의도 분류 파싱 실패 경고 관찰 (2026-06-16 13:12 실측)

📸 참고 자료
---

운영 로그 (2026-06-16 13:12):

```
[Agent Step 1/3] 의도 분류 시작
SuhAiderEngine : Generate 완료 - 응답 길이: 361, 처리 시간: 5066ms
WARN ... ChatbotService : [Agent Step 1/3] 의도 분류 파싱 실패, 기본값(KNOWLEDGE_QUERY) 사용
[Agent Step 1/3] 의도 분류 완료 - type: KNOWLEDGE_QUERY, needsRAG: true, confidence: 0.5
```

관련 파일:
- `Suh-Domain-Chatbot/src/main/java/me/suhsaechan/chatbot/service/ChatbotService.java` (`classifyUserIntentWithRetry`, `isValidIntentResult`, `createFallbackIntent`, `classifyUserIntentInternal`의 JSON 스키마 파싱부)

원인 추정:
- 경량 모델(gemma3:1b)이 강제 JSON 스키마 응답을 안정적으로 못 냄. 모델 교체 또는 파싱 견고성 개선(재시도 강화, 응답에서 JSON 블록 추출 보정 등) 검토 필요.

✅ 예상 동작
---

- 의도 분류 JSON 파싱이 안정적으로 성공하거나, 실패하더라도 사용자 경험에 영향이 없도록 견고하게 폴백한다.
- 파싱 실패 빈도를 줄여 의도 분류 정확도(인사/잡담/지식질문 구분, 응답 형식 결정)를 높인다.

⚙️ 환경 정보
---

- **OS**: -
- **브라우저**: -
- **기기**: -

🙋‍♂️ 담당자
---

- **백엔드**: 서새찬
- **프론트엔드**: -
- **디자인**: -
