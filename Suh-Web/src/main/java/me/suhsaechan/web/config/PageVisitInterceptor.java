package me.suhsaechan.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.statistics.service.StatisticsService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 페이지 방문 기록 인터셉터
 * 사용자 페이지 방문을 비동기로 기록
 * 제외 패턴은 WebConfig.addInterceptors()에서 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PageVisitInterceptor implements HandlerInterceptor {

    private final StatisticsService statisticsService;

    // 기록할 페이지 경로 (명시적 화이트리스트)
    private static final List<String> TRACK_PAGES = List.of(
        "/",
        "/dashboard",
        "/login",
        "/profile",
        "/study",
        "/notice",
        "/docker",
        "/github",
        "/grass",
        "/translate",
        "/chatbot",
        "/ai-server"
    );

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            String path = request.getRequestURI();

            // 기록 대상 페이지인지 확인
            if (!shouldTrack(path)) {
                return;
            }

            // 성공 응답만 기록 (2xx, 3xx)
            int status = response.getStatus();
            if (status >= 400) {
                return;
            }

            // 클라이언트 해시 가져오기
            String clientHash = getClientHash(request);

            // 비동기로 방문 기록 저장
            statisticsService.logPageVisitAsync(
                path,
                clientHash,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                request.getHeader("Referer")
            );

            log.debug("페이지 방문 기록 - path: {}, clientHash: {}", path, clientHash);

        } catch (Exception e) {
            // 방문 기록 실패해도 요청 처리에 영향 없도록 무시
            log.warn("페이지 방문 기록 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 기록 대상 페이지 체크
     */
    private boolean shouldTrack(String path) {
        // 정확히 일치하거나 해당 경로로 시작하는지 확인
        return TRACK_PAGES.stream().anyMatch(page -> {
            if (page.equals("/")) {
                return path.equals("/") || path.equals("/dashboard");
            }
            return path.equals(page) || path.startsWith(page + "/");
        });
    }

    /**
     * 세션에서 클라이언트 해시 가져오기
     */
    private String getClientHash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object clientHash = session.getAttribute(ServerInfo.CLIENT_HASH_SESSION_KEY);
            if (clientHash != null) {
                return clientHash.toString();
            }
        }
        // 세션이 없으면 IP + User-Agent 조합 SHA-256 해시 생성
        return generateAnonymousHash(request);
    }

    /**
     * IP + User-Agent 기반 익명 해시 생성
     * 동일 디바이스/브라우저 식별을 위한 고유 해시
     */
    private String generateAnonymousHash(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String rawData = ip + "|" + (userAgent != null ? userAgent : "unknown");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 8; i++) {  // 앞 8바이트만 사용 (16자리 hex)
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return "anon_" + hexString;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 항상 지원되므로 발생하지 않음
            log.warn("SHA-256 알고리즘 미지원, 폴백 해시 사용");
            return "anon_" + Math.abs((ip + userAgent).hashCode());
        }
    }
}
