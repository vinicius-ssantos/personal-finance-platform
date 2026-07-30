# Arquitetura da plataforma

## Decisão principal

O projeto será um **monorepo com builds independentes**. O backend será um monólito modular Kotlin/Spring; Android e MCP serão adaptadores externos com contratos próprios.

## Contexto

```text
Usuário
  ├─ envia relatórios do Banco Inter ──> Backend Kotlin ──> PostgreSQL
  ├─ usa Android ──HTTPS/REST/OpenAPI──> Backend Kotlin
  └─ usa cliente de IA ─Streamable HTTP─> MCP adapter ──> application services

Open Finance futuro ──provider adapter──> contratos canônicos ──> domínio
```

## Componentes

| Componente | Responsabilidade |
|---|---|
| Backend | Ingestão, domínio, persistência, analytics, metas, REST, MCP e integrações |
| PostgreSQL | Fonte de verdade, histórico, auditoria e resultados versionados |
| REST/OpenAPI | Contrato público para Android e operação de primeira parte |
| Android | Interface, autenticação, cache de leitura, upload e acompanhamento |
| MCP | Tools e resources somente leitura para clientes de IA |
| Provider adapter futuro | Open Finance sem acoplar fornecedor ao domínio |

## Fronteiras de confiança

- **Backend e PostgreSQL:** zona confiável; somente serviços do servidor acessam a fonte de verdade.
- **REST:** fronteira autenticada; toda entrada é validada, autorizada, limitada e auditada.
- **Android:** cliente não confiável; não contém segredo do provider nem cálculo financeiro oficial.
- **MCP:** fronteira altamente restrita; apenas leitura/simulação, com audience e scopes próprios.
- **PDF/CSV:** conteúdo hostil; passa por limites, detecção, parsing versionado, prévia e confirmação.
- **Open Finance:** terceiro autorizado; tokens ficam apenas no backend e o consentimento é revogável.

## Módulos backend

A Release 0.1 materializa seis módulos: `shared`, `audit`, `portfolio`, `ingestion`, `persistence` e `api`. A matriz de dependências, os pacotes raiz e os testes executáveis estão em [Backend module boundaries](MODULES.md).

`analytics`, `goals`, `mcp`, autenticação remota e provider adapters permanecem planejados, mas não são criados como pacotes vazios antes das respectivas issues.

## Regras de dependência

1. `portfolio` não depende de `ingestion`, `api`, `mcp` ou Android.
2. `analytics` consulta portas do domínio, não tabelas diretamente.
3. `goals` usa resultados de `portfolio` e `analytics`.
4. REST e MCP chamam os mesmos application services.
5. Persistência implementa portas definidas pelos módulos.
6. `shared` contém apenas tipos realmente compartilhados.
7. Credenciais de providers permanecem em adapters específicos.
8. Android nunca importa classes do backend.

Spring Modulith e ArchUnit verificam essas fronteiras em cada execução de `backend-check`. A estrutura de pastas serve à direção de dependência, não à criação de camadas vazias.

## Persistência

- PostgreSQL é a fonte de verdade.
- Flyway controla o schema.
- Spring Data JDBC é a escolha inicial.
- Posições e resultados confirmados são imutáveis.
- Correções criam nova versão e apontam qual versão é efetiva.
- Commit de importação, evidências e auditoria ocorre na mesma transação.
- Processamento assíncrono futuro usa jobs duráveis e transactional outbox no PostgreSQL.

## Tipos financeiros

- dinheiro em minor units com moeda explícita;
- quantidade, cotas, preço e taxa em `BigDecimal`;
- sem `float` ou `double` para valores financeiros;
- taxas como razão decimal (`0.30 = 30%`);
- datas financeiras em `LocalDate` e instantes técnicos em `Instant`;
- operações entre moedas exigem taxa e evidência explícitas.

## Contratos

- OpenAPI 3.1 versionado em `contracts/openapi/`;
- JSON em camelCase;
- dinheiro em `{ amountMinor, currency }`;
- datas ISO-8601;
- Problem Details para erros;
- `Cache-Control: no-store` para dados financeiros;
- mudanças incompatíveis exigem nova versão da API.

## Android

A arquitetura móvel seguirá fluxo unidirecional:

```text
Compose -> UserAction -> ViewModel -> UseCase -> Repository
Repository -> Remote/Local -> DomainResult -> StateFlow<UiState> -> Compose
```

O cache será somente leitura, com frescor explícito. Biometria é bloqueio local, não autorização do servidor. Kotlin Multiplatform só será considerado após o Android estabilizar e houver demanda real por iOS.

## MCP

- Streamable HTTP;
- servidor síncrono inicialmente;
- tools e resources ativados;
- prompts e completions desativados;
- audience e scopes separados do mobile;
- sem SQL arbitrário, PDF, texto bruto, CPF, conta completa ou tokens;
- sem ferramentas de Pix, transferência, compra, venda ou resgate.

## Decisões adiadas

Open Finance, OIDC remoto, Android, MCP, KMP e iOS não bloqueiam a Release 0.1. Cada fase só entra após o gate anterior e por ADR quando houver trade-off relevante.
