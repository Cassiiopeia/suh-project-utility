package me.suhsaechan.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 사용자 권한 확인을 위한 유틸리티 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthority {

    private static final String SESSION_USER_ROLE = "USER_ROLE";
    private static final String SESSION_USER_IP = "USER_IP";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_USER = "USER";
    
    @Value("${admin.super.username}")
    private String superAdminUsername;
    
    @Value("${admin.super.password}")
    private String superAdminPassword;

    /**
     * 요청한 사용자가 슈퍼 관리자인지 확인
     * @param session 현재 세션
     * @return 슈퍼 관리자 여부
     */
    public boolean isSuperAdmin(HttpSession session) {
        String role = (String) session.getAttribute(SESSION_USER_ROLE);
        return ROLE_SUPER_ADMIN.equals(role);
    }
    
    /**
     * 요청한 사용자의 IP를 세션에 저장
     * @param request HTTP 요청
     * @param session 현재 세션
     */
    public void saveUserIp(HttpServletRequest request, HttpSession session) {
        String ip = extractIpFromRequest(request);
        session.setAttribute(SESSION_USER_IP, ip);
        log.debug("사용자 IP 저장: {}", ip);
    }
    
    /**
     * 세션에서 사용자 IP 조회
     * @param session 현재 세션
     * @return 사용자 IP
     */
    public String getUserIp(HttpSession session) {
        return (String) session.getAttribute(SESSION_USER_IP);
    }
    
    /**
     * 로그인 시 슈퍼 관리자 여부 확인 및 권한 설정
     * @param username 사용자명
     * @param password 비밀번호
     * @param session 현재 세션
     * @return 슈퍼 관리자 여부
     */
    public boolean checkAndSetSuperAdmin(String username, String password, HttpSession session) {
        if (superAdminUsername.equals(username) && superAdminPassword.equals(password)) {
            session.setAttribute(SESSION_USER_ROLE, ROLE_SUPER_ADMIN);
            log.info("슈퍼 관리자 로그인 성공: {}", username);
            return true;
        }
        session.setAttribute(SESSION_USER_ROLE, ROLE_USER);
        return false;
    }
    
    /**
     * HTTP 요청에서 IP 주소 추출
     * @param request HTTP 요청
     * @return 사용자 IP
     */
    public String extractIpFromRequest(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        return ip;
    }
    
    /**
     * IP 주소 일치 여부 확인 (댓글 작성자와 현재 사용자)
     * @param commentIp 댓글 작성자 IP
     * @param session 현재 세션
     * @return IP 일치 여부
     */
    public boolean isSameIp(String commentIp, HttpSession session) {
        String userIp = getUserIp(session);
        return commentIp != null && commentIp.equals(userIp);
    }
} 