# ShopManageController 接口文档

- Controller: `com.riton.controller.shop.ShopManageController`
- Base URL: `/shop/manage`

## 接口列表

### 1. 维护店铺资料
- Method: `PUT`
- Path: `/shop/manage/profile`
- Body: `Shop`
- 仅可编辑字段：`name`,`images`,`area`,`address`,`openHours`
