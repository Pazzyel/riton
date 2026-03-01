# BlogController 接口文档

- Controller: `com.riton.controller.BlogController`
- Base URL: `/blog`

## 通用说明

### 鉴权
- `/blog/hot` 在白名单内，可匿名访问。
- 其余 `/blog/**` 默认需要登录。

### 统一响应
- `Result{ success, errorMsg, data, total }`

## 接口列表

### 1. 发布博客
- Method: `POST`
- Path: `/blog`
- 请求体: `Blog`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| shopId | Long | 否 | 商户ID |
| title | String | 否 | 标题 |
| images | String | 否 | 图片，逗号分隔 |
| content | String | 否 | 正文 |

- 返回: `Result.data = 发布结果`

### 2. 点赞/取消点赞博客
- Method: `PUT`
- Path: `/blog/like/{id}`
- 路径参数: `id(Long)` 博客ID
- 返回: `Result.data = 操作结果`

### 3. 查询我的博客
- Method: `GET`
- Path: `/blog/of/me`
- 查询参数: `current(Integer, 默认1)`
- 返回: `Result.data = List<Blog>`

### 4. 查询热门博客
- Method: `GET`
- Path: `/blog/hot`
- 查询参数: `current(Integer, 默认1)`
- 返回: `Result.data = List<Blog>`

### 5. 查询博客详情
- Method: `GET`
- Path: `/blog/{id}`
- 路径参数: `id(Long)` 博客ID
- 返回: `Result.data = Blog`

### 6. 查询点赞用户TOP列表
- Method: `GET`
- Path: `/blog/likes/{id}`
- 路径参数: `id(Long)` 博客ID
- 返回: `Result.data = 用户列表`

### 7. 按用户查询博客
- Method: `GET`
- Path: `/blog/of/user`
- 查询参数:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| current | Integer | 否 | 1 | 页码 |
| id | Long | 是 | - | 用户ID |

- 返回: `Result.data = List<Blog>`

### 8. 滚动分页查询关注用户博客流
- Method: `GET`
- Path: `/blog/of/follow`
- 查询参数:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| lastId | Long | 是 | - | 上次查询的最小时间戳 |
| offset | Integer | 否 | 0 | 偏移量 |

- 返回: `Result.data = ScrollResult`（通常含列表、minTime、offset）

### 9. 按商铺查询博客
- Method: `GET`
- Path: `/blog/of/page`
- 查询参数:

| 参数 | 类型 | 必填 | 默认值 | 说明   |
|---|---|---|---|------|
| current | Integer | 否 | 1 | 页码   |
| id | Long | 是 | - | 商铺ID |

- 返回: `Result.data = List<Blog>`
