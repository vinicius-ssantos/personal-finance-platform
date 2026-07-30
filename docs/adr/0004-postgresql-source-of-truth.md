# ADR 0004 — PostgreSQL como fonte de verdade
Status: Accepted
Date: 2026-07-29

## Context

A plataforma precisa preservar histórico, idempotência, auditoria, versões efetivas e reconstrução confiável. Arquivos importados são fontes de evidência, mas não podem ser a única representação operacional do estado confirmado.

## Decision

PostgreSQL é a fonte de verdade do backend. Flyway é o único mecanismo de alteração de schema. Escritas confirmadas, ponteiros de versão efetiva, auditoria e jobs duráveis ficam no banco e respeitam transações explícitas.

O schema da aplicação é `personal_finance`. Geração automática de DDL pelo ORM, edição de migração aplicada e uso de banco em memória como prova de integração são proibidos.

## Consequences

- integridade relacional e concorrência podem ser verificadas no mesmo mecanismo usado localmente;
- migrações tornam mudanças reproduzíveis e auditáveis;
- testes de integração exigem PostgreSQL real via Testcontainers;
- backup, restore e retenção passam a ser responsabilidades operacionais;
- recursos específicos de PostgreSQL podem reduzir portabilidade.

## Alternatives considered

SQLite foi rejeitado por diferenças de concorrência, tipos e migrações. MongoDB foi rejeitado porque a consistência relacional e as versões efetivas são centrais. Arquivos JSON foram rejeitados por não oferecerem transações e controle de concorrência adequados.
