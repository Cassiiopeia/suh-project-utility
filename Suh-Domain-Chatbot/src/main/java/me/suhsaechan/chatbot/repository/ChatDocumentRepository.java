package me.suhsaechan.chatbot.repository;

import java.util.List;
import java.util.UUID;
import me.suhsaechan.chatbot.entity.ChatDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatDocumentRepository extends JpaRepository<ChatDocument, UUID> {

    // 활성화된 문서만 조회
    List<ChatDocument> findByIsActiveTrue();

    // 카테고리별 활성화된 문서 조회
    List<ChatDocument> findByCategoryAndIsActiveTrueOrderByOrderIndexAsc(String category);

    // 처리되지 않은 문서 조회
    List<ChatDocument> findByIsProcessedFalseAndIsActiveTrue();

    // 제목으로 검색
    List<ChatDocument> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title);

    // 카테고리 목록 조회
    List<ChatDocument> findByIsActiveTrueOrderByCategoryAscOrderIndexAsc();

    // 활성화된 문서 정렬 조회
    List<ChatDocument> findByIsActiveTrueOrderByOrderIndexAsc();
}
