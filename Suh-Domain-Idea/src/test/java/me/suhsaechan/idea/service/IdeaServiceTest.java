package me.suhsaechan.idea.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.common.constant.IdeaStatus;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.idea.dto.IdeaRequest;
import me.suhsaechan.idea.dto.IdeaResponse;
import me.suhsaechan.idea.entity.IdeaItem;
import me.suhsaechan.idea.repository.IdeaItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

// IdeaService 단위 테스트 (DB는 mock)
@ExtendWith(MockitoExtension.class)
class IdeaServiceTest {

  @Mock
  private IdeaItemRepository ideaItemRepository;

  private IdeaService ideaService;

  @BeforeEach
  void setUp() {
    ideaService = new IdeaService(ideaItemRepository);
  }

  @Test
  void 키워드만으로_생성시_IDEA_상태() {
    when(ideaItemRepository.save(any(IdeaItem.class))).thenAnswer(this::assignId);

    IdeaResponse response = ideaService.createItem(
        IdeaRequest.builder().keyword("  출퇴근 버스 알림 앱  ").memo("   ").build());

    assertThat(response.getIdeaItem().getKeyword()).isEqualTo("출퇴근 버스 알림 앱");
    assertThat(response.getIdeaItem().getMemo()).isNull();
    assertThat(response.getIdeaItem().getIdeaStatus()).isEqualTo(IdeaStatus.IDEA);
    assertThat(response.getIdeaItem().getIdeaItemId()).isNotNull();
  }

  @Test
  void 빈_키워드_거부() {
    assertThatThrownBy(() -> ideaService.createItem(IdeaRequest.builder().keyword("   ").build()))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IDEA_EMPTY_KEYWORD);
    verify(ideaItemRepository, never()).save(any());
  }

  @Test
  void 키워드_200자_초과_거부() {
    assertThatThrownBy(() -> ideaService.createItem(
        IdeaRequest.builder().keyword("a".repeat(201)).build()))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IDEA_KEYWORD_TOO_LONG);
    verify(ideaItemRepository, never()).save(any());
  }

  @Test
  void 수정시_상태_변경() {
    UUID ideaItemId = UUID.randomUUID();
    IdeaItem item = IdeaItem.builder().ideaItemId(ideaItemId).keyword("old").build();
    when(ideaItemRepository.findById(ideaItemId)).thenReturn(Optional.of(item));
    when(ideaItemRepository.save(any(IdeaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

    IdeaResponse response = ideaService.updateItem(IdeaRequest.builder()
        .ideaItemId(ideaItemId)
        .keyword("new")
        .memo("메모")
        .ideaStatus(IdeaStatus.IN_PROGRESS)
        .build());

    assertThat(response.getIdeaItem().getKeyword()).isEqualTo("new");
    assertThat(response.getIdeaItem().getMemo()).isEqualTo("메모");
    assertThat(response.getIdeaItem().getIdeaStatus()).isEqualTo(IdeaStatus.IN_PROGRESS);
  }

  @Test
  void 상태_미지정_수정시_기존_상태_유지() {
    UUID ideaItemId = UUID.randomUUID();
    IdeaItem item = IdeaItem.builder().ideaItemId(ideaItemId).keyword("old")
        .ideaStatus(IdeaStatus.DONE).build();
    when(ideaItemRepository.findById(ideaItemId)).thenReturn(Optional.of(item));
    when(ideaItemRepository.save(any(IdeaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

    IdeaResponse response = ideaService.updateItem(
        IdeaRequest.builder().ideaItemId(ideaItemId).keyword("new").build());

    assertThat(response.getIdeaItem().getIdeaStatus()).isEqualTo(IdeaStatus.DONE);
  }

  @Test
  void 삭제() {
    UUID ideaItemId = UUID.randomUUID();
    IdeaItem item = IdeaItem.builder().ideaItemId(ideaItemId).keyword("delete me").build();
    when(ideaItemRepository.findById(ideaItemId)).thenReturn(Optional.of(item));

    ideaService.deleteItem(ideaItemId);

    verify(ideaItemRepository).delete(item);
  }

  @Test
  void 없는_항목_수정은_404() {
    UUID unknownId = UUID.randomUUID();
    when(ideaItemRepository.findById(unknownId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> ideaService.updateItem(
        IdeaRequest.builder().ideaItemId(unknownId).keyword("x").build()))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IDEA_ITEM_NOT_FOUND);
  }

  @Test
  void 없는_항목_삭제는_404() {
    UUID unknownId = UUID.randomUUID();
    when(ideaItemRepository.findById(unknownId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> ideaService.deleteItem(unknownId))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IDEA_ITEM_NOT_FOUND);
  }

  private IdeaItem assignId(InvocationOnMock invocation) {
    IdeaItem item = invocation.getArgument(0);
    item.setIdeaItemId(UUID.randomUUID());
    return item;
  }
}
