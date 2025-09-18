package me.suhsaechan.docker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.util.SshCommandExecutor;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.docker.dto.DockerRequest;
import me.suhsaechan.docker.dto.DockerResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerService {

  private final SshCommandExecutor sshCommandExecutor;
  private final ObjectMapper objectMapper;

  // 컨테이너 정보 확인
  public DockerResponse getContainerInfo(DockerRequest request) {
    String command
        = String.format("/volume1/projects/suh-project/bin/docker_manager.sh %s %s", "status", request.getContainerName());
    String output = sshCommandExecutor.executeCommandWithSudoStdin(command);

    // 스크립트 반환값 확인
    if (output.isEmpty()) {
      throw new CustomException(ErrorCode.EMPTY_SCRIPT_RESPONSE);
    }

    DockerResponse response = null;
    try {
      // 기존 DockerScriptResponse 형태의 JSON을 DockerResponse로 변환
      // 하위 호환성을 위해 임시로 Object로 읽고 변환
      @SuppressWarnings("unchecked")
      java.util.Map<String, Object> jsonMap = objectMapper.readValue(output, java.util.Map.class);
      @SuppressWarnings("unchecked")
      java.util.Map<String, Object> data = (java.util.Map<String, Object>) jsonMap.get("data");
      
      if (data != null) {
        response = DockerResponse.builder()
          .container((String) data.get("container"))
          .error((String) data.get("error"))
          .started((String) data.get("started"))
          .stopped((String) data.get("stopped"))
          .restarted((String) data.get("restarted"))
          .status((String) data.get("status"))
          .build();
      } else {
        response = DockerResponse.builder().build();
      }
    } catch (JsonProcessingException e) {
      log.error("DockerService.getContainerInfo() 오류 발생 : {}", e.getMessage());
      throw new RuntimeException(e);
    }
    return response;
  }
}