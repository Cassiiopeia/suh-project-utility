package me.suhsaechan.web.controller.api;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.dto.ServerOptionDto;
import me.suhsaechan.common.dto.ServerOptionRequest;
import me.suhsaechan.common.entity.ServerOption;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 설정 API 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/server-option")
public class ServerOptionController {

  private final ServerOptionService serverOptionService;

  /**
   * 특정 설정 조회
   */
  @PostMapping(value = "/get", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<ServerOptionDto> getOption(@ModelAttribute ServerOptionRequest request) {
    ServerOption option = serverOptionService.getOption(request.getOptionKey());
    
    if (option == null) {
      return ResponseEntity.ok(ServerOptionDto.fromDefault(request.getOptionKey()));
    }

    return ResponseEntity.ok(ServerOptionDto.from(option));
  }

  /**
   * 설정 값 업데이트
   */
  @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<ServerOptionDto> updateOption(@ModelAttribute ServerOptionRequest request) {
    ServerOption option = serverOptionService.setOptionValue(
        request.getOptionKey(), 
        request.getOptionValue()
    );
    return ResponseEntity.ok(ServerOptionDto.from(option));
  }

  /**
   * 모든 설정 목록 조회
   */
  @PostMapping(value = "/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<List<ServerOptionDto>> listOptions() {
    List<ServerOption> options = serverOptionService.getAllOptions();
    List<ServerOptionDto> dtos = options.stream()
        .map(ServerOptionDto::from)
        .collect(Collectors.toList());
    return ResponseEntity.ok(dtos);
  }
}

