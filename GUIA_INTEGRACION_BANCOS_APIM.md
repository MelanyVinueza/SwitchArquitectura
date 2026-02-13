# 🏦 Guía de Integración de Bancos con el Switch vía APIM
## Fecha: 10/Feb/2026 | Versión: 3.0.0 | Estado: EN PRUEBAS

> ⚠️ **CONFIDENCIAL** - Solo para equipos de desarrollo autorizados.

---

## 📊 Estado Actual de la Integración

| Componente | Estado | Validado |
|---|---|---|
| APIM (API Gateway) | ✅ **ACTIVO** | `https://gf0js7uezg.execute-api.us-east-2.amazonaws.com/dev` |
| Cognito (OAuth 2.0) | ⚠️ **VERIFICAR URL AUTH** | Ver sección "Pendientes" |
| Switch ms-nucleo | ✅ Desplegado en EKS | Namespace: `switch` |
| Switch ms-compensación | ✅ Desplegado en EKS | Puerto: 8084 |
| Switch ms-contabilidad | ✅ Desplegado en EKS | Puerto: 8080 |
| Switch ms-devolucion | ✅ Desplegado en EKS | Puerto: 8085 |
| Switch ms-directorio | ✅ Desplegado en EKS | Puerto: 8081 |
| RabbitMQ (Amazon MQ) | ✅ Configurado | Puerto: 5671 (SSL) |
| BANTEC (Banco) | ✅ Desplegado en EKS | Kubernetes |
| ARCBANK (Banco) | ✅ Desplegado en EKS | Kubernetes |

### ⚠️ Hallazgos de las Pruebas Iniciales

| Prueba | Resultado | Diagnóstico |
|---|---|---|
| `GET /api/v2/switch/health` (sin auth) | **503** Service Unavailable | VPC Link existe (`apim-vpc-link`), verificar ALB routes |
| `POST /api/v2/switch/transfers` (sin token) | **401** Unauthorized | ✅ Cognito está protegiendo correctamente |
| `GET /` (raíz) | **404** Not Found | ✅ Normal, no hay ruta en raíz |
| DNS `auth-banca-dev.auth.us-east-2.amazoncognito.com` | ❌ No resuelve | **URL de Auth incorrecta en la documentación** |

---

## � PENDIENTES (Para DevOps/APIM)

### 1. Error 503 en Health Check
**Problema:** `GET /api/v2/switch/health` retorna `503 Service Temporarily Unavailable`.

**Actualización DevOps:** El VPC Link (`apim-vpc-link`) ya está configurado. La ruta debe estar mapeada en el Switch Admin.

**Acción Requerida:**
- [ ] Verificar que la ruta `GET /api/v2/switch/health` está mapeada al ALB Target Group correcto
- [ ] Verificar que los pods responden internamente:
  ```bash
  kubectl exec -it $(kubectl get pod -n switch -l app=switch-ms-nucleo -o name | head -1) -n switch -- curl -s localhost:8082/api/v2/switch/health
  ```

### ✅ URLs Internas de Webhook (EKS → Bancos)
Confirmado por DevOps. El Switch usa red interna de EKS para enviar transferencias a los bancos:

| Banco | URL Interna (Webhook) |
|---|---|
| BANTEC | `http://service-transacciones.bantec.svc.cluster.local:8080/api/v2/switch/transfers` |
| ARCBANK | `http://service-transacciones.arcbank.svc.cluster.local:8080/api/v2/switch/transfers` |

> Estas URLs deben registrarse en el campo `urlDestino` del Directorio (`ms-directorio`).

### 2. URL de Cognito Incorrecta
**Problema:** El dominio `auth-banca-dev.auth.us-east-2.amazoncognito.com` **NO EXISTE** (DNS no resuelve).

**Acción Requerida:**
- [ ] Confirmar la URL correcta del dominio de Cognito (User Pool Domain)
- [ ] Verificar en AWS Console: Cognito → User Pools → App Integration → Domain

### 3. Rutas Faltantes en APIM
Según la documentación de migración, solo hay 2 rutas configuradas. Se necesitan al menos 3 más:

| Ruta | Método | Estado |
|---|---|---|
| `/api/v2/switch/transfers` | POST | ✅ Configurada |
| `/api/v2/compensation/upload` | POST | ✅ Configurada |
| `/api/v2/switch/transfers/{instructionId}` | GET | ❌ **FALTANTE** |
| `/api/v2/switch/account-lookup` | POST | ❌ **FALTANTE** |
| `/api/v2/switch/returns` | POST | ❌ **FALTANTE** |
| `/api/v2/switch/health` | GET | ⚠️ ¿Configurada pero 503? |

---

## 🏦 INSTRUCCIONES PARA LOS BANCOS

### Paso 1: Obtener Token de Acceso (OAuth 2.0)

Cada banco debe autenticarse contra Cognito para obtener un token JWT.

**URL de Autenticación:** `https://<DOMINIO-COGNITO-CORRECTO>.auth.us-east-2.amazoncognito.com/oauth2/token`

> ⚠️ La URL exacta está pendiente de confirmación por DevOps.

**Credenciales por Banco:**

| Banco | Client ID | Client Secret |
|---|---|---|
| BANTEC | `7oj12jtu8d3keilv1e1gjkc8e4` | `16o9bdppi2qku69vuk7qdcim4fqrkmkggder3iq47p9cs29tqqoq` |
| ARKBANK | `3jprfuk1phejsm0sjr8p50n91e` | `1sbg167h178p9cgfradk51c2qla1u4d5en3tnmljc91kc9rbn9nr` |
| NEXUS | `1fp4b5mavvh8vub3qsa2h7rjmk` | `1u85iidgras97j5id0h71abnpku3vt6bumhpnmtgn8qfgkvlp6fp` |
| ECUSOL | `2hsfb89npphc880rti2j8bf509` | `h2omnkmqh182pjtromd88vdrqvvhekpvefrall8c0ulu36n6580` |

**Petición:**
```bash
curl -X POST https://<DOMINIO-COGNITO>/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=<TU_CLIENT_ID>" \
  -d "client_secret=<TU_CLIENT_SECRET>" \
  -d "scope=https://switch-api.com/transfers.write"
```

**Respuesta Esperada (200 OK):**
```json
{
  "access_token": "eyJraWQiOiJ...",
  "expires_in": 3600,
  "token_type": "Bearer"
}
```

### Paso 2: Enviar Transferencia Interbancaria

**Endpoint:** `POST https://gf0js7uezg.execute-api.us-east-2.amazonaws.com/dev/api/v2/switch/transfers`

**Headers:**
```
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
X-Trace-ID: <UUID-para-rastreo>  (Recomendado)
```

**Body (ISO 20022 Simplificado):**
```json
{
  "header": {
    "messageId": "MSG-001",
    "creationDateTime": "2026-02-10T22:00:00Z",
    "originatingBankId": "BANTEC"
  },
  "body": {
    "instructionId": "550e8400-e29b-41d4-a716-446655440000",
    "endToEndId": "E2E-REF-001",
    "amount": {
      "currency": "USD",
      "value": 100.50
    },
    "debtor": {
      "name": "Juan Pérez",
      "account": "1234567890",
      "bankId": "BANTEC"
    },
    "creditor": {
      "name": "María García",
      "account": "0987654321",
      "bankId": "ARCBANK"
    },
    "remittanceInformation": "Pago de servicios"
  }
}
```

**Respuestas Posibles:**

| HTTP Code | Estado | Significado |
|---|---|---|
| `201 Created` | `COMPLETED` | Transferencia procesada exitosamente (síncrono) |
| `202 Accepted` | `QUEUED` | Transferencia encolada en RabbitMQ (asíncrono) |
| `401 Unauthorized` | - | Token JWT inválido o expirado |
| `422 Unprocessable` | `FAILED` | Error de validación (cuenta no existe, fondos insuficientes) |
| `503` | - | Circuit Breaker abierto o backend no disponible |

### Paso 3: Consultar Estado de Transferencia

**Endpoint:** `GET https://gf0js7uezg.execute-api.us-east-2.amazonaws.com/dev/api/v2/switch/transfers/{instructionId}`

```bash
curl -X GET \
  "https://gf0js7uezg.execute-api.us-east-2.amazonaws.com/dev/api/v2/switch/transfers/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### Paso 4: Validar Cuenta Destino (Account Lookup)

**Endpoint:** `POST https://gf0js7uezg.execute-api.us-east-2.amazonaws.com/dev/api/v2/switch/account-lookup`

```json
{
  "header": {
    "messageId": "LKP-001",
    "originatingBankId": "BANTEC"
  },
  "body": {
    "requestId": "REQ-001",
    "targetBankId": "ARCBANK",
    "targetAccountNumber": "0987654321"
  }
}
```

### Paso 5: Procesar Devolución (pacs.004)

**Endpoint:** `POST https://gf0js7uezg.execute-api.us-east-2.amazonaws.com/dev/api/v2/switch/returns`

```json
{
  "header": {
    "messageId": "RET-001",
    "originatingBankId": "BANTEC"
  },
  "body": {
    "returnInstructionId": "RET-INSTR-001",
    "originalInstructionId": "550e8400-e29b-41d4-a716-446655440000",
    "returnReason": "DUPL",
    "returnAmount": {
      "currency": "USD",
      "value": 100.50
    }
  }
}
```

---

## 🐇 Integración con RabbitMQ (Bancos Destino)

Los bancos que **reciben** transferencias deben conectarse a RabbitMQ para consumir mensajes de su cola.

**Conexión:**
```properties
spring.rabbitmq.host=b-455e546c-be71-4fe2-ba0f-bd3112e6c220.mq.us-east-2.on.aws
spring.rabbitmq.port=5671
spring.rabbitmq.ssl.enabled=true
spring.rabbitmq.ssl.algorithm=TLSv1.2
```

**Colas por Banco:**

| Banco | Cola de Entrada | Dead Letter Queue |
|---|---|---|
| BANTEC | `q.bank.BANTEC.in` | `q.bank.BANTEC.dlq` |
| ARCBANK | `q.bank.ARCBANK.in` | `q.bank.ARCBANK.dlq` |
| NEXUS | `q.bank.NEXUS.in` | `q.bank.NEXUS.dlq` |
| ECUSOL | `q.bank.ECUSOL.in` | `q.bank.ECUSOL.dlq` |

**Usuarios RabbitMQ:**

| Banco | Usuario | Password |
|---|---|---|
| BANTEC | `bantec` | `bantecpass` |
| ARCBANK | `arcbank` | `arcbankpass` |

**Implementación del Consumer:**
```java
@RabbitListener(queues = "q.bank.BANTEC.in")  // Cambiar por su cola
public void recibirTransferencia(MensajeISO mensaje) {
    // 1. Validar cuenta
    // 2. Procesar depósito
    // 3. Enviar callback al Switch
}
```

**Callback al Switch (después de procesar):**
```bash
POST https://gf0js7uezg.execute-api.us-east-2.amazonaws.com/dev/api/v1/transacciones/callback
Content-Type: application/json

{
  "header": {
    "messageId": "RESP-001",
    "respondingBankId": "BANTEC",
    "creationDateTime": "2026-02-10T22:05:00Z"
  },
  "body": {
    "originalInstructionId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "COMPLETED",
    "processedDateTime": "2026-02-10T22:05:00Z"
  }
}
```

---

## 🧪 Checklist de Pruebas

### Fase 1: Conectividad Básica
- [ ] Obtener Token JWT desde Cognito (⚠️ Bloqueado: URL Auth incorrecta)
- [ ] Health Check responde 200 (⚠️ Bloqueado: 503 - VPC Link issue)
- [ ] Transferencia sin token retorna 401 ✅

### Fase 2: Transferencias
- [ ] Transferencia BANTEC → ARCBANK (via APIM)
- [ ] Verificar mensaje en cola `q.bank.ARCBANK.in`
- [ ] Transferencia ARCBANK → BANTEC (via APIM)
- [ ] Verificar mensaje en cola `q.bank.BANTEC.in`

### Fase 3: Callbacks
- [ ] ARCBANK envía callback COMPLETED al Switch
- [ ] BANTEC envía callback COMPLETED al Switch
- [ ] Verificar estado cambia de QUEUED a COMPLETED

### Fase 4: Devoluciones
- [ ] Procesar devolución por DUPL (Duplicado)
- [ ] Verificar saldo revertido en Contabilidad

### Fase 5: Compensación
- [ ] Abrir ciclo de compensación
- [ ] Verificar posiciones netas después de transacciones
- [ ] Ejecutar cierre diario
- [ ] Verificar archivo de liquidación generado

---

## 📞 Acciones Inmediatas Requeridas

### Para el Equipo APIM/DevOps:
1. **Verificar VPC Link** - ¿Por qué `/api/v2/switch/health` retorna 503?
2. **Confirmar URL de Cognito** - `auth-banca-dev` no resuelve DNS
3. **Agregar rutas faltantes** - GET transfers/{id}, POST account-lookup, POST returns

### Para el Equipo Switch:
1. Verificar que los pods responden internamente (port-forward)
2. Validar que RabbitMQ está accesible desde los pods
3. Preparar datos de prueba en las bases de datos

### Para los Bancos (BANTEC y ARCBANK):
1. Configurar `client_id` y `client_secret` en sus sistemas
2. Implementar Consumer de RabbitMQ para su cola
3. Implementar endpoint de Callback HTTP
4. Esperar confirmación de URL de Auth correcta

---

**Documento generado:** 10/Feb/2026 22:20  
**Equipo:** Switch Transaccional DIGICONECU
