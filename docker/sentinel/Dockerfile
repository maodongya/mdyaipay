# Sentinel Dashboard 1.8.8；jar 由 scripts/run-sentinel-dashboard-docker.sh 预先下载到 jars/
ARG BASE_IMAGE=mdyaipay-user:local
FROM ${BASE_IMAGE}
ARG SENTINEL_DASHBOARD_VERSION=1.8.8
COPY jars/sentinel-dashboard-${SENTINEL_DASHBOARD_VERSION}.jar /sentinel-dashboard.jar
EXPOSE 8858
ENTRYPOINT ["java", "-jar", "/sentinel-dashboard.jar", "--server.port=8858"]
