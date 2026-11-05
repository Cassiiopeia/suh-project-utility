package me.suhsaechan.clipboard.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClipboardRequest {

  private UUID clipboardItemId;
  private String textContent;
  private MultipartFile file;
}
