# ADR 0043 — Release 0.1 local-first
Status: Accepted
Date: 2026-07-29

## Context

A primeira release deve provar ingestão e snapshot com privacidade antes de introduzir autenticação remota, exposição pública ou operação em nuvem.

## Decision

A Release 0.1 executa localmente e liga o servidor somente em loopback. PostgreSQL do Compose publica porta apenas em `127.0.0.1`. MCP, OIDC remoto, tunnels, Open Finance e deployment público permanecem desabilitados.

Credenciais versionadas são exclusivamente locais e não são defaults para outro ambiente. Perfis não locais precisam receber configuração externa e passar por novo threat model antes de exposição.

## Consequences

- reduz superfície de ataque e custo operacional;
- o produto não pode ser acessado remotamente na primeira release;
- segurança de arquivos e host local continua necessária;
- testes devem provar binding, headers e ausência de MCP;
- evolução remota exige ADR e autenticação apropriada.

## Alternatives considered

Deployment público imediato foi rejeitado por ampliar riscos antes da validação do domínio. Túnel com senha simples foi rejeitado por fronteira insuficiente. OIDC foi adiado porque não é necessário para o corte local.