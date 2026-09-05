# Personal Finance Platform — Catálogo de Erros

> **Versão:** 1.0  
> **Derivado da Product Specification v5.0**

# 1. Convenção

```text
PF_<AREA>_<ERROR>
```

Cada erro possui:

- code;
- HTTP;
- severity;
- RetryClass;
- detail exposure policy.

# 2. RetryClass

```text
NEVER
RETRY_SAFE
RETRY_WITH_BACKOFF
USER_ACTION_REQUIRED
REFRESH_REQUIRED
REPROCESS_REQUIRED
```

# 3. Catálogo inicial

| Código | HTTP | Severity | RetryClass | Descrição |
|---|---:|---|---|---|
| PF_IMPORT_INVALID_TRANSITION | 409 | FAILURE | NEVER | transição inválida |
| PF_IMPORT_DUPLICATE_SOURCE | 409 | INFO/WARNING | USER_ACTION_REQUIRED | fonte já conhecida |
| PF_PDF_PASSWORD_REQUIRED | 409 | INFO | USER_ACTION_REQUIRED | senha requerida |
| PF_PDF_INVALID_PASSWORD | 422 | FAILURE | USER_ACTION_REQUIRED | senha incorreta |
| PF_PDF_PASSWORD_ATTEMPTS_EXCEEDED | 429/422 | FAILURE | USER_ACTION_REQUIRED | limite atingido |
| PF_PDF_EXTRACTION_FAILED | 422 | FAILURE | REPROCESS_REQUIRED | extração falhou |
| PF_LAYOUT_UNSUPPORTED | 422 | FAILURE | REPROCESS_REQUIRED | layout desconhecido |
| PF_PARSER_FAILED | 422/500 | FAILURE | REPROCESS_REQUIRED | parser falhou |
| PF_RECON_BLOCKING_MISMATCH | 422 | BLOCKER | USER_ACTION_REQUIRED | divergência financeira |
| PF_PREVIEW_VERSION_CONFLICT | 409 | FAILURE | REFRESH_REQUIRED | preview stale |
| PF_COMMIT_NOT_ALLOWED | 409/422 | FAILURE | NEVER | estado não commitável |
| PF_DATABASE_UNAVAILABLE | 503 | FAILURE | RETRY_WITH_BACKOFF | DB indisponível |
| PF_INTERNAL_ERROR | 500 | FAILURE | RETRY_WITH_BACKOFF | erro inesperado |

# 4. Regras

- código não muda significado;
- texto humano pode evoluir;
- stack trace não chega ao cliente;
- correlation ID deve existir;
- senha/PII não entra em detail;
- retry não é inferido apenas pelo HTTP.
