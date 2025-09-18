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
     * AI 서버의 터널 정보를 조회합니다.
     */
    @PostMapping(value = "/info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiServerResponse> getAiServerInfo(AiServerRequest request) {
        return ResponseEntity.ok(aiServerService.getTunnelInfo(request));
    }
}
