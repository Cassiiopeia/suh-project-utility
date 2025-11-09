package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.aiserver.dto.AiServerRequest;
import me.suhsaechan.aiserver.dto.AiServerResponse;
import me.suhsaechan.aiserver.service.AiServerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import me.suhsaechan.aiserver.dto.DownloadProgressDto;
import java.util.Map;

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
     * AI 서버의 모델 목록을 조회합니다.
     */
    @PostMapping(value = "/models", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> getModels(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.getModels(request));
    }

    /**
     * AI 서버의 embeddings API를 호출합니다.
     */
    @PostMapping(value = "/embeddings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> callEmbeddings(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.callEmbeddings(request));
    }

    /**
     * AI 서버의 generate API를 호출합니다.
     */
    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> callGenerate(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.callGenerate(request));
    }

    /**
     * AI 서버에서 모델을 다운로드합니다.
     */
    @PostMapping(value = "/models/pull", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> pullModel(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.pullModel(request));
    }

    /**
     * AI 서버에서 모델을 삭제합니다.
     */
    @PostMapping(value = "/models/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> deleteModel(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.deleteModel(request));
    }

    /**
     * AI 서버 모델 다운로드를 SSE로 스트리밍합니다.
     * @param modelName 다운로드할 모델 이름
     * @return SSE Emitter 객체
     */
    @GetMapping(value = "/models/pull/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter pullModelStream(@RequestParam String modelName) {
        log.info("AI 모델 다운로드 스트리밍 시작 - 모델: {}", modelName);
        return aiServerService.pullModelStream(modelName);
    }

    /**
     * 현재 진행 중인 모든 다운로드의 상태를 조회합니다.
     * @return 다운로드 진행 상황 맵
     */
    @GetMapping(value = "/models/pull/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, DownloadProgressDto>> getDownloadStatus() {
        return ResponseEntity.ok(aiServerService.getDownloadStatus());
    }

    /**
     * 특정 모델의 다운로드 상태를 조회합니다.
     * @param modelName 모델 이름
     * @return 다운로드 진행 상황
     */
    @GetMapping(value = "/models/pull/status/{modelName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DownloadProgressDto> getModelDownloadStatus(@RequestParam String modelName) {
        DownloadProgressDto progress = aiServerService.getModelDownloadStatus(modelName);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }
}
