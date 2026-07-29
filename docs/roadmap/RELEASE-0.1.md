# Release 0.1 — posição patrimonial local

## Objetivo

Entregar o menor corte vertical capaz de importar com segurança um extrato consolidado de posição do Banco Inter, reconciliar os valores, exigir revisão humana e disponibilizar um snapshot patrimonial consultável localmente.

## Resultado demonstrável

```text
PDF protegido
  -> upload
  -> senha efêmera
  -> extração textual
  -> detecção do layout
  -> parsing por seção
  -> normalização BRL/USD
  -> reconciliação
  -> preview
  -> commit
  -> snapshot efetivo
  -> GET de resumo patrimonial
```

## Incluído

### Fundação

- monorepo e build backend Kotlin DSL;
- JDK 25 e versões centralizadas;
- Spring Boot e Spring Modulith;
- PostgreSQL, Flyway e Testcontainers;
- convenções Kotlin, Ktlint e Detekt;
- correlation ID e auditoria básica;
- Docker Compose local;
- CI de documentação, backend e contratos quando existirem.

### Domínio

- `Money` em minor units e moeda explícita;
- quantidades e taxas em `BigDecimal`;
- `ImportBatch`, `SourceDocument`, `Asset`, `InvestmentAccount` e `PositionSnapshot`;
- temporalidade separada para posição, geração e referência de mercado;
- evidência, qualidade e confiança;
- versão efetiva de snapshot sem apagar histórico.

### Ingestão

- upload PDF até 15 MB;
- senha efêmera por uso único;
- extração PDFBox sem OCR;
- hash bruto e fingerprint semântico;
- detector `INTER_POSITION_CONSOLIDATED`;
- parsers para resumo, Tesouro, bolsa nacional, renda fixa, internacional e fundos;
- normalização pt-BR, BRL e USD;
- resolução de ativo com revisão para ambiguidades;
- reconciliação por categoria e total;
- preview versionado;
- commit atômico, idempotente e auditado;
- retenção temporária e purge.

### API local mínima

- criar importação;
- consultar estado;
- fornecer senha efêmera;
- consultar preview;
- confirmar ou rejeitar;
- consultar resumo e posições da fotografia efetiva;
- Problem Details e `Cache-Control: no-store`.

## Fora da Release 0.1

- extrato de movimentações;
- notas de renda fixa;
- aportes e retiradas;
- rendimento estimado;
- liquidez avançada e vencimentos;
- concentração;
- projeções e simulação do imóvel;
- OIDC remoto;
- MCP;
- Android;
- Open Finance;
- KMP e iOS;
- OCR generalista;
- Kafka, Redis, Kubernetes e microserviços.

## Ordem de execução

1. Bootstrap Kotlin/JVM e estrutura do monorepo.
2. PostgreSQL, Flyway, Testcontainers e Docker Compose.
3. Primitivos financeiros e temporalidade.
4. Módulos, portas, auditoria e regras arquiteturais.
5. ImportBatch e catálogo de erros.
6. Upload, senha e extração segura.
7. Idempotência e parser registry.
8. Fixtures sintéticas e golden files.
9. Parsers por seção e normalização.
10. Ativos, resolução e persistência.
11. Reconciliação e preview.
12. Commit, snapshot efetivo e purge.
13. API local mínima e contrato OpenAPI.
14. E2E, testes de segurança, backup e restore.
15. Tag `v0.1.0` somente após o gate completo.

## Definition of Done

### Funcional

- fixture protegida completa chega a `PREVIEW_READY`;
- total calculado reconcilia com o declarado por moeda;
- divergência acima da tolerância bloqueia commit;
- commit gera fotografia efetiva consultável;
- reimportação idêntica ou semanticamente equivalente não duplica dados;
- correção cria nova versão sem apagar a anterior;
- dados desconhecidos não aparecem como zero.

### Segurança

- senha não aparece em logs, traces, métricas, banco ou respostas;
- cópia descriptografada é removida em sucesso e falha;
- PDF inválido, disfarçado, grande ou com conteúdo ativo é rejeitado;
- APIs financeiras usam `no-store`;
- aplicação local faz bind somente em loopback;
- nenhum dado real ou PII existe no repositório.

### Qualidade

- unitários, integração e E2E verdes;
- Testcontainers valida PostgreSQL e Flyway;
- golden files cobrem todas as seções observadas;
- property tests cobrem dinheiro, idempotência e reconciliação;
- Ktlint, Detekt, secret scan e dependency scan verdes;
- OpenAPI lintado e exemplos fictícios;
- módulos verificados por Spring Modulith ou ArchUnit.

### Operação

- ambiente local sobe com um comando documentado;
- backup e restore mínimos testados;
- purge de arquivos é verificável;
- logs estruturados possuem correlation ID e não possuem valores;
- limitações conhecidas estão no README e release notes.

## Critérios de não aceitação

A release não será aprovada se:

- usar PDF real como fixture pública;
- exigir OCR para o caminho principal;
- misturar datas financeiras;
- somar moedas sem política explícita;
- confirmar automaticamente sem preview;
- atualizar posição existente silenciosamente;
- depender de Android, MCP, Open Finance ou serviço pago;
- expor o backend fora de loopback por padrão.

## Issues de fundação

As issues deverão ser pequenas, com dependências explícitas, critérios verificáveis e uma única responsabilidade. Um tracking issue da Release 0.1 manterá a ordem e os gates; código só começa após a fundação documental ser mergeada.
