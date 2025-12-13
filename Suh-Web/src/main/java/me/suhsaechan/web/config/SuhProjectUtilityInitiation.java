package me.suhsaechan.web.config;

import static me.suhsaechan.suhlogger.util.SuhLogger.superLog;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

	@Override
	public void run(ApplicationArguments args) throws Exception {
		loadVersionInfo();
		SuhLogger.lineLog("서버 시작");
		superLog(serverInfo);
	}

	/**
	 * version.yml에서 버전 정보를 읽어 ServerInfo에 캐시
	 */
	private void loadVersionInfo() {
		try {
			// 프로젝트 루트의 version.yml 파일 경로
			Path versionFilePath = Paths.get(System.getProperty("user.dir"), "version.yml");

			InputStream inputStream;
			if (Files.exists(versionFilePath)) {
				// 프로젝트 루트에서 읽기
				inputStream = Files.newInputStream(versionFilePath);
				log.info("version.yml 로드: {}", versionFilePath);
			} else {
				// classpath에서 읽기 시도
				ClassPathResource resource = new ClassPathResource("version.yml");
				if (resource.exists()) {
					inputStream = resource.getInputStream();
					log.info("version.yml 로드: classpath:version.yml");
				} else {
					log.warn("version.yml 파일을 찾을 수 없습니다. 기본값 사용");
					return;
				}
			}

			Yaml yaml = new Yaml();
			Map<String, Object> data = yaml.load(inputStream);
			inputStream.close();

			if (data != null && data.containsKey("version")) {
				String version = String.valueOf(data.get("version"));
				serverInfo.setAppVersion(version);
				log.info("버전 정보 로드 완료: v{}", version);
			}

		} catch (Exception e) {
			log.error("version.yml 로드 실패, 기본값 사용: {}", e.getMessage());
		}
	}
}
