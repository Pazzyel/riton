from __future__ import annotations

from typing import Any

from fastmcp import FastMCP

from clients.http_client import call_backend


mcp = FastMCP("riton-search-mcp")


def _auth(authorization: str | None) -> str | None:
    """返回当前请求使用的授权信息。"""
    return authorization


@mcp.tool()
def shop_query_by_id(shop_id: int, authorization: str | None = None) -> dict[str, Any]:
    """按店铺 ID 查询店铺详情。"""
    return call_backend("GET", f"/shop/{shop_id}", authorization=_auth(authorization))


@mcp.tool()
def shop_query_by_name(name: str = "", current: int = 1, authorization: str | None = None) -> dict[str, Any]:
    """按名称关键字分页查询店铺。"""
    params = {"name": name, "current": current}
    return call_backend("GET", "/shop/of/name", params=params, authorization=_auth(authorization))


@mcp.tool()
def shop_query_by_type(
    type_id: int,
    current: int = 1,
    x: float | None = None,
    y: float | None = None,
    authorization: str | None = None,
) -> dict[str, Any]:
    """按店铺类型查询店铺，可选附带坐标参数。"""
    params: dict[str, Any] = {"typeId": type_id, "current": current}
    if x is not None:
        params["x"] = x
    if y is not None:
        params["y"] = y
    return call_backend("GET", "/shop/of/type", params=params, authorization=_auth(authorization))


@mcp.tool()
def voucher_query_by_shop(shop_id: int, authorization: str | None = None) -> dict[str, Any]:
    """查询指定店铺的优惠券列表。"""
    return call_backend("GET", f"/voucher/list/{shop_id}", authorization=_auth(authorization))


@mcp.tool()
def blog_hot(current: int = 1, authorization: str | None = None) -> dict[str, Any]:
    """查询热门博客列表。"""
    return call_backend("GET", "/blog/hot", params={"current": current}, authorization=_auth(authorization))


@mcp.tool()
def search_shop(
    page_no: int = 1,
    page_size: int = 20,
    shop_name: str | None = None,
    type_id: int | None = None,
    x: float | None = None,
    y: float | None = None,
    sort_by: int = 0,
    authorization: str | None = None,
) -> dict[str, Any]:
    """调用搜索服务检索店铺。"""
    params: dict[str, Any] = {"pageNo": page_no, "pageSize": page_size, "sortBy": sort_by}
    if shop_name:
        params["shopName"] = shop_name
    if type_id is not None:
        params["typeId"] = type_id
    if x is not None:
        params["x"] = x
    if y is not None:
        params["y"] = y
    return call_backend("GET", "/search/shop", params=params, authorization=_auth(authorization))


@mcp.tool()
def search_blog(
    page_no: int = 1,
    page_size: int = 20,
    shop_id: int | None = None,
    user_id: int | None = None,
    title: str | None = None,
    content: str | None = None,
    authorization: str | None = None,
) -> dict[str, Any]:
    """调用搜索服务检索博客。"""
    params: dict[str, Any] = {"pageNo": page_no, "pageSize": page_size}
    if shop_id is not None:
        params["shopId"] = shop_id
    if user_id is not None:
        params["userId"] = user_id
    if title:
        params["title"] = title
    if content:
        params["content"] = content
    return call_backend("GET", "/search/blog", params=params, authorization=_auth(authorization))

# 启动MCP服务器
if __name__ == "__main__":
    mcp.run(transport="stdio", show_banner=False)
