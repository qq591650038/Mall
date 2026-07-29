# Mall

一个基于 Spring Boot 3 和 Vue 3 的模块化商城项目，包含用户端、管理端以及商品、营销、订单、用户等核心业务模块。

## 项目介绍

Mall 采用前后端分离架构：后端以 Maven 多模块单体应用运行，前端拆分为用户端和管理端两个独立的 Vue 应用。项目提供商品浏览、购物车、收藏、优惠券、营销活动、订单、支付、退款、会员积分以及后台运营管理等功能。

> 项目仍在持续开发中，部分支付、物流等第三方能力目前保留扩展接口或使用模拟配置。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2
- Spring Security + JWT
- MyBatis-Plus
- MySQL 8
- Redis 6+
- Flyway
- Knife4j / OpenAPI
- Maven

### 前端

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios
- ECharts（管理端）

## 项目结构

```text
mall/
├── mall-common/       # 公共响应、异常、JWT、Redis 和基础配置
├── mall-user/         # 用户、地址、会员、权限和登录认证
├── mall-marketing/    # 商品、SKU、分类、品牌、购物车、优惠券和营销活动
├── mall-order/        # 订单、支付、退款和库存
├── mall-server/       # Spring Boot 启动模块、接口编排和应用配置
├── mall-web/          # 用户端 Vue 应用
├── mall-admin/        # 管理端 Vue 应用
├── docs/              # 架构、部署和开发文档
├── pom.xml            # Maven 父工程
└── .gitignore
```

## 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 20+
- MySQL 8+
- Redis 6+

## 快速开始

### 1. 初始化数据库

创建名为 `mall` 的 MySQL 数据库，并确保当前账号具有建表和执行迁移脚本的权限。

```sql
CREATE DATABASE mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

项目启动时会通过 Flyway 自动执行 `mall-server/src/main/resources/db/migration` 下的迁移脚本。`docs` 目录中的 SQL 文件可用于导入示例数据。

### 2. 启动后端

确认 MySQL 和 Redis 已启动后，在项目根目录执行：

```powershell
mvn -pl mall-server -am spring-boot:run
```

也可以先打包再运行：

```powershell
mvn -pl mall-server -am clean package -DskipTests
java -jar mall-server/target/mall-server-1.0.0.jar
```

后端默认运行在 `http://localhost:8080`。

### 3. 启动用户端

```powershell
cd mall-web
npm install
npm run dev
```

用户端默认地址为 `http://localhost:5173`。

### 4. 启动管理端

在另一个终端执行：

```powershell
cd mall-admin
npm install
npm run dev
```

管理端默认地址为 `http://localhost:5174`。

两个前端开发服务器都会将 `/api` 请求代理到 `http://localhost:8080`。

## 配置说明

主要配置文件为 `mall-server/src/main/resources/application.yml`。开发环境默认连接：

| 配置项 | 默认值 |
| --- | --- |
| 服务端口 | `8080` |
| MySQL 地址 | `localhost:3306/mall` |
| MySQL 用户名 | `root` |
| MySQL 密码 | `123456`（开发配置） |
| Redis 地址 | `localhost:6379` |

生产环境建议通过环境变量覆盖配置，不要将真实密码、JWT 密钥或第三方密钥提交到仓库：

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/mall"
$env:DB_USERNAME = "mall_user"
$env:DB_PASSWORD = "your-password"
$env:REDIS_HOST = "localhost"
$env:REDIS_PASSWORD = "your-redis-password"
$env:JWT_SECRET = "your-long-random-secret"
$env:SPRING_PROFILES_ACTIVE = "prod"
```

## API 文档与健康检查

后端启动后可访问：

- Knife4j：`http://localhost:8080/doc.html`
- 健康检查：`http://localhost:8080/actuator/health`
- 指标接口：`http://localhost:8080/actuator/metrics`

## 构建与测试

执行后端全部测试：

```powershell
mvn -pl mall-server -am test
```

构建用户端或管理端：

```powershell
cd mall-web
npm run build

cd ..\mall-admin
npm run build
```

## 相关文档

- [后端架构说明](docs/architecture.md)
- [部署说明](docs/deployment.md)
- [功能开发交接文档](docs/功能开发交接文档.md)

## GitHub 提交建议

首次提交前请确认 `.gitignore` 已生效，尤其不要提交以下内容：

- `node_modules/`
- `target/`
- `dist/`
- `.env` 和本地配置
- 数据库密码、JWT 密钥及其他敏感信息

推荐的首次提交信息：

```text
feat: 初始化商城项目
```

## License

本项目暂未指定开源许可证。如需公开分发或允许他人修改、商用，请根据实际需求补充 License 文件。
