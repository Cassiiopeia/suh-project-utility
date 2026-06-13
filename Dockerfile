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

# Spring Boot 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 컨테이너 포트 지정
EXPOSE 8080

# Docker 헬스체크 — 30s 간격으로 Traefik이 컨테이너 상태를 신속하게 감지
HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=3 \
CMD curl -f http://localhost:8080/actuator/health || exit 1
