# 并发压测脚本（POST /orders）

该目录提供可直接执行的 PowerShell 压测脚本：`orders-loadtest.ps1`，并附带小规模与大规模一键入口。

## 1. 前置条件

- 后端应用已启动（默认 `http://127.0.0.1:8080`）
- `POST /auth/login` 可正常获取 JWT
- `POST /orders` 可正常下单

建议先切换到 UTF-8 控制台，减少 Windows PowerShell 5.1 中文乱码：

```powershell
chcp 65001
```

## 2. 先做参数干跑（不发请求）

```powershell
cd F:\mianshi\project1\scripts\loadtest
powershell -ExecutionPolicy Bypass -File .\orders-loadtest.ps1 -DryRun

# wrapper 也支持 DryRun（用于快速校验参数与地址）
powershell -ExecutionPolicy Bypass -File .\run-small-scale.ps1 -DryRun
powershell -ExecutionPolicy Bypass -File .\run-large-scale.ps1 -DryRun
```

## 3. 小规模性能测试（基线）

用于快速回归，验证当前版本是否出现明显退化。

```powershell
cd F:\mianshi\project1\scripts\loadtest
powershell -ExecutionPolicy Bypass -File .\run-small-scale.ps1
```

默认参数：`200` 请求、`20` 并发。

## 4. 大规模性能测试（容量）

用于评估高并发压力下的吞吐与稳定性。

```powershell
cd F:\mianshi\project1\scripts\loadtest
powershell -ExecutionPolicy Bypass -File .\run-large-scale.ps1
```

默认参数：`5000` 请求、`200` 并发。

## 5. 幂等压测（固定 requestId）

该模式用于验证重复请求防重逻辑（理论上只应创建 1 笔订单）。

```powershell
cd F:\mianshi\project1\scripts\loadtest
powershell -ExecutionPolicy Bypass -File .\orders-loadtest.ps1 -TotalRequests 50 -Concurrency 10 -FixedRequestId "LOADTEST-IDEMP-001"
```

## 6. 常用参数

- `-BaseUrl`：服务地址，默认 `http://127.0.0.1:8080`
- `-Username/-Password`：登录账号密码，默认 `admin/admin123`
- `-TotalRequests`：总请求数
- `-Concurrency`：并发数
- `-ProductId/-Quantity/-Price`：下单商品参数
- `-TimeoutSeconds`：单请求超时秒数
- `-ReportPath`：CSV 报告输出路径（不传则输出到 `scripts/loadtest/reports`）

## 7. 输出说明

脚本会打印：

- 成功数、失败数、成功率
- 总耗时、QPS
- 延迟 P50/P95/P99
- 失败样本与错误聚合
- CSV 明细报告（每个请求一行）

## 8. 建议验收阈值（可按环境调整）

- 小规模：成功率 `>= 99%`，P95 `< 500ms`
- 大规模：成功率 `>= 98%`，P95 `< 1500ms`
- 幂等：固定 `requestId` 并发压测仅创建 1 笔订单

## 9. 最近一次压测结果（2026-03-16）

- 小规模（`small-scale-20260316-220122.csv`）：`200/20`，成功率 `100%`，QPS `5.37`，P50/P95/P99=`118/141/178ms`
- 大规模（`large-scale-20260316-222146.csv`）：`5000/200`，成功率 `100%`，QPS `4.68`，P50/P95/P99=`100/120/134ms`
- 结论：按当前阈值，小规模和大规模都通过；稳定性达标，后续可继续做更高并发（如 `8000/300`）寻找容量拐点。

