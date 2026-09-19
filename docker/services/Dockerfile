# 本地运行镜像：复制已打包的 Spring Boot 可执行 jar
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ARG APP_JAR
COPY ${APP_JAR} /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Shanghai"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
