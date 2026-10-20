package me.suhsaechan.common.util;

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
import java.util.Optional;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.properties.SshConnectionProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SshCommandExecutor {

  private final SshConnectionProperties sshProps;

  // SSH 연결 타임아웃 (10초)
  private static final int SSH_CONNECT_TIMEOUT_MILLISECONDS = 10_000;
  // 번들 known_hosts 위치 (classpath)
  private static final String BUNDLED_KNOWN_HOSTS_RESOURCE = "ssh/known_hosts";

  public SshCommandExecutor(SshConnectionProperties sshProps) {
    this.sshProps = sshProps;
  }

  /**
   * SSH 세션 연결 (단일 지점): 호스트키 검증 known_hosts 우선순위 ssh.known-hosts-path 파일 > classpath ssh/known_hosts > 검증 생략
   */
  public Session openSession() throws JSchException {
    JSch jsch = new JSch();
    Properties config = new Properties();
    Optional<String> knownHostsPath = Optional.ofNullable(sshProps.getKnownHostsPath()).filter(path -> !path.isBlank());
    if (knownHostsPath.isPresent()) {
      jsch.setKnownHosts(knownHostsPath.get());
      config.put("StrictHostKeyChecking", "yes");
    } else if (loadBundledKnownHosts(jsch)) {
      config.put("StrictHostKeyChecking", "yes");
    } else {
      log.warn("known_hosts 없음 - 호스트키 검증 생략");
      config.put("StrictHostKeyChecking", "no");
    }

    Session session = jsch.getSession(sshProps.getUsername(), sshProps.getHost(), sshProps.getPort());
    session.setPassword(sshProps.getPassword());
    session.setConfig(config);
    session.connect(SSH_CONNECT_TIMEOUT_MILLISECONDS);
    return session;
  }

  // classpath ssh/known_hosts (NAS 호스트 공개키, Suh-Common/src/main/resources) 로드 성공 여부
  private boolean loadBundledKnownHosts(JSch jsch) throws JSchException {
    try (InputStream knownHostsStream = getClass().getClassLoader().getResourceAsStream(BUNDLED_KNOWN_HOSTS_RESOURCE)) {
      if (knownHostsStream == null) {
        return false;
      }
      jsch.setKnownHosts(knownHostsStream);
      return true;
    } catch (IOException e) {
      log.warn("번들 known_hosts 읽기 실패: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 기본 SSH 명령어 실행
   * sudo 없이 바로 실행
   */
  public String executeCommand(String command) {
    Session session = null;
    ChannelExec channel = null;

    try {
      // 1) SSH 세션 연결
      session = openSession();

      // 4) 채널 열기
      channel = (ChannelExec) session.openChannel("exec");
      channel.setCommand(command);
      channel.setErrStream(System.err);

      // 5) 명령어 실행
      InputStream in = channel.getInputStream();
      channel.connect();

      // 6) 결과 읽기 (UTF-8 인코딩)
      StringBuilder outputBuffer = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(
              new InputStreamReader(in, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          outputBuffer.append(line).append("\n");
        }
      }

      // 7) 채널 종료 대기 (최대 60초 타임아웃)
      int waitCount = 0;
      while (!channel.isClosed() && waitCount < 600) { // 60초 = 600 * 100ms
        Thread.sleep(100);
        waitCount++;
      }

      if (!channel.isClosed()) {
        log.warn("SSH 채널이 60초 내에 닫히지 않아 강제 종료합니다. command: {}", command);
        channel.disconnect();
      }

      int exitStatus = channel.getExitStatus();
      log.debug("SSH command exitStatus: {}, command: {}", exitStatus, command);

      return outputBuffer.toString().trim();

    } catch (Exception e) {
      log.error("SSH command execution failed: {}", command, e);
      return "";
    } finally {
      // 8) 자원 정리
      if (channel != null && !channel.isClosed()) {
        channel.disconnect();
      }
      if (session != null && session.isConnected()) {
        session.disconnect();
      }
    }
  }

  /**
   * sudo + 표준입력(-S)으로 비번 전달 + PATH 설정
   *     - 'tty 없이' sudo 인증
   *     - docker 등 PATH 문제 해결을 위해 env 설정
   *
   * 예시:
   *   sudo -S -p '' bash -c "export PATH=$PATH:/usr/local/bin && <command>"  (비번은 채널 stdin 으로 전달)
   */
  public String executeCommandWithSudoStdin(String command) {
    Session session = null;
    ChannelExec channel = null;

    // SSH 접속 비번 == sudo 비번이라고 가정
    String sudoPassword = sshProps.getPassword();

    // docker 등 PATH 설정 후 실행. 비번은 명령 문자열에 넣지 않음 (원격 ps·로그 노출 차단)
    //  ex) docker ps -a --format \"{{.Names}}|{{.Status}}\"
    String wrappedCommand = String.format(
        "sudo -S -p '' bash -c \"export PATH=$PATH:/usr/local/bin && %s\"",
        command
    );

    try {
      session = openSession();

      channel = (ChannelExec) session.openChannel("exec");
      channel.setCommand(wrappedCommand);
      channel.setErrStream(System.err);

      InputStream in = channel.getInputStream();
      OutputStream sudoPasswordInput = channel.getOutputStream();
      channel.connect();

      // sudo 비번 stdin 전달 후 EOF
      sudoPasswordInput.write((sudoPassword + "\n").getBytes(StandardCharsets.UTF_8));
      sudoPasswordInput.flush();
      sudoPasswordInput.close();

      StringBuilder outputBuffer = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(
              new InputStreamReader(in, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          outputBuffer.append(line).append("\n");
        }
      }

      // 채널 종료 대기 (최대 60초 타임아웃)
      int waitCount = 0;
      while (!channel.isClosed() && waitCount < 600) { // 60초 = 600 * 100ms
        Thread.sleep(100);
        waitCount++;
      }

      if (!channel.isClosed()) {
        log.warn("SSH SUDO 채널이 60초 내에 닫히지 않아 강제 종료합니다. command: {}", command);
        channel.disconnect();
      }

      int exitStatus = channel.getExitStatus();
      log.debug("SSH command with SUDO exitStatus: {}, command: {}", exitStatus, wrappedCommand);

      return outputBuffer.toString().trim();

    } catch (Exception e) {
      log.error("SSH sudo command execution failed: {}", command, e);
      return "";
    } finally {
      if (channel != null && !channel.isClosed()) {
        channel.disconnect();
      }
      if (session != null && session.isConnected()) {
        session.disconnect();
      }
    }
  }
}
