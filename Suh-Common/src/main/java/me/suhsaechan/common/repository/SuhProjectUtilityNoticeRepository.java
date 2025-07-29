package me.suhsaechan.common.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import me.suhsaechan.common.entity.SuhProjectUtilityNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SuhProjectUtilityNoticeRepository extends JpaRepository<SuhProjectUtilityNotice, UUID> {
  
  /**
   * 현재 활성화된 공지사항 목록 조회
   * 현재 시간이 게시 기간 내에 있는 공지사항만 조회
   */
  @Query("SELECT n FROM SuhProjectUtilityNotice n WHERE n.isActive = true " +
         "AND (n.startDate IS NULL OR n.startDate <= :now) " +
         "AND (n.endDate IS NULL OR n.endDate >= :now) " +
         "ORDER BY n.isImportant DESC, n.createdDate DESC")
  List<SuhProjectUtilityNotice> findActiveNotices(@Param("now") LocalDateTime now);

  /**
   * 중요 공지사항 목록 조회
   */
  List<SuhProjectUtilityNotice> findByIsImportantTrueAndIsActiveTrueOrderByCreatedDateDesc();
  
  /**
   * 제목으로 공지사항 검색
   */
  List<SuhProjectUtilityNotice> findByTitleContainingOrderByCreatedDateDesc(String title);
  
  /**
   * 내용으로 공지사항 검색
   */
  List<SuhProjectUtilityNotice> findByContentContainingOrderByCreatedDateDesc(String content);
}
