# 📋 SOLICITUD FORMAL AL EQUIPO APIM

**De:** Equipo Switch Transaccional  
**Para:** Equipo APIM (API Gateway Infrastructure)  
**Fecha:** 2026-02-08  
**CC:** Arquitectura, DevOps, Product Owner

---

## 🎯 Objetivo

Solicitar la configuración de **3 rutas adicionales** en el AWS API Gateway (APIM) para completar la migración de Kong a APIM y mantener paridad funcional.

---

## 📊 Estado Actual

### ✅ Rutas YA configuradas en APIM
1. `POST /api/v2/switch/transfers` - Crear transferencia
2. `POST /api/v2/compensation/upload` - Subir archivos compensación

### ❌ Rutas FALTANTES (Bloqueantes)
1. **`GET /api/v2/switch/transfers/{instructionId}`** - Consulta estado ⚠️ CRÍTICO
2. **`POST /api/v2/switch/accounts`** - Validar cuenta destino ⚠️ CRÍTICO
3. **`POST /api/v2/switch/transfers/return`** - Procesar devoluciones ⚠️ CRÍTICO

---

## 🔴 Impacto de NO Configurar

| Ruta Faltante | Funcionalidad Bloqueada | Impacto Negocio |
|--------------|------------------------|----------------|
| GET `/transfers/{id}` | Consulta de estado (RF-04) | Los bancos no pueden rastrear transacciones enviadas |
| POST `/accounts` | Validación de cuentas (RF-06/acmt.023) | Alto riesgo de errores por cuentas inexistentes |
| POST `/transfers/return` | Devoluciones (RF-07/pacs.004) | Incumplimiento regulatorio, imposible reversar errores |

**Sin estas 3 rutas, el Switch NO puede operar en producción.**

---

## 📝 Configuración Terraform Solicitada

### 1. Consulta de Estado
```hcl
resource "aws_apigatewayv2_route" "transfer_status" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "GET /api/v2/switch/transfers/{instructionId}"
  target    = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}
```
**Backend:** `http://ms-nucleo:8082/api/v2/switch/transfers/{instructionId}`

---

### 2. Validación de Cuenta
```hcl
resource "aws_apigatewayv2_route" "account_lookup" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "POST /api/v2/switch/accounts"
  target    = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
  authorization_scopes = ["https://switch-api.com/transfers.write"]
}
```
**Backend:** `http://ms-nucleo:8082/api/v2/switch/accounts`

**Payload ejemplo:**
```json
{
  "header": {"messageId": "uuid", "originatingBankId": "NEXUS"},
  "body": {"targetBankId": "BANTEC", "targetAccountNumber": "1234567890"}
}
```

---

### 3. Devoluciones/Reversos
```hcl
resource "aws_apigatewayv2_route" "returns" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "POST /api/v2/switch/transfers/return"
  target    = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
  authorization_scopes = ["https://switch-api.com/transfers.write"]
}
```
**Backend:** `http://ms-nucleo:8082/api/v2/switch/transfers/return`

**Payload ejemplo:**
```json
{
  "header": {"messageId": "uuid", "originatingBankId": "BANTEC"},
  "body": {
    "originalInstructionId": "uuid-tx-original",
    "returnReason": "AC01",
    "returnAmount": 100.00
  }
}
```

---

## 🔗 Backends Requeridos

| Servicio | Puerto | Path Base | Health Check |
|----------|--------|-----------|--------------|
| `ms-nucleo` | 8082 | `/api/v2/switch/*` | `/api/v2/switch/health` |
| `ms-compensacion` | 8084 | `/api/v2/compensation/*` | `/api/v2/compensation/health` |
| `ms-contabilidad` | 8083 | `/funding` | `/actuator/health` |

**Configuración ALB:**
- Target Group: `apim-backend-tg`
- Protocol: HTTP
- VPC Link: `apim-vpc-link`
- Security Group: `apim-vpc-link-sg` → `backend-internal-sg`

---

## ✅ Checklist de Configuración

- [ ] Agregar ruta `GET /api/v2/switch/transfers/{instructionId}` en `apim_routes.tf`
- [ ] Agregar ruta `POST /api/v2/switch/accounts` en `apim_routes.tf`
- [ ] Agregar ruta `POST /api/v2/switch/transfers/return` en `apim_routes.tf`
- [ ] Configurar header `x-origin-secret` en todas las integraciones
- [ ] Validar que Cognito JWT Authorizer funciona correctamente
- [ ] Probar cada endpoint con Postman/curl usando token JWT válido
- [ ] Comunicar valor de `x-origin-secret` al equipo Switch (vía secret manager)
- [ ] Actualizar documentación de desarrolladores con nuevos endpoints

---

## 🧪 Pruebas de Validación

### Test 1: Consulta Estado
```bash
curl -X GET \
  'https://APIM_ENDPOINT/api/v2/switch/transfers/UUID_TX_VALIDO' \
  -H 'Authorization: Bearer JWT_TOKEN'
  
# Respuesta esperada: {"status": "COMPLETED", "amount": 100.00, ...}
```

### Test 2: Account Lookup
```bash
curl -X POST \
  'https://APIM_ENDPOINT/api/v2/switch/accounts' \
  -H 'Authorization: Bearer JWT_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"header": {...}, "body": {"targetBankId": "BANTEC", "targetAccountNumber": "1234567890"}}'
  
# Respuesta esperada: {"exists": true, "ownerName": "Juan Pérez", ...}
```

### Test 3: Devolución
```bash
curl -X POST \
  'https://APIM_ENDPOINT/api/v2/switch/transfers/return' \
  -H 'Authorization: Bearer JWT_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"header": {...}, "body": {"originalInstructionId": "uuid", "returnReason": "AC01"}}'
  
# Respuesta esperada: {"status": "COMPLETED", "message": "Devolución procesada"}
```

---

## 📅 Timeline Esperado

| Actividad | Responsable | Duración Estimada |
|-----------|-------------|-------------------|
| Configurar rutas en Terraform | Equipo APIM | 2 horas |
| Deploy a ambiente dev | DevOps | 1 hora |
| Pruebas del equipo Switch | Switch Team | 4 horas |
| Deploy a producción | DevOps | 30 minutos |
| **TOTAL** | - | **1 día laboral** |

---

## 📞 Contacto

**Equipo Switch:**
- Lead: [Tu Nombre]
- Email: [Tu Email]
- Slack: #switch-transaccional

**Documentación Técnica Completa:**
- `MIGRACION_APIM_DEVOPS.md` - Guía completa con Secrets de K8s, ALB config, etc.
- `RESUMEN_ENDPOINTS_APIM.md` - Matriz de endpoints implementados vs Kong legacy

---

## 🚀 Próximos Pasos

1. **Equipo APIM:** Revisar solicitud y confirmar factibilidad (ETA: 1 día)
2. **Equipo APIM:** Aplicar configuración Terraform en ambiente dev
3. **Equipo Switch:** Validar endpoints con pruebas funcionales
4. **DevOps:** Deploy a producción tras aprobación de QA
5. **PM:** Comunicar a bancos que migration está completa

---

**Prioridad:** 🔴 **ALTA - BLOQUEANTE PARA GO-LIVE**  
**Fecha límite sugerida:** 2026-02-10 (2 días hábiles)

---

**Firmado:**  
Equipo Switch Transaccional  
Fecha: 2026-02-08
