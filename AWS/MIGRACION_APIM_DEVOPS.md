# 🚀 Migración a AWS APIM - Guía para DevOps

## 📋 Requerimientos del Equipo Switch al Equipo APIM

**De:** Equipo Switch Transaccional  
**Para:** Equipo APIM (API Gateway)  
**Fecha:** 2026-02-08  
**Prioridad:** ALTA - Bloqueante para Go-Live

---

### 🔴 CRÍTICO: Rutas Faltantes en APIM

El equipo Switch requiere que se configuren las siguientes rutas **ADICIONALES** en el APIM para mantener paridad funcional con la configuración legacy de Kong:

#### ✅ Rutas Actuales en APIM (Confirmadas)
| Ruta | Descripción |
|------|-------------|
| `POST /api/v2/switch/transfers` | Transferencias interbancarias (pacs.008) |
| `POST /api/v2/compensation/upload` | Upload de archivos de compensación |

#### ❌ Rutas FALTANTES (Deben agregarse)

| # | Método | Ruta APIM | Backend Destino | Kong Equivalent | ISO 20022 | Prioridad |
|---|--------|-----------|-----------------|-----------------|-----------|-----------|
| 1 | GET | `/api/v2/switch/transfers/{instructionId}` | `ms-nucleo:8082` | `RF04-Consulta-Estado` | Status Query | 🔴 ALTA |
| 2 | POST | `/api/v2/switch/accounts` | `ms-nucleo:8082` | `Account-Lookup-Service` | acmt.023 | 🔴 ALTA |
| 3 | POST | `/api/v2/switch/transfers/return` | `ms-nucleo:8082` | `RF07-Devolucion-Reverso` | pacs.004 | 🔴 ALTA |
| 4 | GET | `/funding` | `ms-contabilidad:8083` | `RF01-1-Fondeo` | Consulta saldos | 🟡 MEDIA |

---

### 📊 Comparación Kong vs APIM

#### Con Kong (Anterior - Funcionando)
```yaml
# Kong exponía 4 rutas públicas con autenticación:
1. POST   /api/v2/switch/transfers        → ms-nucleo (Crear transferencia)
2. GET    /api/v2/switch/transfers/{id}   → ms-nucleo (Consultar estado)
3. POST   /api/v2/switch/accounts         → ms-nucleo (Validar cuenta)
4. POST   /api/v2/switch/transfers/return → ms-nucleo (Devolución)
```

#### Con APIM (Actual - Incompleto)
```yaml
# APIM solo tiene 2 rutas configuradas:
1. POST   /api/v2/switch/transfers        → ✅ Configurada
2. POST   /api/v2/compensation/upload     → ✅ Configurada

# FALTANTES:
3. GET    /api/v2/switch/transfers/{id}   → ❌ NO CONFIGURADA
4. POST   /api/v2/switch/accounts         → ❌ NO CONFIGURADA
5. POST   /api/v2/switch/transfers/return → ❌ NO CONFIGURADA
```

---

### 🎯 Detalle de Rutas Solicitadas

#### 1. **Consulta de Estado de Transferencia** (ALTA Prioridad)
```hcl
resource "aws_apigatewayv2_route" "transfer_status" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "GET /api/v2/switch/transfers/{instructionId}"
  
  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}
```

**Justificación:**
- Requerimiento funcional RF-04: Consulta de estado de transacciones
- Los bancos necesitan consultar el estado de transferencias enviadas
- Estándar ISO 20022: Status Query

**Backend Path:** `http://ms-nucleo:8082/api/v2/switch/transfers/{instructionId}`

---

#### 2. **Account Lookup (Validación de Cuenta)** (ALTA Prioridad)
```hcl
resource "aws_apigatewayv2_route" "account_lookup" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "POST /api/v2/switch/accounts"
  
  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
  authorization_scopes = ["https://switch-api.com/transfers.write"]
}
```

**Justificación:**
- Requerimiento funcional RF-06: Validación de cuentas destino antes de transferir
- Previene errores y devoluciones por cuentas inexistentes
- Estándar ISO 20022: acmt.023 (Account Information Request)

**Backend Path:** `http://ms-nucleo:8082/api/v2/switch/accounts`

**Request Example:**
```json
{
  "header": {
    "messageId": "uuid",
    "originatingBankId": "NEXUS"
  },
  "body": {
    "targetBankId": "BANTEC",
    "targetAccountNumber": "1234567890"
  }
}
```

---

#### 3. **Devoluciones/Reversos** (ALTA Prioridad)
```hcl
resource "aws_apigatewayv2_route" "returns" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "POST /api/v2/switch/transfers/return"
  
  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
  authorization_scopes = ["https://switch-api.com/transfers.write"]
}
```

**Justificación:**
- Requerimiento funcional RF-07: Procesamiento de devoluciones
- Cumplimiento regulatorio: Reversos obligatorios por errores
- Estándar ISO 20022: pacs.004 (Payment Return)

**Backend Path:** `http://ms-nucleo:8082/api/v2/switch/transfers/return`

**Request Example:**
```json
{
  "header": {
    "messageId": "uuid",
    "originatingBankId": "BANTEC"
  },
  "body": {
    "originalInstructionId": "uuid-tx-original",
    "returnReason": "AC01",
    "returnAmount": 100.00
  }
}
```

---

#### 4. **Consulta de Fondeo** (MEDIA Prioridad)
```hcl
resource "aws_apigatewayv2_route" "funding_query" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "GET /funding"
  
  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}
```

**Justificación:**
- Los bancos consultan sus saldos disponibles en el Switch
- Necesario para validar fondeo antes de enviar transferencias

**Backend Path:** `http://ms-contabilidad:8083/funding`

---

## Resumen General

El Switch Transaccional ha migrado de **Kong Gateway** a **AWS API Gateway HTTP (APIM)** para producción. Kong sigue disponible solo para desarrollo local.

---

## 📡 Endpoints APIM Requeridos

DevOps debe configurar las siguientes rutas en el APIM (`apim_routes.tf`):

### 1. Transferencias Interbancarias
```hcl
resource "aws_apigatewayv2_route" "transfers" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "POST /api/v2/switch/transfers"
  
  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
  authorization_scopes = ["https://switch-api.com/transfers.write"]
}
```

### 2. Consulta de Estado
```hcl
resource "aws_apigatewayv2_route" "transfer_status" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "GET /api/v2/switch/transfers/{instructionId}"
  
  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}
```

### 3. **Account Lookup (NUEVO)**
```hcl
resource "aws_apigatewayv2_route" "account_lookup" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "POST /api/v2/switch/account-lookup"
  
  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
  authorization_scopes = ["https://switch-api.com/transfers.write"]
}
```

### 4. **Devoluciones/Reversos (NUEVO)**
```hcl
resource "aws_apigatewayv2_route" "returns" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "POST /api/v2/switch/returns"
  
  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
  authorization_scopes = ["https://switch-api.com/transfers.write"]
}
```

### 5. Compensación (Upload)
```hcl
resource "aws_apigatewayv2_route" "compensation_upload" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "POST /api/v2/compensation/upload"
  
  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
  authorization_scopes = ["https://switch-api.com/transfers.write"]
  
  # Timeout extendido para uploads
  request_parameters = {
    "overwrite:header.integration-timeout" = "29000"
  }
}
```

### 5. Health Checks (sin autenticación)
```hcl
resource "aws_apigatewayv2_route" "health_nucleo" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "GET /api/v2/switch/health"
  
  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
  
  # Sin autenticación para health checks del ALB
  authorization_type = "NONE"
}

resource "aws_apigatewayv2_route" "health_compensacion" {
  api_id    = aws_apigatewayv2_api.apim_gateway.id
  route_key = "GET /api/v2/compensation/health"
  
  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorization_type = "NONE"
}
```

---

## 🔐 Secrets de Kubernetes Requeridos

### switch-ms-nucleo
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: switch-ms-nucleo-secret
  namespace: switch
type: Opaque
stringData:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://rds-switch.xxxxx.us-east-2.rds.amazonaws.com:5432/nucleo_db"
  SPRING_DATASOURCE_USERNAME: "postgres"
  SPRING_DATASOURCE_PASSWORD: "XXXXX"
  RABBITMQ_HOST: "b-455e546c-be71-4fe2-ba0f-bd3112e6c220.mq.us-east-2.on.aws"
  RABBITMQ_USERNAME: "switch_user"
  RABBITMQ_PASSWORD: "XXXXX"
  APIM_ORIGIN_SECRET: "XXXXX"  # ⚠️ CRÍTICO: Secreto que APIM inyecta en header x-origin-secret
```

### switch-ms-compensacion
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: switch-ms-compensacion-secret
  namespace: switch
type: Opaque
stringData:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://rds-switch.xxxxx.us-east-2.rds.amazonaws.com:5432/compensacion_db"
  SPRING_DATASOURCE_USERNAME: "postgres"
  SPRING_DATASOURCE_PASSWORD: "XXXXX"
  RABBITMQ_HOST: "b-455e546c-be71-4fe2-ba0f-bd3112e6c220.mq.us-east-2.on.aws"
  RABBITMQ_USERNAME: "switch_user"
  RABBITMQ_PASSWORD: "XXXXX"
  APIM_ORIGIN_SECRET: "XXXXX"
```

### switch-ms-contabilidad
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: switch-ms-contabilidad-secret
  namespace: switch
type: Opaque
stringData:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://rds-switch.xxxxx.us-east-2.rds.amazonaws.com:5432/contabilidad_db"
  SPRING_DATASOURCE_USERNAME: "postgres"
  SPRING_DATASOURCE_PASSWORD: "XXXXX"
  APIM_ORIGIN_SECRET: "XXXXX"
```

### switch-ms-devolucion
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: switch-ms-devolucion-secret
  namespace: switch
type: Opaque
stringData:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://rds-switch.xxxxx.us-east-2.rds.amazonaws.com:5432/devolucion_db"
  SPRING_DATASOURCE_USERNAME: "postgres"
  SPRING_DATASOURCE_PASSWORD: "XXXXX"
  APIM_ORIGIN_SECRET: "XXXXX"
```

### switch-ms-directorio
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: switch-ms-directorio-secret
  namespace: switch
type: Opaque
stringData:
  SPRING_DATA_MONGODB_URI: "mongodb://admin:XXXXX@docdb-switch.xxxxx.us-east-2.docdb.amazonaws.com:27017/directorio_db?tls=true&tlsAllowInvalidCertificates=true&replicaSet=rs0&readPreference=secondaryPreferred"
  APIM_ORIGIN_SECRET: "XXXXX"
```

---

## 🎯 Deployment ConfigMaps

Cada microservicio necesita estas variables de entorno en su Deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: switch-ms-nucleo
  namespace: switch
spec:
  template:
    spec:
      containers:
      - name: switch-ms-nucleo
        image: YOUR_ECR_REGISTRY/switch-ms-nucleo:latest
        env:
          # Seguridad APIM
          - name: APIM_SECURITY_ENABLED
            value: "true"
          - name: APIM_ORIGIN_SECRET
            valueFrom:
              secretKeyRef:
                name: switch-ms-nucleo-secret
                key: APIM_ORIGIN_SECRET
          
          # Base de Datos
          - name: SPRING_DATASOURCE_URL
            valueFrom:
              secretKeyRef:
                name: switch-ms-nucleo-secret
                key: SPRING_DATASOURCE_URL
          - name: SPRING_DATASOURCE_USERNAME
            valueFrom:
              secretKeyRef:
                name: switch-ms-nucleo-secret
                key: SPRING_DATASOURCE_USERNAME
          - name: SPRING_DATASOURCE_PASSWORD
            valueFrom:
              secretKeyRef:
                name: switch-ms-nucleo-secret
                key: SPRING_DATASOURCE_PASSWORD
          
          # RabbitMQ
          - name: RABBITMQ_HOST
            valueFrom:
              secretKeyRef:
                name: switch-ms-nucleo-secret
                key: RABBITMQ_HOST
          - name: RABBITMQ_USERNAME
            valueFrom:
              secretKeyRef:
                name: switch-ms-nucleo-secret
                key: RABBITMQ_USERNAME
          - name: RABBITMQ_PASSWORD
            valueFrom:
              secretKeyRef:
                name: switch-ms-nucleo-secret
                key: RABBITMQ_PASSWORD
        
        ports:
          - containerPort: 8082
            name: http
        
        livenessProbe:
          httpGet:
            path: /api/v2/switch/health
            port: 8082
          initialDelaySeconds: 60
          periodSeconds: 30
        
        readinessProbe:
          httpGet:
            path: /api/v2/switch/health
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 10
```

---

## 🔧 ALB Target Group Configuration

El ALB (`apim-backend-alb`) debe apuntar a los servicios de Kubernetes:

### Target Group: switch-ms-nucleo-tg
- **Port:** 8082
- **Protocol:** HTTP
- **Health Check Path:** `/api/v2/switch/health`
- **Health Check Interval:** 30s
- **Healthy Threshold:** 2
- **Unhealthy Threshold:** 3

### Target Group: switch-ms-compensacion-tg
- **Port:** 8084
- **Protocol:** HTTP
- **Health Check Path:** `/api/v2/compensation/health`
- **Health Check Interval:** 30s
- **Healthy Threshold:** 2
- **Unhealthy Threshold:** 3

---

## 📋 Checklist de Migración

- [ ] Crear rutas en APIM (`POST /api/v2/switch/account-lookup` **es nueva**)
- [ ] Configurar header `x-origin-secret` en APIM Integration
- [ ] Crear Secrets de K8s con `APIM_ORIGIN_SECRET`
- [ ] Actualizar Deployments con `APIM_SECURITY_ENABLED=true`
- [ ] Configurar ALB con health checks correctos
- [ ] Comunicar valor de `APIM_ORIGIN_SECRET` al equipo Switch
- [ ] Verificar que VPC Link permite tráfico desde APIM a ALB
- [ ] Probar endpoint `/api/v2/switch/account-lookup` desde Postman/curl con JWT

---

## 🧪 Prueba de Conectividad

Desde tu terminal (con token JWT válido):

```bash
# 1. Obtener token
TOKEN=$(curl -X POST \
  'https://banca-ecosistema.auth.us-east-2.amazoncognito.com/oauth2/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials&client_id=YOUR_CLIENT_ID&client_secret=YOUR_SECRET&scope=https://switch-api.com/transfers.write' \
  | jq -r '.access_token')

# 2. Probar Account Lookup
curl -X POST \
  'https://YOUR_APIM_ENDPOINT/api/v2/switch/account-lookup' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "header": {
      "messageNamespace": "acmt.023.001.02",
      "messageId": "TEST-123",
      "originatingBankId": "NEXUS"
    },
    "body": {
      "targetBankId": "BANTEC",
      "targetAccountNumber": "1234567890"
    }
  }'
```

**Respuesta esperada:**
```json
{
  "status": "SUCCESS",
  "data": {
    "exists": true,
    "ownerName": "Juan Pérez",
    "currency": "USD",
    "status": "ACTC"
  }
}
```

---

## 🔗 Documentación Relacionada

- [APIM.md](./APIM.md) - Documentación completa del APIM
- [README.md](./README.md) - README principal del proyecto
- [GUIA_DESARROLLADORES.md](./GUIA_DESARROLLADORES.md) - Guía para desarrolladores

---

**Fecha de Migración:** 2026-02-08  
**Responsable:** Equipo Switch
