# VoucherController 接口文档

- Controllers:
  - `com.riton.controller.common.VoucherCommonController`
  - `com.riton.controller.shop.ShopVoucherManageController`
- Base URL: `/voucher`

## 通用说明

### 鉴权
- `/voucher/list/{shopId}` 为公共查询接口，可匿名访问。
- `/shop/manage/vouchers/**` 为商家管理接口，需要商家登录（shop token）。

### 统一响应
- `Result{ success, errorMsg, data, total }`

## 接口列表

### 1. 查询店铺券列表（公共）
- Method: `GET`
- Path: `/voucher/list/{shopId}`
- 路径参数: `shopId(Long)`
- 返回: `Result.data = 券列表`

### 2. 新增普通券（商家）
- Method: `POST`
- Path: `/shop/manage/vouchers`
- 请求体: `Voucher`
- 返回: `Result.data = voucherId`

### 3. 新增秒杀券（商家）
- Method: `POST`
- Path: `/shop/manage/vouchers/seckill`
- 请求体: `Voucher`（需包含秒杀相关字段，如 `stock/beginTime/endTime`）
- 返回: `Result.data = voucherId`

### 4. 更新普通券（商家）
- Method: `PUT`
- Path: `/shop/manage/vouchers`
- 请求体: `Voucher`（需包含 `id`）
- 返回: `Result.data = 更新结果`

### 5. 更新秒杀券（商家）
- Method: `PUT`
- Path: `/shop/manage/vouchers/seckill`
- 请求体: `Voucher`（需包含 `id` 及秒杀字段）
- 返回: `Result.data = 更新结果`

### 6. 删除券（商家）
- Method: `DELETE`
- Path: `/shop/manage/vouchers/{voucherId}`
- 路径参数: `voucherId(Long)`

- 返回: `Result.data = 删除结果`
