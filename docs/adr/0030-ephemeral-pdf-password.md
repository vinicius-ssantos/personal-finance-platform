# ADR 0030 — Senha de PDF efêmera
Status: Accepted
Date: 2026-07-29

## Context

Relatórios protegidos exigem senha para extração. A senha é um segredo de alto risco e não é necessária após a abertura do documento.

## Decision

A senha é recebida em uma continuação de uso único vinculada ao `importId`, com TTL curto e limite de tentativas. Ela permanece somente em memória durante a operação necessária e nunca é persistida.

Senha, corpo da requisição, erro da biblioteca e valores derivados não aparecem em logs, traces, métricas, auditoria ou respostas. Buffers e artefatos descriptografados são descartados no término possível, e o fluxo usa erros estáveis não reveladores.

## Consequences

- reduz o impacto de acesso ao banco ou logs;
- retomada após expiração exige nova submissão;
- rate limiting e contagem de tentativas são obrigatórios;
- testes precisam inspecionar diagnósticos e resíduos;
- não há recuperação de senha pelo sistema.

## Alternatives considered

Persistir senha criptografada foi rejeitado porque o sistema não precisa recuperá-la. Enviar senha junto ao upload foi rejeitado por dificultar continuação segura e redaction. Armazenar em cache distribuído foi adiado e continuaria exigindo controles equivalentes.
