#!/usr/bin/env python3
"""幂等创建 Elasticsearch ILM、索引模板与 Kibana 报警对象。"""

from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request
from typing import Any
from urllib.parse import urlencode

PROFILES = ("demo", "production")
INDEX_PATTERN = "logs-mdyaipay-*"
TIME_FIELD = "@timestamp"
TAG = "mdyaipay-kibana-alerting"


def read_catalog(path: str) -> dict[str, Any]:
    """读取规则目录 JSON。"""
    with open(path, encoding="utf-8") as handle:
        catalog = json.load(handle)
    if not isinstance(catalog, dict):
        raise ValueError("catalog must be an object")
    return catalog


def load_catalog(path: str, profile: str) -> list[dict[str, Any]]:
    """按 profile 展开 23 条可导入规则。"""
    if profile not in PROFILES:
        raise ValueError(f"unknown profile: {profile}")
    catalog = read_catalog(path)
    rules: list[dict[str, Any]] = []
    error = catalog["error_rules"]
    for service in catalog["services"]:
        rules.append(
            {
                "name": f"mdyaipay {service} ERROR surge ({profile})",
                "service": service,
                "severity": error["severity"],
                "kql": f'service.name: "{service}" and log.level: ERROR',
                "threshold": error["thresholds"][profile],
                "comparator": ">=",
                "window_minutes": error["window_minutes"],
            }
        )
    heartbeat = catalog["heartbeat"]
    window = heartbeat["window_minutes"][profile]
    rules.append(
        {
            "name": f"mdyaipay log-pipeline heartbeat missing ({profile})",
            "service": heartbeat["service"],
            "severity": heartbeat["severity"],
            "kql": (
                f'service.name: "{heartbeat["service"]}" and '
                f'event.action: {heartbeat["event"]}'
            ),
            "threshold": heartbeat["thresholds"][profile],
            "comparator": heartbeat["comparator"],
            "window_minutes": window,
        }
    )
    for item in catalog["event_rules"]:
        rules.append(
            {
                "name": f"mdyaipay {item['service']} {item['event']} ({profile})",
                "service": item["service"],
                "severity": item["severity"],
                "kql": (
                    f'service.name: "{item["service"]}" and '
                    f'event.action: {item["event"]}'
                ),
                "threshold": item["thresholds"][profile],
                "comparator": ">=",
                "window_minutes": 5,
            }
        )
    names = [rule["name"] for rule in rules]
    if len(names) != len(set(names)):
        raise ValueError("duplicate rule names")
    return rules


def action_params(connector_type_id: str, message: str) -> dict[str, Any]:
    """按连接器类型构造通知参数，避免把 Server Log 字段发给 Webhook。"""
    if connector_type_id == ".webhook":
        return {"body": json.dumps({"text": message})}
    return {"message": message, "level": "info"}


def build_rule(rule: dict[str, Any], connectors: list[dict[str, str]]) -> dict[str, Any]:
    """构造 Kibana .index-threshold 规则请求体。"""
    actions = []
    message = (
        "mdyaipay alert {rule.name} severity="
        + str(rule["severity"])
        + " service="
        + str(rule["service"])
        + " window="
        + str(rule["window_minutes"])
        + "m value={{context.value}}"
    )
    for connector in connectors:
        actions.append(
            {
                "group": "threshold met",
                "id": connector["id"],
                "params": action_params(connector["type"], message),
            }
        )
    return {
        "name": rule["name"],
        "tags": [TAG, rule["severity"], rule["service"]],
        "rule_type_id": ".index-threshold",
        "consumer": "alerts",
        "schedule": {"interval": "1m"},
        "notify_when": "onActionGroupChange",
        "params": {
            "index": [INDEX_PATTERN],
            "timeField": TIME_FIELD,
            "aggType": "count",
            "groupBy": "all",
            "timeWindowSize": rule["window_minutes"],
            "timeWindowUnit": "m",
            "thresholdComparator": rule["comparator"],
            "threshold": [rule["threshold"]],
            "filterKuery": rule["kql"],
        },
        "actions": actions,
    }


class JsonClient:
    """最小 HTTP JSON 客户端。"""

    def __init__(self, base_url: str, extra_headers: dict[str, str] | None = None) -> None:
        self.base_url = base_url.rstrip("/")
        self.extra_headers = extra_headers or {}

    def request(self, method: str, path: str, body: Any | None = None) -> Any:
        """发送 JSON 请求；4xx/5xx 抛出。"""
        data = None if body is None else json.dumps(body).encode("utf-8")
        headers = {"Content-Type": "application/json", **self.extra_headers}
        request = urllib.request.Request(
            self.base_url + path,
            data=data,
            headers=headers,
            method=method,
        )
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                raw = response.read().decode("utf-8")
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            exc.close()
            raise RuntimeError(f"{method} {path} failed: {exc.code} {detail}") from exc


def wait_ready(client: JsonClient, path: str, attempts: int = 60) -> None:
    """有限次重试直到目标返回成功。"""
    last_error = "not attempted"
    for _ in range(attempts):
        try:
            client.request("GET", path)
            return
        except Exception as exc:  # noqa: BLE001 — 就绪探测需吞掉瞬时失败
            last_error = str(exc)
            time.sleep(2)
    raise RuntimeError(f"timed out waiting for {client.base_url}{path}: {last_error}")


def ensure_elasticsearch(es: JsonClient) -> None:
    """创建或覆盖 ILM 与索引模板。"""
    es.request(
        "PUT",
        "/_ilm/policy/mdyaipay-logs",
        {
            "policy": {
                "phases": {
                    "hot": {"min_age": "0ms", "actions": {}},
                    "delete": {"min_age": "14d", "actions": {"delete": {}}},
                }
            }
        },
    )
    es.request(
        "PUT",
        "/_index_template/logs-mdyaipay",
        {
            "index_patterns": [INDEX_PATTERN],
            "template": {
                "settings": {"index.lifecycle.name": "mdyaipay-logs"},
                "mappings": {
                    "properties": {
                        "@timestamp": {"type": "date"},
                        "message": {"type": "text"},
                        "log": {"properties": {"level": {"type": "keyword"}}},
                        "service": {"properties": {"name": {"type": "keyword"}}},
                        "event": {
                            "properties": {
                                "action": {"type": "keyword"},
                                "outcome": {"type": "keyword"},
                            }
                        },
                    }
                },
            },
        },
    )


def find_named(items: list[dict[str, Any]], name: str) -> dict[str, Any] | None:
    """按 name 查找对象。"""
    for item in items:
        if item.get("name") == name:
            return item
    return None


def upsert_data_view(kibana: JsonClient) -> None:
    """按固定 id 创建或更新 Data View。"""
    body = {
        "data_view": {
            "id": "mdyaipay-logs",
            "name": "mdyaipay-logs",
            "title": INDEX_PATTERN,
            "timeFieldName": TIME_FIELD,
        }
    }
    try:
        kibana.request("GET", "/api/data_views/data_view/mdyaipay-logs")
        kibana.request("POST", "/api/data_views/data_view/mdyaipay-logs", body)
    except RuntimeError as exc:
        if "404" not in str(exc):
            raise
        kibana.request("POST", "/api/data_views/data_view", body)


def upsert_connector(
    kibana: JsonClient,
    name: str,
    connector_type_id: str,
    config: dict[str, Any],
    secrets: dict[str, Any],
) -> str:
    """按名称创建或更新连接器，返回 id。"""
    existing = kibana.request("GET", "/api/actions/connectors")
    found = find_named(existing if isinstance(existing, list) else [], name)
    payload = {
        "name": name,
        "config": config,
        "secrets": secrets,
    }
    if found:
        kibana.request("PUT", f"/api/actions/connector/{found['id']}", payload)
        return found["id"]
    created = kibana.request(
        "POST",
        "/api/actions/connector",
        {
            "name": name,
            "connector_type_id": connector_type_id,
            "config": config,
            "secrets": secrets,
        },
    )
    return created["id"]


def upsert_rule(kibana: JsonClient, payload: dict[str, Any]) -> str:
    """按名称创建或更新规则。"""
    query = urlencode({"search": payload["name"], "search_fields": "name", "per_page": 100})
    found_page = kibana.request("GET", f"/api/alerting/rules/_find?{query}")
    data = found_page.get("data", []) if isinstance(found_page, dict) else []
    exact = find_named(data, payload["name"])
    if exact:
        update = {
            "name": payload["name"],
            "tags": payload["tags"],
            "schedule": payload["schedule"],
            "params": payload["params"],
            "actions": payload["actions"],
            "notify_when": payload["notify_when"],
        }
        kibana.request("PUT", f"/api/alerting/rule/{exact['id']}", update)
        return exact["id"]
    created = kibana.request("POST", "/api/alerting/rule", payload)
    return created["id"]


def reconcile(
    es: JsonClient,
    kibana: JsonClient,
    catalog_path: str,
    profile: str,
    webhook_url: str,
) -> dict[str, int]:
    """装配 ILM、Data View、连接器与规则。"""
    ensure_elasticsearch(es)
    upsert_data_view(kibana)
    connectors = [
        {
            "id": upsert_connector(kibana, "mdyaipay-server-log", ".server-log", {}, {}),
            "type": ".server-log",
        }
    ]
    if webhook_url:
        connectors.append(
            {
                "id": upsert_connector(
                    kibana,
                    "mdyaipay-webhook",
                    ".webhook",
                    {
                        "method": "post",
                        "hasAuth": False,
                        "url": webhook_url,
                        "headers": {"Content-Type": "application/json"},
                    },
                    {},
                ),
                "type": ".webhook",
            }
        )
        print("optional webhook connector upserted", file=sys.stderr)
    else:
        print("skip optional webhook: WEBHOOK_URL empty", file=sys.stderr)
    count = 0
    for rule in load_catalog(catalog_path, profile):
        upsert_rule(kibana, build_rule(rule, connectors))
        count += 1
    return {"rules": count, "connectors": len(connectors)}


def main() -> int:
    """从环境变量读取目标并装配报警。"""
    es_url = os.environ.get("ELASTICSEARCH_URL", "").strip()
    kibana_url = os.environ.get("KIBANA_URL", "").strip()
    profile = os.environ.get("ALERT_PROFILE", "demo").strip() or "demo"
    catalog_path = os.environ.get("ALERT_CATALOG", "/config/alert-catalog.json")
    webhook_url = os.environ.get("WEBHOOK_URL", "").strip()
    if not es_url or not kibana_url:
        raise SystemExit("ELASTICSEARCH_URL and KIBANA_URL are required")
    es = JsonClient(es_url)
    kibana = JsonClient(kibana_url, {"kbn-xsrf": "true"})
    wait_ready(es, "/")
    wait_ready(kibana, "/api/status")
    result = reconcile(es, kibana, catalog_path, profile, webhook_url)
    print(json.dumps({"ok": True, **result, "profile": profile}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
