# VoucherController 接口文档

- Controller: `com.riton.controller.VoucherController`
- Base URL: `/voucher`

## 通用说明

### 鉴权
- 根据 `MvcConfig`，`/voucher/**` 在白名单内，可匿名访问。

### 统一响应
- `Result{ success, errorMsg, data, total }`

## 接口列表

### 1. 新增普通券
- Method: `POST`
- Path: `/voucher`
- 请求体: `Voucher`
- 返回: `Result.data = voucherId`

### 2. 新增秒杀券
- Method: `POST`
- Path: `/voucher/seckill`
- 请求体: `Voucher`（需包含秒杀相关字段，如 `stock/beginTime/endTime`）
- 返回: `Result.data = voucherId`

### 3. 查询店铺券列表
- Method: `GET`
- Path: `/voucher/list/{shopId}`
- 路径参数: `shopId(Long)`
- 返回: `Result.data = 券列表`

### 4. 更新普通券
- Method: `PUT`
- Path: `/voucher`
- 请求体: `Voucher`（需包含 `id`）
- 返回: `Result.data = 更新结果`

### 5. 更新秒杀券
- Method: `PUT`
- Path: `/voucher/seckill`
- 请求体: `Voucher`（需包含 `id` 及秒杀字段）
- 返回: `Result.data = 更新结果`

### 6. 删除券
- Method: `DELETE`
- Path: `/voucher`
- 查询参数:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| voucherId | Long | 是 | 券ID |

- 返回: `Result.data = 删除结果`
