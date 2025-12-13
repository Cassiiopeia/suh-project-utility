package me.suhsaechan.chatbot.entity;

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
 * 채팅 세션 엔티티
 * 사용자와의 대화 세션을 관리
 */
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ChatSession extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID chatSessionId;

    // 세션 식별자 (브라우저 세션 ID 또는 사용자 ID)
    @Column(nullable = false)
    private String sessionToken;

    // 사용자 IP (익명 사용자 추적용)
    @Column
    private String userIp;

    // 사용자 에이전트
    @Column
    private String userAgent;

    // 마지막 활동 시간
    @Column
    private LocalDateTime lastActivityAt;

    // 세션 활성화 여부
    @Column(nullable = false)
    private Boolean isActive;

    // 총 메시지 수
    @Column
    private Integer messageCount;

    // 세션 제목 (자동 생성 또는 사용자 지정)
    @Column
    private String title;
}
