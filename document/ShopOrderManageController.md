# ShopOrderManageController 接口文档

- Controller: `com.riton.controller.shop.ShopOrderManageController`
- Base URL: `/shop/manage/orders`

## 接口列表

### 1. 分页查询本店订单
- Method: `GET`
- Path: `/shop/manage/orders`
- Query: `page`, `pageSize`, `status`

### 2. 核销订单（已付款->已核销）
- Method: `PUT`
- Path: `/shop/manage/orders/{orderId}/verify`

### 3. 完成退款（退款中->已退款）
- Method: `PUT`
- Path: `/shop/manage/orders/{orderId}/refund/finish`
