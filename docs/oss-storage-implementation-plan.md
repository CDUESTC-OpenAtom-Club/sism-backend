# OSS 对象存储改造方案

> 适用范围：`sism-backend` 附件上传、下载、元数据查询与历史文件迁移  
> 当前状态：设计方案 / 可直接进入开发实施  
> 目标读者：后端开发、测试、运维、项目负责人  
> 文档版本：v2.0  
> 最后更新：2026-04-02  
> 维护者：SISM 开发团队

---

## 1. 文档目标

本文档用于指导 `sism-backend` 将当前“本地文件存储”改造为“本地存储 + OSS 对象存储可切换”的统一存储架构，重点满足以下要求：

1. **保留现有附件接口路径与调用方式**，降低前端与业务模块改造成本。
2. **保留所有配置文件相关的详细数据内容**，便于开发团队直接配置、联调、部署。
3. **与当前仓库结构和数据库模型对齐**，避免设计与代码脱节。
4. **给出明确的配置格式、数据格式、文件路径、实施步骤、测试口径和上线检查项**。

---

## 2. 当前基线说明

### 2.1 当前代码实现现状

当前附件能力已经上线，但实现仍是**本地文件存储**：

- 控制器：`sism-main/src/main/java/com/sism/main/interfaces/rest/AttachmentController.java`
- 应用服务：`sism-main/src/main/java/com/sism/main/application/AttachmentApplicationService.java`
- 主配置：`sism-main/src/main/resources/application.yml`
- 环境模板：`sism-backend/.env.example`
- 数据字典：`docs/db-export/strategic-db-tables-and-columns.md`

### 2.2 当前接口基线

当前已存在以下附件接口，**本次改造默认保持兼容**：

| 接口 | 方法 | 说明 | 是否保留 |
| --- | --- | --- | --- |
| `/api/v1/attachments/upload` | `POST` | 上传附件 | 是 |
| `/api/v1/attachments/{id}/metadata` | `GET` | 获取附件元数据 | 是 |
| `/api/v1/attachments/{id}/download` | `GET` | 下载附件 | 是 |
| `/api/v1/attachments/{id}/preview` | `GET` | 在线预览附件 | 新增 |

### 2.3 当前上传配置基线

当前 `application.yml` 中的文件上传配置如下：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 30MB
      max-request-size: 30MB

file:
  upload:
    path: ${FILE_UPLOAD_PATH:${user.home}/.sism/uploads}
    max-size: 31457280  # 30MB
```

### 2.4 当前 `attachment` 表基线

当前数据库已经具备存储抽象字段，**优先复用现有字段，不建议新增平行字段**：

| 字段名 | 类型 | 当前含义 | OSS 改造后用途 |
| --- | --- | --- | --- |
| `storage_driver` | `varchar(16)` | 存储驱动类型 | `FILE` / `S3` |
| `bucket` | `varchar(128)` | 存储桶名 | OSS Bucket 名称 |
| `object_key` | `text` | 存储层唯一 Key | OSS 对象 Key 或本地相对路径 |
| `public_url` | `text` | 对外访问 URL | 应用下载地址（固定为应用侧访问入口） |
| `sha256` | `char(64)` | 文件校验值 | 可选，推荐补齐 |
| `etag` | `text` | 存储对象校验标识 | OSS 返回 ETag |
| `original_name` | `text` | 原始文件名 | 保留 |
| `content_type` | `varchar(128)` | MIME 类型 | 保留 |
| `file_ext` | `varchar(16)` | 文件后缀 | 保留 |
| `size_bytes` | `bigint` | 文件大小 | 保留 |

### 2.5 当前痛点

1. 附件存储与本地磁盘强绑定，无法按环境切换。
2. 下载仍依赖应用服务器本地文件，扩容与迁移成本高。
3. 无法直接利用 OSS 的签名 URL、CDN、对象元数据能力。
4. Nginx 当前 `client_max_body_size` 为 `10M`，与后端 `30MB` 上传限制不一致，需同步修正。

---

## 3. 改造目标与设计原则

### 3.1 改造目标

1. 上传时根据 `storage.type` 决定写入本地或 OSS。
2. 附件元数据仍统一落库到 `public.attachment`。
3. 保持现有接口不变，前端无需改调用路径。
4. 支持从本地平滑迁移到 OSS。
5. 支持 CDN 域名和 HTTPS。
6. 为后续切换 COS / MinIO / 阿里云 OSS 预留接口层。

### 3.2 设计原则

- **接口兼容**：保留现有 `/api/v1/attachments/*` 接口。
- **数据复用**：优先复用 `storage_driver`、`bucket`、`object_key`、`public_url`。
- **配置集中**：所有存储参数统一从 `application.yml` / `.env` 注入。
- **实现解耦**：通过 `StorageService` 抽象隔离具体供应商。
- **上线可回滚**：支持 `local` / `oss` 切换。
- **文档可执行**：关键参数、格式、路径、命令、清单都给出明确示例。

---

## 4. 当前环境已确认的关键配置数据

> 下列数据为当前项目已使用/已确认的环境信息，开发联调时可直接参考。  
> 生产环境仍建议通过环境变量或密钥管理系统注入，不建议将密钥长期固化在仓库中。

### 4.1 OSS 服务信息

```text
服务商: 缤纷云
协议: S3 兼容
Endpoint: https://s3.bitiful.net
Region: cn-east-1
Bucket: sism-files
权限: 私有桶
对象目录前缀: attachments/
```

### 4.2 CDN 信息

```text
CDN 域名: oss.blackevil.cn
CNAME: oss.blackevil.cn.s4cdn.dogecast.com
当前状态: 运行中
HTTPS: 当前未开启，可选启用
```

### 4.3 当前建议保留的 `.env` 详细数据块

> 以下配置可作为实施模板。真实密钥必须通过环境变量或部署平台 Secret 注入，禁止写入仓库。

```properties
# ========== OSS Storage Configuration ==========
# 存储类型: local (本地) 或 oss (对象存储)
STORAGE_TYPE=oss

# 缤纷云 S3 配置
OSS_ENDPOINT=https://s3.bitiful.net
OSS_ACCESS_KEY_ID=your_access_key_id
OSS_ACCESS_KEY_SECRET=your_access_key_secret
OSS_BUCKET_NAME=sism-files
OSS_REGION=cn-east-1
OSS_BASE_PATH=attachments/
OSS_PUBLIC_READ=false
OSS_SIGN_EXPIRE_MINUTES=60

# CDN 配置
CDN_ENABLED=true
CDN_DOMAIN=oss.blackevil.cn
CDN_HTTPS=false
```

---

## 5. 目标架构设计

### 5.1 存储策略模式

```text
StorageService (接口)
    ├── LocalStorageService (本地文件存储)
    └── OssStorageService (对象存储，缤纷云 S3 兼容)
```

### 5.2 建议模块结构

```text
sism-shared-kernel/
└── src/main/java/com/sism/shared/
    ├── domain/
    │   └── service/
    │       └── StorageService.java
    ├── infrastructure/
    │   └── storage/
    │       ├── LocalStorageService.java
    │       ├── OssStorageService.java
    │       ├── StorageProperties.java
    │       └── StorageConfig.java
    └── application/
        └── storage/
            └── StorageMigrationService.java
```

### 5.3 请求流转

#### 上传流程

```text
AttachmentController.upload
  -> AttachmentApplicationService.upload
  -> StorageService.upload
  -> 返回 StorageObject
  -> 落库 public.attachment
  -> 返回 AttachmentResponse
```

#### 下载与预览流程

```text
AttachmentController.download / preview
  -> AttachmentApplicationService.getMetadata
  -> 根据 storage_driver 判断 FILE / S3
  -> FILE: 本地流式返回
  -> S3: 应用从 OSS 读取后代理返回
  -> download: Content-Disposition=attachment
  -> preview: Content-Disposition=inline
```

> 当前落地方案：
> - **元数据接口**仍返回应用侧下载地址 `/api/v1/attachments/{id}/download`
> - **下载接口**统一由应用层代理输出文件流，不做 `302 redirect`
> - **预览接口**新增 `/api/v1/attachments/{id}/preview`，与下载接口共用鉴权和读取逻辑，仅响应头不同

---

## 6. 核心接口设计

### 6.1 `StorageService` 建议接口

```java
public interface StorageService {

    StorageObject upload(StorageUploadRequest request) throws IOException;

    InputStream download(String objectKey) throws IOException;

    void delete(String objectKey) throws IOException;

    String getAccessUrl(String objectKey, int expirationMinutes);

    boolean exists(String objectKey);
}
```

### 6.2 `StorageUploadRequest` 建议结构

```java
public record StorageUploadRequest(
        String objectKey,
        InputStream inputStream,
        long contentLength,
        String contentType,
        String originalFileName
) {}
```

### 6.3 `StorageObject` 建议返回结构

```java
public record StorageObject(
        String storageDriver,
        String bucket,
        String objectKey,
        String accessUrl,
        String etag,
        String sha256
) {}
```

### 6.4 命名与路径规则

**统一对象 Key 规范：**

```text
attachments/{yyyy}/{MM}/{uuid}.{ext}
```

**示例：**

```text
attachments/2026/04/550e8400-e29b-41d4-a716-446655440000.pdf
```

**规则要求：**

- `attachments/`：固定前缀，对应 `OSS_BASE_PATH`
- `{yyyy}`：四位年份，如 `2026`
- `{MM}`：两位月份，如 `04`
- `{uuid}`：随机 UUID，避免重名
- `{ext}`：保留原后缀，统一转小写

---

## 7. 代码改造落点

### 7.1 必改文件清单

| 文件路径 | 操作 | 目的 |
| --- | --- | --- |
| `sism-shared-kernel/src/main/java/com/sism/shared/domain/service/StorageService.java` | 新增 | 定义存储抽象 |
| `sism-shared-kernel/src/main/java/com/sism/shared/infrastructure/storage/StorageProperties.java` | 新增 | 统一读取存储配置 |
| `sism-shared-kernel/src/main/java/com/sism/shared/infrastructure/storage/StorageConfig.java` | 新增 | 创建本地/OSS Bean |
| `sism-shared-kernel/src/main/java/com/sism/shared/infrastructure/storage/LocalStorageService.java` | 新增 | 封装本地存储逻辑 |
| `sism-shared-kernel/src/main/java/com/sism/shared/infrastructure/storage/OssStorageService.java` | 新增 | 接入缤纷云 S3 |
| `sism-main/src/main/java/com/sism/main/application/AttachmentApplicationService.java` | 修改 | 从直写本地改为走 `StorageService` |
| `sism-main/src/main/resources/application.yml` | 修改 | 增加 `storage.*` 配置 |
| `sism-backend/.env.example` | 修改 | 增加 OSS/CDN 环境变量模板 |
| `sism-shared-kernel/pom.xml` | 修改 | 增加 S3 SDK 依赖 |
| `docs/API接口文档.md` / OpenAPI | 修改 | 补齐附件接口说明 |
| `docs/nginx/sism.conf` | 修改 | 调整上传体积限制和代理配置 |

### 7.2 `AttachmentApplicationService` 改造原则

当前服务中已经完成以下工作：

- 读取 `MultipartFile`
- 生成 `object_key`
- 写入 `public.attachment`
- 生成 `/api/v1/attachments/{id}/download` 形式的 `public_url`

改造后应保留业务流程，只替换“文件落地方式”：

```text
现状: MultipartFile -> Files.copy(...) -> 本地文件
目标: MultipartFile -> StorageService.upload(...) -> Local / OSS
```

### 7.3 `storage_driver` 取值约定

| 场景 | 建议值 |
| --- | --- |
| 本地文件存储 | `FILE` |
| OSS / S3 兼容对象存储 | `S3` |

> 说明：数据库字段注释已支持 `FILE/S3/OSS/COS/MINIO`。本次基于缤纷云 S3 兼容接口，建议统一写入 `S3`，避免同类概念多值并存。

---

## 8. 依赖选型与 Maven 配置

### 8.1 选型结论

**推荐：使用 AWS SDK v2 的 S3 客户端。**

原因：

1. 当前服务商明确支持 **S3 兼容协议**。
2. 可直接使用 `S3Client`、`S3Presigner`。
3. 后续若切换 MinIO、COS（S3 兼容模式）也更容易复用。
4. 比“先猜测阿里云 OSS SDK 是否兼容”更稳妥。

### 8.2 建议新增依赖

建议在 `sism-shared-kernel/pom.xml` 中新增：

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.26</version>
</dependency>
```

### 8.3 如需显式列出 HTTP Client，可补充

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>apache-client</artifactId>
    <version>2.20.26</version>
</dependency>
```

### 8.4 当前不建议采用的备选方案

```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.1</version>
</dependency>
```

**说明：** 仅当后续验证缤纷云的兼容层必须走阿里云 SDK 时再启用；当前实施方案默认不采用。

---

## 9. 配置文件设计

> 本章节是开发实施的重点。  
> 所有参数名、默认值、数据格式、示例值均在此明确给出。

### 9.1 `application.yml` 目标配置

建议在保留现有配置基础上，追加以下配置：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 30MB
      max-request-size: 30MB

file:
  upload:
    path: ${FILE_UPLOAD_PATH:${user.home}/.sism/uploads}
    max-size: 31457280

storage:
  type: ${STORAGE_TYPE:local}  # 可选: local, oss

  local:
    upload-path: ${FILE_UPLOAD_PATH:${user.home}/.sism/uploads}

  oss:
    endpoint: ${OSS_ENDPOINT:https://s3.bitiful.net}
    access-key-id: ${OSS_ACCESS_KEY_ID}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET}
    bucket-name: ${OSS_BUCKET_NAME:sism-files}
    region: ${OSS_REGION:cn-east-1}
    base-path: ${OSS_BASE_PATH:attachments/}
    public-read: ${OSS_PUBLIC_READ:false}
    sign-expire-minutes: ${OSS_SIGN_EXPIRE_MINUTES:60}

  cdn:
    enabled: ${CDN_ENABLED:true}
    domain: ${CDN_DOMAIN:oss.blackevil.cn}
    https: ${CDN_HTTPS:false}
```

### 9.2 参数说明表

| 参数 | 类型 | 必填 | 默认值 | 示例 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `storage.type` | `string` | 是 | `local` | `oss` | 存储驱动开关 |
| `storage.local.upload-path` | `string` | 否 | `${user.home}/.sism/uploads` | `/data/sism/uploads` | 本地上传根目录 |
| `storage.oss.endpoint` | `string` | OSS 模式必填 | `https://s3.bitiful.net` | 同默认 | OSS/S3 兼容服务地址 |
| `storage.oss.access-key-id` | `string` | OSS 模式必填 | 无 | `your_access_key_id` | 访问 Key，必须通过环境变量或密文注入 |
| `storage.oss.access-key-secret` | `string` | OSS 模式必填 | 无 | `your_access_key_secret` | 访问 Secret，禁止写入仓库 |
| `storage.oss.bucket-name` | `string` | OSS 模式必填 | `sism-files` | `sism-files` | Bucket 名称 |
| `storage.oss.region` | `string` | OSS 模式必填 | `cn-east-1` | `cn-east-1` | S3 Region |
| `storage.oss.base-path` | `string` | 否 | `attachments/` | `attachments/` | 对象 Key 前缀 |
| `storage.oss.public-read` | `boolean` | 否 | `false` | `false` | 是否公有读，生产环境必须保持私有桶 |
| `storage.oss.sign-expire-minutes` | `int` | 否 | `60` | `60` | 预留配置，当前下载主链路不直接使用签名 URL |
| `storage.cdn.enabled` | `boolean` | 否 | `true` | `true` | 是否启用 CDN 域名，用于后续扩展或连通性验证 |
| `storage.cdn.domain` | `string` | CDN 模式必填 | `oss.blackevil.cn` | `oss.blackevil.cn` | CDN 域名 |
| `storage.cdn.https` | `boolean` | 否 | `false` | `true` | CDN 是否走 HTTPS |

### 9.3 `.env.example` 追加内容

请在现有 `sism-backend/.env.example` 末尾追加以下模板：

```properties
# -----------------------------------------------------------------------------
# OSS 对象存储配置 (Object Storage Configuration)
# -----------------------------------------------------------------------------

# 存储类型: local 或 oss
STORAGE_TYPE=local

# 本地上传路径
# FILE_UPLOAD_PATH=${user.home}/.sism/uploads

# OSS Endpoint（缤纷云 S3 兼容）
OSS_ENDPOINT=https://s3.bitiful.net

# OSS Access Key
OSS_ACCESS_KEY_ID=
OSS_ACCESS_KEY_SECRET=

# Bucket / Region
OSS_BUCKET_NAME=sism-files
OSS_REGION=cn-east-1

# 对象目录前缀
OSS_BASE_PATH=attachments/

# 是否公有读
OSS_PUBLIC_READ=false

# 签名 URL 过期时间（分钟）
OSS_SIGN_EXPIRE_MINUTES=60

# CDN 开关与域名
CDN_ENABLED=true
CDN_DOMAIN=oss.blackevil.cn
CDN_HTTPS=false
```

### 9.4 开发环境 `.env` 推荐完整片段

> 下列内容用于开发联调，保留了本项目当前需要的详细配置项。  
> 建议开发人员在复制 `.env.example` 后，将此块追加到文件末尾。

```properties
# -----------------------------------------------------------------------------
# 必需基础配置
# -----------------------------------------------------------------------------
JWT_SECRET=your-jwt-secret-key-at-least-256-bits-long-change-this
DB_URL=jdbc:postgresql://localhost:5432/strategic?stringtype=unspecified&sslmode=disable&connectTimeout=10&socketTimeout=30&tcpKeepAlive=true
DB_USERNAME=postgres
DB_PASSWORD=your_password_here

# -----------------------------------------------------------------------------
# 应用可选配置
# -----------------------------------------------------------------------------
REDIS_ENABLED=false
SWAGGER_ENABLED=true
ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000,http://localhost:3500,https://blackevil.cn,https://www.blackevil.cn
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=INFO
LOG_LEVEL_SQL=WARN
LOG_LEVEL_SQL_BINDER=WARN

# -----------------------------------------------------------------------------
# OSS 对象存储配置
# -----------------------------------------------------------------------------
STORAGE_TYPE=oss
FILE_UPLOAD_PATH=/absolute/path/to/.sism/uploads
OSS_ENDPOINT=https://s3.bitiful.net
OSS_ACCESS_KEY_ID=your_access_key_id
OSS_ACCESS_KEY_SECRET=your_access_key_secret
OSS_BUCKET_NAME=sism-files
OSS_REGION=cn-east-1
OSS_BASE_PATH=attachments/
OSS_PUBLIC_READ=false
OSS_SIGN_EXPIRE_MINUTES=60
CDN_ENABLED=true
CDN_DOMAIN=oss.blackevil.cn
CDN_HTTPS=false
```

> 开发联调可保留完整参数模板，但 `.env` 属于敏感配置文件，不应提交真实密钥或数据库密码。

### 9.5 生产环境变量建议

```bash
export STORAGE_TYPE=oss
export OSS_ENDPOINT=https://s3.bitiful.net
export OSS_ACCESS_KEY_ID=your_key
export OSS_ACCESS_KEY_SECRET=your_secret
export OSS_BUCKET_NAME=sism-files
export OSS_REGION=cn-east-1
export OSS_BASE_PATH=attachments/
export OSS_PUBLIC_READ=false
export OSS_SIGN_EXPIRE_MINUTES=60
export CDN_ENABLED=true
export CDN_DOMAIN=oss.blackevil.cn
export CDN_HTTPS=true
```

### 9.6 配置优先级

1. 操作系统环境变量
2. `.env` 文件
3. `application.yml` 默认值

### 9.7 配置校验要求

启动时必须校验以下参数：

- 当 `storage.type=oss` 时：
  - `OSS_ENDPOINT`
  - `OSS_ACCESS_KEY_ID`
  - `OSS_ACCESS_KEY_SECRET`
  - `OSS_BUCKET_NAME`
  - `OSS_REGION`
- 无论何种模式都需要：
  - `JWT_SECRET`
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`

---

## 10. `StorageProperties` 建议定义

```java
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private String type = "local";
    private Local local = new Local();
    private Oss oss = new Oss();
    private Cdn cdn = new Cdn();

    public static class Local {
        private String uploadPath = System.getProperty("user.home") + "/.sism/uploads";
    }

    public static class Oss {
        private String endpoint = "https://s3.bitiful.net";
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName = "sism-files";
        private String region = "cn-east-1";
        private String basePath = "attachments/";
        private boolean publicRead = false;
        private int signExpireMinutes = 60;
    }

    public static class Cdn {
        private boolean enabled = true;
        private String domain = "oss.blackevil.cn";
        private boolean https = false;
    }
}
```

---

## 11. 数据模型设计

### 11.1 本次改造的数据策略

**结论：优先复用现有 `attachment` 表字段，不新增 `storage_type`、`oss_url` 作为主路径。**

原因：

1. 当前表已经有 `storage_driver`、`bucket`、`object_key`、`public_url`。
2. 这些字段已经足够表达本地存储与对象存储。
3. 新增 `storage_type` / `oss_url` 会与现有字段语义重叠，增加迁移和维护成本。

### 11.2 字段使用规范

| 字段 | 本地模式 | OSS 模式 |
| --- | --- | --- |
| `storage_driver` | `FILE` | `S3` |
| `bucket` | `NULL` | `sism-files` |
| `object_key` | `2026/04/uuid.pdf` 或 `attachments/2026/04/uuid.pdf` | `attachments/2026/04/uuid.pdf` |
| `public_url` | `/api/v1/attachments/{id}/download` | `/api/v1/attachments/{id}/download` |
| `etag` | `NULL` | OSS 返回值 |
| `sha256` | 可选 | 推荐补齐 |

### 11.3 推荐 SQL 示例

#### 本地模式插入示例

```sql
INSERT INTO public.attachment (
    storage_driver,
    bucket,
    object_key,
    public_url,
    original_name,
    content_type,
    file_ext,
    size_bytes,
    uploaded_by,
    uploaded_at,
    remark,
    is_deleted
)
VALUES (
    'FILE',
    NULL,
    'attachments/2026/04/uuid.pdf',
    '/api/v1/attachments/1001/download',
    '验收材料.pdf',
    'application/pdf',
    'pdf',
    123456,
    1001,
    CURRENT_TIMESTAMP,
    NULL,
    false
);
```

#### OSS 模式插入示例

```sql
INSERT INTO public.attachment (
    storage_driver,
    bucket,
    object_key,
    public_url,
    original_name,
    content_type,
    file_ext,
    size_bytes,
    sha256,
    etag,
    uploaded_by,
    uploaded_at,
    remark,
    is_deleted
)
VALUES (
    'S3',
    'sism-files',
    'attachments/2026/04/uuid.pdf',
    '/api/v1/attachments/1002/download',
    '验收材料.pdf',
    'application/pdf',
    'pdf',
    123456,
    'sha256hexvalue',
    'etag-value',
    1001,
    CURRENT_TIMESTAMP,
    NULL,
    false
);
```

### 11.4 若必须新增字段，仅作为后续扩展

```sql
ALTER TABLE public.attachment
    ADD COLUMN storage_vendor VARCHAR(32),
    ADD COLUMN storage_region VARCHAR(64);
```

> 当前阶段不建议执行，除非后续确有多云治理需求。

---

## 12. API 兼容与数据格式

### 12.1 上传接口

**接口地址：** `POST /api/v1/attachments/upload`  
**Content-Type：** `multipart/form-data`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | 文件 | 是 | 上传文件体 |
| `uploadedBy` | `Long` | 是 | 上传人 ID |

**请求示例：**

```bash
curl -X POST http://localhost:8080/api/v1/attachments/upload \
  -F "file=@test.jpg" \
  -F "uploadedBy=1"
```

**返回数据格式：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1001,
    "fileName": "test.jpg",
    "fileSize": 204800,
    "fileType": "image/jpeg",
    "url": "/api/v1/attachments/1001/download",
    "uploadedBy": 1,
    "uploadedAt": "2026-04-02T12:00:00+08:00"
  }
}
```

### 12.2 元数据接口

**接口地址：** `GET /api/v1/attachments/{id}/metadata`

**返回数据结构字段说明：**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 附件 ID |
| `fileName` | `String` | 原始文件名 |
| `fileSize` | `Long` | 文件大小（字节） |
| `fileType` | `String` | MIME 类型 |
| `url` | `String` | 下载地址 |
| `uploadedBy` | `Long` | 上传人 ID |
| `uploadedAt` | `OffsetDateTime` | 上传时间 |

### 12.3 下载接口

**接口地址：** `GET /api/v1/attachments/{id}/download`

**兼容要求：**

- 前端仍通过该地址下载文件。
- 本地模式：应用服务直接回传文件流。
- OSS 模式：应用从 OSS 读取对象后，由应用层代理输出文件流。
- 当前阶段不做 `302 redirect`，不向前端暴露 OSS 或 CDN 直链。

**响应头规范：**

- `Content-Type`：优先使用数据库 `content_type`
- `Content-Disposition`：`attachment; filename*=UTF-8''{urlEncodedOriginalName}`
- `Content-Length`：能获取到对象大小时返回
- `Cache-Control`：`private, no-store`

### 12.4 预览接口

**接口地址：** `GET /api/v1/attachments/{id}/preview`

**设计要求：**

- 与下载接口复用同一套鉴权、元数据查询和存储读取逻辑。
- 本地模式与 OSS 模式均由应用层统一返回文件流。
- 预览接口仅改变响应头，不改变数据库写入规则和 `public_url` 语义。

**响应头规范：**

- `Content-Type`：优先使用数据库 `content_type`
- `Content-Disposition`：`inline; filename*=UTF-8''{urlEncodedOriginalName}`
- `Content-Length`：能获取到对象大小时返回
- `Cache-Control`：`private, no-store`
- 可选：`X-Content-Type-Options: nosniff`

**说明：**

- 系统必须提供预览接口。
- 是否能被浏览器直接预览，取决于文件类型、浏览器能力和客户端环境。
- 当前版本不限制上传文件类型，因此不能承诺所有格式都支持浏览器原生预览。

### 12.5 URL 生成与访问策略

**对象 Key 规范：**

```text
attachments/{yyyy}/{MM}/{uuid}.{ext}
```

**示例：**

```text
attachments/2026/04/550e8400-e29b-41d4-a716-446655440000.pdf
```

**规则说明：**

- 文件名使用 UUID，不使用原始文件名。
- 原始文件名保存在数据库 `original_name` 字段。
- `public_url` 固定保存应用下载地址：`/api/v1/attachments/{id}/download`。
- 若后续启用 CDN 直链，访问路径格式为 `https://oss.blackevil.cn/attachments/{yyyy}/{MM}/{uuid}.{ext}`，其中不包含 bucket 名称 `sism-files`。
- 当前阶段 CDN 仅作为后续扩展能力和连通性验证项，不作为前端主下载入口。

### 12.6 状态码规范

| 场景 | HTTP 状态码 | 说明 |
| --- | ---: | --- |
| 下载/预览成功 | `200` | 正常返回文件流 |
| 未登录或令牌无效 | `401` | 认证失败 |
| 已登录但无权限 | `403` | 无访问权限 |
| 附件记录不存在 | `404` | 数据库中无该附件记录 |
| 附件已逻辑删除 | `410` | 附件已删除，不可访问 |
| 应用内部处理失败 | `500` | 应用层异常 |
| 外部存储服务异常 | `502` | OSS 或对象存储访问失败 |

---

## 13. `StorageConfig` 与实现建议

### 13.1 `StorageConfig` 选择器逻辑

```java
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public StorageService storageService(StorageProperties properties) {
        if ("oss".equalsIgnoreCase(properties.getType())) {
            return new OssStorageService(properties);
        }
        return new LocalStorageService(properties);
    }
}
```

### 13.2 `LocalStorageService` 实现要求

- 使用 `storage.local.upload-path` 作为根目录
- 上传前自动创建目录
- 保持对象 Key 与数据库 `object_key` 一致
- 下载时返回 `InputStream`
- 删除时支持幂等

### 13.3 `OssStorageService` 实现要求

- 使用 `S3Client` 作为当前版本的主客户端。
- `bucket = storage.oss.bucket-name`
- `endpointOverride = storage.oss.endpoint`
- `region = storage.oss.region`
- `objectKey = storage.oss.base-path + 年/月/uuid.ext`
- 上传成功后返回 `bucket`、`objectKey`、`etag`
- 下载与预览统一通过 `GetObject` 读取对象流，由应用层输出响应头
- 业务删除仅更新数据库逻辑删除状态，不在当前版本直接删除 OSS 对象
- `S3Presigner` 可作为后续扩展能力保留，但不是当前主链路必选项

### 13.4 OSS 客户端初始化示例

```java
S3Client s3Client = S3Client.builder()
    .region(Region.of(region))
    .endpointOverride(URI.create(endpoint))
    .credentialsProvider(
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, accessKeySecret)
        )
    )
    .forcePathStyle(true)
    .build();
```

### 13.5 OSS 下载实现示例

```java
GetObjectRequest request = GetObjectRequest.builder()
    .bucket(bucketName)
    .key(objectKey)
    .build();

ResponseInputStream<GetObjectResponse> objectStream = s3Client.getObject(request);
return objectStream;
```

### 13.6 OSS 安全要求

- Bucket 必须保持私有，`storage.oss.public-read=false`。
- 真实 `Access Key` / `Secret Key` 只能通过部署环境注入，禁止写入仓库。
- 日志中禁止打印密钥、完整授权头、完整签名 URL。
- 上传、下载、预览前必须先做业务鉴权，不能仅依赖存储层权限。
- 对象不存在、鉴权失败、网络超时等场景必须记录错误日志并返回明确状态码。

---

## 14. 迁移方案

### 14.1 分阶段迁移策略

#### 阶段 1：代码先支持双模式

- 代码上线后支持 `local` / `oss` 配置切换
- 旧数据继续走 `FILE`
- 新文件根据 `storage.type` 决定写入位置

#### 阶段 2：新文件切 OSS

- 设置 `STORAGE_TYPE=oss`
- 新附件写入 `storage_driver='S3'`
- 历史附件保持 `FILE`

#### 阶段 3：历史文件迁移

- 扫描 `storage_driver='FILE'` 且 `is_deleted=false` 的附件
- 读取本地文件
- 上传到 OSS
- 更新 `storage_driver`、`bucket`、`object_key`、`etag`
- `public_url` 仍保留应用下载地址

### 14.2 迁移脚本核心逻辑

```java
public void migrateFileToOss(Attachment attachment) {
    InputStream localStream = localStorageService.download(attachment.getObjectKey());

    StorageUploadRequest request = new StorageUploadRequest(
        attachment.getObjectKey(),
        localStream,
        attachment.getSizeBytes(),
        attachment.getContentType(),
        attachment.getOriginalName()
    );

    StorageObject result = ossStorageService.upload(request);

    attachment.setStorageDriver("S3");
    attachment.setBucket(result.bucket());
    attachment.setObjectKey(result.objectKey());
    attachment.setEtag(result.etag());
    attachment.setPublicUrl("/api/v1/attachments/" + attachment.getId() + "/download");
}
```

### 14.3 迁移 SQL 查询口径

```sql
SELECT id,
       storage_driver,
       bucket,
       object_key,
       original_name,
       content_type,
       file_ext,
       size_bytes,
       uploaded_by,
       uploaded_at
FROM public.attachment
WHERE COALESCE(is_deleted, false) = false
  AND storage_driver = 'FILE';
```

### 14.4 回滚策略

| 场景 | 处理方式 |
| --- | --- |
| OSS 连接失败 | 将 `STORAGE_TYPE` 切回 `local` |
| 迁移中途失败 | 只迁移成功即更新单条，失败项记录日志后继续 |
| CDN 异常 | 临时关闭 `CDN_ENABLED` |
| HTTPS 证书异常 | 临时将 `CDN_HTTPS=false` |

---

## 15. 测试计划

### 15.1 单元测试

- `LocalStorageServiceTest`
- `OssStorageServiceTest`
- `StoragePropertiesTest`
- `AttachmentApplicationServiceTest`

### 15.2 集成测试

| 用例 | 说明 |
| --- | --- |
| 上传文件到本地 | `storage.type=local`，校验 `storage_driver='FILE'` |
| 上传文件到 OSS | `storage.type=oss`，校验 `storage_driver='S3'`、`bucket/object_key/etag` |
| 查询元数据 | 校验 `AttachmentResponse` 与 `url=/api/v1/attachments/{id}/download` |
| 下载本地文件 | 校验文件流、`Content-Disposition=attachment` |
| 下载 OSS 文件 | 校验应用代理读取对象流，返回状态 `200` |
| 预览本地文件 | 校验 `Content-Disposition=inline` |
| 预览 OSS 文件 | 校验应用代理读取对象流，响应头允许浏览器预览 |
| 已逻辑删除附件访问 | 校验返回 `410 Gone` |
| 不存在附件访问 | 校验返回 `404 Not Found` |
| 历史附件迁移 | 校验 `storage_driver` 与 `bucket/object_key` 更新 |

### 15.3 性能测试

- 并发上传测试
- 30MB 大文件上传测试
- 连续下载与预览压测，观察应用代理流输出稳定性
- OSS 模式下对象流读取超时与重试测试

### 15.4 测试数据要求

| 项目 | 要求 |
| --- | --- |
| 测试文件类型 | 推荐覆盖 `jpg`、`png`、`pdf`、`xlsx`、`txt` |
| 测试文件大小 | `50KB`、`2MB`、`30MB` |
| 上传人 ID | 使用真实存在用户 ID，如 `1` |
| 存储模式 | 分别覆盖 `local` 和 `oss` |
| 删除状态 | 至少准备一条 `is_deleted=true` 的附件记录 |

### 15.5 验收标准（Definition of Done）

- [ ] 上传接口在 `local` 模式可正常工作
- [ ] 上传接口在 `oss` 模式可正常工作
- [ ] 下载接口对现有前端无兼容性破坏
- [ ] 预览接口可用于浏览器在线预览
- [ ] `public.attachment` 字段写入符合规范
- [ ] 已删除附件访问返回 `410`
- [ ] 历史文件迁移脚本可重复执行
- [ ] Nginx 上传体积限制与后端一致
- [ ] 仓库与日志中未出现真实密钥

---

## 16. 部署与运维配置

### 16.1 本地启动

```bash
cd sism-backend
cp .env.example .env
./start.sh
```

### 16.2 使用 Maven 启动

```bash
./mvnw -pl sism-main -am spring-boot:run
```

### 16.3 OSS 连接验证

推荐使用占位环境变量验证，禁止在命令中写死真实密钥：

```bash
export OSS_ACCESS_KEY_ID=your_access_key_id && export OSS_ACCESS_KEY_SECRET=your_access_key_secret && curl -X PUT "https://s3.bitiful.net/sism-files/test.txt" -H "Content-Type: text/plain" --aws-sigv4 "aws:amz:cn-east-1:s3" --user "$OSS_ACCESS_KEY_ID:$OSS_ACCESS_KEY_SECRET" -d "Hello OSS"
```

**预期结果：** 返回 `200 OK` 或兼容成功状态。

### 16.4 Nginx 配置修正

当前 `docs/nginx/sism.conf` 中：

```nginx
client_max_body_size 10M;
```

本次改造建议改为：

```nginx
client_max_body_size 30M;
client_body_timeout 60s;
client_header_timeout 60s;
```

### 16.5 Nginx 与后端限制对齐要求

| 层级 | 当前值 | 目标值 |
| --- | --- | --- |
| Nginx `client_max_body_size` | `10M` | `30M` |
| Spring `max-file-size` | `30MB` | `30MB` |
| Spring `max-request-size` | `30MB` | `30MB` |
| `file.upload.max-size` | `31457280` | `31457280` |

### 16.6 安全基线要求

- 生产环境必须使用私有桶，`OSS_PUBLIC_READ=false`。
- 真实密钥只能通过环境变量、CI/CD 密文或部署平台 Secret 注入。
- `.env`、启动脚本、调试命令、截图、日志中禁止出现真实 `Access Key` / `Secret Key`。
- 下载与预览必须先经过业务鉴权，不能直接把对象存储作为匿名公开入口。
- 日志中允许记录 `bucket`、`object_key`、请求耗时，但禁止输出完整授权头和敏感凭据。

---

## 17. CDN 与 HTTPS 配置

### 17.1 当前定位

CDN 与 HTTPS 在当前阶段属于**可选扩展能力**，用于提升后续访问性能和域名统一性；当前下载和预览主链路仍以应用接口为准。

### 17.2 CDN 地址格式

```text
http://oss.blackevil.cn/attachments/2026/04/uuid.jpg
```

启用 HTTPS 后：

```text
https://oss.blackevil.cn/attachments/2026/04/uuid.jpg
```

### 17.3 配置要求

- CDN 源站需正确指向 Bucket `sism-files`
- CDN 路径中不应包含 bucket 名称
- 启用 HTTPS 前需先完成证书配置与回源验证
- 若 CDN 配置异常，不影响当前应用下载和预览主流程

### 17.4 控制台检查项

```text
1. 登录对象存储/CDN 管理后台
2. 确认域名 oss.blackevil.cn 已接入
3. 确认回源 Bucket 为 sism-files
4. 确认 HTTPS 证书状态正常
5. 确认缓存规则不会覆盖鉴权需求
```

---

## 18. 监控与告警

### 18.1 建议日志

```java
log.info("附件上传成功: driver={}, bucket={}, key={}, size={}", driver, bucket, objectKey, size);
log.info("附件下载成功: driver={}, id={}, key={}", driver, attachmentId, objectKey);
log.info("附件预览成功: driver={}, id={}, key={}", driver, attachmentId, objectKey);
log.warn("附件访问被拒绝: id={}, status={}", attachmentId, statusCode);
log.error("OSS访问失败: bucket={}, key={}, error={}", bucket, objectKey, e.getMessage(), e);
```

### 18.2 关键指标

- 上传成功率
- 上传耗时
- 下载耗时
- 预览耗时
- OSS 读取失败率
- `404` / `410` 返回次数
- OSS 存储量

### 18.3 告警建议阈值

| 指标 | 告警阈值 |
| --- | --- |
| 上传失败率 | `> 5%` |
| 单次上传耗时 | `> 30s` |
| OSS 读取失败率 | `> 1%` |
| 单次下载/预览耗时 | `> 10s` |
| OSS 使用量 | `> 80%` |

---

## 19. 常见问题与排查

### 19.1 上传失败 - `403 Forbidden`

**原因：** Access Key / Secret Key 错误或权限不足  
**解决：** 检查 `OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET` 是否由部署环境正确注入。

### 19.2 上传失败 - `404 Not Found`

**原因：** Bucket 不存在或名称错误  
**解决：** 确认 `OSS_BUCKET_NAME=sism-files` 且回源配置正确。

### 19.3 上传失败 - 超过大小限制

**原因：** Nginx 与 Spring 限制不一致  
**解决：** 将 Nginx `client_max_body_size` 调整为 `30M`。

### 19.4 访问附件返回 `410 Gone`

**原因：** 附件记录已被逻辑删除  
**解决：** 检查 `public.attachment.is_deleted` 状态，确认业务上是否允许恢复。

### 19.5 预览失败但下载成功

**原因：** 浏览器不支持当前文件类型在线预览，或 `content_type` 不准确  
**解决：** 检查数据库 `content_type`，并确认客户端是否支持该 MIME 类型。

### 19.6 OSS 对象不存在

**原因：** 数据库记录存在，但对象未上传成功或已被外部误删  
**解决：** 通过 `bucket + object_key` 核对对象，必要时从备份或原始文件重新上传。

### 19.7 调试日志建议

```yaml
logging:
  level:
    software.amazon.awssdk: DEBUG
    com.sism.shared.infrastructure.storage: DEBUG
```

---

## 20. 实施步骤

### 步骤 1：补齐依赖与配置

- [ ] 在 `sism-shared-kernel/pom.xml` 增加 S3 SDK
- [ ] 在 `application.yml` 增加 `storage.*`
- [ ] 在 `.env.example` 增加 OSS/CDN 配置模板
- [ ] 明确生产环境 Secret 注入方式

### 步骤 2：创建存储抽象

- [ ] 创建 `StorageService`
- [ ] 创建 `StorageUploadRequest`
- [ ] 创建 `StorageObject`
- [ ] 创建 `StorageProperties`
- [ ] 创建 `StorageConfig`

### 步骤 3：实现本地与 OSS 存储

- [ ] 实现 `LocalStorageService`
- [ ] 实现 `OssStorageService`
- [ ] 完成 `S3Client` 初始化
- [ ] 补充对象存在性检查和异常映射

### 步骤 4：改造附件应用服务

- [ ] `AttachmentApplicationService` 改为依赖 `StorageService`
- [ ] 写库时补齐 `storage_driver`、`bucket`、`object_key`、`etag`
- [ ] 下载逻辑兼容 FILE / S3
- [ ] 新增预览接口并统一响应头规范
- [ ] 逻辑删除场景统一返回 `410`

### 步骤 5：补齐测试与文档

- [ ] 增加单元测试
- [ ] 增加集成测试
- [ ] 补齐 OpenAPI / API 文档
- [ ] 更新部署文档和 Nginx 配置
- [ ] 补充安全基线与排查手册

### 步骤 6：灰度上线与迁移

- [ ] 测试环境启用 `STORAGE_TYPE=oss`
- [ ] 验证上传/下载/预览/元数据
- [ ] 验证 `410`、`404`、`403` 场景
- [ ] 执行历史文件迁移
- [ ] 监控错误率与耗时

---

## 21. 上线检查清单

### 21.1 开发前

- [ ] 确认缤纷云账号和密钥可用
- [ ] 确认 Bucket `sism-files` 已创建
- [ ] 确认 Secret 注入方案已确定
- [ ] 确认 CDN 域名 `oss.blackevil.cn` 已可解析（如需启用）

### 21.2 开发完成后

- [ ] `storage.type=local` 自测通过
- [ ] `storage.type=oss` 自测通过
- [ ] 上传 30MB 文件通过
- [ ] 下载地址可访问
- [ ] 预览地址可访问
- [ ] 元数据接口字段完整
- [ ] 已删除附件返回 `410`

### 21.3 部署前

- [ ] 服务器已配置 OSS 环境变量
- [ ] Nginx 上传限制已改为 `30M`
- [ ] 如启用 HTTPS，证书已生效
- [ ] OpenAPI 已更新
- [ ] 仓库、镜像、日志中无真实密钥

### 21.4 上线后

- [ ] 验证上传功能
- [ ] 验证下载功能
- [ ] 验证预览功能
- [ ] 检查 OSS 流量与存储量
- [ ] 检查异常日志
- [ ] 如启用 CDN，验证域名连通性与证书状态

---

## 22. 时间评估

| 项目 | 预估耗时 |
| --- | --- |
| 依赖与配置改造 | `0.5 天` |
| 本地存储抽象改造 | `0.5 天` |
| OSS 存储实现 | `1 天` |
| 附件服务接入与联调 | `1 天` |
| 测试与部署配置修正 | `1 天` |
| 历史文件迁移脚本 | `0.5 天` |

**总计：约 `4.5 天`**

---

## 23. 后续优化

1. 支持分片上传
2. 支持断点续传
3. 图片缩略图、水印、压缩处理
4. 为元数据接口补充 `previewUrl` 字段
5. 增加离线孤儿文件回收任务
6. 支持对象生命周期管理
7. 支持多云存储扩展（COS / MinIO / 阿里云 OSS）

---

## 24. 结论

本方案在保持现有接口兼容的前提下，将附件存储从“本地文件直写”升级为“统一存储抽象 + 可切换 OSS”的实现方式。当前可直接落地的关键结论如下：

1. **配置可执行**：`application.yml`、`.env.example`、Nginx 限制、Secret 注入方式已明确。
2. **数据可兼容**：继续复用 `attachment` 现有字段，不改数据库结构。
3. **接口可落地**：保留上传、元数据、下载接口，并新增预览接口；下载与预览均由应用层代理返回文件流。
4. **安全可控**：私有桶、业务鉴权、`410` 删除语义、密钥脱敏、错误映射和排查策略均已定义。

按本文的文件清单、配置模板、接口约束、测试用例和上线检查项推进，开发团队可以直接进入编码、联调和灰度上线阶段。
