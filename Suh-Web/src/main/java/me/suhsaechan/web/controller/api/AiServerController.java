package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.aiserver.dto.AiServerRequest;
import me.suhsaechan.aiserver.dto.AiServerResponse;
import me.suhsaechan.aiserver.dto.DownloadProgressDto;
import me.suhsaechan.aiserver.service.AiServerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ai-server")
@RequiredArgsConstructor
public class AiServerController {

    private final AiServerService aiServerService;

    /**
     * AI 서버의 Health Check를 수행합니다.
     */
    @PostMapping(value = "/health", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> getHealth(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.getHealth(request));
    }

    /**
     * AI 서버에서 모델을 삭제합니다.
     */
    @PostMapping(value = "/models/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> deleteModel(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.deleteModel(request));
    }

    /**
     * 모델 다운로드를 시작합니다 (비동기, 폴링용).
     */
    @PostMapping(value = "/models/pull/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> startModelDownload(AiServerRequest request) {
        log.info("AI 모델 다운로드 시작 요청 - 모델: {}", request.getModelName());
        aiServerService.startModelDownload(request.getModelName());
        return ResponseEntity.ok(AiServerResponse.builder()
                .isActive(true)
                .model(request.getModelName())
                .build());
    }

    /**
     * 현재 진행 중인 모든 다운로드의 상태를 조회합니다.
     */
    @PostMapping(value = "/models/pull/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> getDownloadStatus(AiServerRequest request) {
        return ResponseEntity.ok(AiServerResponse.builder()
                .isActive(true)
                .downloadProgressMap(aiServerService.getDownloadStatus())
                .build());
    }

    /**
     * 특정 모델의 다운로드 상태를 조회합니다.
     */
    @PostMapping(value = "/models/pull/status/model", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> getModelDownloadStatus(AiServerRequest request) {
        DownloadProgressDto progress = aiServerService.getModelDownloadStatus(request.getModelName());
        return ResponseEntity.ok(AiServerResponse.builder()
                .isActive(true)
                .model(request.getModelName())
                .downloadProgress(progress)
                .build());
    }

    /**
     * 모델 다운로드를 취소합니다.
     */
    @PostMapping(value = "/models/pull/cancel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> cancelModelDownload(AiServerRequest request) {
        log.info("AI 모델 다운로드 취소 요청 - 모델: {}", request.getModelName());
        boolean cancelled = aiServerService.cancelModelDownload(request.getModelName());
        return ResponseEntity.ok(AiServerResponse.builder()
                .isActive(cancelled)
                .model(request.getModelName())
                .build());
    }

    /**
     * SuhAiderEngine을 사용하여 Health Check를 수행합니다.
     */
    @PostMapping(value = "/suh-aider/health", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> getSuhAiderHealth(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.getSuhAiderHealth());
    }

    /**
     * SuhAiderEngine을 사용하여 모델 목록을 조회합니다.
     */
    @PostMapping(value = "/suh-aider/models", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> getSuhAiderModels(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.getSuhAiderModels());
    }

    /**
     * SuhAiderEngine을 사용하여 텍스트를 생성합니다.
     */
    @PostMapping(value = "/suh-aider/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> suhAiderGenerate(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.suhAiderGenerate(request));
    }

    /**
     * SuhAiderEngine을 사용하여 임베딩을 생성합니다.
     */
    @PostMapping(value = "/suh-aider/embed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> suhAiderEmbed(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.suhAiderEmbed(request));
    }
}
