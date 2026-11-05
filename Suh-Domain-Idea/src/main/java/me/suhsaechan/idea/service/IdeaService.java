package me.suhsaechan.idea.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.IdeaStatus;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.idea.dto.IdeaItemDto;
import me.suhsaechan.idea.dto.IdeaRequest;
import me.suhsaechan.idea.dto.IdeaResponse;
import me.suhsaechan.idea.entity.IdeaItem;
import me.suhsaechan.idea.repository.IdeaItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아이디어 보따리: 생성, 목록, 최근 목록, 수정(키워드·메모·상태), 삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdeaService {

  // idea_item.keyword 컬럼 길이와 동일
  private static final int MAXIMUM_KEYWORD_LENGTH = 200;

  private final IdeaItemRepository ideaItemRepository;

  @Transactional
  public IdeaResponse createItem(IdeaRequest request) {
    IdeaItem saved = ideaItemRepository.save(IdeaItem.builder()
        .keyword(toValidKeyword(request.getKeyword()))
        .memo(toMemoOrNull(request.getMemo()))
        .ideaStatus(Optional.ofNullable(request.getIdeaStatus()).orElse(IdeaStatus.IDEA))
        .build());
    log.info("아이디어 저장: {}", saved.getIdeaItemId());
    return IdeaResponse.builder()
        .ideaItem(toItemDto(saved))
        .build();
  }

  @Transactional(readOnly = true)
  public IdeaResponse getItems() {
    return toListResponse(ideaItemRepository.findAllByOrderByCreatedDateDesc());
  }

  // 대시보드 빠른 입력 카드용 최근 5개
  @Transactional(readOnly = true)
  public IdeaResponse getRecentItems() {
    return toListResponse(ideaItemRepository.findTop5ByOrderByCreatedDateDesc());
  }

  /**
   * 수정: 키워드 검증 > 메모 교체 > 상태 (미지정 시 유지)
   */
  @Transactional
  public IdeaResponse updateItem(IdeaRequest request) {
    IdeaItem item = findItem(request.getIdeaItemId());
    item.setKeyword(toValidKeyword(request.getKeyword()));
    item.setMemo(toMemoOrNull(request.getMemo()));
    Optional.ofNullable(request.getIdeaStatus()).ifPresent(item::setIdeaStatus);
    IdeaItem saved = ideaItemRepository.save(item);
    log.info("아이디어 수정: {} ({})", saved.getIdeaItemId(), saved.getIdeaStatus());
    return IdeaResponse.builder()
        .ideaItem(toItemDto(saved))
        .build();
  }

  @Transactional
  public void deleteItem(UUID ideaItemId) {
    IdeaItem item = findItem(ideaItemId);
    ideaItemRepository.delete(item);
    log.info("아이디어 삭제: {}", ideaItemId);
  }

  // 키워드 검증 (공백 거부, 200자 제한)
  private String toValidKeyword(String keyword) {
    String trimmedKeyword = Optional.ofNullable(keyword)
        .map(String::trim)
        .filter(text -> !text.isEmpty())
        .orElseThrow(() -> new CustomException(ErrorCode.IDEA_EMPTY_KEYWORD));
    if (trimmedKeyword.length() > MAXIMUM_KEYWORD_LENGTH) {
      throw new CustomException(ErrorCode.IDEA_KEYWORD_TOO_LONG);
    }
    return trimmedKeyword;
  }

  // 공백뿐인 메모 > null
  private String toMemoOrNull(String memo) {
    return Optional.ofNullable(memo)
        .filter(text -> !text.isBlank())
        .orElse(null);
  }

  private IdeaItem findItem(UUID ideaItemId) {
    return Optional.ofNullable(ideaItemId)
        .flatMap(ideaItemRepository::findById)
        .orElseThrow(() -> new CustomException(ErrorCode.IDEA_ITEM_NOT_FOUND));
  }

  private IdeaResponse toListResponse(List<IdeaItem> items) {
    return IdeaResponse.builder()
        .ideaItems(items.stream().map(this::toItemDto).collect(Collectors.toList()))
        .build();
  }

  private IdeaItemDto toItemDto(IdeaItem item) {
    return IdeaItemDto.builder()
        .ideaItemId(item.getIdeaItemId())
        .keyword(item.getKeyword())
        .memo(item.getMemo())
        .ideaStatus(item.getIdeaStatus())
        .createdDate(item.getCreatedDate())
        .updatedDate(item.getUpdatedDate())
        .build();
  }
}
