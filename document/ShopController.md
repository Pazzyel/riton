# ShopController 接口文档

- Controller: `com.riton.controller.ShopController`
- Base URL: `/shop`

## 通用说明

### 鉴权
- 根据 `MvcConfig`，`/shop/**` 在登录拦截器白名单内，可匿名访问。

### 统一响应
所有接口返回 `Result`：

```json
{
  "success": true,
  "errorMsg": null,
  "data": {},
  "total": null
}
```

## 接口列表

### 1. 查询店铺详情
- Method: `GET`
- Path: `/shop/{id}`
- 路径参数:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 店铺ID |

- 返回: `Result.data = Shop`

### 2. 新增店铺
- Method: `POST`
- Path: `/shop`
- 请求体: `Shop`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | String | 否 | 店铺名称 |
| typeId | Long | 否 | 店铺类型ID |
| images | String | 否 | 图片，多图逗号分隔 |
| area | String | 否 | 商圈 |
| address | String | 否 | 地址 |
| x | Double | 否 | 经度 |
| y | Double | 否 | 纬度 |
| avgPrice | Long | 否 | 人均价格 |
| openHours | String | 否 | 营业时间 |

- 返回: `Result.data = 新增店铺ID`

### 3. 更新店铺
- Method: `PUT`
- Path: `/shop`
- 请求体: `Shop`（至少应包含 `id`）
- 返回: `Result.data = null`

### 4. 按名称分页查询店铺
- Method: `GET`
- Path: `/shop/of/name`
- 查询参数:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| name | String | 否 | - | 店铺名称关键字 |
| current | Integer | 否 | 1 | 页码 |

- 分页大小: `SystemConstants.MAX_PAGE_SIZE = 10`
- 返回: `Result.data = List<Shop>`

### 5. 按类型分页查询店铺（可选地理位置）
- Method: `GET`
- Path: `/shop/of/type`
- 查询参数:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| typeId | Integer | 是 | - | 店铺类型ID |
| current | Integer | 否 | 1 | 页码 |
| x | Double | 否 | - | 经度 |
| y | Double | 否 | - | 纬度 |

- 返回: `Result.data = List<Shop>`（带地理查询时可能包含 `distance`）
