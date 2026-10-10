# OpenJDK 17 이미지
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# HEALTHCHECK용 curl 설치
RUN apk add --no-cache curl

# 빌드된 JAR 파일을 복사 (Suh-Web bootJar 고정 파일명)
COPY Suh-Web/build/libs/app.jar app.jar

# 환경 변수 설정 (서울)
ENV TZ=Asia/Seoul

# 세종대 포털 TLS 호환: 세종대 포털은 레거시 cipher(TLS_RSA_WITH_AES_256_CBC_SHA)만 지원하나
# JDK17 Temurin은 jdk.tls.disabledAlgorithms 기본값에 TLS_RSA_* 를 포함해 이를 차단 → handshake_failure.
# 이 값은 JVM이 SSLContext를 최초 초기화할 때 한 번만 읽어 캐싱하므로 앱 코드로는 우회할 수 없다.
# 빌드 시점에 java.security 파일에서 TLS_RSA_* 항목만 제거한다 (다른 보안 제약은 그대로 유지).
RUN JS="$JAVA_HOME/conf/security/java.security" \
    && sed -i -E 's/TLS_RSA_\*,[[:space:]]*//g; s/,[[:space:]]*TLS_RSA_\*//g' "$JS" \
    && echo "세종대 TLS 호환: java.security에서 TLS_RSA_* 제거 완료"

# Spring Boot 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 컨테이너 포트 지정
EXPOSE 8080

# Docker 헬스체크 — 30s 간격으로 Traefik이 컨테이너 상태를 신속하게 감지
HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=3 \
CMD curl -f http://localhost:8080/actuator/health || exit 1
