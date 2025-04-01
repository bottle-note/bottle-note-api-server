# 빌드 스테이지
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# 빌드에 필요한 파일만 복사 (의존성 캐싱 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# 애플리케이션 빌드
RUN ./gradlew build -x test -x asciidoctor -x copyRestDocs

# 실행 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app

# 시간대 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 설정 파일 디렉토리 생성
RUN mkdir -p config

# 빌드 스테이지에서 생성된 JAR 파일만 복사
COPY --from=builder /app/build/libs/*.jar /app.jar

# 환경 변수로 프로필 지정 가능하도록 설정
ENV SPRING_PROFILES_ACTIVE=default

# 실행 시 .env 파일 로드
ENTRYPOINT ["java", "-jar", "/app.jar"]
