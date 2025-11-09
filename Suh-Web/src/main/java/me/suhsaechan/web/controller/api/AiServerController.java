package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.aiserver.dto.AiServerRequest;
import me.suhsaechan.aiserver.dto.AiServerResponse;
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
}
