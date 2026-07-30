# ADR 0005 — Dinheiro em minor units
Status: Accepted
Date: 2026-07-29

## Context

Valores financeiros não podem depender de ponto flutuante binário nem perder a moeda durante cálculos, persistência ou serialização. A Release 0.1 manipula BRL e USD e precisa rejeitar soma implícita entre moedas.

## Decision

Dinheiro será representado por um tipo que combina `Long` em minor units e moeda explícita. Operações exigem moedas iguais, salvo quando recebem uma taxa de câmbio auditável e uma política de arredondamento.

Quantidades, preços unitários e taxas que não cabem em minor units usam `BigDecimal` com escala e arredondamento definidos pelo caso de uso. `Float` e `Double` são proibidos para valores financeiros.

## Consequences

- soma e comparação monetária tornam-se exatas dentro do limite de `Long`;
- contratos usam `{ amountMinor, currency }`;
- conversão de textos e decimais precisa validar escala;
- limites de overflow e arredondamento exigem testes;
- ativos com precisão incomum demandam políticas explícitas.

## Alternatives considered

`BigDecimal` para todo dinheiro foi rejeitado como representação canônica por permitir escalas inconsistentes. `Double` foi rejeitado por erro binário. Um valor sem moeda foi rejeitado por permitir operações economicamente inválidas.
