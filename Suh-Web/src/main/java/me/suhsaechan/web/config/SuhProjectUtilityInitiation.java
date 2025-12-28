package me.suhsaechan.web.config;

import static me.suhsaechan.suhlogger.util.SuhLogger.superLog;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.suhlogger.util.SuhLogger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuhProjectUtilityInitiation implements ApplicationRunner {
	private final ServerInfo serverInfo;
	private final ServerOptionService serverOptionService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		SuhLogger.lineLog("서버 시작");
		superLog(serverInfo);
		
		// 서버 옵션 기본값 초기화
		try {
			log.info("서버 옵션 초기화 시작");
			serverOptionService.initializeDefaultOptions();
			log.info("서버 옵션 초기화 완료");
		} catch (Exception e) {
			log.warn("서버 옵션 초기화 실패 (계속 진행): {}", e.getMessage());
		}
	}
}
