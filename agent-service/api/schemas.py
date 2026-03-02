from pydantic import BaseModel, Field


class SearchRequest(BaseModel):
    """智能搜索请求体，仅包含自然语言查询文本。"""
    query: str = Field(..., description="Natural language query")
