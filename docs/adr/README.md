# Catálogo de decisões arquiteturais

## Convenção

Cada ADR usa o formato:

```text
# ADR NNNN — Título
Status: Proposed | Accepted | Superseded | Rejected
Date: YYYY-MM-DD

## Context
## Decision
## Consequences
## Alternatives considered
```

Decisões bloqueantes da Release 0.1 devem estar `Accepted` antes do primeiro código funcional. Mudanças posteriores não reescrevem ADR aceito; criam novo ADR que o substitui.

## Decisões aceitas na fundação

| ADR | Título | Decisão |
|---:|---|---|
| 0001 | Kotlin/JVM no backend | Linguagem principal do servidor; interoperabilidade Java permitida |
| 0002 | Gradle Kotlin DSL | Builds backend e mobile em `.gradle.kts` |
| 0003 | Monólito modular | Um deploy backend, módulos e contratos internos explícitos |
| 0004 | PostgreSQL como fonte de verdade | Histórico, consistência relacional, auditoria e jobs duráveis |
| 0005 | Dinheiro em minor units | `Long` tipado + moeda; sem ponto flutuante |
| 0006 | Importação com preview | Parser não altera o domínio antes de confirmação |
| 0007 | Snapshots imutáveis | Posições por data e versão, sem sobrescrita silenciosa |
| 0008 | PDFBox sem OCR no MVP | Texto digital determinístico como caminho principal |
| 0009 | Parser registry versionado | Mudanças de layout isoladas e reprocessamento auditável |
| 0010 | Cálculos sem LLM | IA explica; código determinístico calcula |
| 0011 | MCP somente leitura | Nenhuma ferramenta de movimentação financeira |
| 0012 | MCP Streamable HTTP | Transporte futuro escolhido |
| 0013 | REST e MCP compartilham application services | Uma única regra de negócio |
| 0014 | Arquivo bruto efêmero | Minimização, retenção curta e purge verificável |
| 0015 | Open Finance por provider adapter | Domínio não conhece Pluggy, Belvo ou equivalente |
| 0016 | Projeções imutáveis | Inputs, versão, checksum e resultado reproduzíveis |
| 0017 | Dado desconhecido não é zero | Ausência, estimativa e conflito são explícitos |
| 0018 | Android como primeira plataforma | Kotlin + Jetpack Compose |
| 0019 | OpenAPI para mobile e MCP para IA | Protocolos, audiences e clientes separados |
| 0020 | Backend como fonte de verdade | Nenhum cálculo financeiro oficial no dispositivo |
| 0021 | Monorepo com builds isolados | Um repositório; backend e mobile independentes |
| 0022 | Compartilhamento seletivo | Não compartilhar entidades Spring/JDBC com mobile |
| 0023 | KMP preparado, não antecipado | Extração apenas após Android e demanda iOS |
| 0024 | OIDC Authorization Code + PKCE | Autenticação futura de app instalado |
| 0025 | Biometria como bloqueio local | Não substitui autorização do backend |
| 0026 | Cache mobile read-only | Dados mínimos e frescor explícito |
| 0027 | Notificações sem valores | Privacidade na tela bloqueada |
| 0028 | OWASP MASVS como baseline | Segurança móvel verificável |
| 0029 | Relatórios reais como base empírica | PDFs privados orientam fixtures sintéticas |
| 0030 | Senha de PDF efêmera | Segredo não persistido e continuidade por uso único |
| 0031 | Temporalidade financeira explícita | Posição, período, geração e mercado não são colapsados |
| 0032 | Evidência por campo | Origem, qualidade e confiança acompanham dados críticos |
| 0033 | Movimentos ambíguos permanecem não classificados | Sinal não prova natureza econômica |
| 0034 | Reconciliação multimoeda | BRL e USD fecham separadamente antes de conversão |
| 0035 | Notas de renda fixa como enriquecimento | Evidência adicional não sobrescreve fonte original |
| 0036 | Percentuais como razão decimal | `0.30` representa 30% em domínio e contratos |
| 0037 | Jobs duráveis no PostgreSQL | Sem fila somente em memória para trabalho recuperável |
| 0038 | Transactional outbox para efeitos assíncronos | Commit e evento persistem na mesma transação |
| 0039 | Versão efetiva de snapshot | Histórico completo com ponteiro explícito para versão vigente |
| 0040 | Evidência persistida em modelo híbrido | Colunas críticas + JSONB/observações, evitando EAV irrestrito |
| 0041 | Fronteira patrimonial de movimentos | `EXTERNAL`, `INTERNAL` e `UNKNOWN` separados do tipo econômico |
| 0042 | Conversão cambial auditável | Taxa, fonte, data e arredondamento explícitos |
| 0043 | Release 0.1 local-first | Loopback, MCP e acesso remoto desabilitados |
| 0044 | Snapshot síncrono no primeiro corte | Release 0.1 atualiza fotografia efetiva na transação de commit |

## ADRs bloqueantes materializados

Os ADRs abaixo estão `Accepted` e formam o gate arquitetural da Release 0.1:

- [ADR 0001 — Kotlin/JVM no backend](0001-kotlin-jvm-backend.md);
- [ADR 0003 — Monólito modular](0003-modular-monolith.md);
- [ADR 0004 — PostgreSQL como fonte de verdade](0004-postgresql-source-of-truth.md);
- [ADR 0005 — Dinheiro em minor units](0005-money-minor-units.md);
- [ADR 0006 — Importação com preview antes do commit](0006-import-preview-before-commit.md);
- [ADR 0007 — Snapshots imutáveis](0007-immutable-snapshots.md);
- [ADR 0008 — PDFBox sem OCR no MVP](0008-pdfbox-no-ocr-mvp.md);
- [ADR 0009 — Parser registry versionado](0009-versioned-parser-registry.md);
- [ADR 0014 — Arquivo bruto efêmero](0014-ephemeral-source-file.md);
- [ADR 0017 — Dado desconhecido não é zero](0017-unknown-is-not-zero.md);
- [ADR 0021 — Monorepo com builds isolados](0021-monorepo-isolated-builds.md);
- [ADR 0029 — Relatórios reais privados e fixtures sintéticas](0029-real-reports-private-fixtures-synthetic.md);
- [ADR 0030 — Senha de PDF efêmera](0030-ephemeral-pdf-password.md);
- [ADR 0031 — Temporalidade financeira explícita](0031-explicit-financial-temporality.md);
- [ADR 0034 — Reconciliação multimoeda](0034-multi-currency-reconciliation.md);
- [ADR 0036 — Percentuais como razão decimal](0036-percentage-decimal-ratio.md);
- [ADR 0039 — Versão efetiva de snapshot](0039-effective-snapshot-version.md);
- [ADR 0043 — Release 0.1 local-first](0043-release-01-local-first.md);
- [ADR 0044 — Snapshot síncrono na Release 0.1](0044-synchronous-snapshot-release-01.md).

Decisões do catálogo ainda sem arquivo permanecem registradas como direção de produto, mas não bloqueiam o corte atual até que uma issue as materialize.

## Critérios para novo ADR

Criar ADR quando houver:

- mudança de fonte de verdade;
- nova fronteira de confiança;
- alteração de representação financeira;
- quebra de contrato público;
- introdução de serviço externo ou custo recorrente;
- mudança de estratégia de persistência, segurança ou processamento;
- nova plataforma cliente;
- decisão difícil de reverter.

Não criar ADR para refatoração local, nome de classe ou detalhe facilmente reversível.
