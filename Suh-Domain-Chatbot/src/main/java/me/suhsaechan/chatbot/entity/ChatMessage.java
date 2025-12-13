package me.suhsaechan.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.entity.BasePostgresEntity;

/**
 * 채팅 메시지 엔티티
 * 대화 내역 저장
 */
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true, exclude = "chatSession")
public class ChatMessage extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID chatMessageId;

    // 세션 연결
    @ManyToOne(fetch = FetchType.LAZY)
    private ChatSession chatSession;

    // 메시지 역할 (USER, ASSISTANT, SYSTEM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    // 메시지 내용
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 메시지 순서
    @Column(nullable = false)
    private Integer messageIndex;

    // 참조한 문서 ID 목록 (JSON 문자열)
    @Column(columnDefinition = "TEXT")
    private String referencedDocumentIds;

    // 토큰 사용량 (입력)
    @Column
    private Integer inputTokens;

    // 토큰 사용량 (출력)
    @Column
    private Integer outputTokens;

    // 응답 생성 시간 (ms)
    @Column
    private Long responseTimeMs;

    // 피드백 (좋아요/싫어요)
    @Column
    private Boolean isHelpful;

    /**
     * 메시지 역할 Enum
     */
    public enum MessageRole {
        USER,       // 사용자 메시지
        ASSISTANT,  // AI 응답
        SYSTEM      // 시스템 메시지
    }
}
