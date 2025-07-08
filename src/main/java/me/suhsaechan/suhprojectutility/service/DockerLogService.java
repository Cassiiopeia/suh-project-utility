package me.suhsaechan.suhprojectutility.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.config.SshConnectionProperties;
import me.suhsaechan.suhprojectutility.object.request.DockerLogRequest;
import me.suhsaechan.suhprojectutility.util.SshCommandExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

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
    private final Map<String, Thread> runningThreads = new ConcurrentHashMap<>();
    private final Map<String, Session> sshSessions = new ConcurrentHashMap<>();
    private final Map<String, ChannelExec> sshChannels = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Docker 컨테이너 로그를 SSE로 스트리밍
     * 
     * @param request 로그 요청 정보
     * @return SSE Emitter 객체
     */
    public SseEmitter streamContainerLogs(DockerLogRequest request) {
        // 이미 실행 중인 스트리밍이 있다면 중지
        stopLogStreaming(request);
        
        // 기본값 설정
        String containerName = Optional.ofNullable(request.getContainerName()).orElse("sejong-malsami-back");
        long timeout = 3600000L; // 1시간 타임아웃 (필요에 따라 조정 가능)
        
        log.info("Docker 로그 스트리밍 시작 - 컨테이너: {}, 라인 제한: {}", 
                containerName, request.getLineLimit());
        
        // SSE Emitter 생성 (타임아웃 설정)
        SseEmitter emitter = new SseEmitter(timeout);
        activeEmitters.put(containerName, emitter);
        
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
        
        // 비동기적으로 로그 스트리밍 시작
        Thread streamingThread = new Thread(() -> {
            try {
                log.debug("로그 스트리밍 스레드 시작 - 컨테이너: {}", containerName);
                streamDockerLogsWithJSch(emitter, containerName, request.getLineLimit());
            } catch (Exception e) {
                log.error("로그 스트리밍 중 오류 발생: {}", containerName, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("log")
                            .data("로그 스트리밍 오류: " + e.getMessage() + "\n"));
                } catch (IOException ex) {
                    log.error("오류 메시지 전송 실패", ex);
                }
                emitter.completeWithError(e);
            }
        });
        
        streamingThread.setDaemon(true);
        streamingThread.start();
        runningThreads.put(containerName, streamingThread);
        
        log.info("Docker 로그 스트리밍 요청 처리 완료 - 컨테이너: {}", containerName);
        return emitter;
    }
    
    /**
     * Docker 로그 스트리밍 중지
     * 
     * @param request 로그 요청 정보
     */
    public void stopLogStreaming(DockerLogRequest request) {
        String containerName = Optional.ofNullable(request.getContainerName()).orElse("sejong-malsami-back");
        log.info("Docker 로그 스트리밍 중지 요청 - 컨테이너: {}", containerName);
        
        // 실행 중인 SSH 채널이 있다면 종료
        ChannelExec channel = sshChannels.remove(containerName);
        if (channel != null) {
            log.debug("SSH 채널 연결 종료 - 컨테이너: {}", containerName);
            channel.disconnect();
        }
        
        // 실행 중인 SSH 세션이 있다면 종료
        Session session = sshSessions.remove(containerName);
        if (session != null) {
            log.debug("SSH 세션 연결 종료 - 컨테이너: {}", containerName);
            session.disconnect();
        }
        
        // 실행 중인 스레드가 있다면 인터럽트
        Thread thread = runningThreads.remove(containerName);
        if (thread != null && thread.isAlive()) {
            log.debug("로그 스트리밍 스레드 인터럽트 - 컨테이너: {}", containerName);
            thread.interrupt();
        }
        
        // SSE 연결 종료
        SseEmitter emitter = activeEmitters.remove(containerName);
        if (emitter != null) {
            try {
                log.debug("SSE 종료 메시지 전송 - 컨테이너: {}", containerName);
                emitter.send(SseEmitter.event()
                        .name("log")
                        .data("=== 로그 스트리밍 종료 ===\n"));
                emitter.complete();
            } catch (Exception e) {
                log.warn("SSE 종료 메시지 전송 실패: {}", containerName, e);
            }
            log.info("컨테이너 {} SSE 연결 종료 완료", containerName);
        }
    }
    
    /**
     * JSch를 직접 사용하여 SSH 연결을 통해 Docker 로그를 실시간으로 스트리밍
     * 
     * @param emitter SSE Emitter
     * @param containerName 컨테이너 이름
     * @param lineLimit 라인 제한 (null이면 기본값 100)
     */
    private void streamDockerLogsWithJSch(SseEmitter emitter, String containerName, Integer lineLimit) {
        Session session = null;
        ChannelExec channel = null;
        
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
            log.debug("SSH 세션 연결 시도 - 컨테이너: {}", containerName);
            session.connect(10000); // 10초 타임아웃
            log.debug("SSH 세션 연결 성공 - 컨테이너: {}", containerName);
            
            sendLogEvent(emitter, "SSH 연결 성공\n");
            
            // 초기 로그 (최근 N줄) 가져오기
            int tailLines = lineLimit != null && lineLimit > 0 ? lineLimit : 100;

            sendLogEvent(emitter, "최근 " + tailLines + "줄 로그 가져오는 중...\n");

            // executeCommandWithSudoStdin 으로 초기 로그 획득 (한 번에 텍스트 반환)
            String initialLog = sshCommandExecutor.executeCommandWithSudoStdin(
                    "docker logs --tail=" + tailLines + " " + containerName);

            if (initialLog != null && !initialLog.isEmpty()) {
                for (String line : initialLog.split("\n")) {
                    sendLogEvent(emitter, line + "\n");
                }
            }
            int initialLogCount = initialLog != null ? initialLog.split("\n").length : 0;
            log.debug("초기 로그 {} 줄 전송 완료", initialLogCount);
            
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
            out.write((password + "\n").getBytes());
            out.flush();
            log.debug("실시간 로그 채널 연결 성공");
            
            // 현재 채널과 세션을 맵에 저장 (나중에 종료하기 위해)
            sshChannels.put(containerName, channel);
            sshSessions.put(containerName, session);
            
            // 로그 실시간 읽기
            int streamLogCount = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while (!Thread.currentThread().isInterrupted() && 
                       (line = reader.readLine()) != null &&
                       activeEmitters.containsKey(containerName)) {
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
                sendLogEvent(emitter, "SSH 연결 오류: " + e.getMessage() + "\n");
            } catch (IOException ex) {
                log.error("오류 메시지 전송 실패", ex);
            }
            throw new RuntimeException("SSH 연결 오류: " + e.getMessage(), e);
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
                    .data(message));
        } catch (IllegalStateException closed) {
            // Emitter 가 이미 닫혔을 때는 상위 호출부에서 처리하도록 예외 전달
            throw closed;
        } catch (Exception e) {
            log.warn("로그 이벤트 전송 실패: {}", e.getMessage());
            throw e;
        }
    }
} 