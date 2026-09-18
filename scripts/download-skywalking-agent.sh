#!/usr/bin/env bash
# 下载 Java Agent（版本需与 docker/skywalking 中 OAP 大版本一致）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# Java Agent 与 OAP 分开发版；9.x Agent 可对接 OAP 10.2.x（协议兼容）
AGENT_VERSION="${SKYWALKING_AGENT_VERSION:-9.7.0}"
DEST="$ROOT/target/skywalking-agent"
TARBALL="$ROOT/target/apache-skywalking-java-agent-${AGENT_VERSION}.tgz"
URL="https://archive.apache.org/dist/skywalking/java-agent/${AGENT_VERSION}/apache-skywalking-java-agent-${AGENT_VERSION}.tgz"

# Spring Cloud Gateway / WebFlux 插件默认在 optional-plugins，不拷贝则只有心跳无 Trace
enable_gateway_optional_plugins() {
  local plugins="$DEST/plugins"
  local optional="$DEST/optional-plugins"
  [[ -d "$plugins" && -d "$optional" ]] || return 0
  local src
  shopt -s nullglob
  for src in \
    "$optional"/apm-spring-cloud-gateway-4.x-plugin-*.jar \
    "$optional"/apm-spring-webflux-6.x-plugin-*.jar; do
    cp -f "$src" "$plugins/"
    echo "已启用可选插件: $(basename "$src")"
  done
  shopt -u nullglob
}

mkdir -p "$ROOT/target"
if [[ -f "$DEST/skywalking-agent.jar" ]]; then
  echo "Agent 已存在: $DEST/skywalking-agent.jar"
  enable_gateway_optional_plugins
  exit 0
fi

echo "下载 SkyWalking Java Agent ${AGENT_VERSION} ..."
curl -fL "$URL" -o "$TARBALL"
rm -rf "$DEST"
mkdir -p "$DEST"
tar -xzf "$TARBALL" -C "$ROOT/target"
mv "$ROOT/target/skywalking-agent" "$DEST" 2>/dev/null || true
if [[ ! -f "$DEST/skywalking-agent.jar" ]]; then
  # 部分发行包解压目录名带版本号
  found="$(find "$ROOT/target" -maxdepth 2 -name skywalking-agent.jar | head -1)"
  if [[ -n "$found" ]]; then
    agent_home="$(dirname "$found")"
    rm -rf "$DEST"
    mv "$agent_home" "$DEST"
  fi
fi

if [[ ! -f "$DEST/skywalking-agent.jar" ]]; then
  echo "解压后未找到 skywalking-agent.jar" >&2
  exit 1
fi

LOCAL_CFG="$ROOT/docker/skywalking/mdyaipay-local.agent.config"
if [[ -f "$LOCAL_CFG" ]] && [[ -f "$DEST/config/agent.config" ]]; then
  if ! grep -q "mdyaipay-local.agent.config" "$DEST/config/agent.config" 2>/dev/null; then
    {
      echo ""
      echo "# --- mdyaipay local overrides ---"
      cat "$LOCAL_CFG"
    } >>"$DEST/config/agent.config"
  fi
fi

enable_gateway_optional_plugins
echo "Agent 路径: $DEST/skywalking-agent.jar"
