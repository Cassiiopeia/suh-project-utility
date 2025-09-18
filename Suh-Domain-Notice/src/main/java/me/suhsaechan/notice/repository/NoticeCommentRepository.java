package me.suhsaechan.notice.repository;

import java.util.UUID;
import me.suhsaechan.notice.entity.NoticeComment;
import me.suhsaechan.notice.entity.SuhProjectUtilityNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeCommentRepository extends JpaRepository<NoticeComment, UUID> {
    
    List<NoticeComment> findByNoticeOrderByCreatedDateDesc(SuhProjectUtilityNotice notice);
    
    long countByNotice(SuhProjectUtilityNotice notice);

    /**
     * 공지사항 ID로 댓글 목록 조회 (생성일 내림차순)
     * @param noticeId 공지사항 ID
     * @return 댓글 목록
     */
    @Query("SELECT c FROM NoticeComment c WHERE c.notice.noticeId = :noticeId ORDER BY c.createdDate DESC")
    List<NoticeComment> findByNoticeIdOrderByCreatedDateDesc(@Param("noticeId") UUID noticeId);
}
