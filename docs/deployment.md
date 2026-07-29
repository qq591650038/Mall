# Mall 部署说明

## 本地环境

依赖：JDK 17、Maven 3.9+、MySQL 8、Redis 6+。示例数据库账号为 `root/123456`，Redis 默认无密码。

应用配置文件位于 `mall-server/src/main/resources/application.yml`，公共 Redis、MyBatis-Plus 和 OpenAPI 配置由 `mall-common` 自动加载。生产环境建议通过环境变量覆盖：

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_DATA_REDIS_HOST
SPRING_DATA_REDIS_PORT
JWT_SECRET
```

## 构建与启动

```powershell
mvn -pl mall-server -am clean package -DskipTests
java -jar mall-server/target/mall-server-1.0.0.jar
```

开发测试可运行：

```powershell
mvn -pl mall-server -am test
```

API 默认地址为 `http://localhost:8080`，OpenAPI 文档为 `/doc.html`，Actuator 为 `/actuator/health`。

## 数据库迁移

Flyway 随 `mall-server` 启动执行，迁移脚本位于 `mall-server/src/main/resources/db/migration`。已发布脚本不得修改；新增变更请使用递增版本（当前后续使用 V4、V5）。首次部署前确认数据库账号具有建表和执行 DDL 权限。

## Docker 示例

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY mall-server/target/mall-server-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```powershell
docker build -t mall-server .
docker run --rm -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=prod `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/mall" `
  -e SPRING_DATASOURCE_USERNAME=root `
  -e SPRING_DATASOURCE_PASSWORD=123456 `
  mall-server
```

生产环境应使用独立 MySQL/Redis、限制 Actuator 暴露范围，并由现有平台收集标准输出日志和配置告警。本项目暂不绑定具体日志、支付或物流厂商。
