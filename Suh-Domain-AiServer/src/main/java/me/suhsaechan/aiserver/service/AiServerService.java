package me.suhsaechan.aiserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import kr.suhsaechan.ai.model.ModelInfo;
import kr.suhsaechan.ai.model.ModelListResponse;
import kr.suhsaechan.ai.model.SuhAiderRequest;
import kr.suhsaechan.ai.model.SuhAiderResponse;
import kr.suhsaechan.ai.service.SuhAiderEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.aiserver.dto.AiServerRequest;
import me.suhsaechan.aiserver.dto.AiServerResponse;
import me.suhsaechan.aiserver.dto.DownloadProgressDto;
import me.suhsaechan.aiserver.dto.EmbeddingsPayload;
import me.suhsaechan.aiserver.dto.GeneratePayload;
import me.suhsaechan.aiserver.dto.ModelDetailsDto;
import me.suhsaechan.aiserver.dto.ModelDto;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.util.NetworkUtil;
import me.suhsaechan.common.util.SshCommandExecutor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServerService {

    private final NetworkUtil networkUtil;
    private final ObjectMapper objectMapper;
    private final SshCommandExecutor sshCommandExecutor;

    @Autowired(required = false)
    private SuhAiderEngine suhAiderEngine;

    @Value("${ai-server.base-url}")
    private String baseUrl;

    @Value("${ai-server.api-key}")
    private String apiKey;

    // 다운로드 진행 상황 추적을 위한 맵 (모델명 -> 진행상황)
    private final Map<String, DownloadProgressDto> downloadProgressMap = new ConcurrentHashMap<>();


    // OkHttp 클라이언트 (readTimeout 60초 - 스트림 블로킹 감지)
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
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
     * SSH를 통해 curl 명령어를 실행하여 모델 목록을 가져옵니다.
     */
    public AiServerResponse getModels(AiServerRequest request) {
        log.info("AI 서버 모델 목록 조회 시작");

        try {
            String modelsUrl = baseUrl + "/api/tags";
            log.debug("모델 목록 조회 URL: {}", modelsUrl);

            // SSH를 통해 curl 명령어 실행
            String curlCommand = String.format("curl -s %s", modelsUrl);
            log.debug("SSH curl 명령어 실행: {}", curlCommand);
            
            String jsonResponse = sshCommandExecutor.executeCommand(curlCommand);
            log.debug("모델 목록 응답 크기: {} bytes", jsonResponse != null ? jsonResponse.length() : 0);

            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.error("AI 서버로부터 빈 모델 목록 응답을 받았습니다");
                throw new CustomException(ErrorCode.EMPTY_SCRIPT_RESPONSE);
            }

            // JSON 응답 파싱하여 모델 배열 추출
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode modelsNode = rootNode.get("models");
            
            List<ModelDto> models = new ArrayList<>();
            if (modelsNode != null && modelsNode.isArray()) {
                for (JsonNode modelNode : modelsNode) {
                    ModelDto modelDto = objectMapper.treeToValue(modelNode, ModelDto.class);
                    models.add(modelDto);
                }
            }
            
            log.info("모델 목록 파싱 완료 - 모델 개수: {}", models.size());

            // 파싱된 모델 배열과 원본 JSON 모두 반환 (하위 호환성)
            return AiServerResponse.builder()
                    .isActive(true)
                    .currentUrl(baseUrl)
                    .modelsJson(jsonResponse)
                    .models(models)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("모델 목록 JSON 파싱 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.JSON_PARSING_ERROR);
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
     * SuhAiderEngine을 사용하여 모델 삭제 및 캐시 자동 갱신
     */
    public AiServerResponse deleteModel(AiServerRequest request) {
        log.info("AI 서버 모델 삭제 시작 - 모델: {}", request.getModelName());
        checkSuhAiderEngine();

        if (request.getModelName() == null || request.getModelName().trim().isEmpty()) {
            log.error("모델 이름이 비어있습니다");
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        try {
            boolean result = suhAiderEngine.deleteModel(request.getModelName());
            log.info("모델 삭제 완료 - 모델: {}, 결과: {}", request.getModelName(), result);

            return AiServerResponse.builder()
                    .isActive(true)
                    .currentUrl(baseUrl)
                    .model(request.getModelName())
                    .build();

        } catch (Exception e) {
            log.error("모델 삭제 중 오류 발생: {}", e.getMessage(), e);
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

        // SSE Emitter 생성 (30분 타임아웃)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30분

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

        // 초기 이벤트 전송
        try {
            String initialProgressJson = objectMapper.writeValueAsString(progress);
            log.info("[SSE] === 초기 이벤트 전송 시도 === 모델: {}, 스레드: {}", modelName, Thread.currentThread().getName());
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(initialProgressJson));
            log.info("[SSE] === 초기 이벤트 전송 완료 === 모델: {}", modelName);
        } catch (Exception e) {
            log.error("[SSE] === 초기 이벤트 전송 실패 === 모델: {}, 에러: {}, 스택: {}", modelName, e.getMessage(), e);
            downloadProgressMap.remove(modelName);
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
                // 최종 실패 시 무시
            }
            throw new CustomException(ErrorCode.AI_SERVER_STREAM_ERROR);
        }

        // 다운로드 스트리밍 시작 (간단한 스레드 사용)
        Thread downloadThread = new Thread(() -> {
            try {
                log.info("[SSE] === 다운로드 스트리밍 스레드 시작 === 모델: {}, 스레드: {}", modelName, Thread.currentThread().getName());
                streamModelDownload(modelName, emitter);
                log.info("[SSE] === 다운로드 스트리밍 스레드 완료 === 모델: {}", modelName);
            } catch (Exception e) {
                log.error("[SSE] === 다운로드 스트리밍 스레드 실패 === 모델: {}, 에러: {}, 스택: {}", modelName, e.getMessage(), e);
                updateProgressError(modelName, e.getMessage());
                try {
                    String errorJson = objectMapper.writeValueAsString(
                        Map.of("error", "다운로드 중 오류가 발생했습니다: " + e.getMessage(),
                               "modelName", modelName)
                    );
                    log.info("[SSE] === 에러 이벤트 전송 시도 === 모델: {}", modelName);
                    emitter.send(SseEmitter.event().name("error").data(errorJson));
                    emitter.completeWithError(e);
                    log.info("[SSE] === 에러 이벤트 전송 완료 === 모델: {}", modelName);
                } catch (Exception ex) {
                    log.error("[SSE] === 에러 이벤트 전송 실패 === 모델: {}, 에러: {}, 스택: {}", modelName, ex.getMessage(), ex);
                    try {
                        emitter.completeWithError(ex);
                    } catch (Exception ignored) {
                        // 최종 실패 시 무시
                    }
                }
            }
        });
        downloadThread.setDaemon(true);
        downloadThread.setName("DownloadThread-" + modelName);
        downloadThread.start();
        log.info("[SSE] === 다운로드 스트리밍 스레드 시작됨 === 모델: {}, 스레드 ID: {}", modelName, downloadThread.getId());

        // SSE Emitter 완료/타임아웃/에러 핸들러
        emitter.onCompletion(() -> {
            log.info("[SSE] === Emitter 완료 콜백 호출 === 모델: {}, 스레드: {}", modelName, Thread.currentThread().getName());
            downloadProgressMap.remove(modelName); // 완료 시 맵에서 제거
        });

        emitter.onTimeout(() -> {
            log.warn("[SSE] === Emitter 타임아웃 콜백 호출 === 모델: {}, 스레드: {}", modelName, Thread.currentThread().getName());
            updateProgressError(modelName, "타임아웃");
            downloadProgressMap.remove(modelName); // 타임아웃 시 맵에서 제거
        });

        emitter.onError(e -> {
            log.error("[SSE] === Emitter 에러 콜백 호출 === 모델: {}, 에러: {}, 스택: {}, 스레드: {}", 
                     modelName, e.getMessage(), e.getClass().getName(), e, Thread.currentThread().getName());
            updateProgressError(modelName, e.getMessage());
            downloadProgressMap.remove(modelName); // 에러 시 맵에서 제거
        });

        return emitter;
    }

    /**
     * 모델 다운로드를 비동기로 시작합니다 (폴링용).
     * @param modelName 다운로드할 모델 이름
     */
    public void startModelDownload(String modelName) {
        log.info("모델 다운로드 시작 요청 - 모델: {}", modelName);

        if (modelName == null || modelName.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        // 이미 다운로드 중인지 확인
        DownloadProgressDto existingProgress = downloadProgressMap.get(modelName);
        if (existingProgress != null && "downloading".equals(existingProgress.getStatus())) {
            log.warn("모델이 이미 다운로드 중입니다 - 모델: {}", modelName);
            throw new CustomException(ErrorCode.AI_SERVER_MODEL_PULL_ALREADY_IN_PROGRESS);
        }

        // 초기 진행 상황 생성
        long currentTime = System.currentTimeMillis();
        DownloadProgressDto progress = DownloadProgressDto.builder()
                .modelName(modelName)
                .status("downloading")
                .completed(0L)
                .total(0L)
                .percentage(0)
                .message("다운로드 시작 중...")
                .startTime(currentTime)
                .lastUpdateTime(currentTime)
                .build();
        downloadProgressMap.put(modelName, progress);

        // 다운로드 스트리밍 시작 (간단한 스레드 사용)
        Thread downloadThread = new Thread(() -> {
            try {
                log.info("[다운로드] === 다운로드 스트리밍 스레드 시작 === 모델: {}, 스레드: {}", modelName, Thread.currentThread().getName());
                streamModelDownloadWithRetry(modelName);
                log.info("[다운로드] === 다운로드 스트리밍 스레드 완료 === 모델: {}", modelName);
            } catch (Exception e) {
                log.error("[다운로드] === 다운로드 스트리밍 스레드 실패 === 모델: {}, 에러: {}", modelName, e.getMessage(), e);
                updateProgressError(modelName, e.getMessage());
            }
        });
        downloadThread.setDaemon(true);
        downloadThread.setName("DownloadThread-" + modelName);
        downloadThread.start();
        log.info("[다운로드] === 다운로드 스트리밍 스레드 시작됨 === 모델: {}, 스레드 ID: {}", modelName, downloadThread.getId());
    }

    /**
     * 재시도 로직이 포함된 모델 다운로드.
     * 타임아웃이나 연결 끊김 시 최대 3회 재시도합니다.
     */
    private void streamModelDownloadWithRetry(String modelName) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                streamModelDownloadWithoutEmitter(modelName);
                return;
            } catch (java.net.SocketTimeoutException e) {
                retryCount++;
                log.warn("[다운로드] 타임아웃 발생, 재시도 {}/{} - 모델: {}, 에러: {}",
                        retryCount, maxRetries, modelName, e.getMessage());

                if (retryCount >= maxRetries) {
                    log.error("[다운로드] 최대 재시도 횟수 초과 - 모델: {}", modelName);
                    updateProgressError(modelName, "타임아웃으로 다운로드 실패 (재시도 " + maxRetries + "회 초과)");
                    return;
                }

                // 재시도 전 진행상황 업데이트
                DownloadProgressDto progress = downloadProgressMap.get(modelName);
                if (progress != null) {
                    progress.setMessage("재연결 시도 중... (" + retryCount + "/" + maxRetries + ")");
                    progress.setLastUpdateTime(System.currentTimeMillis());
                }

                // 2초 대기 후 재시도
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } catch (IOException e) {
                // 다른 IOException은 완료 체크 후 처리
                DownloadProgressDto currentProgress = downloadProgressMap.get(modelName);
                if (currentProgress != null &&
                    "completed".equals(currentProgress.getStatus())) {
                    log.info("[다운로드] 스트림 종료되었지만 다운로드 완료 상태 - 모델: {}", modelName);
                    return;
                }

                retryCount++;
                log.warn("[다운로드] IO 오류 발생, 재시도 {}/{} - 모델: {}, 에러: {}",
                        retryCount, maxRetries, modelName, e.getMessage());

                if (retryCount >= maxRetries) {
                    updateProgressError(modelName, "연결 오류로 다운로드 실패");
                    return;
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * OkHttp로 모델 다운로드를 스트리밍합니다 (SSE Emitter 없이).
     */
    private void streamModelDownloadWithoutEmitter(String modelName) throws IOException {
        log.debug("[DEBUG] streamModelDownloadWithoutEmitter 시작 - 모델: {}", modelName);
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
                            progress.setLastUpdateTime(System.currentTimeMillis());
                            log.debug("[DEBUG] 진행 상황 맵 업데이트 완료: {}", progress);
                        } else {
                            log.warn("[DEBUG] 진행 상황 맵에서 모델을 찾을 수 없음: {}", modelName);
                        }

                        // 완료 확인: status가 "success"인 경우만 (개별 레이어 완료가 아닌 전체 모델 다운로드 완료)
                        boolean isCompleted = "success".equals(status);

                        log.debug("[DEBUG] 완료 체크 - status: {}, total: {}, completed: {}, isCompleted: {}",
                                  status, total, completed, isCompleted);

                        if (isCompleted) {
                            log.info("[다운로드] 모델 다운로드 완료 (status=success) - 모델: {}, completed: {}/{}", modelName, completed, total);
                            updateProgressCompleted(modelName);
                            break;
                        }

                    } catch (JsonProcessingException e) {
                        log.warn("JSON 파싱 실패, 라인 스킵: {}", line);
                    }
                }
            }

        } catch (IOException e) {
            // 스트림이 중간에 끊긴 경우 (stream was reset 등)
            String errorMessage = e.getMessage();
            log.warn("[다운로드] 스트림 읽기 중 오류 발생 - 모델: {}, 에러: {}", modelName, errorMessage, e);

            // 스트림이 끊긴 경우 현재 상태 확인
            DownloadProgressDto currentProgress = downloadProgressMap.get(modelName);
            if (currentProgress != null) {
                // status가 이미 completed인 경우만 완료로 처리 (개별 레이어 완료가 아닌 실제 완료)
                if ("completed".equals(currentProgress.getStatus())) {
                    log.info("[다운로드] 스트림이 끊겼지만 이미 완료 상태 - 모델: {}", modelName);
                } else {
                    // 스트림이 끊겼고 완료 상태가 아닌 경우 에러 처리
                    log.warn("[다운로드] 스트림이 끊겼고 다운로드 미완료 - 모델: {}, completed: {}/{}",
                            modelName, currentProgress.getCompleted(), currentProgress.getTotal());
                    updateProgressError(modelName, "스트림 연결 오류: " + errorMessage);
                }
            } else {
                log.error("[다운로드] 진행 상황을 찾을 수 없음 - 모델: {}", modelName);
                updateProgressError(modelName, "스트림 연결 오류: " + errorMessage);
            }
            throw e;
        } catch (Exception e) {
            log.error("[다운로드] 모델 다운로드 스트리밍 실패 - 모델: {}, 에러: {}", modelName, e.getMessage(), e);
            updateProgressError(modelName, e.getMessage());
            throw e;
        }
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
                            progress.setLastUpdateTime(System.currentTimeMillis());
                            log.debug("[DEBUG] 진행 상황 맵 업데이트 완료: {}", progress);
                        } else {
                            log.warn("[DEBUG] 진행 상황 맵에서 모델을 찾을 수 없음: {}", modelName);
                        }

                        // SSE로 진행 상황 전송
                        if (progress == null) {
                            log.error("[SSE] progress가 null입니다 - 모델: {}", modelName);
                            return;
                        }
                        
                        try {
                            String progressJson = objectMapper.writeValueAsString(progress);
                            log.info("[SSE] === 진행 상황 전송 시도 === 모델: {}, percentage: {}%, completed: {}/{}, 스레드: {}", 
                                     modelName, percentage, completed, total, Thread.currentThread().getName());
                            
                            // emitter.send() 호출
                            emitter.send(SseEmitter.event()
                                    .name("progress")
                                    .data(progressJson));
                            
                            log.info("[SSE] === 진행 상황 전송 완료 === 모델: {}, percentage: {}%, completed: {}/{}, message: {}",
                                     modelName, percentage, completed, total, status);
                        } catch (IllegalStateException e) {
                            log.error("[SSE] === 전송 실패 (IllegalStateException) === 모델: {}, 에러: {}, 스택: {}", 
                                     modelName, e.getMessage(), e);
                            return; // 스트림 처리 중단
                        } catch (IOException e) {
                            log.error("[SSE] === 전송 실패 (IOException) === 모델: {}, 에러: {}, 스택: {}", 
                                     modelName, e.getMessage(), e);
                            try {
                                emitter.completeWithError(e);
                            } catch (Exception ex) {
                                log.error("[SSE] completeWithError() 호출 실패: {}", ex.getMessage());
                            }
                            return; // 스트림 처리 중단
                        } catch (Exception e) {
                            log.error("[SSE] === 전송 실패 (Exception) === 모델: {}, 에러: {}, 스택: {}", 
                                     modelName, e.getMessage(), e);
                            try {
                                emitter.completeWithError(e);
                            } catch (Exception ex) {
                                log.error("[SSE] completeWithError() 호출 실패: {}", ex.getMessage());
                            }
                            return; // 스트림 처리 중단
                        }

                        // 완료 확인: status가 "success"인 경우만 (개별 레이어 완료가 아닌 전체 모델 다운로드 완료)
                        boolean isCompleted = "success".equals(status);

                        log.debug("[DEBUG] 완료 체크 - status: {}, total: {}, completed: {}, isCompleted: {}",
                                  status, total, completed, isCompleted);

                        if (isCompleted) {
                            log.info("[SSE] 모델 다운로드 완료 (status=success) - 모델: {}, completed: {}/{}", modelName, completed, total);
                            updateProgressCompleted(modelName);

                            // 업데이트된 progress 가져오기
                            DownloadProgressDto completedProgress = downloadProgressMap.get(modelName);
                            log.info("[SSE] 완료 이벤트 전송 시작 - 모델: {}", modelName);
                            try {
                                String completedProgressJson = objectMapper.writeValueAsString(completedProgress);
                                log.debug("[DEBUG] 전송할 완료 JSON 데이터: {}", completedProgressJson);
                                
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data(completedProgressJson));
                                log.info("[SSE] 완료 이벤트 전송 성공 - 모델: {}, emitter.complete() 호출", modelName);
                                emitter.complete();
                            } catch (IllegalStateException e) {
                                // 클라이언트 연결이 끊긴 경우 (emitter가 이미 완료되었거나 닫힌 경우)
                                log.warn("[SSE] Emitter가 이미 닫혔거나 완료됨 (완료 이벤트) - 모델: {}, 에러: {}", modelName, e.getMessage());
                                // 이미 닫혔으므로 추가 처리 불필요
                            } catch (IOException e) {
                                log.error("[SSE] 완료 이벤트 전송 실패 - 모델: {}, 에러: {}, 스택: {}", 
                                         modelName, e.getMessage(), e.getClass().getName(), e);
                                try {
                                    emitter.completeWithError(e);
                                } catch (Exception ex) {
                                    log.error("[ERROR] emitter.completeWithError() 호출 실패: {}", ex.getMessage());
                                }
                            } catch (Exception e) {
                                log.error("[SSE] 완료 이벤트 전송 중 예상치 못한 오류 - 모델: {}, 에러: {}", 
                                         modelName, e.getMessage(), e);
                                try {
                                    emitter.completeWithError(e);
                                } catch (Exception ex) {
                                    log.error("[ERROR] emitter.completeWithError() 호출 실패: {}", ex.getMessage());
                                }
                            }
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
            // 에러 메시지가 이미 "오류: "로 시작하는지 확인
            if (errorMessage != null && errorMessage.startsWith("오류: ")) {
                progress.setMessage(errorMessage);
            } else {
                progress.setMessage("오류: " + errorMessage);
            }
            progress.setEndTime(System.currentTimeMillis());
            log.info("[다운로드] 다운로드 실패 상태로 변경 - 모델: {}, 메시지: {}", modelName, progress.getMessage());
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

    // ============================================================================
    // SuhAiderEngine 기반 API (신규)
    // ============================================================================

    /**
     * SuhAiderEngine이 사용 가능한지 확인합니다.
     */
    private void checkSuhAiderEngine() {
        if (suhAiderEngine == null) {
            log.error("SuhAiderEngine이 주입되지 않았습니다");
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SuhAiderEngine을 사용하여 Health Check를 수행합니다.
     */
    public AiServerResponse getSuhAiderHealth() {
        log.info("SuhAider Health Check 시작");
        checkSuhAiderEngine();

        try {
            boolean isHealthy = suhAiderEngine.isHealthy();
            log.info("SuhAider Health Check 결과: {}", isHealthy);

            return AiServerResponse.builder()
                    .isHealthy(isHealthy)
                    .isActive(isHealthy)
                    .healthMessage(isHealthy ? "SuhAider AI 서버가 정상 작동 중입니다" : "SuhAider AI 서버에 연결할 수 없습니다")
                    .build();
        } catch (Exception e) {
            log.error("SuhAider Health Check 실패: {}", e.getMessage(), e);
            return AiServerResponse.builder()
                    .isHealthy(false)
                    .isActive(false)
                    .healthMessage("SuhAider AI 서버에 연결할 수 없습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * SuhAiderEngine을 사용하여 모델 목록을 조회합니다.
     */
    public AiServerResponse getSuhAiderModels() {
        log.info("SuhAider 모델 목록 조회 시작");
        checkSuhAiderEngine();

        try {
            ModelListResponse modelListResponse = suhAiderEngine.getModels();
            List<ModelInfo> modelInfoList = modelListResponse.getModels();

            // ModelInfo -> ModelDto 변환
            List<ModelDto> modelDtos = modelInfoList.stream()
                    .map(this::convertToModelDto)
                    .collect(Collectors.toList());

            log.info("SuhAider 모델 목록 조회 완료 - 모델 개수: {}", modelDtos.size());

            return AiServerResponse.builder()
                    .isActive(true)
                    .models(modelDtos)
                    .build();
        } catch (Exception e) {
            log.error("SuhAider 모델 목록 조회 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * ModelInfo를 ModelDto로 변환합니다.
     */
    private ModelDto convertToModelDto(ModelInfo modelInfo) {
        ModelDetailsDto details = null;
        if (modelInfo.getDetails() != null) {
            details = ModelDetailsDto.builder()
                    .family(modelInfo.getDetails().getFamily())
                    .parameterSize(modelInfo.getDetails().getParameterSize())
                    .quantizationLevel(modelInfo.getDetails().getQuantizationLevel())
                    .build();
        }

        return ModelDto.builder()
                .name(modelInfo.getName())
                .size(modelInfo.getSize())
                .digest(modelInfo.getDigest())
                .modifiedAt(modelInfo.getModifiedAt())
                .details(details)
                .build();
    }

    /**
     * SuhAiderEngine을 사용하여 텍스트를 생성합니다.
     */
    public AiServerResponse suhAiderGenerate(AiServerRequest request) {
        log.info("SuhAider generate 호출 시작 - 모델: {}, 프롬프트 길이: {}",
                request.getModel(), request.getPrompt() != null ? request.getPrompt().length() : 0);
        checkSuhAiderEngine();

        if (request.getModel() == null || request.getModel().trim().isEmpty()) {
            log.error("모델명이 비어있습니다");
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            log.error("프롬프트가 비어있습니다");
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        try {
            long startTime = System.currentTimeMillis();

            SuhAiderRequest suhAiderRequest = SuhAiderRequest.builder()
                    .model(request.getModel())
                    .prompt(request.getPrompt())
                    .stream(false)
                    .build();

            SuhAiderResponse suhAiderResponse = suhAiderEngine.generate(suhAiderRequest);

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("SuhAider generate 완료 - 처리 시간: {}ms", processingTime);

            return AiServerResponse.builder()
                    .isActive(true)
                    .model(request.getModel())
                    .prompt(request.getPrompt())
                    .generatedText(suhAiderResponse.getResponse())
                    .processingTime(processingTime)
                    .build();
        } catch (Exception e) {
            log.error("SuhAider generate 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SuhAiderEngine을 사용하여 단일 텍스트의 임베딩을 생성합니다.
     */
    public AiServerResponse suhAiderEmbed(AiServerRequest request) {
        log.info("SuhAider embed 호출 시작 - 모델: {}, 입력 길이: {}",
                request.getModel(), request.getInput() != null ? request.getInput().length() : 0);
        checkSuhAiderEngine();

        String inputText = request.getInput() != null ? request.getInput() : request.getPrompt();
        if (inputText == null || inputText.trim().isEmpty()) {
            log.error("입력 텍스트가 비어있습니다");
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        // 모델명이 없으면 기본 임베딩 모델 사용
        String model = request.getModel();
        if (model == null || model.trim().isEmpty()) {
            model = "embeddinggemma:latest";
        }

        try {
            long startTime = System.currentTimeMillis();

            List<Double> embeddingVector = suhAiderEngine.embed(model, inputText);

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("SuhAider embed 완료 - 벡터 차원: {}, 처리 시간: {}ms",
                    embeddingVector.size(), processingTime);

            return AiServerResponse.builder()
                    .isActive(true)
                    .model(model)
                    .input(inputText)
                    .embeddingVector(embeddingVector)
                    .vectorDimension(embeddingVector.size())
                    .processingTime(processingTime)
                    .build();
        } catch (Exception e) {
            log.error("SuhAider embed 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SuhAiderEngine을 사용하여 여러 텍스트의 임베딩을 일괄 생성합니다.
     */
    public AiServerResponse suhAiderEmbedBatch(String model, List<String> texts) {
        log.info("SuhAider embedBatch 호출 시작 - 모델: {}, 텍스트 개수: {}", model, texts.size());
        checkSuhAiderEngine();

        if (texts == null || texts.isEmpty()) {
            log.error("입력 텍스트 목록이 비어있습니다");
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        // 모델명이 없으면 기본 임베딩 모델 사용
        if (model == null || model.trim().isEmpty()) {
            model = "embeddinggemma:latest";
        }

        try {
            long startTime = System.currentTimeMillis();

            List<List<Double>> embeddingVectors = suhAiderEngine.embed(model, texts);

            long processingTime = System.currentTimeMillis() - startTime;
            int dimension = embeddingVectors.isEmpty() ? 0 : embeddingVectors.get(0).size();
            log.info("SuhAider embedBatch 완료 - 벡터 개수: {}, 차원: {}, 처리 시간: {}ms",
                    embeddingVectors.size(), dimension, processingTime);

            return AiServerResponse.builder()
                    .isActive(true)
                    .model(model)
                    .embeddingVectors(embeddingVectors)
                    .vectorDimension(dimension)
                    .processingTime(processingTime)
                    .build();
        } catch (Exception e) {
            log.error("SuhAider embedBatch 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
