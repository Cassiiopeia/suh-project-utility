package me.suhsaechan.common.util;

import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.SystemType;

/**
 * 파일 관련 유틸리티 클래스
 */

@Slf4j
public class FileUtil {

  /**
   * 시스템 운영체제 확인 (MAC, WINDOWS, LINUX, OTHER)
   * - WebP 이미지 지원 여부 체크용
   * @return SystemType 운영체제 타입
   */
  public static SystemType getCurrentSystem() {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) {
      return SystemType.WINDOWS;
    } else if (osName.contains("mac")) {
      return SystemType.MAC;
    } else if (osName.contains("nux") || osName.contains("nix")) {
      return SystemType.LINUX;
    } else {
      return SystemType.OTHER;
    }
  }

}
