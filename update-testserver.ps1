# Baut Paper-Plugin + Fabric-Mod neu und startet beide Testserver.
# Vorher beide laufenden Server mit "stop" beenden!
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

.\gradlew.bat build --console=plain
if ($LASTEXITCODE -ne 0) { Write-Host "Build fehlgeschlagen." -ForegroundColor Red; exit 1 }

Copy-Item "paper\build\libs\FabisOSC-Paper-1.0.0.jar" "server\plugins\" -Force
Write-Host "Paper-Plugin kopiert." -ForegroundColor Green

# Paper-Server (Port 25565) und Fabric-Server (Port 25567) in eigenen Fenstern starten
Start-Process powershell -WorkingDirectory "$PSScriptRoot\server" `
    -ArgumentList '-NoExit', '-Command', 'java -Xms2G -Xmx4G -jar paper.jar --nogui'
Start-Process powershell -WorkingDirectory $PSScriptRoot `
    -ArgumentList '-NoExit', '-Command', '.\gradlew.bat :fabric:runServer --console=plain'
Write-Host "Beide Server starten in eigenen Fenstern." -ForegroundColor Green
