package me.suhsaechan.web.controller.api;

import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import kr.suhsaechan.sejong.auth.model.SejongAuthResult;
import kr.suhsaechan.sejong.auth.model.SejongDhcAuthResult;
import kr.suhsaechan.sejong.auth.model.SejongSisAuthResult;
import kr.suhsaechan.sejong.auth.service.SuhSejongAuthEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;
import me.suhsaechan.statistics.service.StatisticsService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.web.dto.SejongAuthRequest;
import me.suhsaechan.web.dto.SejongAuthResponse;
import me.suhsaechan.web.service.SejongAuthAttemptLimiter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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

	// 학번별 연속 실패 차단 시 응답 코드 (라이브러리 코드 외 lab 전용)
	private static final String TOO_MANY_ATTEMPTS_ERROR_CODE = "TOO_MANY_ATTEMPTS";
	// messages_*.properties: sejongAuth.error.{코드 camelCase}
	private static final String ERROR_MESSAGE_KEY_PREFIX = "sejongAuth.error.";

	private final SuhSejongAuthEngine authEngine;
	private final StatisticsService statisticsService;
	private final SejongAuthAttemptLimiter attemptLimiter;
	private final MessageSource messageSource;

	@PostMapping(value = "/authenticate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@LogMonitor
	public ResponseEntity<SejongAuthResponse> authenticate(@ModelAttribute SejongAuthRequest request) {
		String authType = request.getAuthType();
		if (authType == null || authType.isBlank()) {
			authType = "INTEGRATED";
		}

		// 같은 학번 연속 실패 > 포털 호출 없이 거절 (포털 계정 잠금 방지)
		if (attemptLimiter.isBlocked(request.getStudentId())) {
			log.warn("[sejong auth] rejected : too many failed attempts");
			return failureResponse(authType, TOO_MANY_ATTEMPTS_ERROR_CODE, "Too many failed attempts. Try again in 30 minutes.");
		}

		try {
			ResponseEntity<SejongAuthResponse> response = switch (authType) {
				case "INTEGRATED" -> handleIntegratedAuth(request);
				case "DHC" -> handleDhcAuth(request);
				case "SIS" -> handleSisAuth(request);
				case "DHC_RAW" -> handleDhcRawAuth(request);
				case "SIS_RAW" -> handleSisRawAuth(request);
				default -> handleIntegratedAuth(request);
			};

			SejongAuthResponse body = response.getBody();
			if (body != null && Boolean.TRUE.equals(body.getIsSuccess())) {
				attemptLimiter.recordSuccess(request.getStudentId());
				statisticsService.logFeatureUsageAsync(FeatureType.SEJONG_AUTH, null, authType);
			}

			return response;
		} catch (SejongAuthException e) {
			log.warn("세종대 인증 실패: {}", e.getMessage());
			// 비밀번호 불일치만 실패 횟수에 포함 (포털 잠금 카운트와 같은 기준)
			if (e.getErrorCode() == SejongAuthErrorCode.AUTHENTICATION_FAILED) {
				attemptLimiter.recordFailure(request.getStudentId());
			}
			return failureResponse(authType, e.getErrorCode().name(), e.getMessage());
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

	/**
	 * 실패 응답: 에러 코드 > 화면 언어 메시지 (없으면 기본 메시지)
	 */
	private ResponseEntity<SejongAuthResponse> failureResponse(String authType, String errorCode, String defaultMessage) {
		return ResponseEntity.ok(SejongAuthResponse.builder()
				.isSuccess(false)
				.authType(authType)
				.errorCode(errorCode)
				.errorMessage(localizedErrorMessage(errorCode, defaultMessage))
				.build());
	}

	/**
	 * 에러 코드(ACCOUNT_LOCKED) > 메시지 키(sejongAuth.error.accountLocked)
	 */
	private String localizedErrorMessage(String errorCode, String defaultMessage) {
		StringBuilder camelCaseCode = new StringBuilder();
		for (String word : errorCode.toLowerCase().split("_")) {
			camelCaseCode.append(camelCaseCode.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1));
		}
		return messageSource.getMessage(ERROR_MESSAGE_KEY_PREFIX + camelCaseCode, null, defaultMessage, LocaleContextHolder.getLocale());
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
