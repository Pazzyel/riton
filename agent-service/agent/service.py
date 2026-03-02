from __future__ import annotations

import json
import re
import sys
from typing import Any

from langchain_classic.agents import AgentExecutor, create_tool_calling_agent
from langchain_core.prompts import ChatPromptTemplate
from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain_openai import ChatOpenAI

from agent.dto import AgentSearchDTO, coerce_agent_search_dto_list
from clients.http_client import call_backend
from config.settings import PROJECT_ROOT, get_agent_model, get_llm_api_key, get_llm_base_url


_JSON_ARRAY_RE = re.compile(r"\[[\s\S]*\]")
_JSON_OBJECT_RE = re.compile(r"\{[\s\S]*\}")


def _extract_json_text(text: str) -> str:
    """从模型输出中提取首个可解析的 JSON 文本。"""
    text = text.strip()
    if (text.startswith("[") and text.endswith("]")) or (text.startswith("{") and text.endswith("}")):
        return text
    match = _JSON_ARRAY_RE.search(text)
    if match:
        return match.group(0)
    match = _JSON_OBJECT_RE.search(text)
    if not match:
        raise ValueError(f"Agent output is not JSON: {text}")
    return match.group(0)


def _build_prompt() -> ChatPromptTemplate:
    """构建用于推荐任务的系统提示词模板。"""
    system = (
        "You are a shop recommendation agent for riton local life platform. "
        "You can use MCP tools to query data. "
        "Use search tools first when semantic search is helpful, and fallback to shop tools. "
        "If authorization token is provided by user, pass it as tool argument `authorization`. "
        "Final output MUST be strict JSON array. "
        "Each element must follow schema: {{\"content\": string, \"shopDoc\": object|null}}. "
        "shopDoc should follow fields: id,name,typeId,images,area,address,x,y,avgPrice,sold,score,openHours. "
        "Return some recommended shops, sorted by relevance. "
        "No markdown, no code fence, no extra text."
    )
    return ChatPromptTemplate.from_messages(
        [
            ("system", system),
            ("human", "User query: {input}\nAuthorization token: {authorization_hint}"),
            ("placeholder", "{agent_scratchpad}"),
        ]
    )


def _dto_list_to_json(dto_list: list[AgentSearchDTO]) -> str:
    """将 DTO 列表序列化为 JSON 字符串。"""
    return json.dumps([item.model_dump() for item in dto_list], ensure_ascii=False)


def _fallback_without_llm(user_query: str, authorization: str | None) -> str:
    """当模型或 MCP 调用失败时，走后端搜索接口的降级流程。"""
    result = call_backend(
        "GET",
        "/search/shop",
        params={"pageNo": 1, "pageSize": 5, "shopName": user_query},
        authorization=authorization,
    )
    response = result.get("response") if isinstance(result, dict) else None
    data = response.get("data") if isinstance(response, dict) else None

    if not isinstance(data, list) or not data:
        return _dto_list_to_json([AgentSearchDTO(content="No matched shop found by fallback search.", shopDoc=None)])

    dto_list = []
    for item in data[:5]:
        if isinstance(item, dict):
            dto_list.extend(
                coerce_agent_search_dto_list(
                    {
                        "content": f"Fallback recommendation: {item.get('name', 'unknown')} based on name search.",
                        "shopDoc": item,
                    }
                )
            )
    if not dto_list:
        dto_list = [AgentSearchDTO(content="No result", shopDoc=None)]
    return _dto_list_to_json(dto_list)


async def _build_executor() -> AgentExecutor:
    """构建可调用 MCP 工具的 LangChain AgentExecutor。"""
    client = MultiServerMCPClient(
        {
            "riton-search-mcp": {
                "command": sys.executable,
                "args": ["-m", "agent.mcp_server"],
                "transport": "stdio",
                "cwd": str(PROJECT_ROOT),
            }
        }
    )
    # 获取大模型客户端，提示词，工具
    tools = await client.get_tools()
    llm = ChatOpenAI(
        model=get_agent_model(),
        temperature=0.1,
        api_key=get_llm_api_key(),
        base_url=get_llm_base_url(),
    )
    prompt = _build_prompt()
    # 构建可执行 MCP 调用的 Agent
    agent = create_tool_calling_agent(llm, tools, prompt)
    return AgentExecutor(agent=agent, tools=tools, verbose=False, handle_parsing_errors=True)


async def search_as_agent_search_dto_json(user_query: str, authorization: str | None = None) -> str:
    """主逻辑函数，执行搜索推荐并返回 JSON 数组字符串。"""
    if not user_query or not user_query.strip():
        return _dto_list_to_json([AgentSearchDTO(content="empty query", shopDoc=None)])

    final_auth = authorization or ""
    try:
        executor = await _build_executor()
        result = await executor.ainvoke(
            {
                "input": user_query.strip(),
                "authorization_hint": final_auth if final_auth else "(none)",
            }
        )
        output_text = str(result.get("output", "")).strip()
    except Exception:
        return _fallback_without_llm(user_query.strip(), final_auth or None)

    try:
        parsed: Any = json.loads(_extract_json_text(output_text))
    except Exception:
        return _dto_list_to_json([AgentSearchDTO(content=output_text or "No result", shopDoc=None)])

    dto_list = coerce_agent_search_dto_list(parsed)
    if not dto_list:
        dto_list = [AgentSearchDTO(content="No result", shopDoc=None)]
    return _dto_list_to_json(dto_list)
