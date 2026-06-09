$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RootDir

docker version --format "{{.Server.Version}}" | Out-Null
docker compose up --build -d

$Port = if ($env:SERVER_PORT) { $env:SERVER_PORT } else { "8081" }
Write-Host "Wenzhong OJ is starting at http://localhost:$Port/app/"
Write-Host "Teacher/system status: http://localhost:$Port/app/teacher-management"
