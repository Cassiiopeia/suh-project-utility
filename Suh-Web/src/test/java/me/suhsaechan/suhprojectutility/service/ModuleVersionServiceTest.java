package me.suhsaechan.suhprojectutility.service;

import static me.suhsaechan.suhlogger.util.SuhLogger.lineLog;
import static me.suhsaechan.suhlogger.util.SuhLogger.superLog;
import static me.suhsaechan.suhlogger.util.SuhLogger.timeLog;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.ModuleType;
import me.suhsaechan.common.constant.ModuleUpdateType;
import me.suhsaechan.common.entity.ModuleVersion;
import me.suhsaechan.common.entity.ModuleVersionUpdate;
import me.suhsaechan.common.repository.ModuleVersionRepository;
import me.suhsaechan.common.repository.ModuleVersionUpdateRepository;
import me.suhsaechan.module.service.ModuleVersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
//@Transactional
@Slf4j
class ModuleVersionServiceTest {
  @Autowired
  ModuleVersionService moduleVersionService;
  @Autowired
  ModuleVersionRepository moduleVersionRepository;
  @Autowired
  ModuleVersionUpdateRepository moduleVersionUpdateRepository;

  @Test
  public void mainTest() {
    timeLog(this::번역기_버전_업로드_테스트);
  }

  void 번역기_버전_업로드_테스트() {
    lineLog("번역기 버전 및 업데이트 정보 생성 테스트");

    // 버전 1.1.0 생성 (최신 버전)
    ModuleVersion version110 = moduleVersionRepository.save(
        ModuleVersion.builder()
            .moduleType(ModuleType.TRANSLATOR)
            .versionNumber("1.1.0")
            .releaseDate(LocalDate.of(2025, 3, 16))
            .isLatest(true)
            .build()
    );
    superLog(version110);

    // 버전 1.0.0 생성
    ModuleVersion version100 = moduleVersionRepository.save(
        ModuleVersion.builder()
            .moduleType(ModuleType.TRANSLATOR)
            .versionNumber("1.0.0")
            .releaseDate(LocalDate.of(2025, 3, 15))
            .isLatest(false)
            .build()
    );
    superLog(version100);

    // 버전 1.1.0의 업데이트 항목 추가
    ModuleVersionUpdate update1 = moduleVersionUpdateRepository.save(
        ModuleVersionUpdate.builder()
            .moduleVersion(version110)
            .updateTitle("성능 개선")
            .updateDescription("평균 응답시간: 22초 → 7초 (68% 개선)")
            .moduleUpdateType(ModuleUpdateType.PERFORMANCE)
            .build()
    );
    superLog(update1);

    ModuleVersionUpdate update2 = moduleVersionUpdateRepository.save(
        ModuleVersionUpdate.builder()
            .moduleVersion(version110)
            .updateTitle("번역 품질 향상")
            .updateDescription("번역 엔진 최적화 및 정확도 개선")
            .moduleUpdateType(ModuleUpdateType.OPTIMIZATION)
            .build()
    );
    superLog(update2);

    // 버전 1.0.0의 업데이트 항목 추가
    ModuleVersionUpdate update3 = moduleVersionUpdateRepository.save(
        ModuleVersionUpdate.builder()
            .moduleVersion(version100)
            .updateTitle("출시")
            .updateDescription("SUH-AI 번역기 서비스 시작")
            .moduleUpdateType(ModuleUpdateType.RELEASE)
            .build()
    );
    superLog(update3);

    lineLog("모든 데이터 생성 완료");
  }
}