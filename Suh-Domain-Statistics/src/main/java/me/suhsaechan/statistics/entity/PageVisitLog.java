package me.suhsaechan.statistics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.entity.BasePostgresEntity;

/**
 * 페이지 방문 기록 엔티티
 * 사용자 페이지 방문 통계 수집
 */
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class PageVisitLog extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID pageVisitLogId;

    // 방문 페이지 경로 (예: "/login", "/dashboard", "/study")
    @Column(nullable = false)
    private String pagePath;

    // 클라이언트 해시 (익명 방문자 식별)
    @Column
    private String clientHash;

    // 사용자 IP 주소
    @Column
    private String userIp;

    // 브라우저 정보
    @Column
    private String userAgent;

    // 이전 페이지 (Referer)
    @Column
    private String referrer;

    // 방문 시간
    @Column(nullable = false)
    private LocalDateTime visitedAt;

    // 봇 여부
    @Column(nullable = false)
    private Boolean isBot;
}
