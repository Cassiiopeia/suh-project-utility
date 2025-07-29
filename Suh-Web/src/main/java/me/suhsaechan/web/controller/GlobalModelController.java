package me.suhsaechan.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.config.ServerInfo;
import me.suhsaechan.common.util.security.AESUtil;
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

    // 세션에서 clientHash 가져와서 모델에 추가
    HttpSession session = request.getSession(false);
    if (session != null) {
      String clientHash = (String) session.getAttribute(ServerInfo.CLIENT_HASH_SESSION_KEY);
      model.addAttribute("clientHash", clientHash != null ? clientHash : "");
    }
  }
}
