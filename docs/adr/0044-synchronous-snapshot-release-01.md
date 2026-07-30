# ADR 0044 — Snapshot síncrono na Release 0.1
Status: Accepted
Date: 2026-07-29

## Context

Após confirmar uma preview, a Release 0.1 precisa disponibilizar imediatamente o snapshot efetivo. Introduzir fila e consistência eventual no primeiro corte aumentaria estados intermediários sem necessidade de escala comprovada.

## Decision

Commit da preview, posições imutáveis, evidências, auditoria e atualização do ponteiro efetivo acontecem sincronamente em uma única transação PostgreSQL.

A resposta de commit somente indica sucesso após a transação concluir. Efeitos assíncronos futuros usarão jobs duráveis e transactional outbox, mas não participam do caminho necessário para tornar o snapshot efetivo na Release 0.1.

## Consequences

- leitura após commit vê estado completo;
- falha causa rollback do conjunto confirmado;
- duração da transação precisa permanecer limitada;
- trabalho pesado deve ocorrer antes, durante preparação da preview;
- escala futura pode exigir separar efeitos não essenciais, sem mudar a atomicidade do núcleo.

## Alternatives considered

Atualização assíncrona foi rejeitada por exigir status intermediário e recuperação adicional. Fila em memória foi rejeitada por perda em restart. Transações distribuídas foram rejeitadas porque existe uma única fonte de verdade.
