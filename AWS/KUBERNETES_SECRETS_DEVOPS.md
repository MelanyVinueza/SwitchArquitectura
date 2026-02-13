# 🔐 KUBERNETES SECRETS - Switch Transaccional

**Para:** Equipo DevOps  
**De:** Equipo Switch Transaccional  
**Namespace:** `switch`  
**Cluster:** `eks-banca-ecosistema`

---

## 📋 SECRETS REQUERIDOS

Los workflows actualizados requieren los siguientes Kubernetes Secrets en el namespace `switch`:

---

### 1️⃣ Secret: `switch-db-credentials`

Contiene las credenciales de bases de datos PostgreSQL y MongoDB para todos los microservicios.

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: switch-db-credentials
  namespace: switch
type: Opaque
stringData:
  # MS-NUCLEO (PostgreSQL - RDS)
  nucleo-url: "jdbc:postgresql://switch-nucleo-db.xxxx.us-east-2.rds.amazonaws.com:5432/nucleo_db"
  nucleo-username: "nucleo_user"
  nucleo-password: "CAMBIAR_PASSWORD_SEGURO_1"
  
  # MS-COMPENSACION (PostgreSQL - RDS)
  compensacion-url: "jdbc:postgresql://switch-compensacion-db.xxxx.us-east-2.rds.amazonaws.com:5432/compensacion_db"
  compensacion-username: "compensacion_user"
  compensacion-password: "CAMBIAR_PASSWORD_SEGURO_2"
  
  # MS-DEVOLUCION (PostgreSQL - RDS)
  devolucion-url: "jdbc:postgresql://switch-devolucion-db.xxxx.us-east-2.rds.amazonaws.com:5432/devolucion_db"
  devolucion-username: "devolucion_user"
  devolucion-password: "CAMBIAR_PASSWORD_SEGURO_3"
  
  # MS-CONTABILIDAD (PostgreSQL - RDS)
  contabilidad-url: "jdbc:postgresql://switch-contabilidad-db.xxxx.us-east-2.rds.amazonaws.com:5432/contabilidad_db"
  contabilidad-username: "contabilidad_user"
  contabilidad-password: "CAMBIAR_PASSWORD_SEGURO_4"
  
  # MS-DIRECTORIO (MongoDB - DocumentDB)
  mongodb-uri: "mongodb://directorio_user:CAMBIAR_PASSWORD_SEGURO_5@switch-docdb.cluster-xxxx.us-east-2.docdb.amazonaws.com:27017/directorio_db?tls=true&tlsCAFile=rds-combined-ca-bundle.pem&retryWrites=false"
```

#### Comando para crear:
```bash
kubectl create secret generic switch-db-credentials \
  --from-literal=nucleo-url="jdbc:postgresql://..." \
  --from-literal=nucleo-username="nucleo_user" \
  --from-literal=nucleo-password="CAMBIAR" \
  --from-literal=compensacion-url="jdbc:postgresql://..." \
  --from-literal=compensacion-username="compensacion_user" \
  --from-literal=compensacion-password="CAMBIAR" \
  --from-literal=devolucion-url="jdbc:postgresql://..." \
  --from-literal=devolucion-username="devolucion_user" \
  --from-literal=devolucion-password="CAMBIAR" \
  --from-literal=contabilidad-url="jdbc:postgresql://..." \
  --from-literal=contabilidad-username="contabilidad_user" \
  --from-literal=contabilidad-password="CAMBIAR" \
  --from-literal=mongodb-uri="mongodb://..." \
  -n switch
```

---

### 2️⃣ Secret: `switch-secrets`

Contiene credenciales de servicios compartidos (RabbitMQ, APIM).

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: switch-secrets
  namespace: switch
type: Opaque
stringData:
  # RabbitMQ (Amazon MQ)
  rabbitmq-username: "mqadmin"
  rabbitmq-password: "CAMBIAR_RABBITMQ_PASSWORD"
  
  # APIM Origin Secret (compartido con equipo APIM)
  apim-origin-secret: "CAMBIAR_UUID_SECRETO_COMPARTIDO_CON_APIM"
```

#### Comando para crear:
```bash
kubectl create secret generic switch-secrets \
  --from-literal=rabbitmq-username="mqadmin" \
  --from-literal=rabbitmq-password="CAMBIAR_RABBITMQ" \
  --from-literal=apim-origin-secret="CAMBIAR_UUID_APIM" \
  -n switch
```

---

## 🔍 VERIFICACIÓN

### Ver secrets creados:
```bash
kubectl get secrets -n switch
```

### Ver contenido (base64 decoded):
```bash
# Ver switch-db-credentials
kubectl get secret switch-db-credentials -n switxch -o jsonpath='{.data.nucleo-url}' | base64 -d

# Ver switch-secrets
kubectl get secret switch-secrets -n switch -o jsonpath='{.data.rabbitmq-username}' | base64 -d
```

---

## 📊 TABLA RESUMEN

| Microservicio | Secret | Keys Usadas |
|--------------|--------|-------------|
| **ms-nucleo** | `switch-db-credentials` | `nucleo-url`, `nucleo-username`, `nucleo-password` |
| | `switch-secrets` | `rabbitmq-username`, `rabbitmq-password`, `apim-origin-secret` |
| **ms-compensacion** | `switch-db-credentials` | `compensacion-url`, `compensacion-username`, `compensacion-password` |
| | `switch-secrets` | `rabbitmq-username`, `rabbitmq-password` |
| **ms-devolucion** | `switch-db-credentials` | `devolucion-url`, `devolucion-username`, `devolucion-password` |
| **ms-contabilidad** | `switch-db-credentials` | `contabilidad-url`, `contabilidad-username`, `contabilidad-password` |
| **ms-directorio** | `switch-db-credentials` | `mongodb-uri` |

---

## 🚨 VALORES A REEMPLAZAR

DevOps debe obtener/definir estos valores antes de crear los secrets:

### Bases de Datos (RDS)
- [ ] Endpoint RDS para **nucleo_db**
- [ ] Endpoint RDS para **compensacion_db**
- [ ] Endpoint RDS para **devolucion_db**
- [ ] Endpoint RDS para **contabilidad_db**
- [ ] Passwords seguros para cada DB (AWS Secrets Manager recomendado)

### MongoDB (DocumentDB)
- [ ] Endpoint DocumentDB cluster
- [ ] Usuario y password DocumentDB
- [ ] Descargar certificado CA: `rds-combined-ca-bundle.pem`

### RabbitMQ (Amazon MQ)
- [ ] Usuario admin de RabbitMQ
- [ ] Password de RabbitMQ

### APIM
- [ ] **UUID compartido** con equipo APIM para `x-origin-secret`
  - Este valor debe ser el MISMO que APIM inyecta en el header
  - Formato: UUID v4 (ejemplo: `a7f2c8b4-1e3d-4a5f-9c2b-8d7e6f4a3b2c`)
  - **CRÍTICO:** Coordinar con equipo APIM para obtener este valor

---

## 🔐 MEJORES PRÁCTICAS

### 1. Usar AWS Secrets Manager
```bash
# Crear secret en AWS Secrets Manager
aws secretsmanager create-secret \
  --name switch/db/nucleo \
  --secret-string '{"username":"nucleo_user","password":"SECURE_PASS"}'

# Usar ExternalSecrets Operator para sincronizar a K8s
```

### 2. Rotación de Contraseñas
- [ ] Configurar rotación automática en RDS (30 días)
- [ ] Documentar proceso de actualización de secrets en K8s
- [ ] Reiniciar pods después de cambiar secrets

### 3. Principio de Mínimo Privilegio
- [ ] Cada microservicio solo accede a su propia base de datos
- [ ] Usuario DB con permisos específicos (no usar `postgres` root)

---

## 📞 COORDINACIÓN REQUERIDA

### Con Equipo APIM
- **Obtener:** Valor de `apim-origin-secret` (UUID)
- **Confirmar:** Que APIM inyecta este mismo valor en header `x-origin-secret`
- **Testing:** Probar que la validación funciona correctamente

### Con Equipo Networking
- **Confirmar:** Security Groups permiten tráfico EKS → RDS
- **Confirmar:** Security Groups permiten tráfico EKS → DocumentDB
- **Confirmar:** Security Groups permiten tráfico EKS → Amazon MQ

---

## ✅ CHECKLIST POST-CREACIÓN

Después de crear los secrets:

- [ ] Verificar secrets existen: `kubectl get secrets -n switch`
- [ ] Verificar todas las keys están presentes
- [ ] Hacer deploy de prueba de un microservicio
- [ ] Verificar logs del pod no muestran errores de conexión DB
- [ ] Verificar health checks responden OK
- [ ] Notificar al equipo Switch que secrets están listos

---

## 🚀 PRÓXIMOS PASOS

1. **DevOps crea secrets** (este documento)
2. **Equipo Switch configura GitHub Secrets** (AWS credentials)
3. **Push a main** → GitHub Actions hace deploy automático
4. **Verificar pods** se levantan correctamente
5. **Testing** de integración con APIM

---

**Contacto Switch Team:**  
- Documentación completa: `FLUJOS_COMPLETOS.md`  
- Workflows actualizados: `.github/workflows/deploy.yml` (cada repo)

---

**Fecha:** 2026-02-08  
**Versión:** 3.0.0
