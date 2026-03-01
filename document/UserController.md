# UserController 接口文档

- Controller: `com.riton.controller.UserController`
- Base URL: `/user`

## 通用说明

### 鉴权
- `/user/code`、`/user/login` 在白名单内，可匿名访问。
- 其余 `/user/**` 默认需要登录。

### 统一响应
- `Result{ success, errorMsg, data, total }`

## 接口列表

### 1. 发送手机验证码
- Method: `POST`
- Path: `/user/code`
- 查询参数:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | String | 是 | 手机号 |

- 返回: `Result.data = 发送结果`

### 2. 登录
- Method: `POST`
- Path: `/user/login`
- 请求体: `LoginFormDTO`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | String | 否 | 手机号 |
| code | String | 否 | 验证码登录时使用 |
| password | String | 否 | 密码登录时使用 |

- 返回: `Result.data = 登录结果（通常为token或用户信息）`

### 3. 登出
- Method: `POST`
- Path: `/user/logout`
- 请求参数: 无
- 返回: `Result.data = null`

### 4. 获取当前登录用户
- Method: `GET`
- Path: `/user/me`
- 返回: `Result.data = UserDTO`

### 5. 查询用户详情信息
- Method: `GET`
- Path: `/user/info/{id}`
- 路径参数: `id(Long)` 用户ID
- 返回: `Result.data = UserInfo`（若无数据返回空）

### 6. 根据ID查询用户基础信息
- Method: `GET`
- Path: `/user/{id}`
- 路径参数: `id(Long)` 用户ID
- 返回: `Result.data = UserDTO`（若不存在返回空）

### 7. 用户签到
- Method: `POST`
- Path: `/user/sign`
- 返回: `Result.data = 签到结果`

### 8. 查询当月连续签到天数
- Method: `GET`
- Path: `/user/sign/count`
- 返回: `Result.data = 连续签到天数`
