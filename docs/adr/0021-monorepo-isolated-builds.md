# ADR 0021 — Monorepo com builds isolados
Status: Accepted
Date: 2026-07-29

## Context

Backend, contratos, fixtures, infraestrutura e futuro Android pertencem ao mesmo produto, mas possuem toolchains, ciclos e artefatos diferentes. Um build único anteciparia acoplamento e aumentaria o custo de mudanças locais.

## Decision

O projeto usa um monorepo com diretórios explícitos e builds independentes. O backend possui Gradle Wrapper e version catalog próprios. O futuro mobile terá seu próprio build e não será subprojeto automático do backend.

Comandos na raiz apenas orquestram builds isolados. Contratos compartilhados são artefatos versionados ou especificações, não acesso direto a classes internas.

## Consequences

- descoberta e governança permanecem centralizadas;
- CI pode executar somente áreas afetadas;
- versões e caches de cada build são independentes;
- mudanças coordenadas ainda podem ocorrer em uma PR;
- duplicação pequena de configuração é aceita para preservar isolamento.

## Alternatives considered

Repositórios separados foram adiados porque aumentariam coordenação sem necessidade. Um multi-project Gradle único foi rejeitado por acoplar backend e mobile. Compartilhar entidades compiladas foi rejeitado por vazar frameworks e persistência.
