package me.suhsaechan.aiserver.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import kr.suhsaechan.ai.model.ModelInfo;
import kr.suhsaechan.ai.model.ModelListResponse;
import kr.suhsaechan.ai.model.PullProgress;
import kr.suhsaechan.ai.model.PullResult;
import kr.suhsaechan.ai.model.SuhAiderRequest;
import kr.suhsaechan.ai.model.SuhAiderResponse;
import kr.suhsaechan.ai.service.PullCallback;
import kr.suhsaechan.ai.service.PullHandle;
import kr.suhsaechan.ai.service.SuhAiderEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.aiserver.dto.AiServerRequest;
import me.suhsaechan.aiserver.dto.AiServerResponse;
import me.suhsaechan.aiserver.dto.DownloadProgressDto;
import me.suhsaechan.aiserver.dto.ModelDetailsDto;
import me.suhsaechan.aiserver.dto.ModelDto;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.util.NetworkUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServerService {

    private final NetworkUtil networkUtil;
    private final ServerOptionService serverOptionService;

    @Autowired(required = false)
    private SuhAiderEngine suhAiderEngine;

    @Value("${ai-server.base-url}")
    private String baseUrl;

    @Value("${ai-server.api-key}")
    private String apiKey;

    // 다운로드 진행 상황 추적을 위한 맵 (모델명 -> 진행상황)
    private final Map<String, DownloadProgressDto> downloadProgressMap = new ConcurrentHashMap<>();

    // 다운로드 핸들 추적을 위한 맵 (모델명 -> PullHandle) - 취소 기능 지원
    private final Map<String, PullHandle> pullHandleMap = new ConcurrentHashMap<>();

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
     * 모델 다운로드를 비동기로 시작합니다 (폴링용).
     * SuhAiderEngine의 pullModelStream API를 사용합니다.
     * @param modelName 다운로드할 모델 이름
     */
    public void startModelDownload(String modelName) {
        log.info("모델 다운로드 시작 요청 - 모델: {}", modelName);
        checkSuhAiderEngine();

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

        // SuhAiderEngine의 pullModelStream API 사용
        PullHandle pullHandle = suhAiderEngine.pullModelStream(modelName, new PullCallback() {
            @Override
            public void onProgress(PullProgress pullProgress) {
                DownloadProgressDto currentProgress = downloadProgressMap.get(modelName);
                if (currentProgress != null) {
                    currentProgress.setStatus("downloading");
                    currentProgress.setCompleted(pullProgress.getCompleted());
                    currentProgress.setTotal(pullProgress.getTotal());
                    currentProgress.setPercentage((int) Math.round(pullProgress.getPercent()));
                    currentProgress.setMessage(pullProgress.getStatus());
                    currentProgress.setLastUpdateTime(System.currentTimeMillis());
                    log.debug("[다운로드] 진행 상황 업데이트 - 모델: {}, {}% ({}/{})",
                            modelName, pullProgress.getPercent(),
                            pullProgress.getCompleted(), pullProgress.getTotal());
                }
            }

            @Override
            public void onComplete(PullResult pullResult) {
                log.info("[다운로드] 다운로드 완료 - 모델: {}, 성공: {}, 메시지: {}, 다운로드 시간: {}ms",
                        modelName, pullResult.isSuccess(), pullResult.getErrorMessage(), pullResult.getTotalDurationMs());

                DownloadProgressDto currentProgress = downloadProgressMap.get(modelName);
                if (currentProgress != null) {
                    if (pullResult.isSuccess()) {
                        currentProgress.setStatus("completed");
                        currentProgress.setPercentage(100);
                        currentProgress.setMessage("다운로드 완료");
                    } else {
                        currentProgress.setStatus("failed");
                        currentProgress.setMessage("오류: " + pullResult.getErrorMessage());
                    }
                    currentProgress.setEndTime(System.currentTimeMillis());
                }
                pullHandleMap.remove(modelName);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("[다운로드] 다운로드 오류 - 모델: {}, 에러: {}", modelName, throwable.getMessage(), throwable);
                updateProgressError(modelName, throwable.getMessage());
                pullHandleMap.remove(modelName);
            }
        });

        // PullHandle 저장 (취소 기능 지원)
        pullHandleMap.put(modelName, pullHandle);
        log.info("[다운로드] === 다운로드 시작됨 === 모델: {}", modelName);
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

    /**
     * 모델 다운로드를 취소합니다.
     * @param modelName 취소할 모델 이름
     * @return 취소 성공 여부
     */
    public boolean cancelModelDownload(String modelName) {
        log.info("모델 다운로드 취소 요청 - 모델: {}", modelName);

        if (modelName == null || modelName.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        PullHandle pullHandle = pullHandleMap.get(modelName);
        if (pullHandle == null) {
            log.warn("취소할 다운로드가 없습니다 - 모델: {}", modelName);
            return false;
        }

        if (pullHandle.isDone()) {
            log.info("다운로드가 이미 완료되었습니다 - 모델: {}", modelName);
            pullHandleMap.remove(modelName);
            return false;
        }

        pullHandle.cancel();
        log.info("모델 다운로드 취소됨 - 모델: {}", modelName);

        DownloadProgressDto progress = downloadProgressMap.get(modelName);
        if (progress != null) {
            progress.setStatus("cancelled");
            progress.setMessage("사용자에 의해 취소됨");
            progress.setEndTime(System.currentTimeMillis());
        }

        pullHandleMap.remove(modelName);
        return true;
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
            model = serverOptionService.getOptionValue(ServerOptionKey.CHATBOT_EMBEDDING_MODEL);
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

}
