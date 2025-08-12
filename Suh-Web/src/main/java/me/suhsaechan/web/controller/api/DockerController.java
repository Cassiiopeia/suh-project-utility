package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import me.suhsaechan.docker.dto.DockerScriptResponse;
import me.suhsaechan.docker.service.DockerService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/docker")
@RequiredArgsConstructor
public class DockerController {
  private DockerService dockerService;

  @PostMapping(value = "/get/container-info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<DockerScriptResponse> getModuleVersions(
      @ModelAttribute me.suhsaechan.docker.dto.request.DockerRequest request){
    return ResponseEntity.ok(dockerService.getContainerInfo(request));
  }
}