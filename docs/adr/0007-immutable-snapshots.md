# ADR 0007 — Snapshots imutáveis
Status: Accepted
Date: 2026-07-29

## Context

Uma posição consolidada representa o conhecimento disponível para uma conta e data em uma versão específica. Correções e reimportações precisam preservar o que foi confirmado anteriormente para auditoria e comparação.

## Decision

Linhas de posição confirmadas e seus metadados de evidência são imutáveis. Uma correção cria nova versão de snapshot; não atualiza nem apaga silenciosamente a versão anterior.

A consulta corrente usa um ponteiro explícito para a versão efetiva por conta e data. Exclusão física somente ocorre por política de retenção aplicável a artefatos brutos, nunca para reescrever histórico confirmado.

## Consequences

- histórico e decisões permanecem auditáveis;
- correções exigem nova versão e mudança explícita do ponteiro efetivo;
- armazenamento cresce com o histórico;
- consultas precisam escolher versão efetiva ou histórica de forma consciente;
- invariantes de unicidade e concorrência tornam-se essenciais.

## Alternatives considered

Atualização in-place foi rejeitada por apagar evidência histórica. Soft delete isolado foi rejeitado porque não expressa qual versão substitui qual. Event sourcing completo foi adiado por complexidade desnecessária para a primeira release.