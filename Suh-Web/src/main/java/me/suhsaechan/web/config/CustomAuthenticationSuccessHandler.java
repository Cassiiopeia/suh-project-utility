package me.suhsaechan.web.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.service.UserAuthority;
import me.suhsaechan.common.util.AESUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final AESUtil aesUtil;
  private final UserAuthority userAuthority;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    // clientHash는 서버에서만 생성 (프록시 헤더 기준 IP > 고정 IV AES, 폼 값 신뢰 안 함)
    // 고정 IV: 재로그인해도 같은 IP면 같은 값 (댓글 작성자 확인·방문자 집계 기준)
    String clientHash = aesUtil.encryptWithFixedIv(userAuthority.extractIpFromRequest(request));

    // 세션에 저장
    HttpSession session = request.getSession(true);
    session.setAttribute(ServerInfo.CLIENT_HASH_SESSION_KEY, clientHash);
    
    // 슈퍼관리자 권한 확인
    String username = authentication.getName();
    String password = request.getParameter("password");
    userAuthority.checkAndSetSuperAdmin(username, password, session);
    
    log.debug("로그인 성공: clientHash 세션 저장 - {}", clientHash);

    // 성공 후 리다이렉트
    response.sendRedirect(request.getContextPath() + "/dashboard");
  }
}