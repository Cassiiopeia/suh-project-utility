package me.suhsaechan.suhprojectutility.controller;

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
  public void addGlobalAttributes(Model model) {
    model.addAttribute("encryptionKey", secretKey);
    model.addAttribute("encryptionIV", iv);
  }
}
