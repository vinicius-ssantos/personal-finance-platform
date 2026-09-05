# Personal Finance Platform — Contrato de API

> **Versão:** 1.0  
> **Status:** contrato conceitual da Release 0.1  
> Rotas podem ser refinadas sem alterar a semântica normativa.

# 1. Convenções

JSON:

```text
Content-Type: application/json
```

Upload:

```text
multipart/form-data
```

Headers:

```text
X-Correlation-Id
Idempotency-Key
```

# 2. Erros

Problem Details estendido:

```json
{
  "type": "https://errors.local/pf/preview-version-conflict",
  "title": "Preview desatualizada",
  "status": 409,
  "code": "PF_PREVIEW_VERSION_CONFLICT",
  "detail": "A preview informada não é mais a versão atual.",
  "correlationId": "..."
}
```

Cliente usa `code`, não `title/detail`, para lógica.

# 3. Criar import

```text
POST /imports
```

multipart com arquivo.

Resposta conceitual:

```json
{
  "importId": "uuid",
  "status": "RECEIVED",
  "nextActions": []
}
```

# 4. Consultar import

```text
GET /imports/{importId}
```

Resposta:

```json
{
  "importId": "uuid",
  "status": "PREVIEW_READY",
  "currentPreviewVersion": 3,
  "nextActions": ["VIEW_PREVIEW", "COMMIT", "REJECT"],
  "error": null
}
```

# 5. Enviar senha

```text
POST /imports/{importId}/password
```

```json
{
  "password": "..."
}
```

Senha nunca em query param.

# 6. Consultar preview

```text
GET /imports/{importId}/preview
```

Resposta inclui:

- version;
- source;
- parser;
- accounts;
- positions;
- reconciliation;
- warnings;
- blockers;
- commitAllowed.

# 7. Commit

```text
POST /imports/{importId}/commit
```

```json
{
  "previewVersion": 3,
  "previewDigest": "optional",
  "idempotencyKey": "optional"
}
```

Cliente não envia posições arbitrárias.

# 8. Reject

```text
POST /imports/{importId}/reject
```

Idempotente.

# 9. Portfolio atual

```text
GET /portfolio/current
```

Somente snapshots efetivos.

# 10. Histórico

```text
GET /portfolio/snapshots
GET /portfolio/snapshots/{snapshotId}
```

Ordenação determinística.

# 11. Money

```json
{
  "amountMinor": 12345,
  "currency": "BRL"
}
```

# 12. Decimais

Serializados sem floating point.

Preferência por string decimal quando necessário preservar escala.

# 13. Datas

```text
LocalDate -> YYYY-MM-DD
Instant -> ISO-8601 com offset/UTC
```

# 14. HTTP recomendado

| Caso | HTTP |
|---|---:|
| invalid payload | 400 |
| not found | 404 |
| conflict/stale | 409 |
| domain validation | 422 |
| password attempts/rate | 429/422 |
| unavailable | 503 |
| internal | 500 |
