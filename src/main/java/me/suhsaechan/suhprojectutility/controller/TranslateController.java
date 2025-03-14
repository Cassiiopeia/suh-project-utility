package me.suhsaechan.suhprojectutility.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.object.TranslationRequest;
import me.suhsaechan.suhprojectutility.object.TranslationResponse;
import me.suhsaechan.suhprojectutility.service.TranslateService;
import me.suhsaechan.suhprojectutility.util.log.LogMonitoringInvocation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/translate")
@RequiredArgsConstructor
public class TranslateController {

  private final TranslateService translatorService;

  @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitoringInvocation
  public ResponseEntity<TranslationResponse> translate(
      @ModelAttribute TranslationRequest request) {
    return ResponseEntity.ok(translatorService.translate(request));
  }
}