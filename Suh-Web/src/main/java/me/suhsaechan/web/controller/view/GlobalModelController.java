package me.suhsaechan.web.controller.view;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.web.config.ServerInfo;
import me.suhsaechan.common.util.AESUtil;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalModelController {

  private final ServerInfo serverInfo;
  private final AESUtil aesUtil;
  private final MessageSource messageSource;
  private static final String CLIENT_HASH_KEY = "clientHash";

  // 검색 엔진에 색인을 허용하는 공개 페이지 (경로 -> 설명 메시지 키)
  private static final Map<String, String> INDEXABLE_PAGES = Map.of(
      "/", "dashboard",
      "/dashboard", "dashboard",
      "/issue-helper", "issue-helper",
      "/translator", "translator",
      "/sejong-auth", "sejong-auth",
      "/suh-random", "suh-random",
      "/suh-devops-template", "devops-template");

  @Value("${site.base-url:https://lab.suhsaechan.kr}")
  private String siteBaseUrl;

  @ModelAttribute
  public void addGlobalAttributes(Model model, HttpServletRequest request) {
    // 암호화 키값
    model.addAttribute("encryptionKey", serverInfo.getSecretKey());
    model.addAttribute("encryptionIV", serverInfo.getIv());

    // CSRF 토큰 생성을 위해 세션 생성 (없으면)
    HttpSession session = request.getSession(true);
    
    // 세션에서 clientHash 가져와서 모델에 추가
    String clientHash = (String) session.getAttribute(ServerInfo.CLIENT_HASH_SESSION_KEY);
    model.addAttribute("clientHash", clientHash != null ? clientHash : "");

    addSeoAttributes(model, request);
  }

  private void addSeoAttributes(Model model, HttpServletRequest request) {
    String path = request.getRequestURI();
    boolean isError = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) != null;
    String descriptionKey = INDEXABLE_PAGES.get(path);
    boolean indexable = descriptionKey != null && !isError;
    Locale locale = request.getLocale();

    String canonicalPath = "/".equals(path) ? "/dashboard" : path;
    String description = messageSource.getMessage(
        "seo.desc." + (descriptionKey != null ? descriptionKey : "default"),
        null,
        messageSource.getMessage("seo.desc.default", null, "", locale),
        locale);

    model.addAttribute("siteBaseUrl", siteBaseUrl);
    model.addAttribute("seoCanonicalUrl", siteBaseUrl + canonicalPath);
    model.addAttribute("seoImageUrl", siteBaseUrl + "/images/og-image.png");
    model.addAttribute("seoDescription", description);
    model.addAttribute("seoRobots", indexable ? "index,follow" : "noindex,nofollow");
  }
}
