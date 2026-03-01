# SearchController 接口文档

- Controller: `com.riton.controller.SearchController`
- Base URL: `/search`

## 通用说明

### 鉴权
- `MvcConfig` 未将 `/search/**` 加入白名单，默认需要登录。

### 统一响应
- `Result{ success, errorMsg, data, total }`

## 接口列表

### 1. 搜索店铺
- Method: `GET`
- Path: `/search/shop`
- 查询参数（`ShopPageSearch`）:

| 参数 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|---|---|---|---|---|---|
| pageNo | Integer | 否 | 1 | `>=1` | 页码 |
| pageSize | Integer | 否 | 20 | `>=1` | 每页条数 |
| shopName | String | 否 | - | - | 店铺名称关键词 |
| typeId | Integer | 否 | - | - | 店铺类型ID |
| x | Double | 否 | - | - | 用户经度 |
| y | Double | 否 | - | - | 用户纬度 |
| sortBy | Integer | 否 | 0 | 0/1 | 0按评分，1按距离 |

- 返回: `Result.data = 搜索结果列表`

### 2. 搜索博客
- Method: `GET`
- Path: `/search/blog`
- 查询参数（`BlogPageQuery`）:

| 参数 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|---|---|---|---|---|---|
| pageNo | Integer | 否 | 1 | `>=1` | 页码 |
| pageSize | Integer | 否 | 20 | `>=1` | 每页条数 |
| shopId | Long | 否 | - | - | 商户ID |
| userId | Long | 否 | - | - | 用户ID |
| title | String | 否 | - | - | 标题关键词 |
| content | String | 否 | - | - | 内容关键词 |

- 返回: `Result.data = 搜索结果列表`

### 3. 全量同步店铺到 ES
- Method: `POST`
- Path: `/search/sync/shop`
- 请求参数: 无
- 返回: `Result.data = 同步结果`

### 4. 全量同步博客到 ES
- Method: `POST`
- Path: `/search/sync/blog`
- 请求参数: 无
- 返回: `Result.data = 同步结果`
