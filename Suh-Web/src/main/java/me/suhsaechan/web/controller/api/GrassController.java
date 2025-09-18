package me.suhsaechan.web.controller.api;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.grassplanter.dto.GrassRequest;
import me.suhsaechan.grassplanter.dto.GrassResponse;
import me.suhsaechan.grassplanter.service.GrassService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("")
public class GrassController {

    private final GrassService grassService;

    // API 엔드포인트들
    @PostMapping(value = "/api/grass/profile/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<GrassResponse> createProfile(@ModelAttribute GrassRequest request) {
        log.info("프로필 생성 요청 - 사용자: {}", request.getGithubUsername());
        GrassResponse response = grassService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/api/grass/profile/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<GrassResponse> updateProfile(@ModelAttribute GrassRequest request) {
        log.info("프로필 업데이트 요청 - ID: {}", request.getProfileId());
        GrassResponse response = grassService.updateProfile(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/grass/profile/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<GrassResponse> deleteProfile(@RequestParam UUID profileId) {
        log.info("프로필 삭제 요청 - ID: {}", profileId);
        GrassResponse response = grassService.deleteProfile(profileId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/grass/profile/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<GrassResponse> getProfiles(@ModelAttribute GrassRequest request) {
        log.info("프로필 목록 조회");
        GrassResponse response = grassService.getProfiles(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/grass/commit/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<GrassResponse> executeCommit(@ModelAttribute GrassRequest request) {
        log.info("수동 커밋 실행 요청 - 프로필 ID: {}", request.getProfileId());
        GrassResponse response = grassService.executeCommit(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/grass/commit/logs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<GrassResponse> getCommitLogs(@ModelAttribute GrassRequest request) {
        log.info("커밋 로그 조회 - 프로필 ID: {}", request.getProfileId());
        GrassResponse response = grassService.getCommitLogs(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/grass/contribution/{username}")
    @ResponseBody
    public ResponseEntity<Integer> checkContribution(
            @PathVariable String username,
            @RequestParam(required = false) LocalDate date) {
        log.info("기여도 확인 요청 - 사용자: {}, 날짜: {}", username, date);
        
        try {
            LocalDate checkDate = date != null ? date : LocalDate.now();
            int level = grassService.checkContributionLevel(username, checkDate);
            return ResponseEntity.ok(level);
        } catch (IOException e) {
            log.error("기여도 확인 실패: {}", e.getMessage(), e);
            throw new RuntimeException("기여도 확인 실패", e);
        }
    }
}