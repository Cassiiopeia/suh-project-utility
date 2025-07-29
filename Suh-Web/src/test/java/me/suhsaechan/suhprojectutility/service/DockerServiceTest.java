package me.suhsaechan.suhprojectutility.service;

import static me.suhsaechan.suhlogger.util.SuhLogger.lineLog;
import static me.suhsaechan.suhlogger.util.SuhLogger.superLog;
import static me.suhsaechan.suhlogger.util.SuhLogger.timeLog;

import me.suhsaechan.docker.dto.DockerScriptResponse;
import me.suhsaechan.docker.service.DockerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class DockerServiceTest {
   @Autowired
   DockerService dockerService;

  @Test
  public void mainTest() {
    lineLog("Docker 테스트 스킵 - 멀티모듈 구조 완성 후 재활성화 예정");
    // TODO: 멀티모듈 구조 완성 후 테스트 재활성화
  }

  // void getContainerInfo_테스트() {
  //   DockerRequest request = new DockerRequest();
  //   request.setContainerName("suh-project-utility");
  //   DockerScriptResponse dockerScriptResponse = dockerService.getContainerInfo(request);
  //   superLog(dockerScriptResponse);
  //   lineLog(null);
  // }

}