package me.suhsaechan.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.entity.BasePostgresEntity;

/**
 * 챗봇 설정 엔티티
 * 시스템 프롬프트, 응답 가이드라인 등 챗봇 설정을 저장
 */
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ChatbotConfig extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID chatbotConfigId;

    // 설정 키 (system_prompt, greeting_message 등)
    @Column(nullable = false, unique = true)
    private String configKey;

    // 설정 이름 (관리 화면 표시용)
    @Column(nullable = false)
    private String configName;

    // 설정 값 (실제 프롬프트 내용)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String configValue;

    // 설정 설명
    @Column(columnDefinition = "TEXT")
    private String description;

    // 활성화 여부
    @Column(nullable = false)
    private Boolean isActive;

    // 정렬 순서
    @Column
    private Integer orderIndex;
}
