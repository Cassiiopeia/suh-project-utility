package me.suhsaechan.common.object.script;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BaseScriptResponse<T> {
  @JsonProperty("result")
  private String result;  // "SUCCESS" 또는 "FAIL"


  @JsonProperty("data")
  private T data;         // 작업별 상세 데이터 (제네릭으로 유연성 확보)
}
