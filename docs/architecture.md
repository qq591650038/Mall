# Mall 后端架构说明

## 1. 总体结构

项目采用模块化单体。模块在同一进程运行，通过 Maven 依赖和 Java 接口协作，暂不引入消息队列、搜索引擎等中间件。

```text
mall-server（启动、基础设施、跨域编排）
    └── mall-order（订单、支付、退款、库存）
            ├── mall-user（用户、地址、管理员权限）
            └── mall-marketing（商品、SKU、分类、购物车、优惠券、营销）
                    └── mall-common（结果、异常、安全、Redis、Hutool）
```

依赖方向为 `server -> order -> user/marketing -> common`。业务模块不反向依赖 server，避免循环依赖。

## 2. 模块职责

| 模块 | 职责 |
| --- | --- |
| `mall-common` | `Result`、分页、错误码、全局异常处理、JWT、密码工具、Redis 工具、Redis/MyBatis-Plus 公共配置、订单状态机、Hutool 公共依赖 |
| `mall-user` | 用户注册登录、用户资料、地址地区、管理员、角色权限、操作日志和管理端操作审计切面 |
| `mall-marketing` | 商品、SKU、图片、分类、品牌、Banner、购物车、优惠券、收藏、浏览历史 |
| `mall-order` | 订单创建与状态流转、支付流水、退款、库存扣减/恢复、订单和库存管理 |
| `mall-server` | `MallApplication`、数据源/Flyway/Security/Actuator 应用级配置、文件上传、Dashboard、跨模块编排 |

`mall-server` 不承载业务实体、Mapper、业务 Service、业务 Controller 或管理审计切面；公共配置随 `mall-common` 发布并由启动模块自动加载，确需同时聚合多个模块的 Dashboard 等功能可保留在 server。

## 3. 数据访问约定

- 简单 CRUD 使用 MyBatis-Plus `BaseMapper`。
- 涉及条件更新、库存 CAS 扣减、幂等、批量查询等复杂 SQL 必须写入对应模块的 `resources/mapper/*.xml`，Mapper 接口只保留方法声明。
- 库存操作使用条件更新保证并发安全，并通过 `inventory_log` 记录扣减、恢复和补偿。
- SKU 唯一键冲突使用 `ON DUPLICATE KEY UPDATE`，保存接口可重复调用。

## 4. 核心流程

订单创建先校验商品和库存并预扣库存，再创建订单及明细；支付成功后完成实际库存扣减并更新支付流水。支付失败、取消或超时会恢复预扣库存。订单状态由 `OrderStateMachine` 统一校验，支付回调按第三方流水号幂等处理。

Redis 用于缓存和预扣库存。缓存不可用时商品查询回源 MySQL；库存操作失败必须返回业务错误，不能静默当作缓存降级。

## 5. 可观测性

`mall-server` 提供 Actuator 健康检查和指标端点。业务代码记录接口耗时、异常、库存失败和缓存命中/未命中指标；日志输出到标准输出，生产环境由部署平台收集和告警。

## 6. 安全与边界

用户和管理员使用 JWT，接口权限由 Spring Security 控制。真实微信/支付宝 SDK、物流平台轨迹以及集中日志平台配置暂不在本阶段实现，保留支付回调和物流服务的扩展接口。

## 7. 前端

`mall-web` 是用户端 Vue 3 应用，`mall-admin` 是管理端 Vue 3 应用，分别通过 REST API 访问 `mall-server`。
