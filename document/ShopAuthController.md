# ShopAuthController 接口文档

- Controller: `com.riton.controller.shop.ShopAuthController`
- Base URL: `/shop/auth`

## 接口列表

### 1. 发送验证码
- Method: `POST`
- Path: `/shop/auth/code`
- 参数: `phone` (query)

### 2. 注册商家账号
- Method: `POST`
- Path: `/shop/auth/register`
- Body: `ShopLoginFormDTO` (`phone`,`code`,`password`,`shopId`)

### 3. 登录商家账号
- Method: `POST`
- Path: `/shop/auth/login`
- Body: `ShopLoginFormDTO`（密码登录或验证码登录）

### 4. 登出商家账号
- Method: `POST`
- Path: `/shop/auth/logout`

### 5. 当前商家账号信息
- Method: `GET`
- Path: `/shop/auth/me`
