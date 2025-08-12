# OpenJDK 17 이미지
FROM openjdk:17-jdk-slim

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일을 복사 (Suh-Web bootJar 산출물 고정 파일명)
COPY Suh-Web/build/libs/app.jar app.jar

# 환경 변수 설정 (서울)
ENV TZ=Asia/Seoul

# Spring Boot 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 컨테이너 포트 지정
EXPOSE 8080
