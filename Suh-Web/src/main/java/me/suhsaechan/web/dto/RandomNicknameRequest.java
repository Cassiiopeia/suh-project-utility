package me.suhsaechan.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RandomNicknameRequest {
	private String nicknameType;
	private String locale;
	private Integer numberLength;
	private Integer uuidLength;
	private Boolean isAdultConsent;
}
