# 1. 런타임 환경: 가벼운 알파인(Alpine) 리눅스 + Java 21 설정
FROM eclipse-temurin:21-jre-alpine

# 2. 컨테이너 내 작업 디렉토리 설정
WORKDIR /app

# 3. Github Actions가 빌드해 둔 jar 파일을 컨테이너 내부로 복사
COPY build/libs/*SNAPSHOT.jar app.jar

# 4. 외부로 노출할 포트
EXPOSE 8080

# 5. Spring Boot 서버 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]