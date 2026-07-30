# ADR 0014 — Arquivo bruto efêmero
Status: Accepted
Date: 2026-07-29

## Context

Relatórios financeiros podem conter nome, conta e valores sensíveis. O arquivo é necessário durante ingestão e eventualmente por uma janela curta de recuperação, mas retenção indefinida aumenta impacto de vazamento.

## Decision

O arquivo bruto é armazenado somente em área temporária restrita, com nome aleatório, limites de tamanho e política de purge. Após o terminal definido pela política, o conteúdo é removido e permanecem apenas hashes, metadados mínimos e evidências permitidas.

Caminhos, conteúdo bruto e texto extraído não entram em logs, traces, métricas ou respostas. O código deve limpar artefatos em sucesso, falha e cancelamento.

## Consequences

- minimiza dados sensíveis em repouso;
- idempotência depende de hashes preservados;
- troubleshooting não pode contar com o arquivo indefinidamente;
- purge e ausência de resíduos precisam de testes;
- retenção futura para backup ou auditoria exige nova decisão e controles.

## Alternatives considered

Retenção permanente foi rejeitada por risco desnecessário. Não armazenar nem temporariamente foi rejeitado porque parsing protegido e retomada controlada exigem acesso breve ao arquivo. Object storage remoto foi adiado.