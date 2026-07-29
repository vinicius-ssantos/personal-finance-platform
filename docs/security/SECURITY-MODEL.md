# Modelo de segurança e privacidade

## Objetivo

Proteger documentos, saldos, posições, movimentos, metas e credenciais técnicas sem transformar o projeto pessoal em uma infraestrutura desnecessariamente complexa.

## Classificação de dados

| Classe | Exemplos | Tratamento |
|---|---|---|
| Restrito | valores, posições, movimentos, metas | criptografia, autenticação, `no-store`, auditoria e minimização |
| Confidencial | nome de ativo, conta mascarada, documentos | acesso mínimo e retenção limitada |
| Interno | IDs opacos, métricas sem valor financeiro | logs controlados |
| Público | metodologia, schemas e fixtures sintéticas | pode entrar no repositório e em resources MCP |

## Regras absolutas do repositório público

Nunca commitar:

- PDF real ou texto extraído de documento real;
- CPF, endereço, telefone, agência ou conta completa;
- saldo, posição ou movimentação pessoal;
- senha de PDF;
- token, cookie, secret, chave privada ou credencial de provider;
- dump de banco, backup real ou log financeiro;
- screenshot com informação financeira identificável.

## Autenticação

### Perfil local da Release 0.1

- bind em `127.0.0.1`;
- acesso remoto desabilitado;
- token local aleatório ou mecanismo equivalente;
- MCP desabilitado;
- dados fictícios por padrão.

### Perfil remoto futuro

- TLS obrigatório;
- OAuth 2.1/OIDC;
- issuer, audience, subject e scopes validados;
- access token curto e revogável;
- mobile via Authorization Code + PKCE;
- audiences separadas para API móvel e MCP.

## Autorização planejada

```text
finance:read
finance:import
finance:simulate
finance:mcp
finance:admin
```

MCP recebe somente leitura e simulação. Nenhum escopo permite movimentar dinheiro.

## Upload e parsing

- assinatura mágica e MIME;
- limite de tamanho, páginas, CPU e tempo;
- diretório temporário isolado;
- nomes aleatórios;
- rejeição de conteúdo ativo e anexos quando detectáveis;
- scanner quando disponível;
- PDFBox executado sob limites;
- senha efêmera fora de logs, traces, métricas e persistência;
- exclusão imediata do descriptografado;
- arquivo recebido tratado como conteúdo hostil.

## Logs e observabilidade

Nunca registrar:

- valores individuais ou saldo total;
- PII;
- corpo do PDF ou texto extraído;
- senha, token ou payload integral de provider;
- resultado completo de tool MCP.

Registrar apenas:

- IDs opacos;
- tipo de operação;
- quantidade de registros;
- estado da importação;
- versão do parser;
- duração;
- código de erro;
- correlation ID.

Labels de métricas não contêm usuário, ativo ou valor financeiro.

## MCP e IA

- nenhum texto bruto de PDF é concatenado a prompts;
- LLM não participa do parser crítico;
- tools usam input limitado e output tipado;
- consultas SQL arbitrárias são proibidas;
- resources públicos contêm apenas metodologia e status sanitizado;
- prompt injection em descrição de ativo é tratada como dado, nunca instrução;
- respostas carregam data-base, confiança, qualidade e warnings.

## Android

- HTTPS e Network Security Config restritiva;
- tokens em storage apoiado pelo Android Keystore;
- biometria apenas como bloqueio local;
- cache somente leitura e minimizado;
- ocultação de saldos e tela recente;
- notificações sem valores por padrão;
- PDFs temporários removidos após uso;
- logs e crash reports sem dados financeiros;
- baseline OWASP MASVS.

## Retenção e exclusão

- conteúdo descriptografado: somente durante processamento;
- original criptografado: até 24 horas por padrão;
- retenção longa: opt-in, criptografada e documentada;
- hash e fingerprint: mantidos apenas para idempotência;
- purge verificável;
- exclusão de dados e revogação de sessões com runbook futuro.

## Backup

- PostgreSQL com backup diário apenas quando houver dados reais;
- backup criptografado;
- teste mensal de restore;
- segredos fora do backup de aplicação;
- arquivos já purgados não entram no backup;
- MCP permanece desabilitado durante restore e validação.

## Ameaças prioritárias

| Ameaça | Controle principal |
|---|---|
| Upload malicioso | validação, limites, isolamento e scanner |
| Vazamento de senha | segredo efêmero, redaction e testes automáticos |
| Parser incorreto | detecção versionada, golden files, prévia e reconciliação |
| Duplicação | hash, fingerprint e constraints |
| Exposição em logs | allowlist de campos e testes de não vazamento |
| Alteração silenciosa de cálculo | versão e resultado imutável |
| Acesso MCP indevido | OAuth, audience, scopes e auditoria |
| Cache antigo interpretado como atual | timestamps e frescor explícito |
| Dispositivo perdido | tokens curtos, revogação, biometria e limpeza |
| Webhook forjado futuro | assinatura, timestamp e replay protection |

## Gates de segurança da Release 0.1

- teste prova que senha não aparece em banco, logs, traces ou métricas;
- PDF descriptografado é removido após sucesso e falha;
- path traversal e PDF disfarçado são rejeitados;
- fixture pública não contém PII;
- API financeira usa `Cache-Control: no-store`;
- restore não expõe serviço remoto por padrão;
- secret scan e dependency scan passam na CI.
