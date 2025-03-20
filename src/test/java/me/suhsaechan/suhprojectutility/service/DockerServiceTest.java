package me.suhsaechan.suhprojectutility.service;

import static com.romrom.romback.global.util.LogUtil.lineLog;
import static com.romrom.romback.global.util.LogUtil.superLog;
import static com.romrom.romback.global.util.LogUtil.timeLog;

import me.suhsaechan.suhprojectutility.object.script.DockerScriptResponse;
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
    timeLog(this::getContainerInfo_테스트);
  }

  void getContainerInfo_테스트() {
    DockerScriptResponse dockerScriptResponse = dockerService.getContainerInfo("suh-project-utility");
    superLog(dockerScriptResponse);
    lineLog(null);

  }

}