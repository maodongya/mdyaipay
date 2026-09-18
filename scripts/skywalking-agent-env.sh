# shellcheck shell=bash
# 供其它脚本 source：解析 Agent 路径与 JVM 参数（SkyWalking Java Agent）。
# 用法：source "$ROOT/scripts/skywalking-agent-env.sh" && skywalking_require_agent && JVM="$(skywalking_jvm_arguments "$ROOT" "mdyaipay-payment::local")"

skywalking_agent_home() {
  local root="${1:?root required}"
  echo "${SKYWALKING_AGENT_HOME:-$root/target/skywalking-agent}"
}

skywalking_require_agent() {
  local root="${1:?root required}"
  local jar
  jar="$(skywalking_agent_home "$root")/skywalking-agent.jar"
  if [[ ! -f "$jar" ]]; then
    echo "缺少 SkyWalking Agent: $jar" >&2
    echo "请先执行: $root/scripts/download-skywalking-agent.sh" >&2
    return 1
  fi
}

# 设置 Agent 进程环境变量（SW_AGENT_* 仅认 env，-DSW_AGENT_NAME 无效）
skywalking_export_agent_env() {
  local agent_name="${1:?SW_AGENT_NAME required}"
  export SW_AGENT_NAME="$agent_name"
  export SW_AGENT_COLLECTOR_BACKEND_SERVICES="${SW_AGENT_COLLECTOR_BACKEND_SERVICES:-127.0.0.1:11800}"
}

# 输出 spring-boot.run.jvmArguments 所需字符串（勿含外层引号）
skywalking_jvm_arguments() {
  local root="${1:?root required}"
  local agent_name="${2:?SW_AGENT_NAME required}"
  skywalking_export_agent_env "$agent_name"
  local jar
  jar="$(skywalking_agent_home "$root")/skywalking-agent.jar"
  echo "-javaagent:$jar"
}
