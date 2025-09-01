package me.suhsaechan.docker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.util.SshCommandExecutor;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.docker.dto.DockerRequest;
import me.suhsaechan.docker.dto.DockerScriptResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerService {

  private final SshCommandExecutor sshCommandExecutor;
  private final ObjectMapper objectMapper;

  // 컨테이너 정보 확인
  public DockerScriptResponse getContainerInfo(DockerRequest request) {
    String command
        = String.format("/volume1/projects/suh-project/bin/docker_manager.sh %s %s", "status", request.getContainerName());
    String output = sshCommandExecutor.executeCommandWithSudoStdin(command);

    // 스크립트 반환값 확인
    if (output.isEmpty()) {
      throw new CustomException(ErrorCode.EMPTY_SCRIPT_RESPONSE);
    }

    DockerScriptResponse response = null;
    try {
      response = objectMapper.readValue(output, DockerScriptResponse.class);
    } catch (JsonProcessingException e) {
      log.error("DockerService.getContainerInfo() 오류 발생 : {}", e.getMessage());
      throw new RuntimeException(e);
    }
    return response;
  }
}