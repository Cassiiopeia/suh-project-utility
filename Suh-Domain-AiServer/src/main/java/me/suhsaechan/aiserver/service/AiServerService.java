package me.suhsaechan.aiserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.aiserver.dto.AiServerRequest;
import me.suhsaechan.aiserver.dto.AiServerResponse;
import me.suhsaechan.aiserver.dto.DownloadProgressDto;
import me.suhsaechan.aiserver.dto.EmbeddingsPayload;
import me.suhsaechan.aiserver.dto.GeneratePayload;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.util.NetworkUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    // 다운로드 진행 상황 추적을 위한 맵 (모델명 -> 진행상황)
    private final Map<String, DownloadProgressDto> downloadProgressMap = new ConcurrentHashMap<>();

    // 비동기 작업을 위한 ExecutorService
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // OkHttp 클라이언트 (타임아웃 없음 - 장시간 다운로드 지원)
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

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

    /**
     * AI 서버 모델 다운로드를 SSE로 스트리밍합니다.
     * @param modelName 다운로드할 모델 이름
     * @return SSE Emitter
     */
    public SseEmitter pullModelStream(String modelName) {
        log.info("모델 다운로드 스트리밍 시작 - 모델: {}", modelName);

        if (modelName == null || modelName.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        // 이미 다운로드 중인지 확인
        DownloadProgressDto existingProgress = downloadProgressMap.get(modelName);
        if (existingProgress != null && "downloading".equals(existingProgress.getStatus())) {
            log.warn("모델이 이미 다운로드 중입니다 - 모델: {}", modelName);
            throw new CustomException(ErrorCode.AI_SERVER_MODEL_PULL_ALREADY_IN_PROGRESS);
        }

        // SSE Emitter 생성 (타임아웃 없음 - 장시간 다운로드 지원)
        SseEmitter emitter = new SseEmitter(0L);

        // 초기 진행 상황 생성
        DownloadProgressDto progress = DownloadProgressDto.builder()
                .modelName(modelName)
                .status("downloading")
                .completed(0L)
                .total(0L)
                .percentage(0)
                .message("다운로드 시작 중...")
                .startTime(System.currentTimeMillis())
                .build();
        downloadProgressMap.put(modelName, progress);

        // 비동기로 다운로드 스트리밍 시작
        executorService.execute(() -> {
            try {
                streamModelDownload(modelName, emitter);
            } catch (Exception e) {
                log.error("모델 다운로드 스트리밍 중 오류 발생: {}", e.getMessage(), e);
                updateProgressError(modelName, e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("다운로드 중 오류가 발생했습니다: " + e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    log.error("SSE 에러 전송 실패: {}", ex.getMessage());
                }
            }
        });

        // SSE Emitter 완료/타임아웃/에러 핸들러
        emitter.onCompletion(() -> {
            log.info("SSE 스트리밍 완료 - 모델: {}", modelName);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 스트리밍 타임아웃 - 모델: {}", modelName);
            updateProgressError(modelName, "타임아웃");
        });

        emitter.onError(e -> {
            log.error("SSE 스트리밍 에러 - 모델: {}, 에러: {}", modelName, e.getMessage());
            updateProgressError(modelName, e.getMessage());
        });

        return emitter;
    }

    /**
     * OkHttp로 모델 다운로드를 스트리밍합니다.
     */
    private void streamModelDownload(String modelName, SseEmitter emitter) throws IOException {
        log.debug("[DEBUG] streamModelDownload 시작 - 모델: {}", modelName);
        String pullUrl = baseUrl + "/api/pull";
        log.debug("[DEBUG] Pull URL: {}", pullUrl);

        // JSON 페이로드 생성
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", modelName);
        payload.put("stream", true); // 스트리밍 활성화
        log.debug("[DEBUG] 페이로드 생성 완료: {}", payload);

        String jsonPayload = objectMapper.writeValueAsString(payload);
        log.debug("[DEBUG] JSON 직렬화 완료: {}", jsonPayload);

        // OkHttp 요청 생성
        RequestBody body = RequestBody.create(
                jsonPayload,
                MediaType.parse("application/json; charset=utf-8")
        );
        log.debug("[DEBUG] RequestBody 생성 완료");

        Request request = new Request.Builder()
                .url(pullUrl)
                .addHeader("X-API-Key", apiKey)
                .post(body)
                .build();
        log.debug("[DEBUG] OkHttp Request 빌드 완료");

        // 스트리밍 응답 처리
        log.debug("[DEBUG] OkHttp 요청 실행 시작 (execute() 호출 전)...");
        try (Response response = okHttpClient.newCall(request).execute()) {
            log.debug("[DEBUG] OkHttp 요청 실행 완료 - 응답 코드: {}", response.code());
            if (!response.isSuccessful()) {
                throw new IOException("AI 서버 응답 실패: " + response.code());
            }

            if (response.body() == null) {
                log.error("[DEBUG] 응답 본문이 비어있습니다");
                throw new IOException("응답 본문이 비어있습니다");
            }
            log.debug("[DEBUG] 응답 본문 확인 완료, 스트림 읽기 시작");

            // 라인별로 JSON 파싱하여 진행 상황 업데이트
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {

                log.debug("[DEBUG] BufferedReader 생성 완료, 라인 읽기 시작");
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    log.debug("[DEBUG] 라인 #{} 읽음: {}", lineCount, line.length() > 100 ? line.substring(0, 100) + "..." : line);

                    if (line.trim().isEmpty()) {
                        log.debug("[DEBUG] 빈 라인 스킵");
                        continue;
                    }

                    try {
                        log.debug("[DEBUG] JSON 파싱 시작...");
                        JsonNode jsonNode = objectMapper.readTree(line);
                        log.debug("[DEBUG] JSON 파싱 완료: {}", jsonNode);

                        String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "";
                        long completed = jsonNode.has("completed") ? jsonNode.get("completed").asLong() : 0;
                        long total = jsonNode.has("total") ? jsonNode.get("total").asLong() : 0;
                        log.debug("[DEBUG] 파싱 결과 - status: {}, completed: {}, total: {}", status, completed, total);

                        // 진행률 계산
                        int percentage = 0;
                        if (total > 0) {
                            percentage = (int) ((completed * 100) / total);
                        }
                        log.debug("[DEBUG] 진행률 계산: {}%", percentage);

                        // 진행 상황 업데이트
                        DownloadProgressDto progress = downloadProgressMap.get(modelName);
                        if (progress != null) {
                            progress.setStatus("downloading");
                            progress.setCompleted(completed);
                            progress.setTotal(total);
                            progress.setPercentage(percentage);
                            progress.setMessage(status);
                            log.debug("[DEBUG] 진행 상황 맵 업데이트 완료: {}", progress);
                        } else {
                            log.warn("[DEBUG] 진행 상황 맵에서 모델을 찾을 수 없음: {}", modelName);
                        }

                        // SSE로 진행 상황 전송
                        log.debug("[DEBUG] SSE 진행 상황 전송 시작...");
                        emitter.send(SseEmitter.event()
                                .name("progress")
                                .data(objectMapper.writeValueAsString(progress)));
                        log.debug("[DEBUG] SSE 진행 상황 전송 완료");

                        // 완료 확인
                        if ("success".equals(status) || jsonNode.has("digest")) {
                            log.info("모델 다운로드 완료 - 모델: {}", modelName);
                            updateProgressCompleted(modelName);

                            // 업데이트된 progress 가져오기
                            DownloadProgressDto completedProgress = downloadProgressMap.get(modelName);
                            log.debug("[DEBUG] 완료 이벤트 전송 시작...");
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data(objectMapper.writeValueAsString(completedProgress)));
                            log.debug("[DEBUG] 완료 이벤트 전송 완료, emitter.complete() 호출");
                            emitter.complete();
                            break;
                        }

                    } catch (JsonProcessingException e) {
                        log.warn("JSON 파싱 실패, 라인 스킵: {}", line);
                    }
                }
            }

        } catch (Exception e) {
            log.error("모델 다운로드 스트리밍 실패: {}", e.getMessage(), e);
            updateProgressError(modelName, e.getMessage());
            throw e;
        }
    }

    /**
     * 진행 상황을 완료 상태로 업데이트합니다.
     */
    private void updateProgressCompleted(String modelName) {
        DownloadProgressDto progress = downloadProgressMap.get(modelName);
        if (progress != null) {
            progress.setStatus("completed");
            progress.setPercentage(100);
            progress.setMessage("다운로드 완료");
            progress.setEndTime(System.currentTimeMillis());
        }
    }

    /**
     * 진행 상황을 에러 상태로 업데이트합니다.
     */
    private void updateProgressError(String modelName, String errorMessage) {
        DownloadProgressDto progress = downloadProgressMap.get(modelName);
        if (progress != null) {
            progress.setStatus("failed");
            progress.setMessage("오류: " + errorMessage);
            progress.setEndTime(System.currentTimeMillis());
        }
    }

    /**
     * 모든 다운로드 진행 상황을 조회합니다.
     */
    public Map<String, DownloadProgressDto> getDownloadStatus() {
        return new HashMap<>(downloadProgressMap);
    }

    /**
     * 특정 모델의 다운로드 진행 상황을 조회합니다.
     */
    public DownloadProgressDto getModelDownloadStatus(String modelName) {
        return downloadProgressMap.get(modelName);
    }
}
