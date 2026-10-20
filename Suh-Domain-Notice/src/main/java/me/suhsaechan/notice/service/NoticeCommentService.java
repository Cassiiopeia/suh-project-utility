package me.suhsaechan.notice.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.notice.entity.SuhProjectUtilityNotice;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.notice.repository.SuhProjectUtilityNoticeRepository;
import me.suhsaechan.notice.entity.NoticeComment;
import me.suhsaechan.notice.dto.NoticeCommentDto;
import me.suhsaechan.notice.dto.NoticeRequest;
import me.suhsaechan.notice.dto.NoticeResponse;
import me.suhsaechan.notice.repository.NoticeCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeCommentService {

    private final NoticeCommentRepository commentRepository;
    private final SuhProjectUtilityNoticeRepository noticeRepository;

    /**
     * 공지사항에 달린 댓글 목록 조회
     *
     * @param noticeId 공지사항 ID
     * @return 댓글 목록 응답
     */
    @Transactional(readOnly = true)
    public NoticeResponse getCommentsByNoticeId(UUID noticeId) {
        log.info("공지사항 댓글 목록 조회: {}", noticeId);
        
        // 공지사항 존재 확인
        if (!noticeRepository.existsById(noticeId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_NOTICE);
        }
        
        List<NoticeComment> comments = commentRepository.findByNoticeIdOrderByCreatedDateDesc(noticeId);
        
        List<NoticeCommentDto> commentDtos = comments.stream()
                .map(comment -> NoticeCommentDto.builder()
                        .noticeCommentId(comment.getNoticeCommentId())
                        .author(comment.getAuthor())
                        .content(comment.getContent())
                        .anonymizedIp(comment.getAnonymizedIp())
                        .clientHash(comment.getClientHash())
                        .createdDate(comment.getCreatedDate())
                        .canDelete(true)
                        .build())
                .collect(Collectors.toList());
                
        return NoticeResponse.builder()
                .noticeCommentDtos(commentDtos)
                .build();
    }

    /**
     * 공지사항 댓글 등록
     *
     * @param request 댓글 등록 요청
     * @return 등록된 댓글 응답
     */
    @Transactional
    public NoticeResponse createComment(NoticeRequest request) {
        log.info("공지사항 댓글 등록: {}", request.getNoticeId());
        
        // 유효성 검사
        if (request.getCommentContent() == null || request.getCommentContent().trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }
        
        // 공지사항 조회
        SuhProjectUtilityNotice notice = noticeRepository.findById(request.getNoticeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_NOTICE));
        
        // 댓글 생성
        NoticeComment comment = NoticeComment.builder()
                .notice(notice)
                .author(request.getCommentAuthor() != null ? request.getCommentAuthor() : "익명")
                .authorIp(request.getAuthorIp())
                .clientHash(request.getClientHash()) // clientHash 설정
                .content(request.getCommentContent())
                .build();
        
        commentRepository.save(comment);
        
        // 응답 객체 직접 생성
        NoticeCommentDto commentDto = NoticeCommentDto.builder()
                .noticeCommentId(comment.getNoticeCommentId())
                .author(comment.getAuthor())
                .content(comment.getContent())
                .anonymizedIp(comment.getAnonymizedIp())
                .clientHash(comment.getClientHash())
                .createdDate(comment.getCreatedDate())
                .canDelete(true)
                .build();
                
        return NoticeResponse.builder()
                .noticeCommentDto(commentDto)
                .build();
    }

    /**
     * 공지사항 댓글 삭제 (작성자 또는 슈퍼관리자만)
     *
     * @param commentId 댓글 ID
     * @param requesterClientHash 요청자 세션 clientHash
     * @param isSuperAdmin 슈퍼관리자 여부
     */
    @Transactional
    public NoticeResponse deleteComment(UUID commentId, String requesterClientHash, boolean isSuperAdmin) {
        log.info("공지사항 댓글 삭제: {}", commentId);
        
        NoticeComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_COMMENT));

        // 삭제 권한: 슈퍼관리자 또는 작성자(세션 clientHash 일치)
        boolean isAuthor = Optional.ofNullable(requesterClientHash)
                .map(hash -> hash.equals(comment.getClientHash()))
                .orElse(false);
        if (!isSuperAdmin && !isAuthor) {
            throw new CustomException(ErrorCode.COMMENT_NO_PERMISSION);
        }
        
        commentRepository.delete(comment);
        
        return NoticeResponse.builder().build();
    }
}