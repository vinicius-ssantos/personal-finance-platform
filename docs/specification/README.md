# Especificação do Personal Finance Platform

Esta pasta contém a especificação canônica do produto e seus contratos derivados.

## Ordem de leitura

1. [`PRODUCT-SPECIFICATION.md`](PRODUCT-SPECIFICATION.md) — contrato canônico do produto (326 requisitos `FR-*`, `NFR-*` e `INV-*`).
2. [`REQUIREMENTS-INDEX.md`](REQUIREMENTS-INDEX.md) — índice operacional de requisitos, com link por âncora para cada requisito.
3. [`DOMAIN-MODEL.md`](DOMAIN-MODEL.md) — modelo conceitual do domínio.
4. [`IMPORT-SPECIFICATION.md`](IMPORT-SPECIFICATION.md) — fluxo de ingestão.
5. [`API-CONTRACT.md`](API-CONTRACT.md) — contrato conceitual REST.
6. [`ERROR-CATALOG.md`](ERROR-CATALOG.md) — códigos estáveis de erro.

Os gates e a Definition of Done da Release 0.1 **não** vivem aqui: são mantidos em
[`docs/roadmap/RELEASE-0.1.md`](../roadmap/RELEASE-0.1.md), documento único de release.

## Governança

Em caso de conflito, a precedência é:

```text
PRODUCT-SPECIFICATION
→ ADR aceito aplicável
→ documento de arquitetura
→ contrato derivado desta pasta
→ issue/PR
→ implementação atual
```

O código existente não redefine automaticamente a especificação. Quando a implementação
diverge, ou o código é corrigido, ou a especificação é alterada explicitamente por PR —
nunca por omissão.

A regra de precedência está formalizada em
[ADR 0045](../adr/0045-especificacao-canonica-como-fonte-normativa.md).

## Como referenciar requisitos

Issues e PRs devem citar os IDs que implementam ou alteram:

```text
Implementa FR-IMPORT-001..004, INV-015, NFR-AUDIT-001..004.
```

Formato dos IDs:

```text
FR-<AREA>-<NNN>        requisito funcional
NFR-<AREA>-<NNN>       requisito não funcional
INV-<NNN>              invariante global
```

Doze IDs usam área composta (`NFR-API-VERSION-*`, `NFR-PARSER-DET-*`, `NFR-RECON-DET-*`);
o padrão completo está em `requirementIdPattern` no [`SPEC-MANIFEST.json`](SPEC-MANIFEST.json).

## Alterando a especificação

- alteração normativa exige PR próprio, separado de implementação;
- requisito existente não muda de significado sob o mesmo ID;
- requisito descontinuado é marcado, não apagado;
- ao adicionar ou remover requisitos, regenere o índice e atualize `requirementsCount`.

O índice é derivado dos headings `### <ID>` da especificação canônica e usa âncoras
estáveis — nunca números de linha.
