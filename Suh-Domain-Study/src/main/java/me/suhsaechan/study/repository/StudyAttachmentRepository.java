package me.suhsaechan.study.repository;

import java.util.UUID;
import me.suhsaechan.study.entity.StudyAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * StudyAttachmentRepository는 스터디 첨부파일 정보에 대한 데이터 액세스를 제공합니다.
 */
@Repository
public interface StudyAttachmentRepository extends JpaRepository<StudyAttachment, UUID> {

    /**
     * 특정 포스트에 속한 첨부파일 목록 조회
     */
    List<StudyAttachment> findByPostStudyPostId(UUID studyPostId);
    
    /**
     * 특정 포스트에 속한 첨부파일 목록을 표시 순서대로 조회
     */
    List<StudyAttachment> findByPostStudyPostIdOrderByDisplayOrderAsc(UUID postId);
    
}
