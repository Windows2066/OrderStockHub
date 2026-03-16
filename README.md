# project1 本地运行说明

## 1. 启动基础中间件
在项目根目录执行：

```powershell
cd F:\mianshi\project1\infra
docker compose up -d
```

包含组件：MySQL 8、Redis 7、Nacos 2.3.2、RocketMQ（NameServer+Broker）。

## 2. 启动后端
在项目根目录执行：

```powershell
cd F:\mianshi\project1
.\mvnw.cmd spring-boot:run
```

## 3. 获取 JWT

```powershell
curl -X POST "http://127.0.0.1:8080/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

## 4. 调用下单接口
将 `YOUR_TOKEN` 替换为上一步返回的 token：

```powershell
curl -X POST "http://127.0.0.1:8080/orders" -H "Authorization: Bearer YOUR_TOKEN" -H "Content-Type: application/json" -d "{\"requestId\":\"REQ-20260307-0001\",\"userId\":1,\"items\":[{\"productId\":1001,\"quantity\":2,\"price\":10.50}]}"
```

## 5. 执行测试

```powershell
cd F:\mianshi\project1
.\mvnw.cmd test
```

当前已覆盖最小闭环：下单事务内写订单、扣库存、库存流水、Outbox 事件。

