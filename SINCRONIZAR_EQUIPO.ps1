$currentDir = Get-Location
Write-Host ">>> SINCRONIZANDO CON EL TRABAJO DEL EQUIPO (RESET HARD) <<<" -ForegroundColor Red
Write-Host "ADVERTENCIA: Esto sobrescribirá cambios locales no subidos en 'main' para igualarlos al remoto." -ForegroundColor Yellow
Write-Host "OBJETIVO: Obtener la última versión válida del equipo para auditoría."

$mapping = @(
    @{ Path = "MSNucleoSwitch"; Url = "https://github.com/AlisonTamayo/ms-nucleo.git" },
    @{ Path = "ms-directorio"; Url = "https://github.com/AlisonTamayo/ms-directorio.git" },
    @{ Path = "Switch-ms-contabilidad"; Url = "https://github.com/AlisonTamayo/ms-contabilidad.git" },
    @{ Path = "MSCompensacionSwitch"; Url = "https://github.com/AlisonTamayo/ms-compensacion.git" },
    @{ Path = "MSDevolucionSwitch"; Url = "https://github.com/AlisonTamayo/ms-devolucion.git" },
    @{ Path = "switch-frontend"; Url = "https://github.com/AlisonTamayo/switch-frontend.git" }
)

foreach ($item in $mapping) {
    $folderName = $item.Path
    $remoteUrl = $item.Url
    
    if (Test-Path $folderName) {
        Write-Host ""
        Write-Host "-----------------------------------------------------"
        Write-Host "Sincronizando: $folderName" -ForegroundColor Cyan
        Push-Location $folderName

        # 1. Asegurar remoto correcto
        if (-not (Test-Path ".git")) { 
            Write-Host "   - Inicializando git..."
            git init | Out-Null 
        }
        
        $remotes = git remote -v 2>$null
        if ($remotes -match "origin") { 
            git remote set-url origin $remoteUrl 
        }
        else { 
            git remote add origin $remoteUrl 
        }

        # 2. FETCH (Traer todo lo nuevo)
        Write-Host "   - Descargando historia del equipo (git fetch)..."
        git fetch origin main

        # 3. RESET HARD (Forzar local = remoto)
        Write-Host "   - Descartando conflictos y forzando estado local (git reset --hard)..."
        git checkout main 2>$null
        git reset --hard origin/main

        if ($?) { 
            Write-Host "   OK: Alineado con GitHub." -ForegroundColor Green 
        }
        else { 
            Write-Host "   ERROR: Fallo al sincronizar. Revisa permisos o conexión." -ForegroundColor Red 
            Write-Host "   Intentando estrategia alternativa (allow-unrelated)..."
            git pull origin main --allow-unrelated-histories
        }

        Pop-Location
    }
    else {
        Write-Host "Carpeta no encontrada: $folderName" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host ">>> PROCESO COMPLETADO <<<" -ForegroundColor Cyan
