package me.suhsaechan.web.controller.api;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.docker.dto.DockerRequest;
import me.suhsaechan.docker.dto.ContainerInfoDto;
import me.suhsaechan.docker.service.DockerLogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Docker 컨테이너 로그 스트리밍을 위한 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/docker-log")
public class DockerLogController {
    private final DockerLogService dockerLogService;


    /**
     * Docker 컨테이너 로그를 SSE로 스트리밍
     * @param request 로그 요청 정보 (컨테이너 이름 등)
     * @return SSE Emitter 객체
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamContainerLogs(@ModelAttribute DockerRequest request) {
        String containerName = request.getContainerName() != null ? request.getContainerName() : "sejong-malsami-back";
        Integer lineLimit = request.getLineLimit() != null ? request.getLineLimit() : 100;
        
        log.info("Docker 로그 스트리밍 요청 - 컨테이너: {}, 라인 제한: {}", containerName, lineLimit);
        
        // 요청 객체에 기본값 설정
        if (request.getContainerName() == null) {
            request.setContainerName(containerName);
        }
        if (request.getLineLimit() == null) {
            request.setLineLimit(lineLimit);
        }
        
        return dockerLogService.streamContainerLogs(request);
    }
    
    /**
     * Docker 로그 스트리밍 중지
     * @param request 로그 요청 정보 (컨테이너 이름 등)
     * @return 응답 엔티티
     */
    @PostMapping(value = "/stop", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> stopContainerLogs(@ModelAttribute DockerRequest request) {
        String containerName = request.getContainerName() != null ? request.getContainerName() : "sejong-malsami-back";
        log.info("Docker 로그 스트리밍 중지 요청 - 컨테이너: {}", containerName);
        
        // 요청 객체에 기본값 설정
        if (request.getContainerName() == null) {
            request.setContainerName(containerName);
        }
        
        dockerLogService.stopLogStreaming(request);
        return ResponseEntity.ok("로그 스트리밍이 중지되었습니다.");
    }
    
    /**
     * Docker 로그 내용 초기화 (클라이언트 표시 화면 초기화용)
     * @param request 로그 요청 정보 (컨테이너 이름 등)
     * @return 응답 엔티티
     */
    @PostMapping(value = "/clear", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> clearContainerLogs(@ModelAttribute DockerRequest request) {
        String containerName = request.getContainerName() != null ? request.getContainerName() : "sejong-malsami-back";
        log.info("Docker 로그 화면 초기화 요청 - 컨테이너: {}", containerName);
        return ResponseEntity.ok("로그 화면이 초기화되었습니다.");
    }

    /**
     * 실행 중/중지된 모든 컨테이너 목록 반환
     */
    @GetMapping(value = "/containers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ContainerInfoDto>> listContainers() {
        return ResponseEntity.ok(dockerLogService.listContainers());
    }
} 