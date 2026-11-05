package me.suhsaechan.clipboard.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.common.constant.ClipboardItemType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClipboardItemDto {
  private UUID clipboardItemId;
  private ClipboardItemType itemType;
  private String textContent;
  private String originalFileName;
  private String contentType;
  private Long fileSizeBytes;
  private LocalDateTime createdDate;
}
