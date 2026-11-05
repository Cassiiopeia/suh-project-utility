package me.suhsaechan.idea.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.common.constant.IdeaStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdeaItemDto {
  private UUID ideaItemId;
  private String keyword;
  private String memo;
  private IdeaStatus ideaStatus;
  private LocalDateTime createdDate;
  private LocalDateTime updatedDate;
}
