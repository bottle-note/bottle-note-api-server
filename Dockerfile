FROM eclipse-temurin:21-jre
WORKDIR /app

ARG GIT_COMMIT=unknown
ARG GIT_BRANCH=unknown
ARG BUILD_TIME=unknown

ENV GIT_COMMIT=${GIT_COMMIT}
ENV GIT_BRANCH=${GIT_BRANCH}
ENV BUILD_TIME=${BUILD_TIME}
ENV TZ=Asia/Seoul

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN mkdir -p config

COPY bottlenote-product-api/build/libs/bottlenote-product-api.jar /app.jar

ENV SPRING_PROFILES_ACTIVE=default

ENTRYPOINT ["java", "-jar", "/app.jar"]
