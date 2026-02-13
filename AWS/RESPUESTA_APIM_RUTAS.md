# 📧 RESPUESTA AL EQUIPO APIM - Definición Oficial de Rutas

**Para:** Equipo APIM  
**De:** Equipo Switch Transaccional  
**Asunto:** Aclaración de rutas RESTful para AWS API Gateway  
**Fecha:** 2026-02-08

---

## 🎯 RUTAS OFICIALES - Según Código Backend

Hemos revisado el código backend implementado en `ApimSwitchControlador.java` y definimos las rutas oficiales según **mejores prácticas REST API**.

---

## ✅ RUTAS DEFINITIVAS

### 1. Account Lookup (Validación de Cuenta)

**✅ RUTA CORRECTA:**
```
POST /api/v2/switch/account-lookup
```

**❌ NO USAR:** `/api/v2/switch/accounts` (esto sugiere un CRUD de cuentas, no una validación)

**Justificación REST:**
- `account-lookup` es más **semántico** y describe la **acción** (lookup = búsqueda/validación)
- `/accounts` implica un recurso CRUD (GET /accounts = listar, POST /accounts = crear)
- Esta ruta hace **validación externa** (proxy al banco destino), no gestiona cuentas propias
- **RESTful naming:** Usar verbos en kebab-case cuando la operación no es CRUD puro

**Código backend implementado:**
```java
@PostMapping("/account-lookup")
public ResponseEntity<AccountLookupResponseDTO> validarCuenta(@RequestBody AccountLookupRequestDTO request)
```

---

### 2. Devoluciones/Reversos (Returns)

**✅ RUTA CORRECTA:**
```
POST /api/v2/switch/returns
```

**❌ NO USAR:** `/api/v2/switch/transfers/return` (gramaticalmente incorrecto, no es RESTful)

**Justificación REST:**
- `returns` es un **recurso plural** (colección de devoluciones)
- `/transfers/return` sugiere un singleton, pero manejamos múltiples returns
- **RESTful naming:** Los recursos se nombran en plural (`transfers`, `accounts`, `returns`)
- POST `/returns` = "Crear una nueva devolución" (semánticamente correcto)

**Código backend implementado:**
```java
@PostMapping("/returns")
public ResponseEntity<?> procesarDevolucion(@RequestBody ReturnRequestDTO returnRequest)
```

---

### 3. Funding (No implementado aún)

**✅ RUTA CORRECTA (cuando se implemente):**
```
POST /api/v2/switch/funding
```

**❌ NO USAR:** `/funding` (sin prefijo `/api/v2`)

**Justificación REST:**
- **Consistencia:** Todas las rutas del Switch usan el prefijo `/api/v2/switch/`
- **Versionado:** El `/v2` permite evolución de API sin breaking changes
- **Namespace:** El `/switch/` agrupa lógicamente todas las operaciones del Switch
- **Claridad:** Un cliente debe saber que está llamando a la API v2 del Switch

**Estructura propuesta (futura):**
```java
@PostMapping("/funding")
public ResponseEntity<FundingResponseDTO> procesarFunding(@RequestBody FundingRequestDTO request)
```

---

## 📋 TABLA RESUMEN - RUTAS AWS APIM

| Operación | Método | Ruta Oficial | Backend | Estado |
|-----------|--------|--------------|---------|--------|
| **Transferencia** | POST | `/api/v2/switch/transfers` | ✅ Implementado | ✅ Deploy |
| **Consulta Estado** | GET | `/api/v2/switch/transfers/{id}` | ✅ Implementado | ⏳ PENDIENTE APIM |
| **Account Lookup** | POST | `/api/v2/switch/account-lookup` | ✅ Implementado | ⏳ PENDIENTE APIM |
| **Devoluciones** | POST | `/api/v2/switch/returns` | ✅ Implementado | ⏳ PENDIENTE APIM |
| **Funding** | POST | `/api/v2/switch/funding` | ❌ Futuro | 🔮 Planificado |

---

## 🏗️ BUENAS PRÁCTICAS REST API APLICADAS

### ✅ 1. Recursos en Plural
```
✅ /transfers    (colección)
✅ /returns      (colección)
✅ /accounts     (colección - si fuera CRUD)
❌ /transfer     (singular - incorrecto)
```

### ✅ 2. Acciones con Kebab-Case
```
✅ /account-lookup    (acción específica)
✅ /status-check      (acción específica)
❌ /accountLookup     (camelCase - no REST)
```

### ✅ 3. Jerarquía Lógica
```
✅ /transfers/{id}         (recurso específico)
✅ /transfers/{id}/status  (sub-recurso)
❌ /transfers/return       (verb-noun - confuso)
```

### ✅ 4. Verbos HTTP Apropiados
```
POST   /transfers        → Crear transferencia
GET    /transfers/{id}   → Consultar transferencia
POST   /account-lookup   → Validar cuenta (acción)
POST   /returns          → Crear devolución
```

### ✅ 5. Versionado Explícito
```
✅ /api/v2/switch/*    (versión clara)
❌ /switch/*           (sin versión)
❌ /api/switch/v2/*    (versión en medio)
```

---

## 🔧 CONFIGURACIÓN APIM SOLICITADA

Por favor configurar las siguientes rutas en AWS API Gateway:

### Ruta 1: Consulta de Estado
```hcl
resource "aws_apigatewayv2_route" "status_query" {
  api_id    = aws_apigatewayv2_api.switch_api.id
  route_key = "GET /api/v2/switch/transfers/{instructionId}"
  target    = "integrations/${aws_apigatewayv2_integration.nucleo.id}"
}
```

### Ruta 2: Account Lookup
```hcl
resource "aws_apigatewayv2_route" "account_lookup" {
  api_id    = aws_apigatewayv2_api.switch_api.id
  route_key = "POST /api/v2/switch/account-lookup"
  target    = "integrations/${aws_apigatewayv2_integration.nucleo.id}"
}
```

### Ruta 3: Devoluciones
```hcl
resource "aws_apigatewayv2_route" "returns" {
  api_id    = aws_apigatewayv2_api.switch_api.id
  route_key = "POST /api/v2/switch/returns"
  target    = "integrations/${aws_apigatewayv2_integration.nucleo.id}"
}
```

---

## 📊 COMPARACIÓN: ANTIGUO vs NUEVO

| Ruta Antigua (Incorrecta) | Ruta Nueva (Correcta) | Razón |
|---------------------------|----------------------|--------|
| `/api/v2/switch/accounts` | `/api/v2/switch/account-lookup` | Sem ántica (acción vs recurso) |
| `/api/v2/switch/transfers/return` | `/api/v2/switch/returns` | Plural + jerarquía |
| `/funding` | `/api/v2/switch/funding` | Consistencia + versionado |

---

## 🎯 ACCIÓN REQUERIDA DEL EQUIPO APIM

1. **Actualizar rutas** en Terraform/Console según tabla oficial
2. **Validar** que el header `x-origin-secret` se inyecta en todas las rutas
3. **Configurar Cognito Authorizer** con scopes:
   - `https://switch-api.com/transfers.write`
   - `https://switch-api.com/transfers.read`
4. **Probar** las 3 rutas pendientes contra el backend en development

---

## 📞 CONTACTO

**Equipo:** Switch Transaccional  
**Backend:** switch-ms-nucleo (puerto 8082)  
**Repositorio:** https://github.com/StephaniRiveraE/switch-ms-nucleo  
**Documentación:** MIGRACION_APIM_DEVOPS.md

---

## ✅ CONFIRMACIÓN FINAL

**¿Rutas acordadas?**
- ✅ `/api/v2/switch/account-lookup` (NO `/accounts`)
- ✅ `/api/v2/switch/returns` (NO `/transfers/return`)
- ✅ `/api/v2/switch/funding` (con prefijo completo, cuando se implemente)

**Esperamos confirmación del equipo APIM para proceder con testing de integración.**

---

**Generado por:** Equipo Switch Transaccional  
**Versión API:** 3.0.0  
**Fecha:** 2026-02-08
