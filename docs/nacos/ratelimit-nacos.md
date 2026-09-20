# 限流配置接入 Nacos

## 开关

```yaml
mdyaipay:
  ratelimit:
    nacos:
      enabled: true
      server-addr: 127.0.0.1:8848
      # data-id 默认 {spring.application.name}-ratelimit.yaml
      group: DEFAULT_GROUP
```

本地 yaml 仍作**启动默认值**；Nacos 有配置时会**覆盖**同名字段（rules、default-policy、enabled 等）。

## Nacos 配置格式

`dataId` 示例：`mdyaipay-gateway-ratelimit.yaml`  
正文为 **`mdyaipay.ratelimit` 内部字段**（不要写 `mdyaipay:` 前缀），见 [`examples/`](examples/)。

## 发布示例

```bash
./scripts/run-sentinel-dashboard-docker.sh   # 含 Nacos 8848
chmod +x scripts/publish-nacos-ratelimit-config.sh
./scripts/publish-nacos-ratelimit-config.sh
```

## 与 Sentinel 的关系

| 来源 | 作用 |
|------|------|
| Nacos | 整体限流 `mdyaipay.ratelimit.*`（Redis/Redisson） |
| Sentinel `local:` | 单机 QPS |
| Sentinel `cluster:` | 动态改某条 rule 的 `limit`（仍走 Redis） |

Nacos 变更后无需重启；日志关键字 `event=ratelimit_nacos_applied`。
