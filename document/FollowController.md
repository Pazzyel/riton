# FollowController 接口文档

- Controller: `com.riton.controller.FollowController`
- Base URL: `/follow`

## 通用说明

### 鉴权
- `MvcConfig` 未将 `/follow/**` 加入白名单，默认需要登录。

### 统一响应
- `Result{ success, errorMsg, data, total }`

## 接口列表

### 1. 关注/取关用户
- Method: `PUT`
- Path: `/follow/{id}/{isFollow}`
- 路径参数:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 目标用户ID |
| isFollow | Boolean | 是 | `true`关注，`false`取关 |

- 返回: `Result.data = 操作结果`

### 2. 判断是否已关注
- Method: `GET`
- Path: `/follow/or/not/{id}`
- 路径参数:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 目标用户ID |

- 返回: `Result.data = 是否关注`

### 3. 查询共同关注
- Method: `GET`
- Path: `/follow/common/{id}`
- 路径参数:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 另一个用户ID |

- 返回: `Result.data = 共同关注用户列表`
