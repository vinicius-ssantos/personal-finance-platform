# ADR 0006 — Importação com preview antes do commit
Status: Accepted
Date: 2026-07-29

## Context

Relatórios podem conter layout desconhecido, texto ambíguo, totais divergentes e ativos sem identidade suficiente. Alterar o patrimônio durante parsing transformaria erro de interpretação em estado confirmado.

## Decision

A ingestão possui duas fases. Extração, parsing, normalização e reconciliação produzem uma preview versionada sem escrever posições confirmadas. Somente uma decisão explícita sobre uma preview atual e apta pode executar o commit.

A preview expõe candidatos, evidências, totais, moedas, avisos, bloqueios e resoluções pendentes. Commit e rejeição usam `previewVersion` para detectar decisões obsoletas.

## Consequences

- o usuário revisa o resultado antes de afetar o portfólio;
- parsers permanecem sem acesso direto ao estado confirmado;
- é necessário persistir lifecycle, preview e decisões;
- concorrência e stale preview precisam de regras determinísticas;
- o fluxo tem mais etapas que uma importação automática.

## Alternatives considered

Commit direto foi rejeitado por risco de corrupção silenciosa. Uma transação mantida aberta durante revisão foi rejeitada por duração indeterminada e acoplamento operacional. Revisão apenas em logs foi rejeitada por não ser um contrato de decisão.
