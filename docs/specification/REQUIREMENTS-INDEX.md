# Personal Finance Platform — Índice de Requisitos v5

> Derivado de [`PRODUCT-SPECIFICATION.md`](PRODUCT-SPECIFICATION.md) (Product Specification v5.0).
> Gerado a partir dos headings normativos. As referências são âncoras estáveis, não números de linha.

**Total formal de requisitos/invariantes:** 326

## Famílias

| Família | Requisitos |
|---|---:|
| `INV` | 35 |
| `NFR-SEC` | 27 |
| `NFR-DATA` | 14 |
| `FR-PREVIEW` | 12 |
| `NFR-COMPAT` | 12 |
| `NFR-DB` | 12 |
| `NFR-API` | 10 |
| `FR-PDF` | 9 |
| `FR-RECON` | 9 |
| `FR-ASSET` | 8 |
| `FR-PARSER` | 8 |
| `FR-UPLOAD` | 8 |
| `FR-API` | 7 |
| `FR-COMMIT` | 7 |
| `FR-PASSWORD` | 7 |
| `NFR-ARCH` | 7 |
| `NFR-SOURCE` | 7 |
| `FR-ERROR` | 6 |
| `FR-NORMALIZE` | 6 |
| `FR-SNAPSHOT` | 6 |
| `NFR-OBS` | 6 |
| `NFR-REL` | 6 |
| `NFR-SER` | 6 |
| `NFR-TX` | 6 |
| `FR-EVIDENCE` | 5 |
| `FR-FIXTURE` | 5 |
| `FR-IDEMP` | 5 |
| `FR-REJECT` | 5 |
| `FR-REPROCESS` | 5 |
| `NFR-TIME` | 5 |
| `FR-IMPORT` | 4 |
| `FR-LAYOUT` | 4 |
| `NFR-AUDIT` | 4 |
| `NFR-CONFIG` | 4 |
| `NFR-OPS` | 4 |
| `NFR-PARSER` | 4 |
| `NFR-VERIFY` | 4 |
| `FR-ACCOUNT` | 3 |
| `NFR-AI` | 3 |
| `NFR-DERIVED` | 3 |
| `NFR-EVENT` | 3 |
| `NFR-PERF` | 3 |
| `NFR-RECON` | 3 |
| `NFR-RETRY` | 3 |
| `NFR-SPEC` | 2 |
| `NFR-E2E` | 1 |
| `NFR-ERROR` | 1 |
| `NFR-PORT` | 1 |
| `NFR-RELEASE` | 1 |

## Requisitos

A ordem segue a especificação canônica.

| ID | Requisito | Referência |
|---|---|---|
| `INV-001` | Todo `Money` DEVE possuir moeda. | [ver](PRODUCT-SPECIFICATION.md#inv-001--dinheiro-nunca-sem-moeda) |
| `INV-002` | BRL + USD é inválido sem conversão. | [ver](PRODUCT-SPECIFICATION.md#inv-002--sem-soma-cross-currency-implícita) |
| `INV-003` | `Unknown` e `Exact(0)` são estados diferentes. | [ver](PRODUCT-SPECIFICATION.md#inv-003--desconhecido-diferente-de-zero) |
| `INV-004` | Uma versão confirmada NÃO DEVE ser editada. | [ver](PRODUCT-SPECIFICATION.md#inv-004--snapshot-confirmado-é-imutável) |
| `INV-005` | Para uma mesma chave lógica de conta/data, no máximo uma versão pode ser efetiva. | [ver](PRODUCT-SPECIFICATION.md#inv-005--somente-uma-versão-efetiva) |
| `INV-006` | Parser NÃO DEVE escrever posições confirmadas. | [ver](PRODUCT-SPECIFICATION.md#inv-006--parser-não-confirma-patrimônio) |
| `INV-007` | Criar preview NÃO DEVE alterar versão efetiva. | [ver](PRODUCT-SPECIFICATION.md#inv-007--preview-não-confirma-patrimônio) |
| `INV-008` | Preview obsoleta NÃO DEVE ser commitada. | [ver](PRODUCT-SPECIFICATION.md#inv-008--commit-exige-preview-atual) |
| `INV-009` | Snapshot, posições, auditoria e ponteiro efetivo DEVEM ser persistidos na mesma unidade transacional. | [ver](PRODUCT-SPECIFICATION.md#inv-009--commit-é-atômico) |
| `INV-010` | Relatórios reais NÃO DEVEM ser versionados. | [ver](PRODUCT-SPECIFICATION.md#inv-010--arquivo-real-não-vai-para-git) |
| `INV-011` | Senha de PDF NÃO DEVE ser gravada em banco ou logs. | [ver](PRODUCT-SPECIFICATION.md#inv-011--senha-não-é-persistida) |
| `INV-012` | `Float` e `Double` NÃO DEVEM representar valores financeiros. | [ver](PRODUCT-SPECIFICATION.md#inv-012--dados-financeiros-não-usam-floating-point) |
| `FR-UPLOAD-001` | A aplicação DEVE aceitar um arquivo financeiro suportado. | [ver](PRODUCT-SPECIFICATION.md#fr-upload-001) |
| `FR-UPLOAD-002` | O tamanho do upload DEVE possuir limite configurável. | [ver](PRODUCT-SPECIFICATION.md#fr-upload-002) |
| `FR-UPLOAD-003` | A aplicação NÃO DEVE confiar apenas em extensão de arquivo. | [ver](PRODUCT-SPECIFICATION.md#fr-upload-003) |
| `FR-UPLOAD-004` | O nome de armazenamento temporário DEVE ser gerado pelo servidor. | [ver](PRODUCT-SPECIFICATION.md#fr-upload-004) |
| `FR-UPLOAD-005` | O caminho real do filesystem NÃO DEVE ser retornado. | [ver](PRODUCT-SPECIFICATION.md#fr-upload-005) |
| `FR-UPLOAD-006` | O arquivo fonte DEVE ser tratado como sensível. | [ver](PRODUCT-SPECIFICATION.md#fr-upload-006) |
| `FR-UPLOAD-007` | Arquivo temporário DEVE ser removido ao fim da política de retenção. | [ver](PRODUCT-SPECIFICATION.md#fr-upload-007) |
| `FR-UPLOAD-008` | Falha durante ingestão NÃO DEVE deixar arquivos órfãos indefinidamente. | [ver](PRODUCT-SPECIFICATION.md#fr-upload-008) |
| `FR-PASSWORD-001` | Se o PDF exigir senha, o import DEVE entrar em estado recuperável. | [ver](PRODUCT-SPECIFICATION.md#fr-password-001) |
| `FR-PASSWORD-002` | A senha DEVE ser vinculada a um `ImportBatchId`. | [ver](PRODUCT-SPECIFICATION.md#fr-password-002) |
| `FR-PASSWORD-003` | A senha NÃO DEVE ser persistida. | [ver](PRODUCT-SPECIFICATION.md#fr-password-003) |
| `FR-PASSWORD-004` | A senha NÃO DEVE aparecer em: | [ver](PRODUCT-SPECIFICATION.md#fr-password-004) |
| `FR-PASSWORD-005` | Tentativas DEVEM ter limite. | [ver](PRODUCT-SPECIFICATION.md#fr-password-005) |
| `FR-PASSWORD-006` | O mecanismo DEVE impedir reutilização indefinida da continuação. | [ver](PRODUCT-SPECIFICATION.md#fr-password-006) |
| `FR-PASSWORD-007` | Após TTL, nova submissão da senha DEVE ser exigida. | [ver](PRODUCT-SPECIFICATION.md#fr-password-007) |
| `FR-PDF-001` | A Release 0.1 DEVE usar extração nativa de texto. | [ver](PRODUCT-SPECIFICATION.md#fr-pdf-001) |
| `FR-PDF-002` | PDFBox é a biblioteca selecionada. | [ver](PRODUCT-SPECIFICATION.md#fr-pdf-002) |
| `FR-PDF-003` | OCR automático NÃO DEVE existir na Release 0.1. | [ver](PRODUCT-SPECIFICATION.md#fr-pdf-003) |
| `FR-PDF-004` | PDF sem texto suficiente DEVE falhar de forma segura. | [ver](PRODUCT-SPECIFICATION.md#fr-pdf-004) |
| `FR-PDF-005` | A extração DEVERIA preservar número da página. | [ver](PRODUCT-SPECIFICATION.md#fr-pdf-005) |
| `FR-PDF-006` | Conteúdo ativo do PDF NÃO DEVE ser executado. | [ver](PRODUCT-SPECIFICATION.md#fr-pdf-006) |
| `FR-PDF-007` | A extração DEVE possuir limites de tempo/memória compatíveis com o ambiente local. | [ver](PRODUCT-SPECIFICATION.md#fr-pdf-007) |
| `FR-LAYOUT-001` | Deve identificar: | [ver](PRODUCT-SPECIFICATION.md#fr-layout-001) |
| `FR-LAYOUT-002` | Layout desconhecido DEVE falhar fechado. | [ver](PRODUCT-SPECIFICATION.md#fr-layout-002) |
| `FR-LAYOUT-003` | Não pode existir fallback: | [ver](PRODUCT-SPECIFICATION.md#fr-layout-003) |
| `FR-LAYOUT-004` | O resultado DEVERIA registrar evidências. | [ver](PRODUCT-SPECIFICATION.md#fr-layout-004) |
| `FR-PARSER-001` | Um layout conhecido DEVE selecionar parser explicitamente. | [ver](PRODUCT-SPECIFICATION.md#fr-parser-001) |
| `FR-PARSER-002` | O parser DEVE ser determinístico. | [ver](PRODUCT-SPECIFICATION.md#fr-parser-002) |
| `FR-PARSER-003` | Mudança incompatível DEVE criar nova versão. | [ver](PRODUCT-SPECIFICATION.md#fr-parser-003) |
| `FR-PARSER-004` | A versão usada DEVE ser persistida. | [ver](PRODUCT-SPECIFICATION.md#fr-parser-004) |
| `FR-PARSER-005` | Parser NÃO DEVE acessar persistência de portfolio diretamente. | [ver](PRODUCT-SPECIFICATION.md#fr-parser-005) |
| `FR-PARSER-006` | Parser DEVE produzir candidatos, não entidades confirmadas. | [ver](PRODUCT-SPECIFICATION.md#fr-parser-006) |
| `FR-FIXTURE-001` | Relatórios reais permanecem privados. | [ver](PRODUCT-SPECIFICATION.md#fr-fixture-001) |
| `FR-FIXTURE-002` | Fixtures versionadas DEVEM ser sintéticas. | [ver](PRODUCT-SPECIFICATION.md#fr-fixture-002) |
| `FR-FIXTURE-003` | Fixtures DEVEM preservar estrutura relevante. | [ver](PRODUCT-SPECIFICATION.md#fr-fixture-003) |
| `FR-FIXTURE-004` | Fixtures NÃO DEVEM conter: | [ver](PRODUCT-SPECIFICATION.md#fr-fixture-004) |
| `FR-FIXTURE-005` | Cada parser DEVERIA possuir: | [ver](PRODUCT-SPECIFICATION.md#fr-fixture-005) |
| `FR-RECON-001` | Reconciliação DEVE ocorrer por moeda. | [ver](PRODUCT-SPECIFICATION.md#fr-recon-001) |
| `FR-RECON-002` | BRL e USD NÃO DEVEM ser somados diretamente. | [ver](PRODUCT-SPECIFICATION.md#fr-recon-002) |
| `FR-RECON-003` | Tolerância DEVE ser explícita. | [ver](PRODUCT-SPECIFICATION.md#fr-recon-003) |
| `FR-RECON-004` | Resultado DEVE ser classificado em: | [ver](PRODUCT-SPECIFICATION.md#fr-recon-004) |
| `FR-RECON-005` | `BLOCKER` impede commit. | [ver](PRODUCT-SPECIFICATION.md#fr-recon-005) |
| `FR-RECON-006` | `WARNING` pode permitir commit conforme política. | [ver](PRODUCT-SPECIFICATION.md#fr-recon-006) |
| `FR-PREVIEW-001` | Toda importação commitável DEVE possuir preview. | [ver](PRODUCT-SPECIFICATION.md#fr-preview-001) |
| `FR-PREVIEW-002` | Preview DEVE possuir versão. | [ver](PRODUCT-SPECIFICATION.md#fr-preview-002) |
| `FR-PREVIEW-003` | Preview DEVE informar: | [ver](PRODUCT-SPECIFICATION.md#fr-preview-003) |
| `FR-PREVIEW-004` | Preview NÃO altera snapshot efetivo. | [ver](PRODUCT-SPECIFICATION.md#fr-preview-004) |
| `FR-PREVIEW-005` | Commit DEVE enviar `previewVersion`. | [ver](PRODUCT-SPECIFICATION.md#fr-preview-005) |
| `FR-PREVIEW-006` | Versão obsoleta DEVE resultar em erro estável. | [ver](PRODUCT-SPECIFICATION.md#fr-preview-006) |
| `FR-COMMIT-001` | Somente preview atual pode ser commitada. | [ver](PRODUCT-SPECIFICATION.md#fr-commit-001) |
| `FR-COMMIT-002` | Commit DEVE ser ação explícita. | [ver](PRODUCT-SPECIFICATION.md#fr-commit-002) |
| `FR-COMMIT-003` | Release 0.1 utiliza commit síncrono. | [ver](PRODUCT-SPECIFICATION.md#fr-commit-003) |
| `FR-COMMIT-004` | A transação DEVE incluir: | [ver](PRODUCT-SPECIFICATION.md#fr-commit-004) |
| `FR-COMMIT-005` | Falha causa rollback completo. | [ver](PRODUCT-SPECIFICATION.md#fr-commit-005) |
| `FR-COMMIT-006` | Resposta de sucesso somente após commit do banco. | [ver](PRODUCT-SPECIFICATION.md#fr-commit-006) |
| `FR-COMMIT-007` | Retry do mesmo comando NÃO DEVE criar snapshots duplicados. | [ver](PRODUCT-SPECIFICATION.md#fr-commit-007) |
| `FR-REJECT-001` | Rejeição DEVE tornar o estado terminal. | [ver](PRODUCT-SPECIFICATION.md#fr-reject-001) |
| `FR-REJECT-002` | Rejeição DEVE ser auditada. | [ver](PRODUCT-SPECIFICATION.md#fr-reject-002) |
| `FR-REJECT-003` | Import rejeitado NÃO pode ser commitado posteriormente sem reprocessamento explícito. | [ver](PRODUCT-SPECIFICATION.md#fr-reject-003) |
| `FR-SNAPSHOT-001` | Snapshot confirmado é imutável. | [ver](PRODUCT-SPECIFICATION.md#fr-snapshot-001) |
| `FR-SNAPSHOT-002` | Correção cria nova versão. | [ver](PRODUCT-SPECIFICATION.md#fr-snapshot-002) |
| `FR-SNAPSHOT-003` | Histórico permanece consultável. | [ver](PRODUCT-SPECIFICATION.md#fr-snapshot-003) |
| `FR-SNAPSHOT-004` | Versão efetiva é explícita. | [ver](PRODUCT-SPECIFICATION.md#fr-snapshot-004) |
| `FR-SNAPSHOT-005` | "Último ID" NÃO define estado atual. | [ver](PRODUCT-SPECIFICATION.md#fr-snapshot-005) |
| `FR-SNAPSHOT-006` | "Último created_at" NÃO define necessariamente estado atual. | [ver](PRODUCT-SPECIFICATION.md#fr-snapshot-006) |
| `NFR-DB-001` | PostgreSQL é source of truth. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-001) |
| `NFR-DB-002` | Flyway gerencia schema. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-002) |
| `NFR-DB-003` | Migration aplicada NÃO deve ser editada. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-003) |
| `NFR-DB-004` | Testes de integração usam PostgreSQL real. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-004) |
| `NFR-DB-005` | H2/SQLite não provam compatibilidade. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-005) |
| `NFR-DB-006` | Reset deve ser local-safe. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-006) |
| `NFR-ARCH-001` | Backend permanece monólito modular na 0.1. | [ver](PRODUCT-SPECIFICATION.md#nfr-arch-001) |
| `NFR-ARCH-002` | Fronteiras são verificadas automaticamente. | [ver](PRODUCT-SPECIFICATION.md#nfr-arch-002) |
| `NFR-ARCH-003` | Domínio NÃO depende de API. | [ver](PRODUCT-SPECIFICATION.md#nfr-arch-003) |
| `NFR-ARCH-004` | Domínio NÃO depende de persistence. | [ver](PRODUCT-SPECIFICATION.md#nfr-arch-004) |
| `NFR-ARCH-005` | API não acessa DB diretamente. | [ver](PRODUCT-SPECIFICATION.md#nfr-arch-005) |
| `NFR-ARCH-006` | `shared` permanece framework-free. | [ver](PRODUCT-SPECIFICATION.md#nfr-arch-006) |
| `NFR-ARCH-007` | Adapters implementam ports do domínio/aplicação. | [ver](PRODUCT-SPECIFICATION.md#nfr-arch-007) |
| `NFR-SEC-001` | Servidor local deve bindar loopback. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-001) |
| `NFR-SEC-002` | PostgreSQL local deve bindar loopback. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-002) |
| `NFR-SEC-003` | Release 0.1 não expõe tunnel. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-003) |
| `NFR-SEC-004` | Credencial local não serve como credencial remota. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-004) |
| `NFR-SEC-005` | Senha nunca é persistida. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-005) |
| `NFR-SEC-006` | Logs devem aplicar redaction. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-006) |
| `NFR-SEC-007` | PDF não é enviado a serviços externos. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-007) |
| `NFR-SEC-008` | Fixtures são sintéticas. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-008) |
| `NFR-SEC-009` | Endpoints de administração não devem ser expostos além do necessário. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-009) |
| `NFR-REL-001` | Mesmo input + parser = mesmo output normalizado. | [ver](PRODUCT-SPECIFICATION.md#nfr-rel-001) |
| `NFR-REL-002` | Mesmo commit idempotente não cria duplicata. | [ver](PRODUCT-SPECIFICATION.md#nfr-rel-002) |
| `NFR-REL-003` | Restart não perde estado confirmado. | [ver](PRODUCT-SPECIFICATION.md#nfr-rel-003) |
| `NFR-REL-004` | Migration falha impede readiness. | [ver](PRODUCT-SPECIFICATION.md#nfr-rel-004) |
| `NFR-REL-005` | Falha parcial não deve aparentar sucesso. | [ver](PRODUCT-SPECIFICATION.md#nfr-rel-005) |
| `NFR-REL-006` | Estados terminais permanecem terminais. | [ver](PRODUCT-SPECIFICATION.md#nfr-rel-006) |
| `INV-013` | A Release 0.1 NÃO DEVE permitir selecionar algumas posições da preview e confirmar apenas essas posições. | [ver](PRODUCT-SPECIFICATION.md#inv-013--sem-commit-parcial-da-preview) |
| `FR-IDEMP-001` | O fingerprint da fonte DEVE sobreviver a restart. | [ver](PRODUCT-SPECIFICATION.md#fr-idemp-001--fingerprint-persistente) |
| `FR-IDEMP-002` | Repetir a mesma intenção de commit para: | [ver](PRODUCT-SPECIFICATION.md#fr-idemp-002--commit-idempotente) |
| `FR-IDEMP-003` | Se o commit já tiver sido concluído com sucesso, um retry semanticamente idêntico DEVERIA retornar o resultado já confirmado, ou um resultado equivalente que permita ao cliente reconhecer sucesso anterior. | [ver](PRODUCT-SPECIFICATION.md#fr-idemp-003--resposta-de-retry) |
| `FR-IDEMP-004` | A mesma chave idempotente NÃO DEVE aceitar payload semanticamente diferente. | [ver](PRODUCT-SPECIFICATION.md#fr-idemp-004--conflito-de-intenção) |
| `FR-IDEMP-005` | Mecanismo de idempotência crítico NÃO DEVE depender exclusivamente de cache em memória. | [ver](PRODUCT-SPECIFICATION.md#fr-idemp-005--idempotência-não-depende-de-memória) |
| `NFR-DATA-001` | Logs de rotina NÃO DEVEM conter C3 ou C4. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-001) |
| `NFR-DATA-002` | Auditoria NÃO DEVE conter C4. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-002) |
| `NFR-DATA-003` | Métricas NÃO DEVEM usar C2/C3/C4 como label. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-003) |
| `NFR-DATA-004` | Fixtures versionadas DEVEM ser C0/C1 sintéticas. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-004) |
| `NFR-DATA-005` | Dados efêmeros DEVEM possuir mecanismo de limpeza mesmo após falha. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-005) |
| `NFR-DATA-006` | O sistema DEVERIA permitir identificar artefatos temporários órfãos e removê-los de forma segura. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-006) |
| `FR-NORMALIZE-001` | Parser DEVE interpretar locale de acordo com o layout conhecido. | [ver](PRODUCT-SPECIFICATION.md#fr-normalize-001) |
| `FR-NORMALIZE-002` | O domínio NÃO DEVE receber string numérica ambígua. | [ver](PRODUCT-SPECIFICATION.md#fr-normalize-002) |
| `FR-NORMALIZE-003` | Separadores de milhar DEVEM ser removidos somente segundo regra do layout. | [ver](PRODUCT-SPECIFICATION.md#fr-normalize-003) |
| `FR-NORMALIZE-004` | Uma string ambígua NÃO DEVE ser “corrigida” por heurística permissiva. | [ver](PRODUCT-SPECIFICATION.md#fr-normalize-004) |
| `FR-NORMALIZE-005` | Datas impossíveis ou ambíguas DEVEM gerar erro/warning conforme criticidade. | [ver](PRODUCT-SPECIFICATION.md#fr-normalize-005) |
| `FR-NORMALIZE-006` | Representação canônica interna não depende do locale da máquina onde o backend está executando. | [ver](PRODUCT-SPECIFICATION.md#fr-normalize-006) |
| `FR-ASSET-001` | Ativos com identificador forte igual PODEM ser associados ao mesmo `Asset`. | [ver](PRODUCT-SPECIFICATION.md#fr-asset-001) |
| `FR-ASSET-002` | Nome textual sozinho NÃO DEVE ser tratado como identidade forte. | [ver](PRODUCT-SPECIFICATION.md#fr-asset-002) |
| `FR-ASSET-003` | Associação incerta DEVE preservar a incerteza. | [ver](PRODUCT-SPECIFICATION.md#fr-asset-003) |
| `FR-ASSET-004` | A Release 0.1 NÃO DEVE realizar merge automático de ativos com baixa confiança. | [ver](PRODUCT-SPECIFICATION.md#fr-asset-004) |
| `FR-ASSET-005` | Se a identidade for necessária para commit e não puder ser determinada com confiança suficiente, a preview DEVE possuir blocker. | [ver](PRODUCT-SPECIFICATION.md#fr-asset-005) |
| `FR-EVIDENCE-001` | Todo candidato relevante DEVERIA possuir referência para origem. | [ver](PRODUCT-SPECIFICATION.md#fr-evidence-001) |
| `FR-EVIDENCE-002` | Evidence NÃO DEVE exigir persistência do PDF bruto. | [ver](PRODUCT-SPECIFICATION.md#fr-evidence-002) |
| `FR-EVIDENCE-003` | Evidence DEVERIA permitir responder: | [ver](PRODUCT-SPECIFICATION.md#fr-evidence-003) |
| `FR-EVIDENCE-004` | Evidence sanitizada PODE conter hash de trecho normalizado. | [ver](PRODUCT-SPECIFICATION.md#fr-evidence-004) |
| `FR-EVIDENCE-005` | Evidence NÃO DEVE armazenar segredo. | [ver](PRODUCT-SPECIFICATION.md#fr-evidence-005) |
| `INV-014` | O digest auditado no commit DEVE corresponder exatamente ao conteúdo material confirmado. | [ver](PRODUCT-SPECIFICATION.md#inv-014--digest-corresponde-à-preview-commitada) |
| `FR-PREVIEW-007` | `commitAllowed` DEVE ser derivado de regras, e não fornecido livremente pelo cliente. | [ver](PRODUCT-SPECIFICATION.md#fr-preview-007) |
| `FR-PREVIEW-008` | Cliente NÃO PODE remover blocker apenas alterando payload. | [ver](PRODUCT-SPECIFICATION.md#fr-preview-008) |
| `FR-API-001` | Retryability NÃO DEVE ser inferida apenas pelo status HTTP. | [ver](PRODUCT-SPECIFICATION.md#fr-api-001) |
| `FR-API-002` | Sem sort explícito, snapshots DEVERIAM usar ordem estável documentada. | [ver](PRODUCT-SPECIFICATION.md#fr-api-002) |
| `FR-API-003` | Paginação NÃO DEVE produzir ordem arbitrária. | [ver](PRODUCT-SPECIFICATION.md#fr-api-003) |
| `FR-API-004` | Limite máximo de página DEVE ser configurado. | [ver](PRODUCT-SPECIFICATION.md#fr-api-004) |
| `FR-API-005` | Endpoints de detalhe DEVEM usar IDs tipados externamente representados de forma canônica. | [ver](PRODUCT-SPECIFICATION.md#fr-api-005) |
| `NFR-COMPAT-001` | Uma versão de parser usada em import confirmado DEVE permanecer identificável historicamente. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-001) |
| `NFR-COMPAT-002` | Atualizar parser NÃO DEVE reinterpretar silenciosamente snapshots já confirmados. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-002) |
| `NFR-COMPAT-003` | Reinterpretação histórica exige reprocessamento explícito. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-003) |
| `NFR-COMPAT-004` | Parser removido DEVERIA continuar representável em auditoria mesmo se o código não puder mais executá-lo. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-004) |
| `NFR-COMPAT-005` | Migration de banco deve ser forward-only por padrão. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-005) |
| `NFR-COMPAT-006` | Migration NÃO DEVE apagar histórico financeiro confirmado sem procedimento explícito de migração. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-006) |
| `NFR-COMPAT-007` | Mudança de representação monetária exige validação de equivalência. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-007) |
| `NFR-COMPAT-008` | Mudança de enum/status persistido deve possuir estratégia para valores antigos. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-008) |
| `NFR-COMPAT-009` | Backup anterior a migration crítica DEVERIA ser recomendado quando a operação local puder ser destrutiva. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-009) |
| `NFR-DATA-007` | Excluir arquivo temporário NÃO equivale a excluir import/auditoria. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-007) |
| `NFR-DATA-008` | Snapshot confirmado NÃO DEVE ser fisicamente apagado por uma operação comum de correção. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-008) |
| `NFR-DATA-009` | Uma futura exclusão definitiva de dados financeiros deve ser uma operação explicitamente destrutiva e documentada. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-009) |
| `NFR-TIME-001` | Instantes técnicos DEVEM ser armazenados como instantes absolutos. | [ver](PRODUCT-SPECIFICATION.md#nfr-time-001) |
| `NFR-TIME-002` | UTC é a referência técnica padrão. | [ver](PRODUCT-SPECIFICATION.md#nfr-time-002) |
| `NFR-TIME-003` | Datas financeiras `LocalDate` NÃO DEVEM sofrer conversão de timezone. | [ver](PRODUCT-SPECIFICATION.md#nfr-time-003) |
| `NFR-TIME-004` | Testes do domínio DEVEM poder controlar o clock. | [ver](PRODUCT-SPECIFICATION.md#nfr-time-004) |
| `NFR-TIME-005` | Timezone da máquina local NÃO DEVE alterar uma `PositionDate`. | [ver](PRODUCT-SPECIFICATION.md#nfr-time-005) |
| `NFR-PERF-001` | Timeouts DEVEM existir para operações de IO. | [ver](PRODUCT-SPECIFICATION.md#nfr-perf-001) |
| `NFR-PERF-002` | Uma importação lenta NÃO DEVE manter transação de commit aberta durante parsing. | [ver](PRODUCT-SPECIFICATION.md#nfr-perf-002) |
| `NFR-PERF-003` | Commit deve conter somente trabalho necessário à confirmação. | [ver](PRODUCT-SPECIFICATION.md#nfr-perf-003) |
| `NFR-OBS-001` | Cada request de import DEVERIA produzir correlation ID. | [ver](PRODUCT-SPECIFICATION.md#nfr-obs-001) |
| `NFR-OBS-002` | Lifecycle DEVERIA gerar log técnico por transição importante. | [ver](PRODUCT-SPECIFICATION.md#nfr-obs-002) |
| `NFR-OBS-003` | Logs NÃO DEVEM incluir valores financeiros por padrão. | [ver](PRODUCT-SPECIFICATION.md#nfr-obs-003) |
| `NFR-OBS-004` | Erro interno DEVE ser correlacionável sem expor stack trace ao cliente. | [ver](PRODUCT-SPECIFICATION.md#nfr-obs-004) |
| `NFR-OBS-005` | Métricas de cardinalidade alta NÃO DEVEM usar `importId` como label. | [ver](PRODUCT-SPECIFICATION.md#nfr-obs-005) |
| `NFR-OBS-006` | Health NÃO DEVE revelar credenciais ou detalhes sensíveis. | [ver](PRODUCT-SPECIFICATION.md#nfr-obs-006) |
| `NFR-SEC-010` | Filename fornecido pelo usuário NÃO DEVE ser usado como caminho. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-010) |
| `NFR-SEC-011` | Path traversal DEVE ser impossível. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-011) |
| `NFR-SEC-012` | MIME/type detection DEVE validar conteúdo além de extensão quando possível. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-012) |
| `NFR-SEC-013` | Falha de parsing de arquivo NÃO DEVE causar execução de conteúdo ativo. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-013) |
| `NFR-SEC-014` | Mensagens de biblioteca de PDF DEVEM ser sanitizadas antes de chegar à API quando puderem conter informação sensível. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-014) |
| `NFR-SEC-015` | Diretório temporário DEVERIA possuir permissões restritas ao processo/usuário da aplicação. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-015) |
| `FR-IMPORT-001` | `FAILED` NÃO DEVE voltar silenciosamente para `PARSING`. | [ver](PRODUCT-SPECIFICATION.md#fr-import-001) |
| `FR-IMPORT-002` | Reprocessamento DEVE registrar relação com o import anterior quando aplicável. | [ver](PRODUCT-SPECIFICATION.md#fr-import-002) |
| `FR-IMPORT-003` | Mudança de parser durante reprocessamento DEVE ficar auditável. | [ver](PRODUCT-SPECIFICATION.md#fr-import-003) |
| `FR-IMPORT-004` | Reprocessamento NÃO altera snapshot confirmado até novo commit. | [ver](PRODUCT-SPECIFICATION.md#fr-import-004) |
| `INV-015` | Estado terminal NÃO DEVE aceitar transição de lifecycle normal. | [ver](PRODUCT-SPECIFICATION.md#inv-015--terminalidade) |
| `NFR-DATA-010` | Restore DEVE executar migrations necessárias ou validar compatibilidade antes de liberar readiness. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-010) |
| `NFR-DATA-011` | Após restore, constraints e versão efetiva DEVEM continuar consistentes. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-011) |
| `NFR-CONFIG-001` | Segredo NÃO DEVE ser hardcoded. | [ver](PRODUCT-SPECIFICATION.md#nfr-config-001) |
| `NFR-CONFIG-002` | Configuração de produção/remota futura NÃO DEVE herdar credencial local insegura. | [ver](PRODUCT-SPECIFICATION.md#nfr-config-002) |
| `NFR-CONFIG-003` | Parâmetros de segurança devem possuir defaults conservadores. | [ver](PRODUCT-SPECIFICATION.md#nfr-config-003) |
| `NFR-CONFIG-004` | Configuração inválida crítica DEVE falhar no startup quando possível. | [ver](PRODUCT-SPECIFICATION.md#nfr-config-004) |
| `NFR-VERIFY-001` | Evidência de requisito crítico DEVE ser reproduzível. | [ver](PRODUCT-SPECIFICATION.md#nfr-verify-001) |
| `NFR-VERIFY-002` | Teste manual isolado NÃO DEVERIA ser a única evidência de um invariante de domínio. | [ver](PRODUCT-SPECIFICATION.md#nfr-verify-002) |
| `NFR-VERIFY-003` | Uma CI verde não prova automaticamente todos os requisitos; deve existir vínculo entre requisito e teste. | [ver](PRODUCT-SPECIFICATION.md#nfr-verify-003) |
| `NFR-VERIFY-004` | Requisitos sem teste automatizável DEVEM possuir justificativa e procedimento manual explícito. | [ver](PRODUCT-SPECIFICATION.md#nfr-verify-004) |
| `NFR-API-VERSION-001` | Mudança incompatível de contrato DEVE possuir estratégia de versionamento. | [ver](PRODUCT-SPECIFICATION.md#nfr-api-version-001) |
| `NFR-API-VERSION-002` | Campos novos opcionais PODEM ser adicionados sem nova versão quando compatíveis. | [ver](PRODUCT-SPECIFICATION.md#nfr-api-version-002) |
| `NFR-API-VERSION-003` | Remoção ou mudança semântica de campo NÃO DEVE ocorrer silenciosamente. | [ver](PRODUCT-SPECIFICATION.md#nfr-api-version-003) |
| `NFR-API-VERSION-004` | Códigos de erro estáveis NÃO DEVEM mudar de significado. | [ver](PRODUCT-SPECIFICATION.md#nfr-api-version-004) |
| `NFR-API-VERSION-005` | Clientes NÃO DEVEM depender de texto humano de erro para lógica. | [ver](PRODUCT-SPECIFICATION.md#nfr-api-version-005) |
| `INV-016` | Alterações futuras de escala, moeda ou arredondamento NÃO DEVEM reinterpretar valores históricos. | [ver](PRODUCT-SPECIFICATION.md#inv-016--representação-financeira-semanticamente-estável) |
| `NFR-COMPAT-010` | Mudança de escala persistida exige migration explícita. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-010) |
| `NFR-COMPAT-011` | Migration financeira DEVE possuir teste de equivalência antes/depois. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-011) |
| `NFR-COMPAT-012` | Mudança de regra de rounding NÃO DEVE alterar silenciosamente snapshots confirmados. | [ver](PRODUCT-SPECIFICATION.md#nfr-compat-012) |
| `FR-REPROCESS-001` | Reprocessamento DEVE gerar nova decisão/versionamento de preview. | [ver](PRODUCT-SPECIFICATION.md#fr-reprocess-001) |
| `FR-REPROCESS-002` | Reprocessamento NÃO DEVE alterar snapshot efetivo sem novo commit. | [ver](PRODUCT-SPECIFICATION.md#fr-reprocess-002) |
| `FR-REPROCESS-003` | O parser usado no reprocessamento DEVE ser registrado. | [ver](PRODUCT-SPECIFICATION.md#fr-reprocess-003) |
| `FR-REPROCESS-004` | A relação com o import anterior DEVERIA permanecer consultável. | [ver](PRODUCT-SPECIFICATION.md#fr-reprocess-004) |
| `FR-REPROCESS-005` | Uma melhoria de parser NÃO reescreve automaticamente histórico. | [ver](PRODUCT-SPECIFICATION.md#fr-reprocess-005) |
| `INV-017` | Uma mesma `parserVersion` NÃO DEVE mudar comportamento incompatível após ter produzido snapshot confirmado. | [ver](PRODUCT-SPECIFICATION.md#inv-017--parser-versionado-é-reproduzível) |
| `NFR-PARSER-DET-001` | Correção incompatível exige nova versão de parser. | [ver](PRODUCT-SPECIFICATION.md#nfr-parser-det-001) |
| `NFR-PARSER-DET-002` | O parser NÃO DEVE depender de hora atual para interpretar valores históricos, salvo regra explícita. | [ver](PRODUCT-SPECIFICATION.md#nfr-parser-det-002) |
| `NFR-PARSER-DET-003` | O parser NÃO DEVE depender de locale default da JVM. | [ver](PRODUCT-SPECIFICATION.md#nfr-parser-det-003) |
| `NFR-PARSER-DET-004` | O parser NÃO DEVE chamar serviço externo não versionado para decidir valor financeiro na Release 0.1. | [ver](PRODUCT-SPECIFICATION.md#nfr-parser-det-004) |
| `INV-018` | Mesmo conjunto de candidatos e mesma política de tolerância DEVEM produzir o mesmo resultado. | [ver](PRODUCT-SPECIFICATION.md#inv-018--reconciliação-reproduzível) |
| `NFR-RECON-DET-001` | Tolerância deve ser parametrizada por regra identificável. | [ver](PRODUCT-SPECIFICATION.md#nfr-recon-det-001) |
| `NFR-RECON-DET-002` | A regra usada DEVERIA ficar identificável na preview/auditoria. | [ver](PRODUCT-SPECIFICATION.md#nfr-recon-det-002) |
| `NFR-RECON-DET-003` | Mudança incompatível de reconciliação não deve reinterpretar snapshot confirmado. | [ver](PRODUCT-SPECIFICATION.md#nfr-recon-det-003) |
| `NFR-TX-001` | Parsing NÃO DEVE ocorrer dentro da transação de confirmação. | [ver](PRODUCT-SPECIFICATION.md#nfr-tx-001) |
| `NFR-TX-002` | Extração de PDF NÃO DEVE ocorrer dentro da transação de confirmação. | [ver](PRODUCT-SPECIFICATION.md#nfr-tx-002) |
| `NFR-TX-003` | A transação DEVE revalidar preview/status antes de escrever o estado confirmado. | [ver](PRODUCT-SPECIFICATION.md#nfr-tx-003) |
| `NFR-TX-004` | Constraints de banco DEVEM complementar, e não substituir, invariantes do domínio. | [ver](PRODUCT-SPECIFICATION.md#nfr-tx-004) |
| `NFR-TX-005` | Concorrência entre commits do mesmo contexto DEVE resultar em um único estado efetivo consistente. | [ver](PRODUCT-SPECIFICATION.md#nfr-tx-005) |
| `NFR-TX-006` | Falha de deadlock/lock transitório PODE ser retryable quando a intenção continuar válida. | [ver](PRODUCT-SPECIFICATION.md#nfr-tx-006) |
| `NFR-RETRY-001` | Retry NÃO DEVE depender de “esperar e torcer” para corrigir erro determinístico. | [ver](PRODUCT-SPECIFICATION.md#nfr-retry-001) |
| `NFR-RETRY-002` | Erro determinístico de layout/parser deve exigir mudança de entrada, parser ou decisão. | [ver](PRODUCT-SPECIFICATION.md#nfr-retry-002) |
| `NFR-RETRY-003` | Backoff é apropriado para indisponibilidade técnica transitória, não para blocker financeiro. | [ver](PRODUCT-SPECIFICATION.md#nfr-retry-003) |
| `FR-REJECT-004` | Repetir rejeição de import já rejeitado DEVERIA retornar estado consistente sem criar novo evento financeiro. | [ver](PRODUCT-SPECIFICATION.md#fr-reject-004) |
| `FR-REJECT-005` | Uma segunda rejeição idêntica PODE gerar log técnico, mas NÃO DEVE alterar semântica do import. | [ver](PRODUCT-SPECIFICATION.md#fr-reject-005) |
| `INV-019` | Um evento de auditoria gravado NÃO DEVE ser alterado para “corrigir” histórico. | [ver](PRODUCT-SPECIFICATION.md#inv-019--evento-de-auditoria-não-é-editável) |
| `NFR-AUDIT-001` | Audit event DEVE possuir identidade única. | [ver](PRODUCT-SPECIFICATION.md#nfr-audit-001) |
| `NFR-AUDIT-002` | Audit event DEVE possuir timestamp técnico. | [ver](PRODUCT-SPECIFICATION.md#nfr-audit-002) |
| `NFR-AUDIT-003` | A ordem total entre eventos simultâneos NÃO DEVE depender apenas de timestamp com baixa precisão. | [ver](PRODUCT-SPECIFICATION.md#nfr-audit-003) |
| `NFR-AUDIT-004` | Auditoria DEVERIA permitir reconstruir as principais decisões do lifecycle. | [ver](PRODUCT-SPECIFICATION.md#nfr-audit-004) |
| `NFR-SEC-016` | Redaction NÃO DEVE depender exclusivamente de disciplina manual do desenvolvedor. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-016) |
| `NFR-SEC-017` | Testes DEVERIAM verificar que secrets comuns não aparecem em logs. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-017) |
| `NFR-SEC-018` | Mensagem sanitizada pode preservar correlation ID e error code. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-018) |
| `NFR-SER-001` | API NÃO DEVE serializar valores financeiros via floating point. | [ver](PRODUCT-SPECIFICATION.md#nfr-ser-001) |
| `NFR-SER-002` | A documentação DEVE indicar unidade e convenção. | [ver](PRODUCT-SPECIFICATION.md#nfr-ser-002) |
| `NFR-SER-003` | Datas financeiras devem usar ISO-8601. | [ver](PRODUCT-SPECIFICATION.md#nfr-ser-003) |
| `NFR-SER-004` | Instantes técnicos devem incluir timezone/offset inequívoco. | [ver](PRODUCT-SPECIFICATION.md#nfr-ser-004) |
| `INV-020` | Quando a qualidade do valor for relevante, `null` sozinho NÃO DEVE substituir `Unknown/Estimated/Exact`. | [ver](PRODUCT-SPECIFICATION.md#inv-020--null-não-substitui-qualidade-sem-regra-explícita) |
| `NFR-SER-005` | API deve diferenciar, quando necessário: | [ver](PRODUCT-SPECIFICATION.md#nfr-ser-005) |
| `NFR-SER-006` | Essa distinção DEVE ser documentada no contrato da API. | [ver](PRODUCT-SPECIFICATION.md#nfr-ser-006) |
| `NFR-DATA-012` | Uma futura função de export DEVERIA permitir recuperar estado financeiro confirmado em formato documentado. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-012) |
| `NFR-DATA-013` | Lock-in de formato proprietário não deve ser objetivo do projeto. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-013) |
| `NFR-DERIVED-001` | Dado derivado DEVERIA apontar para versão dos snapshots de origem. | [ver](PRODUCT-SPECIFICATION.md#nfr-derived-001) |
| `NFR-DERIVED-002` | Recalcular analytics NÃO DEVE alterar snapshot histórico. | [ver](PRODUCT-SPECIFICATION.md#nfr-derived-002) |
| `NFR-DERIVED-003` | Mudança de algoritmo analítico deve ser distinguível de mudança dos dados de origem. | [ver](PRODUCT-SPECIFICATION.md#nfr-derived-003) |
| `NFR-OPS-001` | Um desenvolvedor novo DEVERIA conseguir iniciar ambiente local seguindo documentação versionada. | [ver](PRODUCT-SPECIFICATION.md#nfr-ops-001) |
| `NFR-OPS-002` | Comandos destrutivos DEVEM exigir confirmação explícita. | [ver](PRODUCT-SPECIFICATION.md#nfr-ops-002) |
| `NFR-OPS-003` | Comandos destrutivos locais NÃO DEVEM aceitar destino remoto arbitrário por default. | [ver](PRODUCT-SPECIFICATION.md#nfr-ops-003) |
| `NFR-OPS-004` | Runbook deve diferenciar: | [ver](PRODUCT-SPECIFICATION.md#nfr-ops-004) |
| `NFR-SOURCE-001` | `EXPERIMENTAL` NÃO DEVE ser apresentado como suporte completo. | [ver](PRODUCT-SPECIFICATION.md#nfr-source-001) |
| `NFR-SOURCE-002` | Depreciação NÃO deve apagar histórico de qual parser foi usado. | [ver](PRODUCT-SPECIFICATION.md#nfr-source-002) |
| `NFR-SOURCE-003` | Parser deprecated PODE continuar disponível para reprodução limitada. | [ver](PRODUCT-SPECIFICATION.md#nfr-source-003) |
| `NFR-SOURCE-004` | Remoção de execução deve preservar metadados históricos. | [ver](PRODUCT-SPECIFICATION.md#nfr-source-004) |
| `FR-ERROR-001` | Erro inesperado deve gerar `PF_INTERNAL_ERROR` ou equivalente estável. | [ver](PRODUCT-SPECIFICATION.md#fr-error-001) |
| `FR-ERROR-002` | Cliente NÃO deve receber detalhe técnico sensível. | [ver](PRODUCT-SPECIFICATION.md#fr-error-002) |
| `FR-ERROR-003` | Correlation ID deve permitir investigação. | [ver](PRODUCT-SPECIFICATION.md#fr-error-003) |
| `FR-ERROR-004` | Import deve terminar em estado consistente. | [ver](PRODUCT-SPECIFICATION.md#fr-error-004) |
| `FR-ERROR-005` | Erro inesperado durante parsing NÃO deve produzir snapshot parcial. | [ver](PRODUCT-SPECIFICATION.md#fr-error-005) |
| `NFR-API-006` | Enums de status públicos DEVERIAM ser documentados. | [ver](PRODUCT-SPECIFICATION.md#nfr-api-006) |
| `NFR-API-007` | Status persistidos e status de API podem diferir somente com mapping explícito. | [ver](PRODUCT-SPECIFICATION.md#nfr-api-007) |
| `NFR-DB-008` | FKs DEVEM ser usadas quando representarem invariantes relacionais estáveis. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-008) |
| `NFR-DB-009` | Não se deve depender apenas de convenção da aplicação para relações críticas. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-009) |
| `NFR-DB-010` | Deletes em cascata de dados financeiros históricos DEVEM ser evitados salvo decisão explícita. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-010) |
| `NFR-DB-011` | Índice não deve ser adicionado apenas por especulação. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-011) |
| `NFR-DB-012` | Constraint única que protege invariante PODE justificar índice desde o início. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-012) |
| `NFR-SPEC-001` | ID normativo já publicado NÃO DEVE ser reutilizado com novo significado. | [ver](PRODUCT-SPECIFICATION.md#nfr-spec-001) |
| `NFR-SPEC-002` | Contradição identificada DEVE ser resolvida antes de implementar a regra afetada. | [ver](PRODUCT-SPECIFICATION.md#nfr-spec-002) |
| `INV-021` | Um import NÃO DEVE possuir simultaneamente duas previews consideradas atuais. | [ver](PRODUCT-SPECIFICATION.md#inv-021--uma-preview-atual-por-import) |
| `INV-022` | Uma `Position` confirmada DEVE pertencer exatamente a uma versão de snapshot. | [ver](PRODUCT-SPECIFICATION.md#inv-022--posição-pertence-a-uma-versão) |
| `FR-PREVIEW-009` | A preview DEVERIA ser persistida como representação durável suficiente para: | [ver](PRODUCT-SPECIFICATION.md#fr-preview-009) |
| `FR-PREVIEW-010` | Se a implementação optar por reconstruir preview em vez de persistir integralmente, deve provar que: | [ver](PRODUCT-SPECIFICATION.md#fr-preview-010) |
| `INV-023` | Um candidato DEVE possuir identidade estável dentro da mesma `previewVersion`. | [ver](PRODUCT-SPECIFICATION.md#inv-023--candidateid-estável-dentro-da-preview) |
| `FR-PREVIEW-011` | Uma resolução que altera materialmente a preview DEVE gerar nova `previewVersion`. | [ver](PRODUCT-SPECIFICATION.md#fr-preview-011) |
| `FR-PREVIEW-012` | Uma resolução NÃO DEVE editar silenciosamente preview já apresentada ao usuário. | [ver](PRODUCT-SPECIFICATION.md#fr-preview-012) |
| `NFR-SOURCE-005` | O manifesto DEVE ser consistente com parsers realmente registrados. | [ver](PRODUCT-SPECIFICATION.md#nfr-source-005) |
| `NFR-SOURCE-006` | CI DEVERIA falhar se um parser marcado `SUPPORTED` não possuir golden fixture obrigatória. | [ver](PRODUCT-SPECIFICATION.md#nfr-source-006) |
| `INV-024` | Parser NÃO DEVE produzir efeito externo sobre estado financeiro confirmado. | [ver](PRODUCT-SPECIFICATION.md#inv-024--parser-é-side-effect-free-no-domínio-confirmado) |
| `FR-PDF-008` | A ordem das páginas DEVE ser preservada. | [ver](PRODUCT-SPECIFICATION.md#fr-pdf-008) |
| `FR-PDF-009` | `pageNumber` DEVE ser estável e iniciar em convenção documentada. | [ver](PRODUCT-SPECIFICATION.md#fr-pdf-009) |
| `NFR-DATA-014` | Nome original NÃO DEVE ser necessário para idempotência. | [ver](PRODUCT-SPECIFICATION.md#nfr-data-014) |
| `FR-ACCOUNT-001` | Referência externa NÃO DEVE ser usada diretamente como ID interno. | [ver](PRODUCT-SPECIFICATION.md#fr-account-001) |
| `FR-ACCOUNT-002` | Dados potencialmente sensíveis da referência externa DEVEM seguir classificação C2/C3. | [ver](PRODUCT-SPECIFICATION.md#fr-account-002) |
| `FR-ACCOUNT-003` | Máscara visual NÃO deve ser confundida com identidade forte. | [ver](PRODUCT-SPECIFICATION.md#fr-account-003) |
| `FR-ASSET-006` | O parser PODE produzir `AssetCandidate` sem `AssetId` quando identidade ainda não estiver resolvida. | [ver](PRODUCT-SPECIFICATION.md#fr-asset-006) |
| `FR-ASSET-007` | A atribuição de `AssetId` deve ocorrer antes do commit quando necessária ao modelo confirmado. | [ver](PRODUCT-SPECIFICATION.md#fr-asset-007) |
| `FR-ASSET-008` | Tipo desconhecido NÃO DEVE ser forçado a categoria incorreta. | [ver](PRODUCT-SPECIFICATION.md#fr-asset-008) |
| `INV-025` | Uma posição confirmada NÃO DEVE ser movida para outro snapshot. | [ver](PRODUCT-SPECIFICATION.md#inv-025--posição-confirmada-não-muda-de-snapshot) |
| `FR-RECON-007` | O nível da reconciliação DEVE ser identificado. | [ver](PRODUCT-SPECIFICATION.md#fr-recon-007) |
| `FR-RECON-008` | Um total global não deve mascarar mismatch interno de seção quando o relatório fornecer subtotais verificáveis. | [ver](PRODUCT-SPECIFICATION.md#fr-recon-008) |
| `FR-RECON-009` | Quando a fonte não fornecer total comparável, o resultado DEVE indicar ausência de evidência de reconciliação, não `PASS` automático. | [ver](PRODUCT-SPECIFICATION.md#fr-recon-009) |
| `INV-026` | `NOT_VERIFIABLE` NÃO DEVE ser tratado como `PASS`. | [ver](PRODUCT-SPECIFICATION.md#inv-026--não-verificável-não-é-aprovado) |
| `FR-ERROR-006` | Severidade e código são conceitos separados. | [ver](PRODUCT-SPECIFICATION.md#fr-error-006) |
| `NFR-EVENT-001` | Evento interno não implica Kafka. | [ver](PRODUCT-SPECIFICATION.md#nfr-event-001) |
| `NFR-EVENT-002` | Eventos síncronos em memória PODEM ser usados para desacoplamento modular. | [ver](PRODUCT-SPECIFICATION.md#nfr-event-002) |
| `NFR-EVENT-003` | Evento necessário à durabilidade futura deve considerar outbox quando houver consumidor assíncrono real. | [ver](PRODUCT-SPECIFICATION.md#nfr-event-003) |
| `INV-027` | Uma query NÃO DEVE alterar estado financeiro confirmado. | [ver](PRODUCT-SPECIFICATION.md#inv-027--query-sem-efeito-financeiro) |
| `NFR-API-008` | Correlation ID fornecido pelo cliente deve ser validado/sanitizado antes de logging. | [ver](PRODUCT-SPECIFICATION.md#nfr-api-008) |
| `NFR-API-009` | Upload DEVE possuir uma única semântica documentada para arquivo + metadata. | [ver](PRODUCT-SPECIFICATION.md#nfr-api-009) |
| `NFR-API-010` | Senha NÃO DEVERIA ser enviada como query parameter. | [ver](PRODUCT-SPECIFICATION.md#nfr-api-010) |
| `FR-API-008` | Filtros inválidos DEVEM falhar explicitamente. | [ver](PRODUCT-SPECIFICATION.md#fr-api-008) |
| `FR-API-009` | Paginação não deve omitir ou duplicar itens por ordem instável. | [ver](PRODUCT-SPECIFICATION.md#fr-api-009) |
| `INV-028` | Uma resposta de portfolio atual NÃO DEVE combinar duas versões concorrentes da mesma chave lógica. | [ver](PRODUCT-SPECIFICATION.md#inv-028--current-não-mistura-versões) |
| `INV-029` | Consultar snapshot antigo NÃO DEVE recalculá-lo com regra atual de parser. | [ver](PRODUCT-SPECIFICATION.md#inv-029--leitura-histórica-é-imutável) |
| `NFR-ERROR-001` | RetryClass deve refletir semântica, não apenas infraestrutura. | [ver](PRODUCT-SPECIFICATION.md#nfr-error-001) |
| `NFR-SEC-019` | A aplicação DEVE minimizar tempo de vida e escopo da senha em memória. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-019) |
| `NFR-SEC-020` | A aplicação NÃO DEVE copiar senha para estruturas desnecessárias. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-020) |
| `NFR-SEC-021` | A aplicação NÃO DEVE prometer zeroização física garantida da heap JVM quando isso não puder ser provado. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-021) |
| `NFR-SEC-022` | Dependências que processam PDF, rede ou banco DEVERIAM receber atualização regular. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-022) |
| `NFR-SEC-023` | CI DEVERIA possuir verificação de dependências vulneráveis quando o projeto amadurecer. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-023) |
| `NFR-SEC-024` | Atualização de dependência crítica deve rodar golden fixtures e testes de segurança. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-024) |
| `NFR-SEC-025` | Gradle Wrapper DEVE permanecer validado. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-025) |
| `NFR-SEC-026` | Actions de CI DEVERIAM usar versões estáveis/pin quando adequado. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-026) |
| `NFR-SEC-027` | Credenciais de CI NÃO DEVEM ser disponibilizadas a PRs não confiáveis. | [ver](PRODUCT-SPECIFICATION.md#nfr-sec-027) |
| `NFR-DB-013` | Migration destrutiva futura DEVE possuir plano de recuperação. | [ver](PRODUCT-SPECIFICATION.md#nfr-db-013) |
| `INV-030` | Migration NÃO DEVE converter ausência histórica em valor financeiro inventado. | [ver](PRODUCT-SPECIFICATION.md#inv-030--migration-não-inventa-valor) |
| `NFR-RELEASE-001` | Release manifest NÃO contém PII. | [ver](PRODUCT-SPECIFICATION.md#nfr-release-001) |
| `NFR-E2E-001` | Golden flow DEVE ser reproduzível localmente e na CI quando tecnicamente viável. | [ver](PRODUCT-SPECIFICATION.md#nfr-e2e-001) |
| `NFR-SOURCE-007` | Limitações conhecidas DEVEM ser documentadas. | [ver](PRODUCT-SPECIFICATION.md#nfr-source-007) |
| `FR-PARSER-007` | Campo reconhecido mas não utilizado DEVERIA ser documentado. | [ver](PRODUCT-SPECIFICATION.md#fr-parser-007) |
| `FR-PARSER-008` | Campo crítico desconhecido NÃO DEVE ser ignorado silenciosamente. | [ver](PRODUCT-SPECIFICATION.md#fr-parser-008) |
| `NFR-AI-001` | Resultado financeiro confirmado NÃO DEVE depender de uma chamada LLM externa. | [ver](PRODUCT-SPECIFICATION.md#nfr-ai-001) |
| `NFR-AI-002` | Relatório financeiro privado NÃO DEVE ser enviado a LLM externo pelo runtime. | [ver](PRODUCT-SPECIFICATION.md#nfr-ai-002) |
| `NFR-AI-003` | Código produzido com auxílio de IA segue os mesmos gates. | [ver](PRODUCT-SPECIFICATION.md#nfr-ai-003) |
| `NFR-PORT-001` | Abstração não deve existir apenas para trocar tecnologia hipoteticamente. | [ver](PRODUCT-SPECIFICATION.md#nfr-port-001) |
| `INV-031` | `shared` não referencia Spring. | [ver](PRODUCT-SPECIFICATION.md#inv-031) |
| `INV-032` | `portfolio` não referencia controller HTTP. | [ver](PRODUCT-SPECIFICATION.md#inv-032) |
| `INV-033` | `ingestion` não referencia implementação JDBC concreta. | [ver](PRODUCT-SPECIFICATION.md#inv-033) |
| `INV-034` | `api` não executa SQL. | [ver](PRODUCT-SPECIFICATION.md#inv-034) |
| `INV-035` | `persistence` não define regra financeira que pertence ao domínio. | [ver](PRODUCT-SPECIFICATION.md#inv-035) |
