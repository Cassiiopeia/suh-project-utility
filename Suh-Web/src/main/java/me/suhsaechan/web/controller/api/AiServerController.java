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
     * AI 서버 터널 정보 조회 : /api/tunnel-info
     */
    @PostMapping(value = "/info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> getAiServerInfo() {
        return ResponseEntity.ok(aiServerService.getTunnelInfo());
    }

    /**
     * 모델 목록 조회
     */
    @PostMapping(value = "/models", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> getModels() {
        return ResponseEntity.ok(aiServerService.getModels());
    }

    /**
     * embeddings API 호출
     */
    @PostMapping(value = "/embeddings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> callEmbeddings(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.callEmbeddings(request));
    }

    /**
     * generate API 호출
     */
    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> callGenerate(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.callGenerate(request));
    }
}
