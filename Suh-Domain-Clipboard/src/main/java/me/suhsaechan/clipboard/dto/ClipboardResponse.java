package me.suhsaechan.clipboard.dto;

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
public class ClipboardResponse {

  @Builder.Default
  private List<ClipboardItemDto> items = new ArrayList<>();
  private ClipboardItemDto item;
}
