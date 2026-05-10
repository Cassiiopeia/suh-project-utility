package me.suhsaechan.somansabus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteLabelParts {
  private String departureTime;
  private String station;
  private Integer busNumber;

  public static RouteLabelParts empty() {
    return RouteLabelParts.builder().build();
  }
}
