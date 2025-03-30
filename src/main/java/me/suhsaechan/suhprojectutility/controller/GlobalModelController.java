package me.suhsaechan.suhprojectutility.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelController {

  @Value("${aes.secret-key}")
  private String secretKey;

  @Value("${aes.iv}")
  private String iv;

  @ModelAttribute
  public void addGlobalAttributes(Model model, HttpServletRequest request) {
    HttpSession session = request.getSession(false);

    // 기존 암호화 키 설정
    model.addAttribute("encryptionKey", secretKey);
    model.addAttribute("encryptionIV", iv);

    // 세션에서 암호화된 클라이언트 해시 가져와서 모델에 추가
    if (session != null && session.getAttribute("clientHash") != null) {
      model.addAttribute("clientHash", session.getAttribute("clientHash"));
    }
  }
}
