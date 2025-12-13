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
		SuhLogger.lineLog("서버 시작");
		superLog(serverInfo);
	}
}
