package me.suhsaechan.docker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import me.suhsaechan.common.exception.CustomException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// 컨테이너 이름 허용 목록 검증 (SSH 셸 명령 인젝션 차단)
class DockerContainerNameValidatorTest {

  @ParameterizedTest
  @ValueSource(strings = {"sejong-malsami-back", "suh_project.utility", "a", "Redis7"})
  void 정상_컨테이너_이름은_통과(String containerName) {
    assertThat(DockerContainerNameValidator.requireValidContainerName(containerName)).isEqualTo(containerName);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {
      "app; rm -rf /", "app && id", "$(id)", "`id`", "app|cat /etc/passwd",
      "app\"", "app'", "app name", "-app", "app\nid"})
  void 셸_메타문자_포함_이름은_거부(String containerName) {
    assertThatThrownBy(() -> DockerContainerNameValidator.requireValidContainerName(containerName))
        .isInstanceOf(CustomException.class);
  }
}
