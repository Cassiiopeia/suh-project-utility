package me.suhsaechan.suhprojectutility.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.config.ServerInfo;
import me.suhsaechan.suhprojectutility.util.security.AESUtil;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalModelController {

  private final ServerInfo serverInfo;
  private final AESUtil aesUtil;
  private static final String CLIENT_HASH_KEY = "clientHash";

  @ModelAttribute
  public void addGlobalAttributes(Model model, HttpServletRequest request) {
    // 암호화 키값
    model.addAttribute("encryptionKey", serverInfo.getSecretKey());
    model.addAttribute("encryptionIV", serverInfo.getIv());

    // 클라이언트 해시 처리
    String clientHash = getOrCreateClientHash(request);
    model.addAttribute("clientHash", clientHash);
  }

  private String getOrCreateClientHash(HttpServletRequest request) {
    // 1. 세션에서 clientHash 확인 (세션이 있는 경우)
    HttpSession session = request.getSession(false);
    if (session != null) {
      String sessionHash = (String) session.getAttribute(CLIENT_HASH_KEY);
      if (sessionHash != null && !sessionHash.isEmpty()) {
        log.debug("세션에서 clientHash 조회: {}", sessionHash);
        return sessionHash;
      }
    }

    // 2. 세션이 없거나 clientHash가 없는 경우 생성
    String clientIp = request.getRemoteAddr();
    log.debug("IP 기반 clientHash 생성: {}", clientIp);
    String newHash = aesUtil.encrypt(clientIp);

    // 세션에 저장 (있을 경우)
    if (session != null) {
      session.setAttribute(CLIENT_HASH_KEY, newHash);
    }

    return newHash;
  }
}
