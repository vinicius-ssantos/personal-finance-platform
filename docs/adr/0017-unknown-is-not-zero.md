# ADR 0017 — Dado desconhecido não é zero
Status: Accepted
Date: 2026-07-29

## Context

Relatórios podem omitir quantidade, preço, liquidez, taxa ou classificação. Substituir ausência por zero cria uma afirmação econômica falsa e contamina reconciliação, analytics e decisões futuras.

## Decision

Ausência, desconhecido, estimado, derivado, conflitante e exato são estados distintos. Valores opcionais permanecem ausentes ou são acompanhados de qualidade e evidência explícitas. Zero somente é aceito quando a fonte ou uma regra determinística comprova zero.

DTOs, domínio, persistência e contratos devem preservar essa distinção. Regras que exigem um valor podem bloquear o commit ou produzir aviso, mas não inventam um default silencioso.

## Consequences

- evita precisão fictícia;
- modelos e APIs precisam representar optionalidade e qualidade;
- consumidores tratam mais estados;
- reconciliação pode separar bloqueios de avisos;
- migrações não podem usar defaults que convertam ausência em zero.

## Alternatives considered

Zero como default foi rejeitado por semântica incorreta. Strings como `N/A` no domínio foram rejeitadas por perder tipagem. Um único booleano de confiança foi rejeitado por não distinguir origem, estimativa e conflito.
