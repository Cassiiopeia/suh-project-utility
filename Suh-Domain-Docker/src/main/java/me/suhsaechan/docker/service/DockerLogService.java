package me.suhsaechan.docker.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.util.SshCommandExecutor;
import me.suhsaechan.docker.dto.DockerRequest;
import me.suhsaechan.docker.dto.DockerLogResponse;
import me.suhsaechan.docker.dto.ContainerInfoDto;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Docker 로그 서비스 (폴링 + SSE 스트리밍 방식)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DockerLogService {

    private final SshCommandExecutor sshCommandExecutor;

    private static final int SSH_CONNECT_TIMEOUT = 10000;
    private static final int STREAM_READ_TIMEOUT = 100;

    /**
     * Docker 컨테이너 로그 조회 (폴링용, sudo 없이)
     *
     * @param request 로그 요청 정보
     * @return 로그 응답
     */
    public DockerLogResponse getContainerLogs(DockerRequest request) {
        String containerName = Optional.ofNullable(request.getContainerName()).orElse("sejong-malsami-back");
        Integer lineLimit = Optional.ofNullable(request.getLineLimit()).orElse(100);

        log.info("Docker 로그 조회 요청 (sudo 없음) - 컨테이너: {}, 라인 제한: {}", containerName, lineLimit);

        String command = String.format(
            "export PATH=$PATH:/usr/local/bin && docker logs --tail=%d %s 2>&1",
            lineLimit, DockerContainerNameValidator.requireValidContainerName(containerName)
        );

        String logs = sshCommandExecutor.executeCommand(command);
        int totalLines = logs.isEmpty() ? 0 : logs.split("\n").length;

        log.info("로그 조회 완료 - 컨테이너: {}, 총 {} 줄", containerName, totalLines);

        return DockerLogResponse.builder()
                .logs(logs)
                .totalLines(totalLines)
                .build();
    }

    /**
     * Docker 컨테이너 로그 실시간 스트리밍 (SSE용, sudo 없이)
     * docker logs -f 명령어를 사용하여 실시간으로 로그를 전송
     *
     * @param containerName 컨테이너 이름
     * @param tailLines 초기 로드할 라인 수
     * @param emitter SSE 이미터
     * @param isRunning 스트리밍 실행 상태 플래그
     */
    public void streamContainerLogs(String containerName, int tailLines, SseEmitter emitter, AtomicBoolean isRunning) {
        Session session = null;
        ChannelExec channel = null;

        try {
            log.info("SSE 스트리밍 시작 (sudo 없음) - 컨테이너: {}, tail: {}", containerName, tailLines);
            session = sshCommandExecutor.openSession();

            channel = (ChannelExec) session.openChannel("exec");
            String command = String.format(
                "export PATH=$PATH:/usr/local/bin && docker logs -f --tail=%d %s 2>&1",
                tailLines, DockerContainerNameValidator.requireValidContainerName(containerName)
            );
            channel.setCommand(command);

            InputStream inputStream = channel.getInputStream();
            channel.connect(SSH_CONNECT_TIMEOUT);

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;

            while (isRunning.get() && !channel.isClosed()) {
                if (reader.ready()) {
                    line = reader.readLine();
                    if (line != null) {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("log")
                                .data(line));
                        } catch (IOException e) {
                            log.debug("SSE 전송 실패 (클라이언트 연결 종료): {}", e.getMessage());
                            break;
                        }
                    }
                } else {
                    Thread.sleep(STREAM_READ_TIMEOUT);
                }
            }

            log.info("SSE 스트리밍 종료 - 컨테이너: {}", containerName);

        } catch (JSchException e) {
            log.error("SSE 스트리밍 SSH 연결 실패: {}", e.getMessage());
            sendErrorEvent(emitter, "SSH 연결 실패: " + e.getMessage());
        } catch (IOException e) {
            log.error("SSE 스트리밍 IO 오류: {}", e.getMessage());
            sendErrorEvent(emitter, "로그 읽기 오류: " + e.getMessage());
        } catch (InterruptedException e) {
            log.debug("SSE 스트리밍 인터럽트: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    private void sendErrorEvent(SseEmitter emitter, String errorMessage) {
        try {
            emitter.send(SseEmitter.event()
                .name("error")
                .data(errorMessage));
        } catch (IOException ignored) {
        }
    }

    /**
     * 서버의 모든 Docker 컨테이너 목록 조회 (ps -a)
     */
    public List<ContainerInfoDto> listContainers() {
        String result = sshCommandExecutor.executeCommand(
                "export PATH=$PATH:/usr/local/bin && docker ps -a --format \"{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}\"" );
        List<ContainerInfoDto> list = new ArrayList<>();
        if (result != null && !result.isEmpty()) {
            for (String line : result.split("\n")) {
                String[] parts = line.split("\\|");
                if (parts.length == 4) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String image = parts[2].trim();
                    String status = parts[3].trim();
                    boolean running = status.toLowerCase().startsWith("up");
                    list.add(ContainerInfoDto.builder()
                        .id(id)
                        .name(name)
                        .image(image)
                        .status(status)
                        .isRunning(running)
                        .build());
                }
            }
        }
        return list;
    }
}
