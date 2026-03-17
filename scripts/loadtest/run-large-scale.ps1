param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$ProductIds = "1001,1002,1003,1004,1005",
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Normalize console encoding for Windows PowerShell 5.1 to reduce mojibake risk.
chcp 65001 > $null
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$report = Join-Path $scriptDir ("reports/large-scale-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".csv")

Write-Host "[Large] Start capacity load test (5000 requests / 200 concurrency)..."

$forwardDryRun = @()
if ($DryRun) {
    $forwardDryRun += "-DryRun"
}

powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir "orders-loadtest.ps1") `
    -BaseUrl $BaseUrl `
    -TotalRequests 5000 `
    -Concurrency 200 `
    -ProductIds $ProductIds `
    -Quantity 1 `
    -Price 9.90 `
    -ReportPath $report `
    @forwardDryRun

Write-Host "[Large] Done. Report: $report"

