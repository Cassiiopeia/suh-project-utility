package me.suhsaechan.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.translate.service.TranslateService;
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
  @LogMonitor
  public ResponseEntity<me.suhsaechan.translate.object.response.TranslationResponse> translate(
      @ModelAttribute me.suhsaechan.translate.object.request.TranslationRequest request) {
    return ResponseEntity.ok(translatorService.translate(request));
  }
}