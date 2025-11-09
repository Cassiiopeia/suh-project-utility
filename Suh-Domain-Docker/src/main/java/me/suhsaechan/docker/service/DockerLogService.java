package me.suhsaechan.docker.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.util.SshCommandExecutor;
import me.suhsaechan.common.properties.SshConnectionProperties;
import me.suhsaechan.docker.dto.DockerRequest;
import me.suhsaechan.docker.dto.ContainerInfoDto;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Docker 로그 스트리밍 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DockerLogService {

    private final SshCommandExecutor sshCommandExecutor;
    private final SshConnectionProperties sshProps;
    
    // 현재 실행 중인 로그 스트리밍 작업을 저장하는 맵 (컨테이너 이름 -> 실행 작업)
    // 메모리 최적화: 컨테이너별 상태를 하나의 객체로 관리
    private final Map<String, ContainerStreamContext> activeStreams = new ConcurrentHashMap<>();

    // 메모리 최적화: 고정 크기 스레드 풀 사용 (최대 10개 동시 스트리밍)
    private final ExecutorService executorService = Executors.newFixedThreadPool(
        10,
        r -> {
            Thread t = new Thread(r);
            t.setDaemon(true); // 메모리 누수 방지: 데몬 스레드로 설정
            t.setName("docker-log-stream-" + System.currentTimeMillis());
            return t;
        }
    );

    /**
     * 컨테이너 스트리밍 컨텍스트 (메모리 최적화를 위한 통합 관리)
     */
    private static class ContainerStreamContext {
        private final SseEmitter emitter;
        private final Thread thread;
        private volatile Session session;
        private volatile ChannelExec channel;
        private volatile boolean stopped = false;

        ContainerStreamContext(SseEmitter emitter, Thread thread) {
            this.emitter = emitter;
            this.thread = thread;
        }

        synchronized void setSession(Session session) {
            this.session = session;
        }

        synchronized void setChannel(ChannelExec channel) {
            this.channel = channel;
        }

        synchronized void stop() {
            if (stopped) return;
            stopped = true;

            // 채널 종료
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }

            // 세션 종료
            if (session != null && session.isConnected()) {
                session.disconnect();
            }

            // 스레드 인터럽트
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }

        synchronized boolean isStopped() {
            return stopped;
        }
    }

    /**
     * Docker 컨테이너 로그를 SSE로 스트리밍
     * 
     * @param request 로그 요청 정보
     * @return SSE Emitter 객체
     */
    public SseEmitter streamContainerLogs(DockerRequest request) {
        // 이미 실행 중인 스트리밍이 있다면 중지
        stopLogStreaming(request);
        
        // 기본값 설정
        String containerName = Optional.ofNullable(request.getContainerName()).orElse("sejong-malsami-back");
        long timeout = 3600000L; // 1시간 타임아웃 (필요에 따라 조정 가능)
        
        log.info("Docker 로그 스트리밍 시작 - 컨테이너: {}, 라인 제한: {}", 
                containerName, request.getLineLimit());
        
        // SSE Emitter 생성 (타임아웃 설정)
        SseEmitter emitter = new SseEmitter(timeout);

        // 완료, 타임아웃, 에러 이벤트 핸들러 등록
        emitter.onCompletion(() -> {
            log.info("컨테이너 로그 스트리밍 완료: {}", containerName);
            stopLogStreaming(request);
        });

        emitter.onTimeout(() -> {
            log.info("컨테이너 로그 스트리밍 타임아웃: {}", containerName);
            stopLogStreaming(request);
        });

        emitter.onError(e -> {
            log.error("컨테이너 로그 스트리밍 오류: {}", containerName, e);
            stopLogStreaming(request);
        });
        
        try {
            // 초기 연결 확인 메시지 전송
            emitter.send(SseEmitter.event()
                    .name("log")
                    .data("=== 연결 초기화 중... ===\n"));
            
            log.debug("SSE 초기 연결 메시지 전송 완료");
        } catch (IOException e) {
            log.error("초기 연결 메시지 전송 실패", e);
            emitter.completeWithError(e);
            return emitter;
        }
        
        // 메모리 최적화: 비동기 로그 스트리밍 작업 생성
        Thread streamingThread = new Thread(() -> {
            try {
                log.debug("로그 스트리밍 스레드 시작 - 컨테이너: {}", containerName);
                streamDockerLogsWithJSch(containerName, request.getLineLimit());
            } catch (Exception e) {
                log.error("로그 스트리밍 중 오류 발생: {}", containerName, e);

                // 컨텍스트를 통해 안전하게 에러 전송
                ContainerStreamContext ctx = activeStreams.get(containerName);
                if (ctx != null && !ctx.isStopped()) {
                    try {
                        ctx.emitter.send(SseEmitter.event()
                                .name("log")
                                .data("로그 스트리밍 오류: " + e.getMessage() + "\n"));
                        ctx.emitter.completeWithError(e);
                    } catch (IOException ex) {
                        log.error("오류 메시지 전송 실패", ex);
                    }
                }
            }
        });

        streamingThread.setDaemon(true);
        streamingThread.setName("docker-log-" + containerName + "-" + System.currentTimeMillis());

        // 메모리 최적화: 컨텍스트 생성 및 원자적 저장
        ContainerStreamContext context = new ContainerStreamContext(emitter, streamingThread);
        activeStreams.put(containerName, context);

        streamingThread.start();

        log.info("Docker 로그 스트리밍 요청 처리 완료 - 컨테이너: {}", containerName);
        return emitter;
    }
    
    /**
     * Docker 로그 스트리밍 중지
     * 
     * @param request 로그 요청 정보
     */
    public void stopLogStreaming(DockerRequest request) {
        String containerName = Optional.ofNullable(request.getContainerName()).orElse("sejong-malsami-back");
        log.info("Docker 로그 스트리밍 중지 요청 - 컨테이너: {}", containerName);
        
        // 컨텍스트 조회 및 제거
        ContainerStreamContext context = activeStreams.remove(containerName);
        if (context != null) {
            log.debug("스트림 컨텍스트 종료 - 컨테이너: {}", containerName);
            context.stop(); // 모든 리소스 정리
        }
    }
    
    /**
     * JSch를 직접 사용하여 SSH 연결을 통해 Docker 로그를 실시간으로 스트리밍
     * 메모리 최적화: 컨텍스트에서 emitter를 가져와서 사용
     *
     * @param containerName 컨테이너 이름
     * @param lineLimit 라인 제한 (null이면 기본값 100)
     */
    private void streamDockerLogsWithJSch(String containerName, Integer lineLimit) {
        Session session = null;
        ChannelExec channel = null;

        // 컨텍스트에서 emitter 가져오기
        ContainerStreamContext context = activeStreams.get(containerName);
        if (context == null || context.isStopped()) {
            log.warn("스트리밍 컨텍스트가 없거나 이미 종료됨: {}", containerName);
            return;
        }
        SseEmitter emitter = context.emitter;

        try {
            // SSH 연결 정보 가져오기
            String host = sshProps.getHost();
            String username = sshProps.getUsername();
            String password = sshProps.getPassword();
            int port = sshProps.getPort();
            
            log.debug("SSH 연결 정보 - 호스트: {}, 포트: {}, 사용자: {}", host, port, username);
            
            // 로그 시작 메시지 전송
            sendLogEvent(emitter, "=== Docker 로그 스트리밍 시작: " + containerName + " ===\n");
            sendLogEvent(emitter, "SSH 연결 중... (" + host + ":" + port + ")\n");
            
            // JSch 초기화
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            
            // 호스트 키 검사 비활성화
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            
            // 세션 연결
            log.info("SSH 세션 연결 시도 - 호스트: {}:{}, 사용자: {}", host, port, username);
            try {
                session.connect(10000); // 10초 타임아웃으로 감소 (빠른 실패)
                log.info("SSH 세션 연결 성공 - 컨테이너: {}", containerName);
            } catch (JSchException e) {
                log.error("SSH 세션 연결 실패 - 호스트: {}:{}, 에러: {}", host, port, e.getMessage());

                // 구체적인 오류 메시지 제공
                String errorMessage;
                if (e.getMessage().contains("timeout") || e.getMessage().contains("Connection timed out")) {
                    errorMessage = "SSH 서버 연결 타임아웃 - 네트워크 상태나 방화벽을 확인해주세요.";
                } else if (e.getMessage().contains("Connection refused")) {
                    errorMessage = "SSH 서비스가 실행되지 않거나 포트가 차단되었습니다.";
                } else if (e.getMessage().contains("Auth fail")) {
                    errorMessage = "SSH 인증 실패 - 사용자명/비밀번호를 확인해주세요.";
                } else {
                    errorMessage = "SSH 연결 실패: " + e.getMessage();
                }

                sendLogEvent(emitter, errorMessage + "\n");
                sendLogEvent(emitter, "서버 상태를 확인하거나 관리자에게 문의하세요.\n");

                // 연결 실패 시 즉시 emitter 종료
                emitter.completeWithError(e);
                return; // 예외를 던지지 않고 메서드 종료
            }
            
            sendLogEvent(emitter, "SSH 연결 성공\n");

            // 초기 로그 (최근 N줄) 가져오기
            int tailLines = lineLimit != null && lineLimit > 0 ? lineLimit : 100;

            sendLogEvent(emitter, "최근 " + tailLines + "줄 로그 가져오는 중...\n");

            // 이미 연결된 session으로 초기 로그 채널 생성
            ChannelExec initialChannel = null;
            int initialLogCount = 0;
            try {
                initialChannel = (ChannelExec) session.openChannel("exec");
                String initialCommand = String.format(
                    "sudo -S -p '' bash -c 'export PATH=$PATH:/usr/local/bin && docker logs --tail=%d %s'",
                    tailLines, containerName
                );
                initialChannel.setCommand(initialCommand);
                initialChannel.setPty(true);

                InputStream initialIn = initialChannel.getInputStream();
                OutputStream initialOut = initialChannel.getOutputStream();
                initialChannel.connect(10000); // 10초 타임아웃

                // sudo 비밀번호 전달
                initialOut.write((password + "\n").getBytes(StandardCharsets.UTF_8));
                initialOut.flush();

                // 초기 로그 읽기 (타임아웃 30초)
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(initialIn, StandardCharsets.UTF_8))) {
                    String line;
                    long startTime = System.currentTimeMillis();
                    // 최대 30초 타임아웃
                    while ((line = reader.readLine()) != null) {
                        sendLogEvent(emitter, line + "\n");
                        initialLogCount++;

                        // 타임아웃 체크 (30초)
                        if (System.currentTimeMillis() - startTime > 30000) {
                            log.warn("초기 로그 읽기 타임아웃 (30초 초과)");
                            break;
                        }
                    }
                }

                log.info("초기 로그 {} 줄 전송 완료", initialLogCount);

            } catch (Exception e) {
                log.error("초기 로그 가져오기 실패: {}", e.getMessage(), e);
                sendLogEvent(emitter, "초기 로그 가져오기 실패: " + e.getMessage() + "\n");
            } finally {
                if (initialChannel != null && initialChannel.isConnected()) {
                    initialChannel.disconnect();
                }
            }

            // 초기 로그 이후 실시간 스트리밍 준비
            
            // 실시간 스트리밍을 위한 새 채널 열기 (follow 모드)
            sendLogEvent(emitter, "--- 실시간 로그 스트리밍 시작 ---\n");
            
            channel = (ChannelExec) session.openChannel("exec");
            channel.setPty(true);
            // sudo -S -p '' 로 프롬프트 제거 후 패스워드 STDIN 으로 전달
            String followCommand = "sudo -S -p '' bash -c 'export PATH=$PATH:/usr/local/bin && docker logs -f " + containerName + "'";
            log.debug("실시간 로그 명령어 실행: {}", followCommand);
            channel.setCommand(followCommand);

            // 입출력 스트림 설정
            InputStream in = channel.getInputStream();
            OutputStream out = channel.getOutputStream();
            channel.connect();
            // sudo 비밀번호 전달
            out.write((password + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            log.debug("실시간 로그 채널 연결 성공");

            // 현재 채널과 세션을 컨텍스트에 저장 (나중에 종료하기 위해)
            if (context != null) {
                context.setChannel(channel);
                context.setSession(session);
            }

            // 로그 실시간 읽기
            int streamLogCount = 0;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while (!Thread.currentThread().isInterrupted() &&
                       (line = reader.readLine()) != null &&
                       activeStreams.containsKey(containerName)) {
                    // 새 로그 라인을 SSE로 전송
                    try {
                        sendLogEvent(emitter, line + "\n");
                        streamLogCount++;
                        if (streamLogCount % 100 == 0) {
                            log.debug("실시간 로그 {} 줄 전송 중...", streamLogCount);
                        }
                    } catch (IllegalStateException closed) {
                        // Emitter 가 이미 종료된 경우 루프 탈출
                        log.info("Emitter closed - 실시간 로그 전송 중단: {}", containerName);
                        break;
                    }
                }
            }
            
            log.info("컨테이너 {} 로그 스트리밍 종료 - 총 {} 줄 전송", containerName, initialLogCount + streamLogCount);
            
        } catch (JSchException e) {
            log.error("SSH 연결 오류: {}", e.getMessage(), e);
            try {
                if (e.getMessage().contains("socket is not established") ||
                    e.getMessage().contains("connection is closed") ||
                    e.getMessage().contains("timeout")) {
                    sendLogEvent(emitter, "네트워크 연결 문제가 발생했습니다.\n");
                    sendLogEvent(emitter, "서버 상태를 확인하거나 잠시 후 다시 시도해주세요.\n");
                } else if (e.getMessage().contains("Auth")) {
                    sendLogEvent(emitter, "SSH 인증 실패: 계정 정보를 확인해주세요.\n");
                } else {
                    sendLogEvent(emitter, "SSH 연결 오류: " + e.getMessage() + "\n");
                }
                sendLogEvent(emitter, "연결이 중단되었습니다. 다시 시작하려면 \"로그 시작\" 버튼을 클릭하세요.\n");
                emitter.completeWithError(e);
            } catch (IOException ex) {
                log.error("오류 메시지 전송 실패", ex);
                emitter.completeWithError(ex);
            }
            return; // 예외를 던지지 않고 메서드 종료
        } catch (IOException e) {
            log.error("로그 스트리밍 중 I/O 오류: {}", e.getMessage(), e);
            try {
                sendLogEvent(emitter, "로그 스트리밍 오류: " + e.getMessage() + "\n");
            } catch (IOException ex) {
                log.error("오류 메시지 전송 실패", ex);
            }
            throw new RuntimeException("로그 스트리밍 오류: " + e.getMessage(), e);
        } finally {
            // 자원 정리
            if (channel != null && channel.isConnected()) {
                log.debug("실시간 로그 채널 연결 종료 - 컨테이너: {}", containerName);
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                log.debug("SSH 세션 연결 종료 - 컨테이너: {}", containerName);
                session.disconnect();
            }
        }
    }
    
    /**
     * SSE 이벤트로 로그 메시지 전송
     * 
     * @param emitter SSE Emitter
     * @param message 로그 메시지
     * @throws IOException I/O 예외
     */
    private void sendLogEvent(SseEmitter emitter, String message) throws IOException {
        if (message == null || message.isEmpty()) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("log")
                    .data(message, org.springframework.http.MediaType.valueOf("text/plain;charset=UTF-8")));
        } catch (IllegalStateException closed) {
            // Emitter 가 이미 닫혔을 때는 상위 호출부에서 처리하도록 예외 전달
            throw closed;
        } catch (Exception e) {
            log.warn("로그 이벤트 전송 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 서버의 모든 Docker 컨테이너 목록 조회 (ps -a)
     */
    public List<ContainerInfoDto> listContainers() {
        String result = sshCommandExecutor.executeCommandWithSudoStdin(
                "docker ps -a --format \\\"{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}\\\"" );
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