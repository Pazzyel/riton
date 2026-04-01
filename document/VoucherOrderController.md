# VoucherOrderController 接口文档

- Controllers:
  - `com.riton.controller.user.VoucherOrderUserController`
  - `com.riton.controller.shop.ShopOrderManageController`
- Base URL: `/voucher-order`

## 通用说明

### 鉴权
- 用户下单接口 `/voucher-order/**` 需要用户登录。
- 商家订单管理接口 `/shop/manage/orders/**` 需要商家登录。

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

### 3. 商家分页查询本店订单
- Method: `GET`
- Path: `/shop/manage/orders`
- 查询参数:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |
| status | Integer | 否 | 按订单状态筛选 |

- 返回: `Result.data = 订单列表`, `Result.total = 总数`

### 4. 商家核销订单（已付款->已核销）
- Method: `PUT`
- Path: `/shop/manage/orders/{orderId}/verify`
- 路径参数: `orderId(Long)`
- 返回: `Result.data = orderId`

### 5. 商家完成退款（退款中->已退款）
- Method: `PUT`
- Path: `/shop/manage/orders/{orderId}/refund/finish`
- 路径参数: `orderId(Long)`
- 返回: `Result.data = orderId`
