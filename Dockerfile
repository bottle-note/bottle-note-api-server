# 빌드 스테이지
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# 빌드에 필요한 파일만 복사 (의존성 캐싱 최적화)
# 프로젝트 전체 복사
COPY . .

# 애플리케이션 빌드
RUN ./gradlew clean build -x test -x asciidoctor -x copyRestDocs

# 실행 스테이지
FROM eclipse-temurin:21-jre
WORKDIR /app

# 시간대 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 설정 파일 디렉토리 생성
RUN mkdir -p config

# 빌드 스테이지에서 생성된 JAR 파일만 복사 (legacy 모듈의 실행 가능한 JAR)
COPY --from=builder /app/bottlenote-legacy/build/libs/bottlenote-legacy-app.jar /app.jar

# 환경 변수로 프로필 지정 가능하도록 설정
ENV SPRING_PROFILES_ACTIVE=default

# 실행 시 .env.dev 파일 로드
ENTRYPOINT ["java", "-jar", "/app.jar"]
