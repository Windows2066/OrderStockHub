param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$Username = "admin",
    [string]$Password = "admin123",
    [int]$TotalRequests = 200,
    [int]$Concurrency = 20,
    [long]$UserId = 1,
    [long]$ProductId = 1001,
    [int]$Quantity = 1,
    [decimal]$Price = 9.90,
    [string]$FixedRequestId = "",
    [int]$TimeoutSeconds = 10,
    [string]$ReportPath = "",
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Windows PowerShell 5.1 默认不一定预加载该程序集，先显式加载以避免类型找不到。
Add-Type -AssemblyName System.Net.Http

if ($TotalRequests -le 0) { throw "TotalRequests must be > 0" }
if ($Concurrency -le 0) { throw "Concurrency must be > 0" }
if ($Quantity -le 0) { throw "Quantity must be > 0" }
if ($TimeoutSeconds -le 0) { throw "TimeoutSeconds must be > 0" }

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$ordersUrl = "$BaseUrl/orders"
$loginUrl = "$BaseUrl/auth/login"
$healthUrl = "$BaseUrl/actuator/health"

# Dry run only prints the plan and exits.
if ($DryRun) {
    Write-Host "[DryRun] Load test config:"
    Write-Host "  BaseUrl=$BaseUrl"
    Write-Host "  TotalRequests=$TotalRequests, Concurrency=$Concurrency"
    Write-Host "  UserId=$UserId, ProductId=$ProductId, Quantity=$Quantity, Price=$Price"
    if ([string]::IsNullOrWhiteSpace($FixedRequestId)) {
        Write-Host "  requestId mode=unique per request"
    } else {
        Write-Host "  requestId mode=fixed ($FixedRequestId)"
    }
    Write-Host "  Health=$healthUrl"
    Write-Host "  Login=$loginUrl"
    Write-Host "  Orders=$ordersUrl"
    exit 0
}

# Reuse one HttpClient to avoid connection setup noise in metrics.
$handler = New-Object System.Net.Http.HttpClientHandler
$client = New-Object System.Net.Http.HttpClient($handler)
$client.Timeout = [System.TimeSpan]::FromSeconds($TimeoutSeconds)

try {
    try {
        $healthResp = $client.GetAsync($healthUrl).GetAwaiter().GetResult()
        Write-Host "[PreCheck] /actuator/health HTTP=$([int]$healthResp.StatusCode)"
    } catch {
        Write-Warning "[PreCheck] health failed: $($_.Exception.Message)"
    }

    $loginPayload = @{ username = $Username; password = $Password } | ConvertTo-Json -Compress
    $loginContent = [System.Net.Http.StringContent]::new($loginPayload, [System.Text.Encoding]::UTF8, "application/json")
    $loginResp = $client.PostAsync($loginUrl, $loginContent).GetAwaiter().GetResult()
    $loginBody = $loginResp.Content.ReadAsStringAsync().GetAwaiter().GetResult()

    if (-not $loginResp.IsSuccessStatusCode) {
        throw "Login failed, HTTP=$([int]$loginResp.StatusCode), body=$loginBody"
    }

    $loginJson = $loginBody | ConvertFrom-Json
    if ($null -eq $loginJson.data -or [string]::IsNullOrWhiteSpace($loginJson.data.token)) {
        throw "Login response has no token: $loginBody"
    }

    $token = [string]$loginJson.data.token
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $token)

    $results = [System.Collections.Concurrent.ConcurrentBag[object]]::new()
    $startedAt = Get-Date
    $suiteSw = [System.Diagnostics.Stopwatch]::StartNew()

    # 使用 RunspacePool 并发执行请求，降低 Start-Job 在 PS5.1 下的进程开销。
    $requestWorker = {
        param(
            [int]$Index,
            [string]$OrdersUrl,
            [string]$RequestId,
            [long]$ReqUserId,
            [long]$ReqProductId,
            [int]$ReqQuantity,
            [decimal]$ReqPrice,
            [string]$JwtToken,
            [int]$ReqTimeoutSeconds
        )

        Add-Type -AssemblyName System.Net.Http
        $handler = New-Object System.Net.Http.HttpClientHandler
        $localClient = New-Object System.Net.Http.HttpClient($handler)
        $localClient.Timeout = [System.TimeSpan]::FromSeconds($ReqTimeoutSeconds)
        $localClient.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $JwtToken)

        $reqSw = [System.Diagnostics.Stopwatch]::StartNew()
        $httpCode = 0
        $bizCode = -1
        $bizMessage = ""
        $orderNo = ""
        $err = ""

        try {
            $orderPayload = @{
                requestId = $RequestId
                userId = $ReqUserId
                items = @(@{ productId = $ReqProductId; quantity = $ReqQuantity; price = [decimal]::Round($ReqPrice, 2) })
            } | ConvertTo-Json -Compress

            $content = [System.Net.Http.StringContent]::new($orderPayload, [System.Text.Encoding]::UTF8, "application/json")
            $resp = $localClient.PostAsync($OrdersUrl, $content).GetAwaiter().GetResult()
            $httpCode = [int]$resp.StatusCode
            $body = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()

            if (-not [string]::IsNullOrWhiteSpace($body)) {
                $json = $body | ConvertFrom-Json
                if ($null -ne $json.code) { $bizCode = [int]$json.code }
                if ($null -ne $json.message) { $bizMessage = [string]$json.message }
                if ($null -ne $json.data -and $null -ne $json.data.orderNo) { $orderNo = [string]$json.data.orderNo }
            }
        } catch {
            $err = $_.Exception.Message
        } finally {
            $reqSw.Stop()
            if ($null -ne $localClient) { $localClient.Dispose() }
            if ($null -ne $handler) { $handler.Dispose() }
        }

        $ok = ($httpCode -eq 200 -and $bizCode -eq 0)
        [pscustomobject]@{
            index = $Index
            requestId = $RequestId
            httpCode = $httpCode
            bizCode = $bizCode
            bizMessage = $bizMessage
            orderNo = $orderNo
            elapsedMs = $reqSw.ElapsedMilliseconds
            success = $ok
            error = $err
        }
    }

    $pool = [RunspaceFactory]::CreateRunspacePool(1, $Concurrency)
    $pool.Open()

    $pending = New-Object System.Collections.Generic.List[object]
    try {
        for ($i = 0; $i -lt $TotalRequests; $i++) {
            $requestId = if ([string]::IsNullOrWhiteSpace($FixedRequestId)) {
                "LT-$runId-$i-$([guid]::NewGuid().ToString('N').Substring(0, 8))"
            } else {
                $FixedRequestId
            }

            $ps = [PowerShell]::Create()
            $ps.RunspacePool = $pool
            [void]$ps.AddScript($requestWorker.ToString())
            [void]$ps.AddArgument($i)
            [void]$ps.AddArgument($ordersUrl)
            [void]$ps.AddArgument($requestId)
            [void]$ps.AddArgument($UserId)
            [void]$ps.AddArgument($ProductId)
            [void]$ps.AddArgument($Quantity)
            [void]$ps.AddArgument($Price)
            [void]$ps.AddArgument($token)
            [void]$ps.AddArgument($TimeoutSeconds)

            $handle = $ps.BeginInvoke()
            $pending.Add([pscustomobject]@{
                index = $i
                requestId = $requestId
                ps = $ps
                handle = $handle
            })
        }

        foreach ($task in $pending) {
            try {
                $taskResults = $task.ps.EndInvoke($task.handle)
                foreach ($taskResult in $taskResults) {
                    $results.Add($taskResult)
                }
            } catch {
                $results.Add([pscustomobject]@{
                    index = $task.index
                    requestId = $task.requestId
                    httpCode = 0
                    bizCode = -1
                    bizMessage = ""
                    orderNo = ""
                    elapsedMs = 0
                    success = $false
                    error = $_.Exception.Message
                })
            } finally {
                $task.ps.Dispose()
            }
        }
    } finally {
        if ($null -ne $pool) {
            $pool.Close()
            $pool.Dispose()
        }
    }

    $suiteSw.Stop()
    $endedAt = Get-Date

    $rows = @($results)
    $successRows = @($rows | Where-Object { $_.success })
    $failedRows = @($rows | Where-Object { -not $_.success })

    $latencies = @($rows | Select-Object -ExpandProperty elapsedMs | Sort-Object)
    $p50 = 0
    $p95 = 0
    $p99 = 0
    if ($latencies.Count -gt 0) {
        $p50 = $latencies[[Math]::Min([int]([Math]::Ceiling($latencies.Count * 0.50)) - 1, $latencies.Count - 1)]
        $p95 = $latencies[[Math]::Min([int]([Math]::Ceiling($latencies.Count * 0.95)) - 1, $latencies.Count - 1)]
        $p99 = $latencies[[Math]::Min([int]([Math]::Ceiling($latencies.Count * 0.99)) - 1, $latencies.Count - 1)]
    }

    $qps = if ($suiteSw.Elapsed.TotalSeconds -gt 0) {
        [Math]::Round($rows.Count / $suiteSw.Elapsed.TotalSeconds, 2)
    } else {
        0
    }

    Write-Host ""
    Write-Host "================ Summary ================"
    Write-Host "Start: $startedAt"
    Write-Host "End:   $endedAt"
    Write-Host "Total: $($rows.Count)"
    Write-Host "Conc:  $Concurrency"
    Write-Host "OK:    $($successRows.Count)"
    Write-Host "Fail:  $($failedRows.Count)"
    Write-Host "Rate:  $([Math]::Round(($successRows.Count * 100.0 / [Math]::Max($rows.Count, 1)), 2))%"
    Write-Host "Cost:  $([Math]::Round($suiteSw.Elapsed.TotalMilliseconds, 0)) ms"
    Write-Host "QPS:   $qps"
    Write-Host "P50/P95/P99: $p50 / $p95 / $p99 ms"

    if ($failedRows.Count -gt 0) {
        Write-Host ""
        Write-Host "---- Failed samples (top 10) ----"
        $failedRows |
            Select-Object -First 10 index, requestId, httpCode, bizCode, bizMessage, error, elapsedMs |
            Format-Table -AutoSize |
            Out-String |
            Write-Host

        Write-Host "---- Failed groups ----"
        $failedRows |
            Group-Object -Property httpCode, bizCode, bizMessage |
            Sort-Object Count -Descending |
            Select-Object -First 10 Count, Name |
            Format-Table -AutoSize |
            Out-String |
            Write-Host
    }

    if ([string]::IsNullOrWhiteSpace($ReportPath)) {
        $reportDir = Join-Path (Split-Path -Parent $PSCommandPath) "reports"
        if (-not (Test-Path $reportDir)) {
            New-Item -ItemType Directory -Path $reportDir | Out-Null
        }
        $ReportPath = Join-Path $reportDir "orders-loadtest-$runId.csv"
    }

    $rows |
        Sort-Object index |
        Export-Csv -Path $ReportPath -NoTypeInformation -Encoding UTF8

    Write-Host "Report exported: $ReportPath"
}
finally {
    if ($null -ne $client) {
        $client.Dispose()
    }
    if ($null -ne $handler) {
        $handler.Dispose()
    }
}

