package me.suhsaechan.suhprojectutility.controller;

import lombok.RequiredArgsConstructor;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.suhprojectutility.object.request.DockerRequest;
import me.suhsaechan.suhprojectutility.object.script.DockerScriptResponse;
import me.suhsaechan.suhprojectutility.service.DockerService;
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
  private final DockerService dockerService;

  @PostMapping(value = "/get/container-info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<DockerScriptResponse> getModuleVersions(
      @ModelAttribute DockerRequest request){
    return ResponseEntity.ok(dockerService.getContainerInfo(request));
  }
}