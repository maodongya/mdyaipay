#!/usr/bin/env python3
"""校验报警目录展开与 Bootstrap 幂等装配。"""

from __future__ import annotations

import json
import pathlib
import sys
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import bootstrap_alerts

CATALOG = pathlib.Path(__file__).resolve().parent / "alert-catalog.json"


class FakeStore:
    """内存保存 Kibana/ES 对象，便于断言创建与更新次数。"""

    def __init__(self) -> None:
        self.ilm = 0
        self.templates = 0
        self.data_views: dict[str, dict] = {}
        self.connectors: dict[str, dict] = {}
        self.rules: dict[str, dict] = {}
        self.connector_posts = 0
        self.rule_posts = 0
        self.connector_puts = 0
        self.rule_puts = 0


class FakeHandler(BaseHTTPRequestHandler):
    """模拟 Kibana 与 Elasticsearch 的最小 REST 面。"""

    protocol_version = "HTTP/1.0"
    store: FakeStore

    def log_message(self, format: str, *args) -> None:  # noqa: A003
        return

    def _send(self, code: int, body: object) -> None:
        raw = json.dumps(body).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def _read_json(self) -> dict:
        length = int(self.headers.get("Content-Length", "0"))
        if length == 0:
            return {}
        return json.loads(self.rfile.read(length).decode("utf-8"))

    def do_GET(self) -> None:  # noqa: N802
        path = urlparse(self.path).path
        if path in ("/", "/api/status"):
            self._send(200, {"status": "ok"})
            return
        if path == "/api/data_views/data_view/mdyaipay-logs":
            view = self.store.data_views.get("mdyaipay-logs")
            if view is None:
                self._send(404, {"message": "not found"})
                return
            self._send(200, view)
            return
        if path == "/api/actions/connectors":
            self._send(200, list(self.store.connectors.values()))
            return
        if path.startswith("/api/alerting/rules/_find"):
            self._send(200, {"data": list(self.store.rules.values())})
            return
        self._send(404, {"message": path})

    def do_PUT(self) -> None:  # noqa: N802
        path = urlparse(self.path).path
        body = self._read_json()
        if path == "/_ilm/policy/mdyaipay-logs":
            self.store.ilm += 1
            self._send(200, {"acknowledged": True})
            return
        if path == "/_index_template/logs-mdyaipay":
            self.store.templates += 1
            self._send(200, {"acknowledged": True})
            return
        if path.startswith("/api/actions/connector/"):
            connector_id = path.rsplit("/", 1)[-1]
            current = self.store.connectors[connector_id]
            current.update(body)
            self.store.connector_puts += 1
            self._send(200, current)
            return
        if path.startswith("/api/alerting/rule/"):
            rule_id = path.rsplit("/", 1)[-1]
            current = self.store.rules[rule_id]
            current.update(body)
            self.store.rule_puts += 1
            self._send(200, current)
            return
        self._send(404, {"message": path})

    def do_POST(self) -> None:  # noqa: N802
        path = urlparse(self.path).path
        body = self._read_json()
        if path == "/api/data_views/data_view":
            view = body.get("data_view", body)
            self.store.data_views[view["id"]] = view
            self._send(200, view)
            return
        if path == "/api/data_views/data_view/mdyaipay-logs":
            view = body.get("data_view", body)
            self.store.data_views["mdyaipay-logs"] = view
            self._send(200, view)
            return
        if path == "/api/actions/connector":
            connector_id = f"conn-{len(self.store.connectors) + 1}"
            item = {"id": connector_id, **body}
            self.store.connectors[connector_id] = item
            self.store.connector_posts += 1
            self._send(200, item)
            return
        if path == "/api/alerting/rule":
            rule_id = f"rule-{len(self.store.rules) + 1}"
            item = {"id": rule_id, **body}
            self.store.rules[rule_id] = item
            self.store.rule_posts += 1
            self._send(200, item)
            return
        self._send(404, {"message": path})


class CatalogTest(unittest.TestCase):
    """规则目录静态约束。"""

    def test_profiles_generate_23_unique_rules(self) -> None:
        for profile in ("demo", "production"):
            rules = bootstrap_alerts.load_catalog(str(CATALOG), profile)
            self.assertEqual(23, len(rules))
            self.assertEqual(23, len({rule["name"] for rule in rules}))

    def test_production_thresholds_are_not_lower(self) -> None:
        catalog = bootstrap_alerts.read_catalog(str(CATALOG))
        for rule in catalog["event_rules"]:
            self.assertGreaterEqual(
                rule["thresholds"]["production"],
                rule["thresholds"]["demo"],
            )

    def test_unknown_profile_fails(self) -> None:
        with self.assertRaises(ValueError):
            bootstrap_alerts.load_catalog(str(CATALOG), "staging")


class ReconcileTest(unittest.TestCase):
    """首次创建与重复更新不产生重复对象。"""

    def setUp(self) -> None:
        self.store = FakeStore()
        FakeHandler.store = self.store
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), FakeHandler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.base = f"http://127.0.0.1:{self.server.server_address[1]}"

    def tearDown(self) -> None:
        self.server.shutdown()
        self.server.server_close()

    def test_first_create_and_repeat_update(self) -> None:
        es = bootstrap_alerts.JsonClient(self.base)
        kibana = bootstrap_alerts.JsonClient(self.base, {"kbn-xsrf": "true"})
        first = bootstrap_alerts.reconcile(es, kibana, str(CATALOG), "demo", "")
        self.assertEqual(23, first["rules"])
        self.assertEqual(1, first["connectors"])
        self.assertEqual(1, self.store.connector_posts)
        self.assertEqual(23, self.store.rule_posts)
        second = bootstrap_alerts.reconcile(es, kibana, str(CATALOG), "demo", "")
        self.assertEqual(23, second["rules"])
        self.assertEqual(1, self.store.connector_posts)
        self.assertEqual(23, self.store.rule_posts)
        self.assertEqual(23, self.store.rule_puts)
        self.assertEqual(1, len(self.store.connectors))
        self.assertEqual(23, len(self.store.rules))


if __name__ == "__main__":
    unittest.main()
