package me.suhsaechan.suhprojectutility.util.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.config.ServerInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final AESUtil aesUtil;
  private final ServerInfo serverInfo;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    // 클라이언트 IP 주소 추출 후 암호화
    String clientIp = request.getRemoteAddr();
    String clientHash = aesUtil.encrypt(clientIp);

    // 세션에 저장
    HttpSession session = request.getSession(true);
    session.setAttribute("clientHash", clientHash);
    log.debug("로그인 성공: clientHash 세션 저장 - {}", clientHash);

    // 성공 -> /dashboard 리다이렉트
    response.sendRedirect(request.getContextPath() + "/dashboard");
  }
}