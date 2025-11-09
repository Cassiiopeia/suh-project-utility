package me.suhsaechan.docker.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.util.SshCommandExecutor;
import me.suhsaechan.common.properties.SshConnectionProperties;
import me.suhsaechan.docker.dto.DockerRequest;
import me.suhsaechan.docker.dto.DockerLogResponse;
import me.suhsaechan.docker.dto.ContainerInfoDto;
import org.springframework.stereotype.Service;

/**
 * Docker 로그 서비스 (폴링 방식)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DockerLogService {

    private final SshCommandExecutor sshCommandExecutor;
    private final SshConnectionProperties sshProps;

    /**
     * Docker 컨테이너 로그 조회 (폴링용)
     * 
     * @param request 로그 요청 정보
     * @return 로그 응답
     */
    public DockerLogResponse getContainerLogs(DockerRequest request) {
        String containerName = Optional.ofNullable(request.getContainerName()).orElse("sejong-malsami-back");
        Integer lineLimit = Optional.ofNullable(request.getLineLimit()).orElse(100);
        
        log.info("Docker 로그 조회 요청 - 컨테이너: {}, 라인 제한: {}", containerName, lineLimit);
        
        Session session = null;
        ChannelExec channel = null;
        
        try {
            // SSH 연결 정보 가져오기
            String host = sshProps.getHost();
            String username = sshProps.getUsername();
            String password = sshProps.getPassword();
            int port = sshProps.getPort();
            
            log.debug("SSH 연결 정보 - 호스트: {}, 포트: {}, 사용자: {}", host, port, username);
            
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
            session.connect(10000);
            log.info("SSH 세션 연결 성공 - 컨테이너: {}", containerName);
            
            // 로그 조회 명령 실행
            channel = (ChannelExec) session.openChannel("exec");
            String command = String.format(
                "sudo -S -p '' bash -c 'export PATH=$PATH:/usr/local/bin && docker logs --tail=%d %s'",
                lineLimit, containerName
            );
            channel.setCommand(command);
            channel.setPty(true);
            
            InputStream in = channel.getInputStream();
            OutputStream out = channel.getOutputStream();
            channel.connect(10000);
            
            // sudo 비밀번호 전달
            out.write((password + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            
            // 로그 읽기
            StringBuilder logBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int totalLines = 0;
            
            while (!channel.isClosed()) {
                int bytesRead = in.read(buffer);
                if (bytesRead < 0) {
                    break;
                }
                if (bytesRead > 0) {
                    String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    logBuilder.append(chunk);
                    // 줄 수 계산
                    totalLines = logBuilder.toString().split("\n").length;
                }
            }
            
            String logs = logBuilder.toString();
            log.info("로그 조회 완료 - 컨테이너: {}, 총 {} 줄", containerName, totalLines);
            
            return DockerLogResponse.builder()
                    .logs(logs)
                    .totalLines(totalLines)
                    .build();
                    
        } catch (JSchException e) {
            log.error("SSH 연결 실패 - 호스트: {}:{}, 에러: {}", sshProps.getHost(), sshProps.getPort(), e.getMessage());
            
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
            
            return DockerLogResponse.builder()
                    .error(errorMessage)
                    .build();
                    
        } catch (IOException e) {
            log.error("로그 읽기 오류: {}", e.getMessage(), e);
            return DockerLogResponse.builder()
                    .error("로그 읽기 오류: " + e.getMessage())
                    .build();
        } finally {
            // 자원 정리
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
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
