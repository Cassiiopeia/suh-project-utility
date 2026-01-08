package me.suhsaechan.web.controller.api;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;
import me.suhsaechan.statistics.service.StatisticsService;
import me.suhsaechan.suhnicknamegenerator.core.SuhRandomKit;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.web.dto.RandomNicknameRequest;
import me.suhsaechan.web.dto.RandomNicknameResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/suh-random")
@RequiredArgsConstructor
public class RandomNicknameController {

	private final StatisticsService statisticsService;

	@PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@LogMonitor
	public ResponseEntity<RandomNicknameResponse> generateNickname(@ModelAttribute RandomNicknameRequest request) {
		String nicknameType = request.getNicknameType();
		if (nicknameType == null || nicknameType.isBlank()) {
			nicknameType = "SIMPLE";
		}

		String locale = request.getLocale();
		if (locale == null || locale.isBlank()) {
			locale = "ko";
		}

		Integer numberLength = request.getNumberLength();
		if (numberLength == null || numberLength < 1 || numberLength > 10) {
			numberLength = 4;
		}

		Integer uuidLength = request.getUuidLength();
		if (uuidLength == null || uuidLength < 1 || uuidLength > 10) {
			uuidLength = 4;
		}

		Boolean isAdultConsent = Boolean.TRUE.equals(request.getIsAdultConsent());

		if (nicknameType.startsWith("MATURE") && !isAdultConsent) {
			throw new IllegalArgumentException("성인용 콘텐츠 사용을 위해서는 동의가 필요합니다.");
		}

		SuhRandomKit.SuhRandomKitBuilder builder = SuhRandomKit.builder()
				.locale(locale)
				.numberLength(numberLength)
				.uuidLength(uuidLength);

		if (isAdultConsent) {
			builder.enableAdultContent(true);
		}

		SuhRandomKit generator = builder.build();

		String nickname = generateByType(generator, nicknameType, numberLength, uuidLength);

		statisticsService.logFeatureUsageAsync(FeatureType.SUH_RANDOM, null, nicknameType);

		return ResponseEntity.ok(RandomNicknameResponse.builder()
				.nickname(nickname)
				.nicknameType(nicknameType)
				.locale(locale)
				.numberLength(numberLength)
				.uuidLength(uuidLength)
				.generatedAt(LocalDateTime.now())
				.build());
	}

	private String generateByType(SuhRandomKit generator, String nicknameType, Integer numberLength, Integer uuidLength) {
		return switch (nicknameType) {
			case "SIMPLE" -> generator.simpleNickname();
			case "NUMBER" -> generator.nicknameWithNumber(numberLength);
			case "UUID" -> generator.nicknameWithUuid(uuidLength);
			case "POLITICIAN" -> generator.politicianNickname();
			case "POLITICIAN_NUMBER" -> generator.politicianNicknameWithNumber(numberLength);
			case "POLITICIAN_UUID" -> generator.politicianNicknameWithUuid(uuidLength);
			case "MATURE" -> generator.matureNickname();
			case "MATURE_NUMBER" -> generator.matureNicknameWithNumber(numberLength);
			case "MATURE_UUID" -> generator.matureNicknameWithUuid(uuidLength);
			default -> generator.simpleNickname();
		};
	}
}
