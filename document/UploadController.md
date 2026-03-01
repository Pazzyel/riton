# UploadController 接口文档

- Controller: `com.riton.controller.UploadController`
- Base URL: `/upload`

## 通用说明

### 鉴权
- 根据 `MvcConfig`，`/upload/**` 在白名单内，可匿名访问。

### 统一响应
- `Result{ success, errorMsg, data, total }`

## 接口列表

### 1. 上传博客图片
- Method: `POST`
- Path: `/upload/blog`
- Content-Type: `multipart/form-data`
- 表单参数:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | MultipartFile | 是 | 图片文件 |

- 返回: `Result.data = 文件相对路径`（示例：`/blogs/a/b/uuid.jpg`）

### 2. 删除博客图片
- Method: `GET`
- Path: `/upload/blog/delete`
- 查询参数:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | String | 是 | 文件相对路径 |

- 返回: `Result.data = null`
