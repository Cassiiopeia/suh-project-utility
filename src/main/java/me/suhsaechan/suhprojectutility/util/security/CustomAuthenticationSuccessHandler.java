package me.suhsaechan.suhprojectutility.util.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.config.ServerInfo;
import me.suhsaechan.suhprojectutility.config.UserAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final AESUtil aesUtil;
  private final ServerInfo serverInfo;
  private final UserAuthority userAuthority;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    // 폼에서 전송된 clientHash 값 확인
    String clientHashFromForm = request.getParameter("clientHash");
    String clientHash;

    // 폼에서 전송된 암호화된 값 있으면 사용
    if (clientHashFromForm != null && !clientHashFromForm.isEmpty()) {
      clientHash = clientHashFromForm;
    } else {
      // 없으면 : IP 주소 추출 후 암호화
      String clientIp = request.getRemoteAddr();
      clientHash = aesUtil.encrypt(clientIp);
    }

    // 세션에 저장
    HttpSession session = request.getSession(true);
    session.setAttribute(ServerInfo.CLIENT_HASH_SESSION_KEY, clientHash);
    
    // IP 저장
    userAuthority.saveUserIp(request, session);
    
    // 슈퍼관리자 권한 확인
    String username = authentication.getName();
    String password = request.getParameter("password");
    userAuthority.checkAndSetSuperAdmin(username, password, session);
    
    log.debug("로그인 성공: clientHash 세션 저장 - {}", clientHash);

    // 성공 후 리다이렉트
    response.sendRedirect(request.getContextPath() + "/dashboard");
  }
}