param(
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$AppDir = Join-Path $RootDir "online-judge"
$FrontendDir = Join-Path $AppDir "frontend"

$BackendPort = 8081
$FrontendPort = 5173
$BackendUrl = "http://localhost:$BackendPort/"
$FrontendUrl = "http://localhost:$FrontendPort/app/"

$BackendOut = Join-Path $AppDir "run.out.log"
$BackendErr = Join-Path $AppDir "run.err.log"
$FrontendOut = Join-Path $FrontendDir "vite-dev.out.log"
$FrontendErr = Join-Path $FrontendDir "vite-dev.err.log"

function Fail($Message) {
    Write-Host ""
    Write-Host "Error: $Message" -ForegroundColor Red
    Write-Host ""
    Write-Host "Backend logs:"
    Write-Host "  $BackendOut"
    Write-Host "  $BackendErr"
    Write-Host "Frontend logs:"
    Write-Host "  $FrontendOut"
    Write-Host "  $FrontendErr"
    exit 1
}

function Test-CommandExists($Name) {
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Test-PortListening($Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Test-HttpReady($Url) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    } catch {
        return $false
    }
}

function Wait-HttpReady($Url, $Name, $TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-HttpReady $Url) {
            Write-Host "$Name is ready: $Url" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Start-BackgroundCommand($Name, $WorkingDirectory, $CommandLine, $OutFile, $ErrFile) {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $OutFile) | Out-Null
    Set-Content -LiteralPath $OutFile -Value "" -Encoding UTF8
    Set-Content -LiteralPath $ErrFile -Value "" -Encoding UTF8

    $runnerDir = Join-Path $AppDir "logs"
    New-Item -ItemType Directory -Force -Path $runnerDir | Out-Null
    $runner = Join-Path $runnerDir "start-$($Name.ToLowerInvariant()).cmd"
    $runnerLines = @(
        "@echo off",
        "cd /d `"$WorkingDirectory`"",
        "call $CommandLine 1>`"$OutFile`" 2>`"$ErrFile`""
    )
    Set-Content -LiteralPath $runner -Value $runnerLines -Encoding UTF8

    $launchCommand = "start `"$Name`" /b cmd.exe /d /c call `"$runner`""
    & "$env:ComSpec" /d /c $launchCommand
    if ($LASTEXITCODE -ne 0) {
        Fail "failed to start $Name. Windows launcher exit code: $LASTEXITCODE"
    }
    Write-Host "$Name launch command was sent."
}

function Get-MavenCommand {
    $pathMaven = Get-Command "mvn.cmd" -ErrorAction SilentlyContinue
    if ($pathMaven -and $pathMaven.Source) {
        return $pathMaven.Source
    }

    $homeCandidates = @(
        $env:USERPROFILE,
        $HOME,
        "C:\Users\28974"
    ) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -Unique

    foreach ($homeCandidate in $homeCandidates) {
        $wrapperDists = Join-Path $homeCandidate ".m2\wrapper\dists"
        if (Test-Path $wrapperDists) {
            $localMaven = Get-ChildItem -LiteralPath $wrapperDists -Recurse -Filter "mvn.cmd" -ErrorAction SilentlyContinue |
                Sort-Object FullName -Descending |
                Select-Object -First 1
            if ($localMaven -and $localMaven.FullName) {
                return $localMaven.FullName
            }
        }
    }

    return Join-Path $AppDir "mvnw.cmd"
}

if (!(Test-Path (Join-Path $AppDir "mvnw.cmd"))) {
    Fail "backend startup file not found: $AppDir\mvnw.cmd"
}

if (!(Test-Path (Join-Path $FrontendDir "package.json"))) {
    Fail "frontend package file not found: $FrontendDir\package.json"
}

if (!(Test-CommandExists "java")) {
    Fail "Java 17+ was not found on this machine."
}

if (!(Test-CommandExists "node")) {
    Fail "Node.js was not found on this machine."
}

if (!(Test-CommandExists "npm.cmd")) {
    Fail "npm was not found on this machine."
}

if (!(Test-Path (Join-Path $FrontendDir "node_modules"))) {
    Write-Host "Installing frontend dependencies..."
    Push-Location $FrontendDir
    try {
        & npm.cmd install
        if ($LASTEXITCODE -ne 0) {
            Fail "frontend dependency installation failed."
        }
    } finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "Starting Wenzhong AI Learning Platform..."
Write-Host "Backend:  $BackendUrl"
Write-Host "Frontend: $FrontendUrl"
Write-Host ""

if (Test-HttpReady $BackendUrl) {
    Write-Host "Backend is already running: $BackendUrl" -ForegroundColor Green
} elseif (Test-PortListening $BackendPort) {
    Fail "port $BackendPort is occupied, but backend is not responding."
} else {
    Write-Host "Starting backend service on port $BackendPort..."
    $maven = Get-MavenCommand
    Write-Host "Using Maven: $maven"
    Start-BackgroundCommand "Backend" $AppDir "`"$maven`" -Dskip.frontend=true spring-boot:run" $BackendOut $BackendErr
}

if (Test-HttpReady $FrontendUrl) {
    Write-Host "Frontend is already running: $FrontendUrl" -ForegroundColor Green
} elseif (Test-PortListening $FrontendPort) {
    Fail "port $FrontendPort is occupied, but frontend is not responding."
} else {
    Write-Host "Starting frontend service on port $FrontendPort..."
    Start-BackgroundCommand "Frontend" $FrontendDir "npm.cmd run dev -- --host 127.0.0.1" $FrontendOut $FrontendErr
}

if (!(Wait-HttpReady $BackendUrl "Backend" 180)) {
    Fail "backend did not become ready in time."
}

if (!(Wait-HttpReady $FrontendUrl "Frontend" 90)) {
    Fail "frontend did not become ready in time."
}

Write-Host ""
Write-Host "Website is ready." -ForegroundColor Green
Write-Host "Open: $FrontendUrl"

if (!$NoBrowser) {
    cmd.exe /d /c start "" "$FrontendUrl" | Out-Null
}

exit 0
