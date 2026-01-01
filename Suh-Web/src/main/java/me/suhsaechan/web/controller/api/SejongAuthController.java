package me.suhsaechan.web.controller.api;

import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import kr.suhsaechan.sejong.auth.model.SejongAuthResult;
import kr.suhsaechan.sejong.auth.model.SejongDhcAuthResult;
import kr.suhsaechan.sejong.auth.model.SejongSisAuthResult;
import kr.suhsaechan.sejong.auth.service.SuhSejongAuthEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.web.dto.SejongAuthRequest;
import me.suhsaechan.web.dto.SejongAuthResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/sejong-auth")
@RequiredArgsConstructor
public class SejongAuthController {

	private final SuhSejongAuthEngine authEngine;

	@PostMapping(value = "/authenticate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@LogMonitor
	public ResponseEntity<SejongAuthResponse> authenticate(@ModelAttribute SejongAuthRequest request) {
		String authType = request.getAuthType();
		if (authType == null || authType.isBlank()) {
			authType = "INTEGRATED";
		}

		try {
			return switch (authType) {
				case "INTEGRATED" -> handleIntegratedAuth(request);
				case "DHC" -> handleDhcAuth(request);
				case "SIS" -> handleSisAuth(request);
				case "DHC_RAW" -> handleDhcRawAuth(request);
				case "SIS_RAW" -> handleSisRawAuth(request);
				default -> handleIntegratedAuth(request);
			};
		} catch (SejongAuthException e) {
			log.warn("세종대 인증 실패: {}", e.getMessage());
			return ResponseEntity.ok(SejongAuthResponse.builder()
					.isSuccess(false)
					.authType(authType)
					.errorCode(e.getErrorCode().name())
					.errorMessage(e.getMessage())
					.build());
		} catch (Exception e) {
			log.error("세종대 인증 중 예외 발생: {}", e.getMessage(), e);
			return ResponseEntity.ok(SejongAuthResponse.builder()
					.isSuccess(false)
					.authType(authType)
					.errorCode("UNKNOWN_ERROR")
					.errorMessage("인증 처리 중 오류가 발생했습니다.")
					.build());
		}
	}

	private ResponseEntity<SejongAuthResponse> handleIntegratedAuth(SejongAuthRequest request) {
		SejongAuthResult result = authEngine.authenticate(request.getStudentId(), request.getPassword());
		return ResponseEntity.ok(SejongAuthResponse.builder()
				.isSuccess(result.isSuccess())
				.authType("INTEGRATED")
				.major(result.getMajor())
				.studentId(result.getStudentId())
				.name(result.getName())
				.grade(result.getGrade())
				.status(result.getStatus())
				.email(result.getEmail())
				.phoneNumber(result.getPhoneNumber())
				.englishName(result.getEnglishName())
				.classicReading(result.getClassicReading())
				.authenticatedAt(result.getAuthenticatedAt())
				.build());
	}

	private ResponseEntity<SejongAuthResponse> handleDhcAuth(SejongAuthRequest request) {
		SejongDhcAuthResult result = authEngine.authenticateWithDHC(request.getStudentId(), request.getPassword());
		return ResponseEntity.ok(SejongAuthResponse.builder()
				.isSuccess(result.isSuccess())
				.authType("DHC")
				.major(result.getMajor())
				.studentId(result.getStudentId())
				.name(result.getName())
				.grade(result.getGrade())
				.status(result.getStatus())
				.classicReading(result.getClassicReading())
				.authenticatedAt(result.getAuthenticatedAt())
				.build());
	}

	private ResponseEntity<SejongAuthResponse> handleSisAuth(SejongAuthRequest request) {
		SejongSisAuthResult result = authEngine.authenticateWithSIS(request.getStudentId(), request.getPassword());
		return ResponseEntity.ok(SejongAuthResponse.builder()
				.isSuccess(result.isSuccess())
				.authType("SIS")
				.major(result.getMajor())
				.studentId(result.getStudentId())
				.name(result.getName())
				.email(result.getEmail())
				.phoneNumber(result.getPhoneNumber())
				.englishName(result.getEnglishName())
				.authenticatedAt(result.getAuthenticatedAt())
				.build());
	}

	private ResponseEntity<SejongAuthResponse> handleDhcRawAuth(SejongAuthRequest request) {
		SejongDhcAuthResult result = authEngine.authenticateWithDHCRaw(request.getStudentId(), request.getPassword());
		return ResponseEntity.ok(SejongAuthResponse.builder()
				.isSuccess(result.isSuccess())
				.authType("DHC_RAW")
				.major(result.getMajor())
				.studentId(result.getStudentId())
				.name(result.getName())
				.grade(result.getGrade())
				.status(result.getStatus())
				.classicReading(result.getClassicReading())
				.rawHtml(result.getRawHtml())
				.authenticatedAt(result.getAuthenticatedAt())
				.build());
	}

	private ResponseEntity<SejongAuthResponse> handleSisRawAuth(SejongAuthRequest request) {
		SejongSisAuthResult result = authEngine.authenticateWithSISRaw(request.getStudentId(), request.getPassword());
		return ResponseEntity.ok(SejongAuthResponse.builder()
				.isSuccess(result.isSuccess())
				.authType("SIS_RAW")
				.major(result.getMajor())
				.studentId(result.getStudentId())
				.name(result.getName())
				.email(result.getEmail())
				.phoneNumber(result.getPhoneNumber())
				.englishName(result.getEnglishName())
				.rawJson(result.getRawJson())
				.authenticatedAt(result.getAuthenticatedAt())
				.build());
	}
}
