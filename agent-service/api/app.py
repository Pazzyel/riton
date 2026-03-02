from __future__ import annotations

from fastapi import FastAPI, Request, Response

from api.schemas import SearchRequest
from agent.service import search_as_agent_search_dto_json


app = FastAPI(title="riton-langchain-agent", version="1.0.0")


@app.get("/health")
async def health() -> dict[str, str]:
    """健康检查接口。"""
    return {"status": "ok"}


@app.post("/search/agent")
async def agent_search(req: SearchRequest, request: Request) -> Response:
    """接收自然语言查询并返回推荐结果 JSON 数组，授权信息从请求头读取。"""
    authorization = request.headers.get("authorization")
    payload = await search_as_agent_search_dto_json(req.query, authorization)
    return Response(content=payload, media_type="application/json")
