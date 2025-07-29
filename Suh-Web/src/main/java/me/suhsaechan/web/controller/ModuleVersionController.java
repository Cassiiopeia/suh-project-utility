package me.suhsaechan.web.controller;

import lombok.RequiredArgsConstructor;
import me.suhsaechan.module.service.ModuleVersionService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
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
  public ResponseEntity<me.suhsaechan.module.object.response.ModuleVersionResponse> getModuleVersions(
      @ModelAttribute me.suhsaechan.module.object.request.ModuleVersionRequest request){
    return ResponseEntity.ok(moduleVersionService.getModuleVersions(request));
  }
}