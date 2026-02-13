$currentDir = Get-Location
Write-Host ">>> INICIANDO SUBIDA A REPOSITORIOS AWS <<<" -ForegroundColor Cyan

# Mapeo: Carpeta Local -> URL Remota (Repositorios AWS)
$mapping = @(
    @{ Path = "MSNucleoSwitch"; Url = "https://github.com/StephaniRiveraE/switch-ms-nucleo.git" },
    @{ Path = "MSCompensacionSwitch"; Url = "https://github.com/StephaniRiveraE/switch-ms-compensacion.git" },
    @{ Path = "MSDevolucionSwitch"; Url = "https://github.com/StephaniRiveraE/switch-ms-devolucion.git" },
    @{ Path = "Switch-ms-contabilidad"; Url = "https://github.com/StephaniRiveraE/switch-ms-contabilidad.git" },
    @{ Path = "ms-directorio"; Url = "https://github.com/StephaniRiveraE/switch-ms-directorio.git" },
    @{ Path = "switch-frontend"; Url = "https://github.com/StephaniRiveraE/switch-admin-frontend.git" },
    @{ Path = "repo_switch_transaccional"; Url = "https://github.com/StephaniRiveraE/switch-gateway-server.git" }
)

foreach ($item in $mapping) {
    $folderName = $item.Path
    $remoteUrl = $item.Url
    
    Write-Host ""
    Write-Host "-----------------------------------------------------"
    
    if (Test-Path $folderName) {
        Write-Host "Procesando: $folderName" -ForegroundColor Yellow
        Push-Location $folderName

        # 1. Inicializar Git
        if (-not (Test-Path ".git")) {
            Write-Host "   - Inicializando git..."
            git init | Out-Null
        }

        # 2. Main Branch
        git branch -m main 2>$null

        # 3. Remote
        $remotes = git remote -v 2>$null
        if ($remotes -match "origin") {
            Write-Host "   - Actualizando remoto: $remoteUrl"
            git remote set-url origin $remoteUrl
        }
        else {
            Write-Host "   - Configurando remoto: $remoteUrl"
            git remote add origin $remoteUrl
        }

        # 4. Commit
        Write-Host "   - Git Add All (incluyendo borrados)..."
        git add -A 2>$null
        
        $status = git status --porcelain
        if ($status) {
            Write-Host "   - Git Commit..."
            git commit -m "Migracion AWS APIM - v3.0.0 - Switch Transaccional" | Out-Null
        }
        else {
            Write-Host "   - Nada nuevo por subir."
        }

        # 5. Push
        Write-Host "   Subiendo a GitHub..."
        git push -u origin main
        
        if ($?) {
            Write-Host "   OK: Subido Correctamente." -ForegroundColor Green
        }
        else {
            Write-Host "   ERROR: Fallo el push. Intenta manual: git push -f" -ForegroundColor Red
        }

        Pop-Location
    }
    else {
        Write-Host "ADVERTENCIA: Carpeta no encontrada: $folderName" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host ">>> FIN DEL PROCESO <<<" -ForegroundColor Cyan
