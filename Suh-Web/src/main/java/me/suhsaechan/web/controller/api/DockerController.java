package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.docker.dto.DockerRequest;
import me.suhsaechan.docker.dto.DockerResponse;
import me.suhsaechan.docker.service.DockerService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/docker")
@RequiredArgsConstructor
public class DockerController {
  
  private final DockerService dockerService;

  @PostMapping(value = "/get/container-info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<DockerResponse> getContainerInfo(
      @ModelAttribute DockerRequest request) {
    return ResponseEntity.ok(dockerService.getContainerInfo(request));
  }
}