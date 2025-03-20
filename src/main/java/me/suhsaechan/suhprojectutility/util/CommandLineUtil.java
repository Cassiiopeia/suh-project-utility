package me.suhsaechan.suhprojectutility.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.object.constant.SystemType;
import me.suhsaechan.suhprojectutility.util.log.LogMonitoringInvocation;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Linux 환경에서만 명령어 실행을 허용하며,
 * 그 외 OS는 실행하지 않도록 하는 유틸 클래스
 */
@Slf4j
@Component
public class CommandLineUtil {

  /**
   * 기본 타임아웃(30초)
   */
  private static final long DEFAULT_TIMEOUT_MS = 30_000L;

  /**
   * 1) 단순 실행, 결과 문자열 반환
   *    - Linux 환경이 아닐 경우 빈 문자열("") 반환
   *    - 성공/실패 구분 없이 stdout+stderr 통합하여 반환
   */
  public String executeCommand(String cmd) {
    if (!isLinux()) {
      log.warn("Not a Linux system. Skipping command: {}", cmd);
      return "";
    }
    if (!StringUtils.hasText(cmd)) {
      log.warn("Command is empty or null.");
      return "";
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      CommandLine commandLine = CommandLine.parse(cmd);

      DefaultExecutor executor = new DefaultExecutor();
      // 표준 출력, 표준 에러를 모두 outputStream에 모아줌
      executor.setStreamHandler(new PumpStreamHandler(outputStream));

      int exitCode = executor.execute(commandLine);
      log.debug("Command executed (executeCommand): {}, exitCode: {}", cmd, exitCode);

      return outputStream.toString().trim();
    } catch (IOException e) {
      log.error("Failed to executeCommand: {}", cmd, e);
      return "";
    }
  }

  /**
   * 2) 결과를 무시하고 실행
   *    - Linux 환경이 아닐 경우 동작 스킵
   *    - stdout, stderr 무시
   */
  public void executeCommandIgnoreResult(String cmd) {
    if (!isLinux()) {
      log.warn("Not a Linux system. Skipping command: {}", cmd);
      return;
    }
    if (!StringUtils.hasText(cmd)) {
      log.warn("Command is empty or null.");
      return;
    }

    try {
      CommandLine commandLine = CommandLine.parse(cmd);
      DefaultExecutor executor = new DefaultExecutor();

      // 결과 무시하므로 별도 스트림 핸들러를 null로
      executor.setStreamHandler(new PumpStreamHandler(null, null));
      int exitCode = executor.execute(commandLine);
      log.debug("Command executed (executeCommandIgnoreResult): {}, exitCode: {}", cmd, exitCode);

    } catch (IOException e) {
      log.error("Failed to executeCommandIgnoreResult: {}", cmd, e);
    }
  }

  /**
   * 3) ExitCode(정수) 반환
   *    - Linux가 아닐 경우 -1 반환
   */
  public int executeCommandReturnCode(String cmd) {
    if (!isLinux()) {
      log.warn("Not a Linux system. Skipping command: {}", cmd);
      return -1;
    }
    if (!StringUtils.hasText(cmd)) {
      log.warn("Command is empty or null.");
      return -1;
    }

    try {
      CommandLine commandLine = CommandLine.parse(cmd);
      DefaultExecutor executor = new DefaultExecutor();

      int exitCode = executor.execute(commandLine);
      log.debug("Command executed (executeCommandReturnCode): {}, exitCode: {}", cmd, exitCode);

      return exitCode;
    } catch (IOException e) {
      log.error("Failed to executeCommandReturnCode: {}", cmd, e);
      return -1;
    }
  }

  /**
   * 4) Boolean 반환 (exitCode == 0 일 때 true, 아니면 false)
   *    - Linux 환경이 아닐 경우 false
   */
  public boolean executeCommandAsBoolean(String cmd) {
    if (!isLinux()) {
      log.warn("Not a Linux system. Skipping command: {}", cmd);
      return false;
    }
    if (!StringUtils.hasText(cmd)) {
      log.warn("Command is empty or null.");
      return false;
    }

    try {
      CommandLine commandLine = CommandLine.parse(cmd);
      DefaultExecutor executor = new DefaultExecutor();
      int exitCode = executor.execute(commandLine);
      boolean success = (exitCode == 0);

      log.debug("Command executed (executeCommandAsBoolean): {}, exitCode: {}, success: {}",
          cmd, exitCode, success);

      return success;
    } catch (IOException e) {
      log.error("Failed to executeCommandAsBoolean: {}", cmd, e);
      return false;
    }
  }

  /**
   * 5) 타임아웃 적용하여 결과 문자열 반환
   *    - DEFAULT_TIMEOUT_MS (30초) 사용
   *    - Linux가 아닐 경우 ""
   */
  public String executeCommandWithDefaultTimeout(String cmd) {
    return executeCommandWithTimeout(cmd, DEFAULT_TIMEOUT_MS);
  }

  /**
   * 6) 사용자 지정 타임아웃으로 결과 문자열 반환
   *    - Linux가 아닐 경우 ""
   *    - 타임아웃 초과 시 예외(ExecuteException) 발생 → catch 후 빈 문자열 반환
   */
  @LogMonitoringInvocation
  public String executeCommandWithTimeout(String cmd, long timeoutMillis) {
    if (!isLinux()) {
      log.warn("Not a Linux system. Skipping command: {}", cmd);
      return "";
    }
    if (!StringUtils.hasText(cmd)) {
      log.warn("Command is empty or null.");
      return "";
    }

    if (timeoutMillis <= 0) {
      timeoutMillis = DEFAULT_TIMEOUT_MS;
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      CommandLine commandLine = CommandLine.parse(cmd);
      DefaultExecutor executor = new DefaultExecutor();

      // stdout+stderr 통합
      executor.setStreamHandler(new PumpStreamHandler(outputStream));

      // Watchdog(타임아웃) 설정
      ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutMillis);
      executor.setWatchdog(watchdog);

      int exitCode = executor.execute(commandLine);
      log.debug("Command executed (executeCommandWithTimeout): {}, exitCode: {}", cmd, exitCode);

      return outputStream.toString().trim();
    } catch (ExecuteException e) {
      // 타임아웃 or 다른 실행 에러
      log.error("Command timeout or execute failure: {}", cmd, e);
      return "";
    } catch (IOException e) {
      log.error("Failed to executeCommandWithTimeout: {}", cmd, e);
      return "";
    }
  }

  /**
   * 7) sudo 권한으로 실행 (결과 문자열 반환)
   *    - Linux가 아닐 경우 ""
   *    - sudo 명령어가 실제 Linux 환경에서 동작해야 하므로, OS 체크 필수
   */
  public String executeCommandWithSudo(String cmd) {
    if (!isLinux()) {
      log.warn("Not a Linux system. Skipping command: {}", cmd);
      return "";
    }
    if (!StringUtils.hasText(cmd)) {
      log.warn("Command is empty or null.");
      return "";
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      // sudo를 사용하되, 뒤에 붙일 인자( /bin/sh -c “cmd” ) 형태로 해도 됨
      CommandLine commandLine = new CommandLine("sudo");
      commandLine.addArgument("bash");  // bash or /bin/sh
      commandLine.addArgument("-c");
      commandLine.addArgument(cmd, false);  // false → 스페이스 등 인자 그대로 파싱

      DefaultExecutor executor = new DefaultExecutor();
      executor.setStreamHandler(new PumpStreamHandler(outputStream));
      int exitCode = executor.execute(commandLine);

      log.debug("Command executed (executeCommandWithSudo): sudo bash -c '{}', exitCode: {}",
          cmd, exitCode);

      return outputStream.toString().trim();
    } catch (IOException e) {
      log.error("Failed to executeCommandWithSudo: {}", cmd, e);
      return "";
    }
  }

  /**
   * 8) sudo 권한 + 타임아웃 적용
   *    - Linux가 아닐 경우 ""
   */
  public String executeCommandWithSudoAndTimeout(String cmd, long timeoutMillis) {
    if (!isLinux()) {
      log.warn("Not a Linux system. Skipping command: {}", cmd);
      return "";
    }
    if (!StringUtils.hasText(cmd)) {
      log.warn("Command is empty or null.");
      return "";
    }
    if (timeoutMillis <= 0) {
      timeoutMillis = DEFAULT_TIMEOUT_MS;
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      CommandLine commandLine = new CommandLine("sudo");
      commandLine.addArgument("bash");
      commandLine.addArgument("-c");
      commandLine.addArgument(cmd, false);

      DefaultExecutor executor = new DefaultExecutor();
      executor.setStreamHandler(new PumpStreamHandler(outputStream));

      // 타임아웃(ExecuteWatchdog) 설정
      ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutMillis);
      executor.setWatchdog(watchdog);

      int exitCode = executor.execute(commandLine);

      log.debug("Command executed (executeCommandWithSudoAndTimeout): sudo bash -c '{}', exitCode: {}",
          cmd, exitCode);

      return outputStream.toString().trim();
    } catch (ExecuteException e) {
      log.error("Command timeout or execute failure (sudo): {}", cmd, e);
      return "";
    } catch (IOException e) {
      log.error("Failed to executeCommandWithSudoAndTimeout: {}", cmd, e);
      return "";
    }
  }

  /**
   * OS가 LINUX 인지 판별 (FileUtil.getCurrentSystem() 이용)
   */
  private boolean isLinux() {
    return (FileUtil.getCurrentSystem() == SystemType.LINUX);
  }
}
