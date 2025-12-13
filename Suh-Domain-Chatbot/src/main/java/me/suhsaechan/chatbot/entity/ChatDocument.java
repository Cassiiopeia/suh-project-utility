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
 * RAG 문서 엔티티
 * 챗봇이 참조할 문서의 메타데이터를 저장
 * 실제 벡터 데이터는 Qdrant에 저장
 */
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ChatDocument extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID chatDocumentId;

    // 문서 제목
    @Column(nullable = false)
    private String title;

    // 카테고리 (site-info, features, faq 등)
    @Column(nullable = false)
    private String category;

    // 원본 내용 (Markdown)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 문서 설명 (선택)
    @Column(columnDefinition = "TEXT")
    private String description;

    // 활성화 여부 (비활성화 시 검색에서 제외)
    @Column(nullable = false)
    private Boolean isActive;

    // 청크 처리 완료 여부
    @Column(nullable = false)
    private Boolean isProcessed;

    // 청크 수
    @Column
    private Integer chunkCount;

    // 정렬 순서
    @Column
    private Integer orderIndex;
}
