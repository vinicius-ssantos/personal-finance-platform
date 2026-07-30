# ADR 0031 — Temporalidade financeira explícita
Status: Accepted
Date: 2026-07-29

## Context

Um relatório pode possuir data da posição, instante de geração, data de mercado e período de movimentos. Colapsar esses conceitos em uma única `referenceDate` produz comparações e cálculos incorretos.

## Decision

Cada conceito temporal recebe nome e tipo próprios. Datas econômicas sem horário usam `LocalDate`; instantes técnicos e de auditoria usam `Instant` com clock injetável. Períodos têm início e fim explícitos.

`positionDate`, `generatedAt`, `marketReferenceDate` e datas de evidência não são intercambiáveis. Ausência de uma delas permanece desconhecida e segue ADR 0017.

## Consequences

- contratos e tabelas têm mais campos, porém sem ambiguidade;
- testes podem controlar o tempo técnico;
- ordenação e efetividade usam o conceito correto;
- fusos horários precisam ser tratados somente para instantes;
- parsers devem preservar a origem de cada data.

## Alternatives considered

Uma data genérica foi rejeitada por ambiguidade. `Instant` para tudo foi rejeitado porque datas financeiras frequentemente não representam um momento global. Usar relógio do sistema diretamente foi rejeitado por testes não determinísticos.
