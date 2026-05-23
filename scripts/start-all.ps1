# 一键启动：Redis(6379) -> 后端(8080) -> nginx(80)
# 依赖：MySQL80 服务开机自启；JDK17、Maven 已配好
$ErrorActionPreference = 'Stop'
# 项目根目录取脚本上一级，Redis 和 nginx 的目录允许用环境变量覆盖
$root = Split-Path -Parent $PSScriptRoot
$redis = if ($env:FRESHMART_REDIS_HOME) { $env:FRESHMART_REDIS_HOME } else { 'D:\Redis-x64-3.2.100' }
$nginx = if ($env:FRESHMART_NGINX_HOME) { $env:FRESHMART_NGINX_HOME } else { 'D:\nginx-1.20.2- FreshMart' }
$jar = Join-Path $root 'freshmart-server\target\freshmart-server-1.0-SNAPSHOT.jar'

function Test-Port($port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

if (-not (Get-Service MySQL80 -ErrorAction SilentlyContinue).Status -eq 'Running') {
    Write-Host 'MySQL80 未运行，尝试启动...' -ForegroundColor Yellow
    Start-Service MySQL80
}

if (-not (Test-Port 6379)) {
    Write-Host '启动 Redis ...' -ForegroundColor Cyan
    Start-Process (Join-Path $redis 'redis-server.exe') -WorkingDirectory $redis -WindowStyle Hidden
    Start-Sleep -Seconds 2
} else { Write-Host 'Redis 已在运行' -ForegroundColor DarkGray }

if (-not (Test-Port 8080)) {
    if (-not (Test-Path $jar)) {
        Write-Host '未找到 jar，先打包：mvn -DskipTests clean package' -ForegroundColor Yellow
        Push-Location $root
        mvn -q -DskipTests clean package
        Pop-Location
    }
    Write-Host '启动后端 8080 ...' -ForegroundColor Cyan
    Start-Process -FilePath 'java' -ArgumentList '-jar', $jar `
        -RedirectStandardOutput (Join-Path $root 'logs\app.out.log') `
        -RedirectStandardError (Join-Path $root 'logs\app.err.log') `
        -WindowStyle Hidden
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Port 8080) { break }
    }
} else { Write-Host '后端已在运行' -ForegroundColor DarkGray }

if (-not (Test-Port 80)) {
    Write-Host '启动 nginx 80 ...' -ForegroundColor Cyan
    Start-Process (Join-Path $nginx 'nginx.exe') -WorkingDirectory $nginx -WindowStyle Hidden
    Start-Sleep -Seconds 2
} else { Write-Host 'nginx 已在运行' -ForegroundColor DarkGray }

foreach ($p in 6379, 8080, 80) {
    $ok = Test-Port $p
    Write-Host ("{0,-5} {1}" -f $p, $(if ($ok) { 'OK' } else { 'FAILED' })) -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' })
}
Write-Host ''
Write-Host '运营后台  http://localhost        admin / 123456'
Write-Host '接口文档  http://localhost:8080/doc.html'
