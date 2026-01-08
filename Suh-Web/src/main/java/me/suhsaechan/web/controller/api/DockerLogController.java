package me.suhsaechan.web.controller.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.docker.dto.DockerRequest;
import me.suhsaechan.docker.dto.DockerLogResponse;
import me.suhsaechan.docker.dto.ContainerInfoDto;
import me.suhsaechan.docker.service.DockerLogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Docker 컨테이너 로그를 위한 컨트롤러 (폴링 + SSE 스트리밍)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/docker-log")
public class DockerLogController {
    private final DockerLogService dockerLogService;

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<SseEmitter, AtomicBoolean> activeEmitters = new ConcurrentHashMap<>();

    /**
     * Docker 컨테이너 로그 조회 (폴링용)
     * @param request 로그 요청 정보 (컨테이너 이름 등)
     * @return 로그 응답
     */
    @GetMapping(value = "/poll", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DockerLogResponse> pollContainerLogs(@ModelAttribute DockerRequest request) {
        String containerName = request.getContainerName() != null ? request.getContainerName() : "sejong-malsami-back";
        Integer lineLimit = request.getLineLimit() != null ? request.getLineLimit() : 100;
        
        log.info("Docker 로그 폴링 요청 - 컨테이너: {}, 라인 제한: {}", containerName, lineLimit);
        
        // 요청 객체에 기본값 설정
        if (request.getContainerName() == null) {
            request.setContainerName(containerName);
        }
        if (request.getLineLimit() == null) {
            request.setLineLimit(lineLimit);
        }
        
        DockerLogResponse response = dockerLogService.getContainerLogs(request);
        return ResponseEntity.ok(response);
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

    /**
     * Docker 컨테이너 로그 실시간 스트리밍 (SSE)
     * @param containerName 컨테이너 이름
     * @param tailLines 초기 로드할 라인 수
     * @return SSE 이미터
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamContainerLogs(
            @RequestParam(defaultValue = "sejong-malsami-back") String containerName,
            @RequestParam(defaultValue = "100") int tailLines) {

        log.info("SSE 스트리밍 요청 - 컨테이너: {}, tail: {}", containerName, tailLines);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicBoolean isRunning = new AtomicBoolean(true);
        activeEmitters.put(emitter, isRunning);

        emitter.onCompletion(() -> {
            log.debug("SSE 연결 완료 - 컨테이너: {}", containerName);
            cleanupEmitter(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE 연결 타임아웃 - 컨테이너: {}", containerName);
            cleanupEmitter(emitter);
        });

        emitter.onError((e) -> {
            log.debug("SSE 연결 에러 - 컨테이너: {}, 에러: {}", containerName, e.getMessage());
            cleanupEmitter(emitter);
        });

        executorService.execute(() -> {
            dockerLogService.streamContainerLogs(containerName, tailLines, emitter, isRunning);
        });

        return emitter;
    }

    private void cleanupEmitter(SseEmitter emitter) {
        AtomicBoolean isRunning = activeEmitters.remove(emitter);
        if (isRunning != null) {
            isRunning.set(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("DockerLogController 종료 - 활성 SSE 연결 정리");
        activeEmitters.forEach((emitter, isRunning) -> {
            isRunning.set(false);
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        activeEmitters.clear();
        executorService.shutdown();
    }
}
