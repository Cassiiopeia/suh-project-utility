package me.suhsaechan.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 문서 청크 엔티티
 * 문서를 분할한 청크의 메타데이터 저장
 * 실제 벡터 데이터는 Qdrant에 저장 (pointId로 연결)
 */
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true, exclude = "chatDocument")
public class ChatDocumentChunk extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID chatDocumentChunkId;

    // 부모 문서
    @ManyToOne(fetch = FetchType.LAZY)
    private ChatDocument chatDocument;

    // Qdrant 포인트 ID (UUID)
    @Column(nullable = false)
    private UUID pointId;

    // 청크 순서 (문서 내 순서)
    @Column(nullable = false)
    private Integer chunkIndex;

    // 청크 내용
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 토큰 수 (대략적인 값)
    @Column
    private Integer tokenCount;
}
