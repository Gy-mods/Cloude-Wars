# Builds Cloude Wars with Maven and copies the jar into the bedwars1 test server as
# servers/bedwars1/plugins/CloudeWars.jar. Stop that server first: Windows keeps the loaded jar locked.
# Usage: powershell -File tools\deploy.ps1 [-SkipTests]
param([switch]$SkipTests)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

# Maven from PATH, else the copy bundled with IntelliJ IDEA
$mvn = (Get-Command mvn -ErrorAction SilentlyContinue).Source
if (-not $mvn) {
    $mvn = Get-ChildItem 'C:\Program Files\JetBrains\*\plugins\maven\lib\maven3\bin\mvn.cmd' -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $mvn) { throw 'Maven not found: install it or IntelliJ IDEA.' }

$mavenArgs = @('-B', '-q', 'package')
if ($SkipTests) { $mavenArgs += '-DskipTests' }
Push-Location $root
try {
    & $mvn @mavenArgs
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed (exit $LASTEXITCODE)." }
} finally {
    Pop-Location
}

$jar = Get-ChildItem "$root\target\CloudeWars-*.jar" | Where-Object { $_.Name -notlike 'original-*' } |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
New-Item -ItemType Directory -Force "$root\servers\bedwars1\plugins" | Out-Null
Copy-Item $jar.FullName "$root\servers\bedwars1\plugins\CloudeWars.jar" -Force
Write-Output "Deployed $($jar.Name) to servers\bedwars1\plugins\CloudeWars.jar"
