package me.suhsaechan.statistics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 기능 사용 기록 엔티티
 * 각 기능별 사용 통계 수집 (확장성 고려)
 */
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class FeatureUsageLog extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID featureUsageLogId;

    // 기능 이름
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeatureType featureName;

    // 클라이언트 해시 (익명 사용자 식별)
    @Column
    private String clientHash;

    // 사용 시간
    @Column(nullable = false)
    private LocalDateTime usedAt;

    // 추가 정보 (JSON 형태로 저장 가능)
    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    /**
     * 기능 타입 Enum (확장 가능)
     */
    public enum FeatureType {
        CHATBOT,              // AI 챗봇
        GITHUB_ISSUE_HELPER,  // GitHub 이슈 헬퍼
        AI_TRANSLATOR,        // AI 번역기
        STUDY_VIEW,           // 스터디 조회
        NOTICE_VIEW,          // 공지사항 조회
        CONTAINER_LOG,        // 컨테이너 로그
        GRASS_PLANTER,        // Grass Planter
        SWAGGER_GENERATOR,    // Swagger 생성기
        TS_API_CONVERTER,     // TS API 변환기
        LOGIN,                // 로그인
        PROFILE_VIEW,         // 프로필 조회
        SEJONG_AUTH           // 세종대 인증
    }
}
