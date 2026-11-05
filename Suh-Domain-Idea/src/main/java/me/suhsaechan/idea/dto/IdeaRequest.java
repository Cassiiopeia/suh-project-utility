package me.suhsaechan.idea.dto;

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
public class IdeaRequest {

  private UUID ideaItemId;
  private String keyword;
  private String memo;
  private IdeaStatus ideaStatus;
}
