package me.suhsaechan.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SejongAuthResponse {
	private Boolean isSuccess;
	private String authType;

	private String major;
	private String studentId;
	private String name;
	private String grade;
	private String status;

	private String email;
	private String phoneNumber;
	private String englishName;

	private Object classicReading;

	private String rawHtml;
	private String rawJson;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime authenticatedAt;

	private String errorCode;
	private String errorMessage;
}
