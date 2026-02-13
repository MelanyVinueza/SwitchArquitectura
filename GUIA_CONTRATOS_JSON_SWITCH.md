# 📄 Guía de Contratos JSON - Switch Transaccional (APIM v2.0)

Esta guía define las estructuras exactas de datos (JSON) para la integración de bancos con el Switch a través del API Gateway (APIM).

---

## 🔐 Seguridad y Headers (Requerido por Terraform)

Antes de cualquier envío, el banco debe asegurarse de incluir los siguientes headers. Sin estos, el APIM rechazará la petición antes de llegar al Switch.

| Header | Valor / Ejemplo | Descripción |
| :--- | :--- | :--- |
| `Authorization` | `Bearer <JWT_TOKEN>` | Token obtenido de Cognito. |
| `Content-Type` | `application/json` | Obligatorio para todos los POST. |
| `X-Trace-ID` | `uuid-unico-por-peticion` | Recomendado para rastreo en CloudWatch. |

> **Nota sobre el Token:** Al solicitar el token a Cognito, asegúrese de pedir el scope: `https://switch-api.com/transfers.write`.

---

## 1. Validación de Cuentas (Account Lookup - acmt.023)
Permite verificar si una cuenta existe en otro banco y obtener el nombre del titular.

**Endpoint:** `POST /api/v2/switch/account-lookup`

### 📥 Request (Lo que el banco envía)
```json
{
  "header": {
    "originatingBankId": "BANTEC",
    "messageId": "VAL-20260213-001"
  },
  "body": {
    "targetBankId": "ARCBANK",
    "targetAccountNumber": "0987654321"
  }
}
```

### 📤 Response (Lo que el Switch responde)
```json
{
  "status": "SUCCESS",
  "data": {
    "exists": true,
    "ownerName": "JUAN PEREZ",
    "currency": "USD",
    "status": "ACTIVE",
    "accountName": "JUAN PEREZ",
    "mensaje": "Consulta exitosa"
  }
}
```

---

## 2. Transferencia Interbancaria (pacs.008)
Inicia una transferencia de fondos entre un cliente de origen y uno de destino.

**Endpoint:** `POST /api/v2/switch/transfers`

### 📥 Request (Lo que el banco envía)
```json
{
  "header": {
    "messageId": "MSG-TX-998877",
    "creationDateTime": "2026-02-13T08:00:00Z",
    "originatingBankId": "BANTEC"
  },
  "body": {
    "instructionId": "INSTR-554433",
    "endToEndId": "E2E-REF-7766",
    "amount": {
      "currency": "USD",
      "value": 25.50
    },
    "debtor": {
      "name": "MARIA LOPEZ",
      "accountId": "1122334455",
      "accountType": "SAVINGS",
      "bankId": "BANTEC"
    },
    "creditor": {
      "name": "JUAN PEREZ",
      "accountId": "0987654321",
      "accountType": "SAVINGS",
      "bankId": "ARCBANK"
    },
    "remittanceInformation": "Pago de servicios varios"
  }
}
```

### 📤 Response (Lo que el Switch responde)
*   **HTTP 201 (Created):** Transacción aceptada síncronamente.
*   **HTTP 202 (Accepted):** Transacción encolada (Asíncrona).

```json
{
  "idInstruccion": "INSTR-554433",
  "idMensaje": "MSG-TX-998877",
  "referenciaRed": "SW-REF-102030",
  "monto": 25.50,
  "moneda": "USD",
  "codigoBicOrigen": "BANTEC",
  "codigoBicDestino": "ARCBANK",
  "estado": "ACCEPTED",
  "codigoReferencia": "TX-PROC-01",
  "fechaCreacion": "2026-02-13T08:00:05"
}
```

---

## 3. Devoluciones / Reversos (pacs.004)
Solicita la devolución de una transferencia previamente aceptada (por error de cuenta, moneda, etc).

**Endpoint:** `POST /api/v2/switch/returns`

### 📥 Request (Lo que el banco envía)
```json
{
  "header": {
    "messageId": "MSG-RET-0099",
    "creationDateTime": "2026-02-13T08:15:00Z",
    "originatingBankId": "ARCBANK"
  },
  "body": {
    "returnInstructionId": "RET-INSTR-4455",
    "originalInstructionId": "INSTR-554433",
    "returnReason": "AC03",
    "returnAmount": {
      "currency": "USD",
      "value": 25.50
    }
  }
}
```

### 📤 Response (Lo que el Switch responde)
```json
{
  "status": "COMPLETED",
  "message": "Devolución procesada exitosamente"
}
```

---

## 🔍 Glosario de Estados de Respuesta (`estado`)

| Estado | Significado | Acción del Banco |
| :--- | :--- | :--- |
| `ACCEPTED` | El Switch aceptó y procesó la transferencia. | Mostrar éxito al usuario. |
| `QUEUED` | La transferencia está en proceso asíncrono. | Esperar callback o consultar estado después. |
| `FAILED` | Rechazada por validación técnica o de negocio. | Ver `codigoReferencia` para el motivo. |
| `TIMEOUT` | El banco destino no respondió a tiempo. | Consultar estado en un minuto. |

---
**Nota:** Todas las fechas deben seguir el formato ISO 8601 UTC (`YYYY-MM-DDTHH:mm:ssZ`).
