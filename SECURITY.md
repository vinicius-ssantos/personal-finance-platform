# Security Policy

## Supported versions

O projeto ainda está em fase de fundação. Enquanto não houver release estável, apenas a branch padrão e a release mais recente receberão correções de segurança.

## Reporting a vulnerability

Não abra issue pública contendo:

- credenciais, tokens ou segredos;
- dados financeiros ou PII;
- PDF bancário real;
- passo a passo de exploração contra uma instância ativa;
- dumps, logs ou screenshots sensíveis.

Use o canal privado de Security Advisories do GitHub do repositório. Inclua impacto, pré-condições, versão afetada, reprodução mínima sanitizada e sugestão de mitigação quando possível.

## Response targets

- confirmação inicial: até 7 dias;
- classificação e plano: até 14 dias;
- correção crítica: prioridade imediata, conforme capacidade do mantenedor;
- divulgação: somente após correção ou mitigação acordada.

Esses prazos são objetivos de um projeto pessoal, não SLA contratual.

## Scope

São relevantes:

- vazamento de senha de PDF, token ou dado financeiro;
- bypass de autenticação/autorização;
- acesso indevido a REST ou MCP;
- path traversal, SSRF, upload hostil ou execução de conteúdo ativo;
- retenção indevida de arquivo descriptografado;
- exposição em logs, traces, métricas, backup ou cache;
- adulteração silenciosa de cálculo, snapshot ou auditoria;
- vulnerabilidades de dependências com impacto real no projeto.

Fora de escopo:

- engenharia social contra o mantenedor;
- ataques de negação de serviço sem demonstração segura;
- achados apenas teóricos sem caminho de impacto;
- dados ou ambientes de terceiros não controlados pelo projeto.

## Safe harbor

Pesquisa de boa-fé, limitada à própria instalação ou ambiente explicitamente autorizado, será tratada de forma colaborativa. Não acesse dados de outras pessoas, não persista após confirmar o achado e não publique detalhes antes da correção.
