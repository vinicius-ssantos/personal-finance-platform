# ADR 0003 — Monólito modular
Status: Accepted
Date: 2026-07-29

## Context

A plataforma terá ingestão, patrimônio, auditoria, API e persistência, mas a Release 0.1 é local, operada por uma pessoa e não possui requisitos de escala ou autonomia de equipes que justifiquem serviços distribuídos.

## Decision

O backend será um único deploy Spring Boot organizado como monólito modular. Os módulos iniciais são `shared`, `audit`, `portfolio`, `ingestion`, `persistence` e `api`.

Spring Modulith declara e verifica os módulos. ArchUnit reforça as proibições de domínio para adapters e mantém `shared` livre de frameworks. Subpacotes são internos por padrão; integrações entre módulos usam APIs de raiz, portas ou eventos explícitos.

## Consequences

- uma transação pode cobrir importação, auditoria e snapshot;
- operação, debugging e testes permanecem simples;
- fronteiras precisam ser verificadas para evitar um monólito acoplado;
- uma futura extração de serviço partirá de módulos já delimitados;
- não há independência de deploy ou banco entre módulos.

## Alternatives considered

Microserviços foram rejeitados por adicionarem rede, observabilidade distribuída, consistência eventual e custo operacional sem benefício atual. Um monólito sem fronteiras foi rejeitado por dificultar evolução e testes arquiteturais.