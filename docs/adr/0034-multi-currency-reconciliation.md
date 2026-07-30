# ADR 0034 — Reconciliação multimoeda
Status: Accepted
Date: 2026-07-29

## Context

Relatórios consolidados podem listar posições em BRL e USD. Somar moedas ou comparar subtotais após conversão implícita esconde divergências e depende de uma taxa não evidenciada.

## Decision

Reconciliação ocorre separadamente por moeda, seção e total declarado. BRL e USD fecham de forma independente dentro de tolerância definida em minor units.

Conversão somente acontece em caso de uso explícito que receba taxa, fonte, data de referência e política de arredondamento. A Release 0.1 não usa conversão implícita para tornar uma preview commit-ready.

## Consequences

- mismatches permanecem localizados na moeda original;
- preview e contratos exibem totais por moeda;
- um patrimônio consolidado convertido exige política adicional;
- arredondamentos não podem mascarar divergência de origem;
- fixtures devem cobrir BRL e USD separadamente.

## Alternatives considered

Converter tudo para BRL durante parsing foi rejeitado por falta de evidência. Somar valores numéricos ignorando moeda foi proibido. Usar taxa atual para relatório histórico foi rejeitado por alterar o significado temporal.
