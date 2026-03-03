from __future__ import annotations

import os
from pathlib import Path
from typing import Any

import yaml


PROJECT_ROOT = Path(__file__).resolve().parents[1] # agent-service
APP_YAML_PATH = Path(__file__).resolve().parent / "application.yaml"


def _read_yaml(path: Path) -> dict[str, Any]:
    """读取 YAML 配置文件并返回字典，文件不存在或格式异常时返回空字典。"""
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8") as f:
        data = yaml.safe_load(f) or {}
    if not isinstance(data, dict):
        return {}
    return data


def _normalize_host(host: str) -> str:
    """规范化主机地址，修正常见拼写错误并去除首尾空白。"""
    host = host.strip()
    if host.startswith("loalhost"):
        host = "localhost" + host[len("loalhost"):]
    return host


def _get_config() -> dict[str, Any]:
    """读取当前服务主配置。"""
    return _read_yaml(APP_YAML_PATH)


def get_backend_base_url() -> str:
    """获取后端服务基础地址。"""
    conf = _get_config()
    tool = conf.get("tool")
    if isinstance(tool, dict):
        address = tool.get("address")
        if isinstance(address, str) and address.strip():
            fixed = _normalize_host(address)
            if fixed.startswith("http://") or fixed.startswith("https://"):
                return fixed.rstrip("/")
            return f"http://{fixed.rstrip('/')}"
    raise ValueError("Invalid tool.address in config/application.yaml")


def get_agent_port() -> int:
    """获取 Agent 服务监听端口。"""
    conf = _get_config()
    server = conf.get("server")
    if isinstance(server, dict):
        port = server.get("port")
        if isinstance(port, int):
            return port
    raise ValueError("Invalid server.port in config/application.yaml")


def get_agent_model() -> str:
    """获取大模型名称，未配置时返回默认模型。"""
    conf = _get_config()
    model = conf.get("model")
    if isinstance(model, str) and model.strip():
        return model.strip()
    return "qwen-plus"


def get_llm_base_url() -> str:
    """获取 OpenAI-compatible 接口基础地址。"""
    conf = _get_config()
    llm = conf.get("llm")
    if isinstance(llm, dict):
        base_url = llm.get("baseUrl")
        if isinstance(base_url, str) and base_url.strip():
            return base_url.strip().rstrip("/")
    return "https://dashscope.aliyuncs.com/compatible-mode/v1"


def get_llm_api_key() -> str:
    """获取大模型 API Key，优先读取配置文件，再回退环境变量。"""
    conf = _get_config()
    llm = conf.get("llm")
    if isinstance(llm, dict):
        api_key = llm.get("apiKey")
        if isinstance(api_key, str) and api_key.strip():
            return api_key.strip()

    for env_key in ("OPENAI_API_KEY", "DASHSCOPE_API_KEY"):
        value = os.getenv(env_key)
        if value and value.strip():
            return value.strip()

    raise ValueError(
        "Missing LLM API key. Configure llm.apiKey in config/application.yaml "
        "or set OPENAI_API_KEY/DASHSCOPE_API_KEY."
    )


def get_redis_host() -> str:
    """获取 Redis 主机地址。"""
    conf = _get_config()
    redis = conf.get("data", {}).get("redis", {})
    host = redis.get("host")
    if isinstance(host, str) and host.strip():
        return host.strip()
    return "127.0.0.1"


def get_redis_port() -> int:
    """获取 Redis 端口。"""
    conf = _get_config()
    redis = conf.get("data", {}).get("redis", {})
    port = redis.get("port")
    if isinstance(port, int):
        return port
    return 6379


def get_redis_password() -> str:
    """获取 Redis 密码，未配置时返回空字符串。"""
    conf = _get_config()
    redis = conf.get("data", {}).get("redis", {})
    password = redis.get("password")
    if isinstance(password, str):
        return password
    return ""
