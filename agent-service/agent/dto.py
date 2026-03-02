from __future__ import annotations

from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class ShopDoc(BaseModel):
    id: int | None = None
    name: str | None = None
    typeId: int | None = None
    images: str | None = None
    area: str | None = None
    address: str | None = None
    x: float | None = None
    y: float | None = None
    avgPrice: int | None = None
    sold: int | None = None
    score: int | None = None
    openHours: str | None = None

    model_config = ConfigDict(extra="allow")


class AgentSearchDTO(BaseModel):
    content: str = Field(default="")
    shopDoc: ShopDoc | None = None

    model_config = ConfigDict(extra="allow")


def coerce_agent_search_dto(data: dict[str, Any]) -> AgentSearchDTO:
    """将任意字典安全转换为 AgentSearchDTO。"""
    content = data.get("content")
    if not isinstance(content, str):
        content = ""
    shop_doc_raw = data.get("shopDoc")
    shop_doc = None
    if isinstance(shop_doc_raw, dict):
        # 容错：模型可能把 images 组织为数组，这里统一转为逗号分隔字符串
        images_value = shop_doc_raw.get("images")
        if isinstance(images_value, list):
            shop_doc_raw = dict(shop_doc_raw)
            shop_doc_raw["images"] = ",".join(str(item) for item in images_value if item is not None)
        shop_doc = ShopDoc.model_validate(shop_doc_raw)
    return AgentSearchDTO(content=content, shopDoc=shop_doc)


def coerce_agent_search_dto_list(data: Any) -> list[AgentSearchDTO]:
    """将对象或对象数组统一规整为 AgentSearchDTO 列表。"""
    if isinstance(data, list):
        result: list[AgentSearchDTO] = []
        for item in data:
            if isinstance(item, dict):
                result.append(coerce_agent_search_dto(item))
        return result
    if isinstance(data, dict):
        return [coerce_agent_search_dto(data)]
    return []
