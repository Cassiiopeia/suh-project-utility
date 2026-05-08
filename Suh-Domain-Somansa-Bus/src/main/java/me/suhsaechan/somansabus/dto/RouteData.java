package me.suhsaechan.somansabus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteData {
  private Integer disptid;
  private String caralias;
  private Boolean isShuttle;
  private String description;
  private String departureTime;
  private String station;
  private Integer busNumber;
}
