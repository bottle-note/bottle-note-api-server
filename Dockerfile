# 빌드 스테이지
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Gradle wrapper 및 설정 파일 복사 (의존성 캐싱 최적화)
COPY gradlew gradlew.bat gradle.properties settings.gradle build.gradle ./
COPY gradle ./gradle

# 각 모듈의 build.gradle 복사
COPY bottlenote-mono/build.gradle bottlenote-mono/
COPY bottlenote-product-api/build.gradle bottlenote-product-api/
COPY bottlenote-admin-api/build.gradle.kts bottlenote-admin-api/
COPY bottlenote-batch/build.gradle bottlenote-batch/

# 의존성만 다운로드 (이 레이어가 캐시됨!)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사
COPY . .

# 애플리케이션 빌드
RUN ./gradlew build -x test -x asciidoctor --build-cache --parallel

# 실행 스테이지
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드 메타데이터 (GitHub Actions에서 주입)
ARG GIT_COMMIT=unknown
ARG GIT_BRANCH=unknown
ARG BUILD_TIME=unknown

ENV GIT_COMMIT=${GIT_COMMIT}
ENV GIT_BRANCH=${GIT_BRANCH}
ENV BUILD_TIME=${BUILD_TIME}

# 시간대 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 설정 파일 디렉토리 생성
RUN mkdir -p config

# 빌드 스테이지에서 생성된 JAR 파일만 복사 (product-api 모듈의 실행 가능한 JAR)
COPY --from=builder /app/bottlenote-product-api/build/libs/bottlenote-product-api.jar /app.jar

# 환경 변수로 프로필 지정 가능하도록 설정
ENV SPRING_PROFILES_ACTIVE=default

# 실행 시 .env.dev 파일 로드
ENTRYPOINT ["java", "-jar", "/app.jar"]
