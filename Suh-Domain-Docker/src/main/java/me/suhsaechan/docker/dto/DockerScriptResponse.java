package me.suhsaechan.docker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.suhsaechan.docker.dto.DockerScriptResponse.Data;

public class DockerScriptResponse extends me.suhsaechan.common.object.script.BaseScriptResponse<Data> {

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @ToString
  public static class Data {
    private String container; // 컨테이너 이름
    private String error;     // 에러 메시지 (실패 시)
    private String started;   // 시작된 컨테이너 이름
    private String stopped;   // 중지된 컨테이너 이름
    private String restarted; // 재시작된 컨테이너 이름
    private String status;    // 컨테이너 상태 (running, exited 등)
    private java.util.List<ContainerInfo> containers; // list 작업용

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class ContainerInfo {
      @JsonProperty("ID")
      private String id;

      @JsonProperty("Name")
      private String name;

      @JsonProperty("Image")
      private String image;

      @JsonProperty("Status")
      private String status;
    }
  }
}
