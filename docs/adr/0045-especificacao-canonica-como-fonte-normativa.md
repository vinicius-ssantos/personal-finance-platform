# ADR 0045 — Especificação canônica como fonte normativa
Status: Accepted
Date: 2026-09-04

## Context

A fundação documental original distribuiu regras de produto entre `docs/product/PRODUCT.md`, `docs/architecture/*` e `docs/roadmap/RELEASE-0.1.md`. Esses documentos descrevem intenção em prosa, sem identificadores estáveis, e não permitem afirmar que uma issue, PR ou teste cobre uma regra específica.

A especificação de produto v5 formaliza 326 requisitos e invariantes com IDs `FR-*`, `NFR-*` e `INV-*`. Ao ser versionada, ela passou a coexistir com documentos de arquitetura que já haviam fixado decisões equivalentes com outro vocabulário, notadamente a máquina de estados do `ImportBatch` e os códigos de erro do fluxo de senha.

Sem uma regra de precedência explícita, dois documentos igualmente mergeados em `main` se contradizem e quem abre uma issue não sabe qual seguir.

## Decision

`docs/specification/PRODUCT-SPECIFICATION.md` é a fonte normativa do produto.

A precedência em caso de conflito é:

```text
PRODUCT-SPECIFICATION
→ ADR aceito aplicável
→ documento de arquitetura
→ contrato derivado em docs/specification
→ issue/PR
→ implementação atual
```

Consequências operacionais da regra:

- documentos de arquitetura reproduzem a especificação, não competem com ela;
- alteração normativa exige PR próprio, separado de implementação;
- requisito não muda de significado sob o mesmo ID; requisito descontinuado é marcado, não apagado;
- issues e PRs de comportamento citam os IDs que implementam;
- implementação divergente é defeito de código ou pedido de alteração de spec, nunca uma terceira verdade tolerada;
- a Definition of Done da release permanece em `docs/roadmap/RELEASE-0.1.md`, documento único de release, subordinado à especificação.

Esta decisão não revoga nenhum ADR aceito. ADR aceito continua acima dos documentos de arquitetura e só é substituído por novo ADR.

## Consequences

- conflitos de vocabulário passam a ter resolução determinística;
- a cadeia requisito → issue → PR → teste fica verificável por ID;
- `docs/architecture/INGESTION.md` foi realinhada à especificação na adoção desta ADR;
- manter a especificação passa a ter custo real: toda mudança de comportamento exige tocá-la;
- o índice de requisitos precisa ser regenerado quando requisitos entram ou saem;
- documentos de arquitetura ficam parcialmente redundantes e podem divergir por descuido, o que exige revisão explícita em PR.

## Alternatives considered

Manter a especificação como documento consultivo, sem precedência, foi rejeitado: reproduz exatamente a ambiguidade que motivou a ADR.

Tratar os documentos de arquitetura como fonte e a especificação como derivada foi rejeitado porque a especificação é mais completa, tem IDs estáveis e seu texto normativo já referencia os próprios nomes de estado.

Fundir a especificação dentro de `docs/architecture` foi rejeitado por misturar contrato de produto com decisão técnica e dificultar o versionamento independente da spec.
