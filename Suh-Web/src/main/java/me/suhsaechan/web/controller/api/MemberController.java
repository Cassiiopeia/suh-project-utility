package me.suhsaechan.web.controller.api;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhlogger.annotation.LogMonitor;

/**
 * 회원 관련 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {
    
    /**
     * 클라이언트 해시값 반환
     * 세션에서 clientHash를 가져와 반환
     */
    @PostMapping(value = "/client-hash", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogMonitor
    public ResponseEntity<Map<String, String>> getClientHash(@ModelAttribute Map<String, Object> request, 
                                                           HttpServletRequest httpRequest) {
        log.info("클라이언트 해시값 조회 요청");
        
        HttpSession session = httpRequest.getSession();
        String clientHash = (String) session.getAttribute("clientHash");
        
        Map<String, String> response = new HashMap<>();
        response.put("clientHash", clientHash);
        
        return ResponseEntity.ok(response);
    }
}