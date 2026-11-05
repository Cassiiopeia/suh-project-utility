package me.suhsaechan.clipboard.dto;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 파일 내려받기용: 디스크 경로 + 응답 헤더 정보
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClipboardFileDto {
  private Path filePath;
  private String originalFileName;
  private String contentType;
  private Long fileSizeBytes;
  private Boolean isImage;
}
