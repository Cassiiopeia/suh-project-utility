package me.suhsaechan.study.repository;

import java.util.List;
import java.util.UUID;
import me.suhsaechan.study.entity.StudyPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * StudyPostRepository는 스터디 포스트 정보에 대한 데이터 액세스를 제공합니다.
 */
@Repository
public interface StudyPostRepository extends JpaRepository<StudyPost, UUID> {

    /**
     * 특정 카테고리에 속한 포스트 목록 조회 (페이징)
     */
    Page<StudyPost> findByCategoryStudyCategoryId(UUID categoryId, Pageable pageable);

  /**
     * 제목이나 내용에 특정 키워드가 포함된 포스트 검색
     */
    @Query("SELECT p FROM StudyPost p WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) AND p.isPublic = true")
    Page<StudyPost> searchByKeyword(String keyword, Pageable pageable);
    
    /**
     * 태그로 포스트 검색
     */
    @Query("SELECT p FROM StudyPost p WHERE p.tags LIKE %:tag% AND p.isPublic = true")
    Page<StudyPost> findByTag(String tag, Pageable pageable);
    
    /**
     * 최근에 생성된 포스트 조회
     */
    List<StudyPost> findTop5ByIsPublicTrueOrderByCreatedDateDesc();
    
    /**
     * 조회수 기준 인기 포스트 조회
     */
    List<StudyPost> findTop5ByIsPublicTrueOrderByViewCountDesc();

    /**
     * 특정 카테고리에 속한 포스트 수 조회
     */
    long countByCategoryStudyCategoryId(UUID categoryId);

    /**
     * 총 조회수 합계 (통계용)
     */
    @Query("SELECT COALESCE(SUM(p.viewCount), 0) FROM StudyPost p WHERE p.viewCount IS NOT NULL")
    Long sumTotalViewCount();
}
