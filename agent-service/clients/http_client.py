from __future__ import annotations

from typing import Any

import requests

from config.settings import get_backend_base_url


TIMEOUT = 12


def _headers(authorization: str | None) -> dict[str, str]:
    """构造后端请求头，按需附带 authorization。"""
    headers: dict[str, str] = {}
    if authorization:
        headers["authorization"] = authorization
    return headers


def call_backend(
    method: str,
    path: str,
    *,
    params: dict[str, Any] | None = None,
    json_body: dict[str, Any] | None = None,
    authorization: str | None = None,
) -> dict[str, Any]:
    """统一调用后端 HTTP 接口并返回标准化响应结构。"""
    url = f"{get_backend_base_url()}{path}"
    try:
        response = requests.request(
            method=method,
            url=url,
            params=params,
            json=json_body,
            headers=_headers(authorization),
            timeout=TIMEOUT,
        )
    except requests.RequestException as e:
        return {
            "status_code": 0,
            "ok": False,
            "url": url,
            "response": {"error": str(e)},
        }

    content_type = response.headers.get("Content-Type", "")
    data: Any
    if "application/json" in content_type:
        try:
            data = response.json()
        except ValueError:
            data = {"raw": response.text}
    else:
        data = {"raw": response.text}

    return {
        "status_code": response.status_code,
        "ok": response.ok,
        "url": str(response.url),
        "response": data,
    }
