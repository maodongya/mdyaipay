# shellcheck shell=bash
# 打包 jar 并构建 mdyaipay-user/payment/gateway/sentinel 本地镜像（source 使用）

mdyaipay_build_app_images() {
  local root=$1
  local version="${MDYAIPAY_VERSION:-1.0.0-SNAPSHOT}"
  local jar_dir="$root/docker/services/jars"
  local sentinel_jar_dir="$root/docker/sentinel/jars"
  local sentinel_ver="${SENTINEL_DASHBOARD_VERSION:-1.8.8}"

  echo "package user / payment / gateway ..."
  mvn -f "$root/pom.xml" -pl mdyaipay-user,mdyaipay-payment,mdyaipay-gateway -am package -DskipTests

  mkdir -p "$jar_dir"
  cp "$root/mdyaipay-user/target/mdyaipay-user-${version}-boot.jar" "$jar_dir/user.jar"
  cp "$root/mdyaipay-payment/target/mdyaipay-payment-${version}-boot.jar" "$jar_dir/payment.jar"
  cp "$root/mdyaipay-gateway/target/mdyaipay-gateway-${version}-boot.jar" "$jar_dir/gateway.jar"

  echo "docker build 应用镜像 ..."
  docker build -f "$root/docker/services/Dockerfile" \
    --build-arg APP_JAR=jars/user.jar \
    -t mdyaipay-user:local "$root/docker/services"
  docker build -f "$root/docker/services/Dockerfile" \
    --build-arg APP_JAR=jars/payment.jar \
    -t mdyaipay-payment:local "$root/docker/services"
  docker build -f "$root/docker/services/Dockerfile" \
    --build-arg APP_JAR=jars/gateway.jar \
    -t mdyaipay-gateway:local "$root/docker/services"

  mkdir -p "$sentinel_jar_dir"
  local sentinel_jar="$sentinel_jar_dir/sentinel-dashboard-${sentinel_ver}.jar"
  if [[ ! -f "$sentinel_jar" ]]; then
    echo "下载 sentinel-dashboard-${sentinel_ver}.jar ..."
    curl -fL -o "$sentinel_jar" \
      "https://github.com/alibaba/Sentinel/releases/download/${sentinel_ver}/sentinel-dashboard-${sentinel_ver}.jar"
  fi
  docker build -f "$root/docker/sentinel/Dockerfile" \
    --build-arg BASE_IMAGE=mdyaipay-user:local \
    --build-arg SENTINEL_DASHBOARD_VERSION="$sentinel_ver" \
    -t mdyaipay-sentinel-dashboard:local "$root/docker/sentinel"
}
