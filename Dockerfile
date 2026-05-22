# OpenJDK 17 이미지
FROM eclipse-temurin:17-jre-alpine

# wget 설치 (HEALTHCHECK 및 Traefik healthcheck 용)
RUN apk add --no-cache wget

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일을 복사 (Suh-Web bootJar 고정 파일명)
COPY Suh-Web/build/libs/app.jar app.jar

# 환경 변수 설정 (서울)
ENV TZ=Asia/Seoul

# Docker 자체 헬스체크 (Traefik healthcheck 와 이중 안전망)
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# Spring Boot 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 컨테이너 포트 지정
EXPOSE 8080
