# VoucherOrderController 接口文档

- Controller: `com.riton.controller.VoucherOrderController`
- Base URL: `/voucher-order`

## 通用说明

### 鉴权
- `MvcConfig` 未将 `/voucher-order/**` 加入白名单，默认需要登录。

### 统一响应
- `Result{ success, errorMsg, data, total }`

## 接口列表

### 1. 秒杀下单
- Method: `POST`
- Path: `/voucher-order/seckill/{id}`
- 路径参数: `id(Long)` 秒杀券ID
- 限流: `@RateLimit(limitType = API, rate = 1000)`
- 返回: `Result.data = 下单结果`

### 2. 普通领券/下单
- Method: `POST`
- Path: `/voucher-order/voucher/{id}`
- 路径参数: `id(Long)` 券ID
- 返回: `Result.data = 下单结果`
