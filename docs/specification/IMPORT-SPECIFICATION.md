# Personal Finance Platform — Especificação de Importação

> **Versão:** 1.0  
> **Derivado da Product Specification v5.0**

# 1. Objetivo

Formalizar o fluxo completo de ingestão da Release 0.1.

# 2. Pipeline

```text
UPLOAD
→ FINGERPRINT
→ PASSWORD?
→ EXTRACTION
→ LAYOUT DETECTION
→ PARSER SELECTION
→ PARSING
→ NORMALIZATION
→ RECONCILIATION
→ PREVIEW
→ COMMIT/REJECT
```

# 3. Upload

- PDF com limite configurável.
- Não confiar em extensão.
- Nome temporário gerado pelo servidor.
- Path traversal impossível.
- Arquivo classificado como sensível.
- Retenção efêmera.

Defaults recomendados:

```text
25 MiB
250 páginas
5 tentativas de senha
TTL de senha: 15 minutos
```

# 4. Fingerprint

SHA-256 recomendado.

Fingerprint:

- é persistente;
- não depende do filename;
- suporta detecção de duplicata;
- não autoriza commit duplicado.

# 5. Duplicata

Mesmo fingerprint = fonte conhecida.

Sistema não deve criar snapshot duplicado silenciosamente.

# 6. Password continuation

Senha:

- vinculada ao ImportBatch;
- efêmera;
- nunca persistida;
- nunca logada;
- tentativa limitada.

# 7. Extração

PDFBox.

Sem OCR na 0.1.

Output:

```text
ExtractedDocument
- fingerprint
- pages
- metadataSanitized
```

# 8. Layout detection

Fail-closed.

Output:

```text
institution
documentFamily
layoutVersion
evidence
```

Unknown layout -> `PF_LAYOUT_UNSUPPORTED`.

# 9. Parser registry

Chave:

```text
institution
documentFamily
layoutVersion
parserVersion
```

Parser é side-effect free sobre portfolio confirmado.

# 10. Parsing

Produz dados específicos da fonte.

Não produz entidade confirmada.

# 11. Normalização

Converte:

- moedas;
- números;
- datas;
- qualidade;
- identifiers;
- evidências.

Locale da máquina não influencia resultado.

# 12. Asset resolution

Merge conservador.

Nome sozinho não é identidade forte.

Ambiguidade crítica -> blocker.

# 13. Reconciliação

Por:

- moeda;
- conta;
- seção;
- categoria, quando aplicável.

Outcomes:

- PASS
- WARNING
- BLOCKER
- NOT_APPLICABLE
- NOT_VERIFIABLE

# 14. Preview

Persistência durável é a opção preferida.

Preview:

- possui version;
- possui blockers/warnings;
- não altera portfolio;
- é a unidade de decisão.

# 15. Resolução

Mudança material gera nova previewVersion.

# 16. Commit

Commit recebe referência à preview do servidor.

Cliente não envia posições editadas arbitrariamente.

Transação:

```text
validate
lock
insert snapshot
insert positions
insert evidence
insert audit
update effective pointer
mark committed
COMMIT
```

# 17. Reject

Terminal e idempotente.

# 18. Reprocessamento

Nova interpretação controlada.

Não altera snapshot confirmado sem novo commit.

# 19. Golden flows

Obrigatórios:

- sucesso;
- password;
- blocker;
- rollback;
- duplicate;
- stale preview.
