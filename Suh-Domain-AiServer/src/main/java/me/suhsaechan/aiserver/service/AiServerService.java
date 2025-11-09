package me.suhsaechan.aiserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.aiserver.dto.AiServerRequest;
import me.suhsaechan.aiserver.dto.AiServerResponse;
import me.suhsaechan.aiserver.dto.EmbeddingsPayload;
import me.suhsaechan.aiserver.dto.GeneratePayload;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.util.NetworkUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServerService {

    private final NetworkUtil networkUtil;
    private final ObjectMapper objectMapper;

    @Value("${ai-server.base-url}")
    private String baseUrl;

    @Value("${ai-server.api-key}")
    private String apiKey;

    /**
     * 모델별 지원 기능을 확인합니다.
     */
    public boolean isModelSupportsGenerate(String modelName) {
        if (modelName == null) return false;
        // embedding 전용 모델들은 generate를 지원하지 않음
        return !modelName.toLowerCase().contains("embedding");
    }

    public boolean isModelSupportsEmbedding(String modelName) {
        if (modelName == null) return false;
        // embedding 모델이거나 일반 모델 중 embedding을 지원하는 모델들
        return modelName.toLowerCase().contains("embedding") ||
               modelName.toLowerCase().contains("all-minilm") ||
               modelName.toLowerCase().contains("sentence");
    }

    /**
     * AI 서버 Health Check를 수행합니다.
     */
    public AiServerResponse getHealth(AiServerRequest request) {
        log.info("AI 서버 Health Check 시작: {}", baseUrl);

        try {
            // 헤더 설정
            Map<String, String> headers = new HashMap<>();
            headers.put("X-API-Key", apiKey);

            // Ollama의 기본 엔드포인트로 health check
            String healthResponse = networkUtil.sendGetRequest(baseUrl, headers);
            log.debug("Health check 응답: {}", healthResponse);

            // "Ollama is running" 문구 확인
            Boolean isHealthy = healthResponse != null &&
                               healthResponse.toLowerCase().contains("ollama is running");

            return AiServerResponse.builder()
                    .isHealthy(isHealthy)
                    .isActive(isHealthy)
                    .currentUrl(baseUrl)
                    .healthMessage(isHealthy ? "AI 서버가 정상 작동 중입니다" : "AI 서버 응답이 올바르지 않습니다")
                    .build();

        } catch (Exception e) {
            log.error("AI 서버 Health Check 실패: {}", e.getMessage(), e);
            return AiServerResponse.builder()
                    .isHealthy(false)
                    .isActive(false)
                    .currentUrl(baseUrl)
                    .healthMessage("AI 서버에 연결할 수 없습니다")
                    .build();
        }
    }

    /**
     * AI 서버의 모델 목록을 조회합니다.
     */
    public AiServerResponse getModels(AiServerRequest request) {
        log.info("AI 서버 모델 목록 조회 시작");

        try {
            String modelsUrl = baseUrl + "/api/tags";
            log.debug("모델 목록 조회 URL: {}", modelsUrl);

            // 헤더 설정
            Map<String, String> headers = new HashMap<>();
            headers.put("X-API-Key", apiKey);

            // NetworkUtil을 통해 HTTP GET 요청 수행
            String jsonResponse = networkUtil.sendGetRequest(modelsUrl, headers);
            log.debug("모델 목록 응답 크기: {} bytes", jsonResponse.length());

            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.error("AI 서버로부터 빈 모델 목록 응답을 받았습니다");
                throw new CustomException(ErrorCode.EMPTY_SCRIPT_RESPONSE);
            }

            // JSON 응답을 그대로 반환 (프론트엔드에서 파싱)
            return AiServerResponse.builder()
                    .isActive(true)
                    .currentUrl(baseUrl)
                    .modelsJson(jsonResponse)
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 모델 목록 조회 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AI 서버의 generate API를 호출합니다.
     */
    public AiServerResponse callGenerate(AiServerRequest request) {
        log.info("AI 서버 generate 호출 시작 - 모델: {}, 프롬프트: {}", request.getModel(), request.getPrompt());

        // 모델이 generate를 지원하는지 확인
        if (!isModelSupportsGenerate(request.getModel())) {
            log.error("모델 {}는 generate 기능을 지원하지 않습니다", request.getModel());
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        try {
            String generateUrl = baseUrl + "/api/generate";
            log.debug("generate 호출 URL: {}", generateUrl);

            // stream 기본값 설정
            Boolean stream = request.getStream() != null ? request.getStream() : false;

            // JSON 페이로드 객체 생성
            GeneratePayload payload = GeneratePayload.builder()
                    .model(request.getModel())
                    .prompt(request.getPrompt())
                    .stream(stream)
                    .build();

            // 헤더 설정
            Map<String, String> headers = new HashMap<>();
            headers.put("X-API-Key", apiKey);
            headers.put("Content-Type", "application/json");

            // NetworkUtil을 통해 HTTP POST 요청 수행 (JSON 객체 직렬화)
            String jsonResponse = networkUtil.sendPostJsonRequest(generateUrl, headers, payload);
            log.debug("generate 응답 크기: {} bytes", jsonResponse.length());

            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.error("AI 서버로부터 빈 generate 응답을 받았습니다");
                throw new CustomException(ErrorCode.EMPTY_SCRIPT_RESPONSE);
            }

            // JSON 응답을 그대로 반환 (프론트엔드에서 파싱)
            return AiServerResponse.builder()
                    .isActive(true)
                    .currentUrl(baseUrl)
                    .generatedJson(jsonResponse)
                    .model(request.getModel())
                    .prompt(request.getPrompt())
                    .stream(stream)
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 generate 호출 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AI 서버의 embeddings API를 호출합니다.
     */
    public AiServerResponse callEmbeddings(AiServerRequest request) {
        log.info("AI 서버 embeddings 호출 시작 - 모델: {}, 입력: {}", request.getModel(), request.getInput());

        // 모델이 embedding을 지원하는지 확인
        if (!isModelSupportsEmbedding(request.getModel())) {
            log.error("모델 {}는 embedding 기능을 지원하지 않습니다", request.getModel());
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        try {
            String embeddingsUrl = baseUrl + "/api/embeddings";
            log.debug("embeddings 호출 URL: {}", embeddingsUrl);

            // JSON 페이로드 객체 생성
            EmbeddingsPayload payload = EmbeddingsPayload.builder()
                    .model(request.getModel())
                    .input(request.getInput())
                    .build();

            // 헤더 설정
            Map<String, String> headers = new HashMap<>();
            headers.put("X-API-Key", apiKey);
            headers.put("Content-Type", "application/json");

            // NetworkUtil을 통해 HTTP POST 요청 수행 (JSON 객체 직렬화)
            String jsonResponse = networkUtil.sendPostJsonRequest(embeddingsUrl, headers, payload);
            log.debug("embeddings 응답 크기: {} bytes", jsonResponse.length());

            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.error("AI 서버로부터 빈 embeddings 응답을 받았습니다");
                throw new CustomException(ErrorCode.EMPTY_SCRIPT_RESPONSE);
            }

            // JSON 응답을 그대로 반환 (프론트엔드에서 파싱)
            return AiServerResponse.builder()
                    .isActive(true)
                    .currentUrl(baseUrl)
                    .embeddingsJson(jsonResponse)
                    .model(request.getModel())
                    .input(request.getInput())
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 embeddings 호출 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AI 서버에서 모델을 다운로드합니다.
     */
    public AiServerResponse pullModel(AiServerRequest request) {
        log.info("AI 서버 모델 다운로드 시작 - 모델: {}", request.getModelName());

        if (request.getModelName() == null || request.getModelName().trim().isEmpty()) {
            log.error("모델 이름이 비어있습니다");
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        try {
            String pullUrl = baseUrl + "/api/pull";
            log.debug("모델 다운로드 URL: {}", pullUrl);

            // JSON 페이로드 생성
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", request.getModelName());
            payload.put("stream", false); // 단순화를 위해 스트림 모드 비활성화

            // 헤더 설정
            Map<String, String> headers = new HashMap<>();
            headers.put("X-API-Key", apiKey);
            headers.put("Content-Type", "application/json");

            // NetworkUtil을 통해 HTTP POST 요청 수행
            String jsonPayload = objectMapper.writeValueAsString(payload);
            String jsonResponse = networkUtil.sendPostRequest(pullUrl, headers, jsonPayload);
            log.info("모델 다운로드 성공 - 모델: {}", request.getModelName());

            return AiServerResponse.builder()
                    .isActive(true)
                    .currentUrl(baseUrl)
                    .pullProgressJson(jsonResponse)
                    .model(request.getModelName())
                    .build();

        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.JSON_PARSING_ERROR);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("모델 다운로드 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AI_SERVER_MODEL_PULL_FAILED);
        }
    }

    /**
     * AI 서버에서 모델을 삭제합니다.
     */
    public AiServerResponse deleteModel(AiServerRequest request) {
        log.info("AI 서버 모델 삭제 시작 - 모델: {}", request.getModelName());

        if (request.getModelName() == null || request.getModelName().trim().isEmpty()) {
            log.error("모델 이름이 비어있습니다");
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        try {
            String deleteUrl = baseUrl + "/api/delete";
            log.debug("모델 삭제 URL: {}", deleteUrl);

            // JSON 페이로드 생성
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", request.getModelName());

            // 헤더 설정
            Map<String, String> headers = new HashMap<>();
            headers.put("X-API-Key", apiKey);
            headers.put("Content-Type", "application/json");

            // NetworkUtil을 통해 HTTP DELETE 요청 수행
            String jsonPayload = objectMapper.writeValueAsString(payload);
            String jsonResponse = networkUtil.sendPostRequest(deleteUrl, headers, jsonPayload);
            log.info("모델 삭제 성공 - 모델: {}", request.getModelName());

            return AiServerResponse.builder()
                    .isActive(true)
                    .currentUrl(baseUrl)
                    .model(request.getModelName())
                    .build();

        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.JSON_PARSING_ERROR);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("모델 삭제 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AI_SERVER_MODEL_DELETE_FAILED);
        }
    }
}
