package me.suhsaechan.idea.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdeaResponse {

  @Builder.Default
  private List<IdeaItemDto> ideaItems = new ArrayList<>();
  private IdeaItemDto ideaItem;
}
