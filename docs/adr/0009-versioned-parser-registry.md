# ADR 0009 — Parser registry versionado
Status: Accepted
Date: 2026-07-29

## Context

Instituições alteram layouts sem garantir compatibilidade. Um parser único com heurísticas permissivas pode interpretar um relatório novo como se fosse conhecido e produzir dados incorretos.

## Decision

Parsers são selecionados por um registry com instituição, família de documento, versão de layout e versão do parser. A seleção retorna evidências e confiança; layout desconhecido falha de forma segura.

O parser escolhido e sua versão são persistidos no import batch e na preview. Mudanças incompatíveis criam nova versão, mantendo a possibilidade de reproduzir ou explicar importações anteriores.

## Consequences

- regressões ficam isoladas por layout;
- reprocessamento pode usar a mesma versão ou uma atualização deliberada;
- o catálogo e as fixtures crescem com os layouts suportados;
- seleção e parsing precisam ser determinísticos;
- fallback por melhor palpite é proibido.

## Alternatives considered

Um parser universal foi rejeitado por fragilidade. Seleção manual sem evidência foi rejeitada por dificultar automação e auditoria. Atualizar o parser existente sem versão foi rejeitado por quebrar reprodutibilidade.
