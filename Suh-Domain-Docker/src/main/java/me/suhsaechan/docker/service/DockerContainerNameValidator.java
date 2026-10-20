package me.suhsaechan.docker.service;

import java.util.Optional;
import java.util.regex.Pattern;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;

/**
 * 컨테이너 이름 검증: SSH 셸 명령에 넣기 전 허용 문자만 통과 (명령 인젝션 차단)
 */
public final class DockerContainerNameValidator {

  // Docker 컨테이너 이름 규칙: 영숫자 시작, 이후 영숫자·_·.·- (최대 128자)
  private static final Pattern CONTAINER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$");

  private DockerContainerNameValidator() {
  }

  public static String requireValidContainerName(String containerName) {
    return Optional.ofNullable(containerName)
        .filter(name -> CONTAINER_NAME_PATTERN.matcher(name).matches())
        .orElseThrow(() -> new CustomException(ErrorCode.DOCKER_INVALID_CONTAINER_NAME));
  }
}
