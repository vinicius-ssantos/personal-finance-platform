# ADR 0036 — Percentuais como razão decimal
Status: Accepted
Date: 2026-07-29

## Context

Percentuais podem ser representados como `30`, `0.30` ou texto `30%`. Misturar convenções provoca erros de cem vezes em taxas, concentração e projeções.

## Decision

O domínio e os contratos canônicos representam percentuais como razão decimal: `0.30` significa 30%. O tipo usa `BigDecimal`, escala controlada e validação de intervalo conforme o conceito.

Parsing de textos com `%` converte uma única vez na fronteira. Formatação para usuário multiplica por cem somente na apresentação. Nomes e exemplos devem explicitar a convenção.

## Consequences

- multiplicação em fórmulas torna-se direta;
- serialização precisa documentar razão decimal;
- entrada humana exige conversão e validação;
- políticas de escala e arredondamento são necessárias;
- valores acima de 100% continuam possíveis apenas quando o conceito permitir.

## Alternatives considered

Armazenar `30` foi rejeitado por aumentar risco em fórmulas. `Double` foi rejeitado por precisão. Aceitar ambas as convenções no domínio foi rejeitado por ambiguidade; compatibilidade de entrada deve ser resolvida na fronteira.
