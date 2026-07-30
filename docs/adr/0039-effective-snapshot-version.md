# ADR 0039 — Versão efetiva de snapshot
Status: Accepted
Date: 2026-07-29

## Context

Snapshots imutáveis preservam várias versões para a mesma conta e data. Consultas correntes precisam saber qual versão foi aceita sem inferir por maior ID, horário ou ordem de importação.

## Decision

A persistência mantém um ponteiro explícito para a versão efetiva de cada conta e data. Criar uma correção grava uma nova versão e altera o ponteiro na mesma transação de confirmação.

Somente uma versão pode ser efetiva por chave lógica. Consultas históricas selecionam versão deliberadamente; consultas correntes seguem o ponteiro. O histórico não é apagado quando a efetividade muda.

## Consequences

- o estado corrente é determinístico;
- correção e auditoria ficam separadas de ordenação técnica;
- concorrência exige constraint e locking apropriados;
- resumo nunca pode combinar versões da mesma chave;
- mudanças de efetividade precisam de evento de auditoria.

## Alternatives considered

Escolher a versão mais recente por timestamp foi rejeitado por races e reprocessamento. Marcar um booleano em cada snapshot foi rejeitado por maior risco de múltiplos verdadeiros sem ponteiro transacional. Sobrescrever foi rejeitado pelo ADR 0007.