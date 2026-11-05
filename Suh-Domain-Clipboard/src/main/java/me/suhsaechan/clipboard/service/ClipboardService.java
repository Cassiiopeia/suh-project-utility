package me.suhsaechan.clipboard.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.clipboard.dto.ClipboardFileDto;
import me.suhsaechan.clipboard.dto.ClipboardItemDto;
import me.suhsaechan.clipboard.dto.ClipboardRequest;
import me.suhsaechan.clipboard.dto.ClipboardResponse;
import me.suhsaechan.clipboard.entity.ClipboardItem;
import me.suhsaechan.clipboard.repository.ClipboardItemRepository;
import me.suhsaechan.common.constant.ClipboardItemType;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 웹 클립보드: 텍스트·파일 저장, 목록, 삭제, 파일 로드
 * 파일 본문 > storageDir/{UUID}, 원래 파일명·종류 > DB
 */
@Slf4j
@Service
public class ClipboardService {

  private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";
  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
  private static final String DEFAULT_FILE_NAME = "clipboard-file";

  private final ClipboardItemRepository clipboardItemRepository;
  private final Path storageDirectory;

  public ClipboardService(ClipboardItemRepository clipboardItemRepository,
      // application.yml 은 gitignore·CI 시크릿 주입이라 키가 없어도 기동되도록 기본값 지정
      @Value("${clipboard.storage-dir:/mnt/suh-project-utility/clipboard}") String storageDir) {
    this.clipboardItemRepository = clipboardItemRepository;
    this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
  }

  /**
   * 항목 1건 저장: 파일 있으면 파일, 없으면 텍스트, 둘 다 없으면 거부
   */
  @Transactional
  public ClipboardResponse createItem(ClipboardRequest request) {
    Optional<MultipartFile> uploadedFile = Optional.ofNullable(request.getFile())
        .filter(file -> !file.isEmpty());
    if (uploadedFile.isPresent()) {
      return ClipboardResponse.builder()
          .item(toItemDto(saveFileItem(uploadedFile.get())))
          .build();
    }

    String textContent = Optional.ofNullable(request.getTextContent())
        .filter(text -> !text.isBlank())
        .orElseThrow(() -> new CustomException(ErrorCode.CLIPBOARD_EMPTY_CONTENT));
    ClipboardItem saved = clipboardItemRepository.save(ClipboardItem.builder()
        .itemType(ClipboardItemType.TEXT)
        .textContent(textContent)
        .build());
    log.info("클립보드 텍스트 저장: {}자", textContent.length());
    return ClipboardResponse.builder()
        .item(toItemDto(saved))
        .build();
  }

  @Transactional(readOnly = true)
  public ClipboardResponse getItems() {
    List<ClipboardItemDto> items = clipboardItemRepository.findAllByOrderByCreatedDateDesc().stream()
        .map(this::toItemDto)
        .collect(Collectors.toList());
    return ClipboardResponse.builder()
        .items(items)
        .build();
  }

  /**
   * 항목 삭제: DB 행 삭제 > 디스크 파일 삭제 (파일 삭제 실패는 로그만)
   */
  @Transactional
  public void deleteItem(UUID clipboardItemId) {
    ClipboardItem item = findItem(clipboardItemId);
    clipboardItemRepository.delete(item);
    Optional.ofNullable(item.getStoredFileName()).ifPresent(this::deleteStoredFile);
    log.info("클립보드 항목 삭제: {} ({})", clipboardItemId, item.getItemType());
  }

  /**
   * 파일 로드: 항목 조회 > 저장 경로 계산(storageDir 밖 차단) > 존재 확인
   */
  @Transactional(readOnly = true)
  public ClipboardFileDto loadFile(UUID clipboardItemId) {
    ClipboardItem item = findItem(clipboardItemId);
    Path filePath = Optional.ofNullable(item.getStoredFileName())
        .map(this::resolveStoredPath)
        .filter(Files::isRegularFile)
        .orElseThrow(() -> new CustomException(ErrorCode.CLIPBOARD_ITEM_NOT_FOUND));
    return ClipboardFileDto.builder()
        .filePath(filePath)
        .originalFileName(item.getOriginalFileName())
        .contentType(item.getContentType())
        .fileSizeBytes(item.getFileSizeBytes())
        .isImage(ClipboardItemType.IMAGE.equals(item.getItemType()))
        .build();
  }

  /**
   * 파일 저장: 디스크 기록 > DB 저장 (DB 실패 시 디스크 파일 제거)
   */
  private ClipboardItem saveFileItem(MultipartFile file) {
    String contentType = Optional.ofNullable(file.getContentType())
        .filter(type -> !type.isBlank())
        .orElse(DEFAULT_CONTENT_TYPE);
    String storedFileName = UUID.randomUUID().toString();
    writeToStorage(file, storedFileName);

    ClipboardItemType itemType = contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX)
        ? ClipboardItemType.IMAGE
        : ClipboardItemType.FILE;
    try {
      ClipboardItem saved = clipboardItemRepository.save(ClipboardItem.builder()
          .itemType(itemType)
          .originalFileName(toSafeOriginalFileName(file.getOriginalFilename()))
          .storedFileName(storedFileName)
          .contentType(contentType)
          .fileSizeBytes(file.getSize())
          .build());
      log.info("클립보드 파일 저장: {} ({}, {} bytes)", storedFileName, itemType, file.getSize());
      return saved;
    } catch (RuntimeException e) {
      deleteStoredFile(storedFileName);
      throw e;
    }
  }

  private void writeToStorage(MultipartFile file, String storedFileName) {
    try (InputStream inputStream = file.getInputStream()) {
      Files.createDirectories(storageDirectory);
      Files.copy(inputStream, resolveStoredPath(storedFileName));
    } catch (IOException e) {
      log.error("클립보드 파일 저장 실패: {}", e.getMessage(), e);
      throw new CustomException(ErrorCode.CLIPBOARD_FILE_STORAGE_FAILED);
    }
  }

  private void deleteStoredFile(String storedFileName) {
    try {
      Files.deleteIfExists(resolveStoredPath(storedFileName));
    } catch (IOException | CustomException e) {
      log.warn("클립보드 파일 삭제 실패: {} - {}", storedFileName, e.getMessage());
    }
  }

  // 저장 경로 계산 (storageDir 밖으로 벗어나면 거부)
  private Path resolveStoredPath(String storedFileName) {
    Path resolvedPath = storageDirectory.resolve(storedFileName).normalize();
    if (!resolvedPath.startsWith(storageDirectory)) {
      throw new CustomException(ErrorCode.CLIPBOARD_ITEM_NOT_FOUND);
    }
    return resolvedPath;
  }

  // 원래 파일명에서 경로 부분 제거 (표시·내려받기 이름 용도, 저장 경로에는 쓰지 않음)
  private String toSafeOriginalFileName(String originalFileName) {
    return Optional.ofNullable(originalFileName)
        .map(name -> name.replaceAll(".*[\\\\/]", ""))
        .filter(name -> !name.isBlank())
        .orElse(DEFAULT_FILE_NAME);
  }

  private ClipboardItem findItem(UUID clipboardItemId) {
    return Optional.ofNullable(clipboardItemId)
        .flatMap(clipboardItemRepository::findById)
        .orElseThrow(() -> new CustomException(ErrorCode.CLIPBOARD_ITEM_NOT_FOUND));
  }

  private ClipboardItemDto toItemDto(ClipboardItem item) {
    return ClipboardItemDto.builder()
        .clipboardItemId(item.getClipboardItemId())
        .itemType(item.getItemType())
        .textContent(item.getTextContent())
        .originalFileName(item.getOriginalFileName())
        .contentType(item.getContentType())
        .fileSizeBytes(item.getFileSizeBytes())
        .createdDate(item.getCreatedDate())
        .build();
  }
}
