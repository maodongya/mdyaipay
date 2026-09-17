#!/usr/bin/env python3
"""创建 ENABLED 压测商户：凭证经 user 落库 merchant_api_credential（AES），并写 JSON 供压测读明文 secret。"""
from __future__ import annotations

import hashlib
import hmac
import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid

USER_BASE = os.environ.get("USER_BASE_URL", "http://127.0.0.1:8082")
OUT_FILE = os.environ.get(
    "LOADTEST_MERCHANT_CREDENTIALS_FILE",
    "target/loadtest-merchant-credentials.json",
)
OWNER_USER_ID = int(os.environ.get("LOADTEST_OWNER_USER_ID", "80001"))


def post(path: str, body: dict) -> dict:
    url = USER_BASE.rstrip("/") + path
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    if payload.get("code") != 0:
        raise RuntimeError(f"POST {path} failed: {payload}")
    return payload.get("data") or {}


def sign_envelope(app_key: str, secret: str, business: dict[str, str]) -> dict:
    ts = int(time.time() * 1000)
    nonce = uuid.uuid4().hex
    params = {
        "app_key": app_key,
        "timestamp": str(ts),
        "nonce": nonce,
        "sign_method": "HMAC_SHA256",
    }
    params.update(business)
    canonical = "&".join(f"{k}={params[k]}" for k in sorted(params.keys()) if k != "sign")
    sig = hmac.new(secret.encode(), canonical.encode(), hashlib.sha256).hexdigest()
    return {
        "appKey": app_key,
        "timestampMillis": ts,
        "nonce": nonce,
        "signMethod": "HMAC_SHA256",
        "sign": sig,
    }


def main() -> int:
    name = os.environ.get("LOADTEST_MERCHANT_NAME", "loadtest-merchant")
    print(f"User API: {USER_BASE}")

    created = post("/api/v1/merchants", {"merchantName": name, "ownerUserId": OWNER_USER_ID})
    merchant_id = int(created["merchantId"])
    print(f"merchantId={merchant_id}")

    cred = post(
        "/api/v1/merchants/credentials/issue",
        {"merchantId": merchant_id, "operator": "loadtest-seed"},
    )
    app_key = cred["appKey"]
    app_secret = cred["appSecret"]
    print(f"appKey={app_key}")
    print("credential persisted: mdyaipay_user.merchant_api_credential (secret_cipher AES)")

    audit_sig = sign_envelope(
        app_key,
        app_secret,
        {
            "merchant_id": str(merchant_id),
            "operator_user_id": str(OWNER_USER_ID),
        },
    )
    post(
        "/api/v1/merchants/audit/submit",
        {
            "signature": audit_sig,
            "merchantId": merchant_id,
            "operatorUserId": OWNER_USER_ID,
        },
    )
    post(
        "/api/v1/merchants/audit/approve",
        {"merchantId": merchant_id, "auditor": "loadtest-seed"},
    )
    print("merchant ENABLED")

    out = {
        "merchantId": merchant_id,
        "appKey": app_key,
        "appSecret": app_secret,
        "ownerUserId": OWNER_USER_ID,
    }
    os.makedirs(os.path.dirname(OUT_FILE) or ".", exist_ok=True)
    with open(OUT_FILE, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)
        f.write("\n")
    print(f"wrote {OUT_FILE}")
    print("export LOADTEST_MERCHANT_APP_KEY=" + app_key)
    print("export LOADTEST_MERCHANT_APP_SECRET=" + app_secret)
    print("export LOADTEST_MERCHANT_ID=" + str(merchant_id))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.URLError as ex:
        print(f"user service unreachable: {ex}", file=sys.stderr)
        sys.exit(1)
