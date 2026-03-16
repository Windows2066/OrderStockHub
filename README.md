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

## 5. 功能测试与可靠性测试

### 5.1 单元测试（默认）

```powershell
cd F:\mianshi\project1
.\mvnw.cmd test
```

说明：默认会排除 `integration/performance` 标签，仅执行轻量单元测试。

### 5.2 集成测试（事务、幂等、并发、Outbox）

```powershell
cd F:\mianshi\project1
.\mvnw.cmd verify -DskipTests=false
```

说明：`failsafe` 会执行 `integration` 标签用例，覆盖下单+扣库存+库存流水+Outbox 的端到端链路。

### 5.3 覆盖率报告

执行 `verify` 后查看：`target/site/jacoco/index.html`。

## 6. 性能测试（小规模与大规模）

压测脚本目录：`scripts/loadtest`。

### 6.1 小规模基线压测

```powershell
cd F:\mianshi\project1\scripts\loadtest
powershell -ExecutionPolicy Bypass -File .\run-small-scale.ps1
```

### 6.2 大规模容量压测

```powershell
cd F:\mianshi\project1\scripts\loadtest
powershell -ExecutionPolicy Bypass -File .\run-large-scale.ps1
```

### 6.3 幂等专项压测（固定 requestId）

```powershell
cd F:\mianshi\project1\scripts\loadtest
powershell -ExecutionPolicy Bypass -File .\orders-loadtest.ps1 -TotalRequests 50 -Concurrency 10 -FixedRequestId "LOADTEST-IDEMP-001"
```

更多参数与验收建议见：`scripts/loadtest/README.md`。
