package me.suhsaechan.common.config;

import static me.suhsaechan.suhlogger.util.SuhLogger.superLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhlogger.util.SuhLogger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

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
