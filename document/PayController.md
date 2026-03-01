# PayController 接口文档

- Controller: `com.riton.controller.PayController`
- Base URL: `/pay`

## 通用说明

### 鉴权
- `MvcConfig` 未将 `/pay/**` 加入白名单，默认需要登录。

### 统一响应
- 下单支付接口返回 `Result{ success, errorMsg, data, total }`
- 第三方回调接口返回 `String`（通常为支付平台要求的确认字符串）

## 接口列表

### 1. 发起支付
- Method: `POST`
- Path: `/pay`
- 请求体: `PaymentDTO`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| orderId | Long | 是 | 订单ID |
| payMethod | Long | 是 | 支付方式（枚举由服务层约定） |

- 返回: `Result.data = 支付结果`

### 2. 支付宝异步回调
- Method: `POST`
- Path: `/pay/callback/alipay`
- 请求参数: `application/x-www-form-urlencoded` 回调参数映射 `Map<String,String>`
- 返回: `String`

### 3. 微信支付异步回调
- Method: `POST`
- Path: `/pay/callback/wechat`
- Header 参数:

| 参数 | 必填 | 说明 |
|---|---|---|
| Wechatpay-Nonce | 是 | 随机串 |
| Wechatpay-Signature | 是 | 签名 |
| Wechatpay-Serial | 是 | 平台证书序列号 |
| Wechatpay-Timestamp | 是 | 时间戳 |

- Body: 回调原始报文字符串
- 返回: `String`
