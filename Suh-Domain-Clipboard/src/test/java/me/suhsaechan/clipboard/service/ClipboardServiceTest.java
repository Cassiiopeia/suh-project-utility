package me.suhsaechan.clipboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.clipboard.dto.ClipboardRequest;
import me.suhsaechan.clipboard.dto.ClipboardResponse;
import me.suhsaechan.clipboard.entity.ClipboardItem;
import me.suhsaechan.clipboard.repository.ClipboardItemRepository;
import me.suhsaechan.common.constant.ClipboardItemType;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

// ClipboardService 단위 테스트 (DB는 mock, 저장소는 임시 폴더)
@ExtendWith(MockitoExtension.class)
class ClipboardServiceTest {

  @Mock
  private ClipboardItemRepository clipboardItemRepository;

  @TempDir
  Path storageDirectory;

  private ClipboardService clipboardService;

  @BeforeEach
  void setUp() {
    // 하위 폴더 지정: 첫 저장 시 폴더 생성까지 확인
    clipboardService = new ClipboardService(clipboardItemRepository,
        storageDirectory.resolve("clipboard").toString());
  }

  @Test
  void 텍스트_저장() {
    when(clipboardItemRepository.save(any(ClipboardItem.class))).thenAnswer(this::assignId);

    ClipboardResponse response = clipboardService.createItem(
        ClipboardRequest.builder().textContent("hello clipboard").build());

    assertThat(response.getItem().getItemType()).isEqualTo(ClipboardItemType.TEXT);
    assertThat(response.getItem().getTextContent()).isEqualTo("hello clipboard");
    assertThat(response.getItem().getClipboardItemId()).isNotNull();
  }

  @Test
  void 빈_텍스트와_파일_없음_거부() {
    assertThatThrownBy(() -> clipboardService.createItem(
        ClipboardRequest.builder().textContent("   ").build()))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CLIPBOARD_EMPTY_CONTENT);
    verify(clipboardItemRepository, never()).save(any());
  }

  @Test
  void 이미지_파일_저장시_UUID_파일명과_IMAGE_종류() throws Exception {
    when(clipboardItemRepository.save(any(ClipboardItem.class))).thenAnswer(this::assignId);
    MockMultipartFile imageFile = new MockMultipartFile(
        "file", "../../evil.png", "image/png", new byte[]{1, 2, 3});

    ClipboardResponse response = clipboardService.createItem(
        ClipboardRequest.builder().file(imageFile).build());

    assertThat(response.getItem().getItemType()).isEqualTo(ClipboardItemType.IMAGE);
    assertThat(response.getItem().getOriginalFileName()).isEqualTo("evil.png");
    Path clipboardDirectory = storageDirectory.resolve("clipboard");
    try (var storedFiles = Files.list(clipboardDirectory)) {
      Path storedFile = storedFiles.findFirst().orElseThrow();
      assertThat(UUID.fromString(storedFile.getFileName().toString())).isNotNull();
      assertThat(Files.readAllBytes(storedFile)).containsExactly(1, 2, 3);
    }
  }

  @Test
  void 일반_파일은_FILE_종류() {
    when(clipboardItemRepository.save(any(ClipboardItem.class))).thenAnswer(this::assignId);
    MockMultipartFile textFile = new MockMultipartFile(
        "file", "memo.txt", "text/plain", "memo".getBytes(StandardCharsets.UTF_8));

    ClipboardResponse response = clipboardService.createItem(
        ClipboardRequest.builder().file(textFile).build());

    assertThat(response.getItem().getItemType()).isEqualTo(ClipboardItemType.FILE);
  }

  @Test
  void 삭제시_파일도_제거() throws Exception {
    Path clipboardDirectory = Files.createDirectories(storageDirectory.resolve("clipboard"));
    String storedFileName = UUID.randomUUID().toString();
    Path storedFile = Files.write(clipboardDirectory.resolve(storedFileName), new byte[]{9});
    UUID clipboardItemId = UUID.randomUUID();
    ClipboardItem item = ClipboardItem.builder()
        .clipboardItemId(clipboardItemId)
        .itemType(ClipboardItemType.FILE)
        .storedFileName(storedFileName)
        .build();
    when(clipboardItemRepository.findById(clipboardItemId)).thenReturn(Optional.of(item));

    clipboardService.deleteItem(clipboardItemId);

    verify(clipboardItemRepository).delete(item);
    assertThat(Files.exists(storedFile)).isFalse();
  }

  @Test
  void 없는_항목_삭제는_404() {
    UUID unknownId = UUID.randomUUID();
    when(clipboardItemRepository.findById(unknownId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> clipboardService.deleteItem(unknownId))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CLIPBOARD_ITEM_NOT_FOUND);
  }

  @Test
  void 저장경로_밖_파일명은_로드_거부() {
    UUID clipboardItemId = UUID.randomUUID();
    ClipboardItem item = ClipboardItem.builder()
        .clipboardItemId(clipboardItemId)
        .itemType(ClipboardItemType.FILE)
        .storedFileName("../outside")
        .build();
    when(clipboardItemRepository.findById(clipboardItemId)).thenReturn(Optional.of(item));

    assertThatThrownBy(() -> clipboardService.loadFile(clipboardItemId))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CLIPBOARD_ITEM_NOT_FOUND);
  }

  private ClipboardItem assignId(org.mockito.invocation.InvocationOnMock invocation) {
    ClipboardItem item = invocation.getArgument(0);
    item.setClipboardItemId(UUID.randomUUID());
    return item;
  }
}
