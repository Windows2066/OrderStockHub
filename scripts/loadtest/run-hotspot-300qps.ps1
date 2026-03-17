param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$Username = "admin",
    [string]$Password = "admin123",
    [int]$DurationSeconds = 300,
    [int]$TargetQps = 300,
    [int]$Concurrency = 200,
    [long]$UserId = 1,
    [int]$Quantity = 1,
    [decimal]$Price = 9.90,
    [int]$OnSaleStartProductId = 1001,
    [int]$OnSaleSkuCount = 1200,
    [int]$HotSkuCount = 50,
    [int]$UltraHotSkuCount = 2,
    [double]$UltraHotWeight = 0.20,
    [double]$HotWeight = 0.70,
    [double]$TailWeight = 0.10,
    [int]$TimeoutSeconds = 10,
    [string]$ReportPath = "",
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

if ($DurationSeconds -le 0) { throw "DurationSeconds must be > 0" }
if ($TargetQps -le 0) { throw "TargetQps must be > 0" }
if ($Concurrency -le 0) { throw "Concurrency must be > 0" }
if ($Quantity -le 0) { throw "Quantity must be > 0" }
if ($TimeoutSeconds -le 0) { throw "TimeoutSeconds must be > 0" }
if ($UltraHotSkuCount -le 0) { throw "UltraHotSkuCount must be > 0" }
if ($HotSkuCount -le 0) { throw "HotSkuCount must be > 0" }
if (($UltraHotSkuCount + $HotSkuCount) -ge $OnSaleSkuCount) {
    throw "UltraHotSkuCount + HotSkuCount must be < OnSaleSkuCount"
}

$weightSum = $UltraHotWeight + $HotWeight + $TailWeight
if ([Math]::Abs($weightSum - 1.0) -gt 0.000001) {
    throw "UltraHotWeight + HotWeight + TailWeight must equal 1.0"
}

# Raise client connection caps for Windows PowerShell/.NET Framework to avoid artificial throttling.
[System.Net.ServicePointManager]::DefaultConnectionLimit = [Math]::Max(1024, $Concurrency * 8)
[System.Net.ServicePointManager]::Expect100Continue = $false

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$ordersUrl = "$BaseUrl/orders"
$loginUrl = "$BaseUrl/auth/login"
$healthUrl = "$BaseUrl/actuator/health"
$plannedTotal = $DurationSeconds * $TargetQps

$ultraHotIds = @($OnSaleStartProductId..($OnSaleStartProductId + $UltraHotSkuCount - 1))
$hotStart = $OnSaleStartProductId + $UltraHotSkuCount
$hotIds = @($hotStart..($hotStart + $HotSkuCount - 1))
$tailStart = $hotStart + $HotSkuCount
$tailCount = $OnSaleSkuCount - $UltraHotSkuCount - $HotSkuCount
$tailIds = @($tailStart..($tailStart + $tailCount - 1))

function Get-RandomElement {
    param([long[]]$Ids)
    if ($Ids.Count -eq 1) {
        return $Ids[0]
    }
    $idx = Get-Random -Minimum 0 -Maximum $Ids.Count
    return $Ids[$idx]
}

function Get-WeightedProductId {
    param(
        [double]$UltraW,
        [double]$HotW,
        [long[]]$UltraIds,
        [long[]]$HotIds,
        [long[]]$TailIds
    )

    $r = Get-Random -Minimum 0.0 -Maximum 1.0
    if ($r -lt $UltraW) {
        return (Get-RandomElement -Ids $UltraIds)
    }

    if ($r -lt ($UltraW + $HotW)) {
        return (Get-RandomElement -Ids $HotIds)
    }

    return (Get-RandomElement -Ids $TailIds)
}

if ($DryRun) {
    Write-Host "[DryRun] Hotspot load test config:"
    Write-Host "  DurationSeconds=$DurationSeconds, TargetQps=$TargetQps, Concurrency=$Concurrency"
    Write-Host "  PlannedTotal=$plannedTotal"
    Write-Host "  TotalSku=6000, OnSaleSku=$OnSaleSkuCount"
    Write-Host "  UltraHotSku=$UltraHotSkuCount ($UltraHotWeight), HotSku=$HotSkuCount ($HotWeight), TailSku=$tailCount ($TailWeight)"
    Write-Host "  UltraHotRange=$($ultraHotIds[0])..$($ultraHotIds[$ultraHotIds.Count - 1])"
    Write-Host "  HotRange=$($hotIds[0])..$($hotIds[$hotIds.Count - 1])"
    Write-Host "  TailRange=$($tailIds[0])..$($tailIds[$tailIds.Count - 1])"
    Write-Host "  Health=$healthUrl"
    Write-Host "  Login=$loginUrl"
    Write-Host "  Orders=$ordersUrl"
    exit 0
}

$handler = New-Object System.Net.Http.HttpClientHandler
$client = New-Object System.Net.Http.HttpClient($handler)
$client.Timeout = [TimeSpan]::FromSeconds($TimeoutSeconds)

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
    $results = [System.Collections.Concurrent.ConcurrentBag[object]]::new()
    $startedAt = Get-Date
    $suiteSw = [System.Diagnostics.Stopwatch]::StartNew()

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

        if ($null -eq $script:localHandler) {
            $script:localHandler = New-Object System.Net.Http.HttpClientHandler
            if ($script:localHandler.PSObject.Properties.Name -contains 'MaxConnectionsPerServer') {
                $script:localHandler.MaxConnectionsPerServer = 1024
            }
            $script:localClient = New-Object System.Net.Http.HttpClient($script:localHandler)
            $script:localClient.Timeout = [TimeSpan]::FromSeconds($ReqTimeoutSeconds)
        }
        $script:localClient.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $JwtToken)

        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $httpCode = 0
        $bizCode = -1
        $bizMessage = ""
        $orderNo = ""
        $err = ""

        try {
            $payload = @{
                requestId = $RequestId
                userId = $ReqUserId
                items = @(@{
                    productId = $ReqProductId
                    quantity = $ReqQuantity
                    price = [decimal]::Round($ReqPrice, 2)
                })
            } | ConvertTo-Json -Compress

            $content = [System.Net.Http.StringContent]::new($payload, [System.Text.Encoding]::UTF8, "application/json")
            $resp = $script:localClient.PostAsync($OrdersUrl, $content).GetAwaiter().GetResult()
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
            $sw.Stop()
        }

        [pscustomobject]@{
            index = $Index
            requestId = $RequestId
            productId = $ReqProductId
            httpCode = $httpCode
            bizCode = $bizCode
            bizMessage = $bizMessage
            orderNo = $orderNo
            elapsedMs = $sw.ElapsedMilliseconds
            success = ($httpCode -eq 200 -and $bizCode -eq 0)
            error = $err
        }
    }

    $pool = [RunspaceFactory]::CreateRunspacePool(1, $Concurrency)
    $pool.Open()
    $pending = New-Object System.Collections.Generic.List[object]

    try {
        for ($i = 0; $i -lt $plannedTotal; $i++) {
            $expectedMs = [int](($i + 1) * 1000.0 / $TargetQps)
            while ($suiteSw.ElapsedMilliseconds -lt $expectedMs) {
                for ($p = $pending.Count - 1; $p -ge 0; $p--) {
                    $task = $pending[$p]
                    if ($task.handle.IsCompleted) {
                        try {
                            $taskResults = $task.ps.EndInvoke($task.handle)
                            foreach ($result in $taskResults) { $results.Add($result) }
                        } catch {
                            $results.Add([pscustomobject]@{
                                index = $task.index
                                requestId = $task.requestId
                                productId = $task.productId
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
                            $pending.RemoveAt($p)
                        }
                    }
                }
                Start-Sleep -Milliseconds 1
            }

            while ($pending.Count -ge $Concurrency) {
                $drained = $false
                for ($p = $pending.Count - 1; $p -ge 0; $p--) {
                    $task = $pending[$p]
                    if ($task.handle.IsCompleted) {
                        $drained = $true
                        try {
                            $taskResults = $task.ps.EndInvoke($task.handle)
                            foreach ($result in $taskResults) { $results.Add($result) }
                        } catch {
                            $results.Add([pscustomobject]@{
                                index = $task.index
                                requestId = $task.requestId
                                productId = $task.productId
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
                            $pending.RemoveAt($p)
                        }
                    }
                }

                if (-not $drained) {
                    Start-Sleep -Milliseconds 2
                }
            }

            $requestId = "HT-$runId-$i-$([guid]::NewGuid().ToString('N').Substring(0, 8))"
            $productId = Get-WeightedProductId -UltraW $UltraHotWeight -HotW $HotWeight -UltraIds $ultraHotIds -HotIds $hotIds -TailIds $tailIds

            $ps = [PowerShell]::Create()
            $ps.RunspacePool = $pool
            [void]$ps.AddScript($requestWorker.ToString())
            [void]$ps.AddArgument($i)
            [void]$ps.AddArgument($ordersUrl)
            [void]$ps.AddArgument($requestId)
            [void]$ps.AddArgument($UserId)
            [void]$ps.AddArgument($productId)
            [void]$ps.AddArgument($Quantity)
            [void]$ps.AddArgument($Price)
            [void]$ps.AddArgument($token)
            [void]$ps.AddArgument($TimeoutSeconds)

            $handle = $ps.BeginInvoke()
            $pending.Add([pscustomobject]@{
                index = $i
                requestId = $requestId
                productId = $productId
                ps = $ps
                handle = $handle
            })
        }

        while ($pending.Count -gt 0) {
            for ($p = $pending.Count - 1; $p -ge 0; $p--) {
                $task = $pending[$p]
                if ($task.handle.IsCompleted) {
                    try {
                        $taskResults = $task.ps.EndInvoke($task.handle)
                        foreach ($result in $taskResults) { $results.Add($result) }
                    } catch {
                        $results.Add([pscustomobject]@{
                            index = $task.index
                            requestId = $task.requestId
                            productId = $task.productId
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
                        $pending.RemoveAt($p)
                    }
                }
            }
            Start-Sleep -Milliseconds 2
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

    $actualQps = if ($suiteSw.Elapsed.TotalSeconds -gt 0) {
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
    Write-Host "TargetQps: $TargetQps"
    Write-Host "ActualQps: $actualQps"
    Write-Host "OK:    $($successRows.Count)"
    Write-Host "Fail:  $($failedRows.Count)"
    Write-Host "Rate:  $([Math]::Round(($successRows.Count * 100.0 / [Math]::Max($rows.Count, 1)), 2))%"
    Write-Host "Cost:  $([Math]::Round($suiteSw.Elapsed.TotalMilliseconds, 0)) ms"
    Write-Host "P50/P95/P99: $p50 / $p95 / $p99 ms"

    if ($failedRows.Count -gt 0) {
        Write-Host ""
        Write-Host "---- Failed samples (top 10) ----"
        $failedRows |
            Select-Object -First 10 index, requestId, productId, httpCode, bizCode, bizMessage, error, elapsedMs |
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
        $ReportPath = Join-Path $reportDir "hotspot-300qps-$runId.csv"
    }

    $rows |
        Sort-Object index |
        Export-Csv -Path $ReportPath -NoTypeInformation -Encoding UTF8

    Write-Host "Report exported: $ReportPath"
}
finally {
    if ($null -ne $client) { $client.Dispose() }
    if ($null -ne $handler) { $handler.Dispose() }
}

