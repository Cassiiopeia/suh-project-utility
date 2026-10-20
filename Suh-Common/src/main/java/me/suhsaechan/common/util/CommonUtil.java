package me.suhsaechan.common.util;


public class CommonUtil {

  /**
   * IP 주소를 익명화하여 반환합니다.
   * IPv4 주소의 경우 마지막 옥텟을 'x'로 변경합니다.
   * 
   * @param ipAddress 원본 IP 주소
   * @return 익명화된 IP 주소
   */
  public static String anonymizeIpAddress(String ipAddress) {
    if (ipAddress == null || ipAddress.isEmpty()) {
      return "알 수 없음";
    }
    
    // IPv4 형식인 경우 (예: 192.168.0.1)
    String[] parts = ipAddress.split("\\.");
    if (parts.length == 4) {
      return parts[0] + "." + parts[1] + "." + parts[2] + ".x";
    }
    
    // 다른 형식의 IP인 경우 마지막 문자만 x로 변경
    return ipAddress.replaceAll(".$", "x");
  }

  /**
   * Object 값이 null이면 기본값(빈 문자열) 반환
   * - null → 빈 문자열 반환
   * - "null" 문자열 → 빈 문자열 반환
   * - 빈 문자열/공백 → 빈 문자열 반환
   *
   * @param obj 검증할 객체
   * @return 문자열 변환 값 또는 빈 문자열
   */
  public static String nvl(Object obj) {
    return nvl(obj, "");
  }

  /**
   * Object 값이 null이면 기본값 반환
   * - null → 기본값 반환
   * - "null" 문자열 → 기본값 반환
   * - 빈 문자열/공백 → 기본값 반환
   *
   * @param obj 검증할 객체
   * @param defaultValue obj가 null인 경우 반환할 기본값
   * @return 문자열 변환 값 또는 기본값
   */
  public static String nvl(Object obj, String defaultValue) {
    if (obj == null) {
      return defaultValue;
    }

    String str = obj.toString();
    if (str.equals("null") || str.isBlank()) {
      return defaultValue;
    }

    return str;
  }

}

