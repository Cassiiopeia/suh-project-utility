package me.suhsaechan.suhprojectutility.util.security;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final AESUtil aesUtil;

  public CustomAuthenticationSuccessHandler(AESUtil aesUtil) {
    this.aesUtil = aesUtil;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    // 클라이언트 IP 주소 추출 후 바로 암호화
    String clientIp = request.getRemoteAddr();
    String clientHash = aesUtil.encrypt(clientIp);

    // 세션에 암호화된 해시값 저장
    log.debug("Login Client Hash: {}", clientHash);
    HttpSession session = request.getSession();
    session.setAttribute("clientHash", clientHash);

    // 성공 ->  /dashboard 리다이렉트
    response.sendRedirect(request.getContextPath() + "/dashboard");
  }
}