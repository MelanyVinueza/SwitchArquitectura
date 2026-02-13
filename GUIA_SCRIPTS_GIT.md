# 📦 Scripts de Subida a GitHub

Este proyecto tiene **DOS scripts** de subida a GitHub para diferentes propósitos:

---

## 🔵 `SUBIR_A_GITHUB.ps1` - Repositorios de Desarrollo

**Propósito:** Subir a repositorios de desarrollo/testing  
**Owner:** `AlisonTamayo`  
**Uso:** Desarrollo local, pruebas, experimentos

### Repositorios
- https://github.com/AlisonTamayo/switch-transaccional.git
- https://github.com/AlisonTamayo/switch-frontend.git
- https://github.com/AlisonTamayo/MSCompensacionSwitch.git
- https://github.com/AlisonTamayo/MSDevolucionSwitch.git
- https://github.com/AlisonTamayo/MSNucleoSwitch.git
- https://github.com/AlisonTamayo/ms-directorio.git

### Cuándo usar
- ✅ Durante desarrollo activo
- ✅ Para pruebas de integración
- ✅ Para compartir código con el equipo
- ✅ Para backups incrementales

---

## 🟢 `SUBIR_AWS_REPOS.ps1` - Repositorios de Producción AWS

**Propósito:** Subir a repositorios oficiales conectados a AWS CI/CD  
**Owner:** `StephaniRiveraE`  
**Uso:** Deploy a producción AWS EKS

### Repositorios (Autorizados para AWS)
- https://github.com/StephaniRiveraE/switch-ms-nucleo.git
- https://github.com/StephaniRiveraE/switch-ms-compensacion.git
- https://github.com/StephaniRiveraE/switch-ms-devolucion.git
- https://github.com/StephaniRiveraE/switch-ms-contabilidad.git
- https://github.com/StephaniRiveraE/switch-ms-directorio.git
- https://github.com/StephaniRiveraE/switch-admin-frontend.git
- https://github.com/StephaniRiveraE/switch-gateway-server.git

### Cuándo usar
- ✅ Cuando el código está listo para producción
- ✅ Para activar GitHub Actions workflows de AWS
- ✅ Para deploy a EKS cluster
- ✅ Después de completar testing en repos de desarrollo

---

## 📋 Mapeo de Carpetas

| Carpeta Local | Repo Desarrollo (AlisonTamayo) | Repo Producción AWS (StephaniRiveraE) |
|---------------|-------------------------------|---------------------------------------|
| `MSNucleoSwitch` | MSNucleoSwitch | switch-ms-nucleo |
| `MSCompensacionSwitch` | MSCompensacionSwitch | switch-ms-compensacion |
| `MSDevolucionSwitch` | MSDevolucionSwitch | switch-ms-devolucion |
| `Switch-ms-contabilidad` | - | switch-ms-contabilidad |
| `ms-directorio` | ms-directorio | switch-ms-directorio |
| `switch-frontend` | switch-frontend | switch-admin-frontend |
| `repo_switch_transaccional` | switch-transaccional | switch-gateway-server |

---

## 🚀 Flujo de Trabajo Recomendado

```mermaid
graph LR
    A[Desarrollar localmente] --> B[SUBIR_A_GITHUB.ps1]
    B --> C[Probar en repos dev]
    C --> D{¿Listo para prod?}
    D -->|Sí| E[SUBIR_AWS_REPOS.ps1]
    D -->|No| A
    E --> F[GitHub Actions]
    F --> G[Deploy a AWS EKS]
```

### Paso a Paso

1. **Desarrollo y Testing**
   ```powershell
   .\SUBIR_A_GITHUB.ps1
   ```
   - Sube a repos de `AlisonTamayo`
   - Permite iteración rápida
   - Sin impacto en producción

2. **Validación**
   - Revisar código
   - Ejecutar tests locales
   - Verificar que todo funciona

3. **Deploy a AWS**
   ```powershell
   .\SUBIR_AWS_REPOS.ps1
   ```
   - Sube a repos de `StephaniRiveraE`
   - Activa GitHub Actions
   - Deploy automático a EKS

---

## ⚙️ Configuración Requerida (Solo AWS Repos)

### GitHub Secrets (en cada repositorio)
Para que los workflows de GitHub Actions funcionen, debes configurar:

```yaml
AWS_ACCESS_KEY_ID: "AKIAXXXXXXXXXXXXXXXX"
AWS_SECRET_ACCESS_KEY: "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

**Cómo configurar:**
1. Ir a cada repositorio en GitHub (StephaniRiveraE/switch-ms-*)
2. Settings → Secrets and variables → Actions
3. New repository secret
4. Agregar `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY`

---

## 🔐 Seguridad

### ⚠️ IMPORTANTE
- **NO** subir credenciales en el código
- **NO** subir `application.properties` con valores reales de producción
- **SÍ** usar variables de entorno en AWS
- **SÍ** usar Kubernetes Secrets para valores sensibles

### Archivos Sensibles (en .gitignore)
```
*.env
application-prod.properties
*.key
*.pem
```

---

## 📝 Checklist Pre-Deploy AWS

Antes de ejecutar `SUBIR_AWS_REPOS.ps1`, verifica:

- [ ] Código testeado localmente
- [ ] `Dockerfile` actualizado con Java 21
- [ ] `.github/workflows/deploy.yml` configurado
- [ ] No hay valores hardcodeados (usar `${VARIABLE}`)
- [ ] `README.md` actualizado
- [ ] GitHub Secrets configurados en repos AWS
- [ ] Kubernetes Secrets preparados (APIM_ORIGIN_SECRET, etc.)

---

## 🆘 Troubleshooting

### Error: "Authentication failed"
**Solución:** 
1. Verifica tus credenciales de GitHub
2. Usa Personal Access Token en vez de password
3. Configura Git credential helper:
   ```powershell
   git config --global credential.helper wincred
   ```

### Error: "Repository not found"
**Solución:**
1. Verifica que tienes acceso a los repos de `StephaniRiveraE`
2. Contacta al owner para que te agregue como colaborador

### Error: "Push rejected"
**Solución:**
El script usa `--force` pero si hay conflictos:
```powershell
cd MSNucleoSwitch
git pull origin main --rebase
git push origin main
```

---

## 📚 Documentación Relacionada

- `MIGRACION_APIM_DEVOPS.md` - Guía técnica completa
- `ESTADO_FINAL_MIGRACION.md` - Estado actual del proyecto
- `SOLICITUD_RUTAS_APIM.md` - Requerimientos al equipo APIM
- `AUDITORIA_MIGRACION_AWS.md` - Reporte de auditoría

---

**Última actualización:** 2026-02-08  
**Versión:** 3.0.0
