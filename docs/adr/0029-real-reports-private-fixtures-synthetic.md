# ADR 0029 — Relatórios reais privados e fixtures sintéticas
Status: Accepted
Date: 2026-07-29

## Context

A estrutura real dos relatórios é necessária para construir parsers confiáveis, mas os documentos contêm PII e dados financeiros. Copiá-los para o repositório, CI ou ferramentas públicas seria incompatível com o modelo de privacidade.

## Decision

Relatórios reais podem ser inspecionados apenas em ambiente privado e temporário para descobrir estrutura, campos e variações. Todo artefato versionado deve ser sintético, com identidades, códigos, datas e valores inventados.

Golden files reproduzem formato e invariantes observados sem copiar texto identificável. Revisões e testes verificam ausência de PII, segredos e valores reais. Arquivos reais não são anexados a issues, PRs ou artifacts de CI.

## Consequences

- o parser mantém base empírica sem publicar dados pessoais;
- fixtures precisam ser geradas e revisadas cuidadosamente;
- diferenças entre fonte privada e fixture devem ser documentadas;
- reprodução de um caso real pode exigir criar um equivalente sintético;
- análise privada não substitui testes públicos.

## Alternatives considered

Anonimização manual de PDFs reais foi rejeitada porque metadados e conteúdo residual podem escapar. Fixtures inventadas sem observar estrutura real foram rejeitadas por baixa fidelidade. Uso de dados de produção em CI foi proibido.
