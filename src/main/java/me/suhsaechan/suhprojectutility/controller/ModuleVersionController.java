package me.suhsaechan.suhprojectutility.controller;

import lombok.RequiredArgsConstructor;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.suhprojectutility.object.request.ModuleVersionRequest;
import me.suhsaechan.suhprojectutility.object.response.ModuleVersionResponse;
import me.suhsaechan.suhprojectutility.service.ModuleVersionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/module")
@RequiredArgsConstructor
public class ModuleVersionController {
  private final ModuleVersionService moduleVersionService;

  @PostMapping(value = "/get/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<ModuleVersionResponse> getModuleVersions(
      @ModelAttribute ModuleVersionRequest request){
    return ResponseEntity.ok(moduleVersionService.getModuleVersions(request));
  }
}