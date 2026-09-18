#!/usr/bin/env bash
# 带 SkyWalking Agent 启动单个 Spring Boot 模块
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=skywalking-agent-env.sh
source "$ROOT/scripts/skywalking-agent-env.sh"

MODULE="${1:-mdyaipay-payment}"
SW_ENV="${SW_AGENT_ENV:-local}"
DEFAULT_NAME="$MODULE::$SW_ENV"
export SW_AGENT_NAME="${SW_AGENT_NAME:-$DEFAULT_NAME}"

if ! skywalking_require_agent "$ROOT"; then
  "$ROOT/scripts/download-skywalking-agent.sh"
  skywalking_require_agent "$ROOT"
fi

JVM="$(skywalking_jvm_arguments "$ROOT" "$SW_AGENT_NAME")"
cd "$ROOT"
exec mvn -pl "$MODULE" -am spring-boot:run -Dspring-boot.run.jvmArguments="$JVM"
