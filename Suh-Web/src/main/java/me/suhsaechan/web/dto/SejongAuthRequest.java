package me.suhsaechan.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SejongAuthRequest {
	private String studentId;
	// @LogMonitor 가 인자를 toString 으로 로그에 남김 > 비밀번호 제외 (#263)
	@ToString.Exclude
	private String password;
	private String authType;
}
