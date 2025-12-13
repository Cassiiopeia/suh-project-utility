package me.suhsaechan.chatbot.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.chatbot.entity.ChatDocument;
import me.suhsaechan.chatbot.entity.ChatDocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatDocumentChunkRepository extends JpaRepository<ChatDocumentChunk, UUID> {

    // 문서별 청크 조회 (순서대로)
    List<ChatDocumentChunk> findByChatDocumentOrderByChunkIndexAsc(ChatDocument chatDocument);

    // 문서 ID로 청크 조회
    List<ChatDocumentChunk> findByChatDocumentChatDocumentIdOrderByChunkIndexAsc(UUID chatDocumentId);

    // Qdrant pointId로 청크 조회
    Optional<ChatDocumentChunk> findByPointId(UUID pointId);

    // 문서별 청크 수
    long countByChatDocument(ChatDocument chatDocument);

    // 문서별 청크 삭제
    void deleteByChatDocument(ChatDocument chatDocument);

    // 문서 ID로 청크 삭제
    void deleteByChatDocumentChatDocumentId(UUID chatDocumentId);
}
