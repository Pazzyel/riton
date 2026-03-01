# ShopTypeController 接口文档

- Controller: `com.riton.controller.ShopTypeController`
- Base URL: `/shop-type`

## 通用说明

### 鉴权
- 根据 `MvcConfig`，`/shop-type/**` 在白名单内，可匿名访问。

### 统一响应
- `Result{ success, errorMsg, data, total }`

## 接口列表

### 1. 查询店铺类型列表
- Method: `GET`
- Path: `/shop-type/list`
- 请求参数: 无
- 返回: `Result.data = List<ShopType>`（按 `sort` 升序）
