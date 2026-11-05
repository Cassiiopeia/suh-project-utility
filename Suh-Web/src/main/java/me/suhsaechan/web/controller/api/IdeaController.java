package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.idea.dto.IdeaRequest;
import me.suhsaechan.idea.dto.IdeaResponse;
import me.suhsaechan.idea.service.IdeaService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/idea")
public class IdeaController {

  private final IdeaService ideaService;

  @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<IdeaResponse> createItem(@ModelAttribute IdeaRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ideaService.createItem(request));
  }

  @PostMapping(value = "/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<IdeaResponse> getItems() {
    return ResponseEntity.ok(ideaService.getItems());
  }

  @PostMapping(value = "/recent", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<IdeaResponse> getRecentItems() {
    return ResponseEntity.ok(ideaService.getRecentItems());
  }

  @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<IdeaResponse> updateItem(@ModelAttribute IdeaRequest request) {
    return ResponseEntity.ok(ideaService.updateItem(request));
  }

  @PostMapping(value = "/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> deleteItem(@ModelAttribute IdeaRequest request) {
    ideaService.deleteItem(request.getIdeaItemId());
    return ResponseEntity.noContent().build();
  }
}
