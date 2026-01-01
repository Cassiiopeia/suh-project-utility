package me.suhsaechan.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SejongAuthRequest {
	private String studentId;
	private String password;
	private String authType;
}
