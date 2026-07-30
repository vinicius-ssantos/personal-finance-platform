# Personal Finance Platform

Plataforma pessoal de finanças, de usuário único, para consolidar investimentos a partir de documentos reais do Banco Inter, preservar evidências, calcular indicadores determinísticos e apoiar metas financeiras com transparência sobre qualidade e incerteza.

> **Estado:** fundação técnica em implementação. O bootstrap Kotlin/JVM está em `main`; PostgreSQL, Flyway, Testcontainers e Compose estão sendo entregues pela issue #3. Ainda não existe comportamento financeiro nem dado real no repositório.

## Visão do produto

O produto será organizado em três superfícies independentes dentro de um único monorepo:

- **Backend Kotlin/JVM:** importação, parsing, reconciliação, persistência, analytics, metas, REST e integrações.
- **Aplicativo Android:** consulta, importação de documentos, acompanhamento de metas e alertas, sem duplicar regras financeiras oficiais.
- **MCP somente leitura:** consultas e simulações estruturadas para clientes de IA, sem documentos brutos, credenciais bancárias ou operações financeiras.

O backend é a fonte de verdade. Android e MCP são adaptadores externos não confiáveis.

## Primeiro corte implementável

A Release 0.1 será local e limitada a:

1. backend Kotlin/Spring Boot;
2. PostgreSQL, Flyway e Testcontainers;
3. upload seguro de PDF;
4. suporte a PDF protegido por senha efêmera;
5. detecção e parsing do extrato consolidado de posição do Banco Inter;
6. normalização de BRL e USD;
7. reconciliação por seção e total;
8. prévia antes de confirmação;
9. commit atômico e idempotente;
10. snapshot patrimonial efetivo;
11. API REST local mínima;
12. auditoria, retenção curta, backup e restore básicos.

Movimentações, rendimento, metas, MCP, Android, Open Finance e KMP permanecem fora desse primeiro corte.

## Princípios

- determinismo antes de IA;
- dado ausente nunca vira zero;
- importar não significa confirmar;
- histórico financeiro é imutável;
- evidência e confiança acompanham os campos relevantes;
- dinheiro não usa `float` ou `double`;
- documentos e clientes são entradas não confiáveis;
- menor privilégio e minimização de dados;
- monólito modular antes de microserviços;
- contratos públicos, não compartilhamento de entidades internas.

## Estrutura planejada

```text
personal-finance-platform/
├── backend/                 # Kotlin/JVM + Spring Boot
├── mobile/                  # Android Kotlin + Jetpack Compose
├── contracts/               # OpenAPI, schemas e exemplos fictícios
├── fixtures/                # PDFs e golden files sintéticos
├── docs/                    # produto, arquitetura, ADRs, segurança e runbooks
├── infra/                   # Compose, observabilidade e deployment
└── .github/                 # workflows, templates e governança
```

Backend e mobile terão builds Gradle independentes. O contrato entre eles será OpenAPI; o aplicativo não importará entidades, repositories ou classes Spring do backend.

## Stack planejada

- Kotlin/JVM 2.2+ e JDK 25;
- Spring Boot 4.1.x e Spring Modulith;
- Spring Data JDBC;
- PostgreSQL e Flyway;
- PDFBox;
- OpenAPI 3.1 e Problem Details;
- JUnit 5, AssertJ, MockK, Testcontainers, ArchUnit;
- Android Kotlin, Jetpack Compose e Material 3;
- Spring AI MCP Streamable HTTP, somente leitura, em fase posterior.

As versões definitivas serão travadas pelo version catalog e registradas em ADR quando o bootstrap técnico começar.

## Execução da fundação local

Com Docker, JDK 25 e `just` instalados:

```bash
just backend-run
```

O comando inicia o PostgreSQL somente em loopback, aguarda o health check e executa o backend com o perfil local. Para validar lint, análise estática, migrações e testes com PostgreSQL real:

```bash
just backend-check
```

Operação, credenciais locais e reset protegido estão documentados em [Infraestrutura local](infra/README.md).

## Privacidade

Este repositório é público, mas **não conterá**:

- PDFs reais;
- CPF, endereço, telefone, agência ou conta;
- saldos, posições ou movimentações pessoais;
- senha de documento;
- tokens bancários, de agregadores ou de autenticação;
- dumps de banco ou logs com dados financeiros.

Fixtures públicas serão sintéticas e semanticamente equivalentes aos layouts observados.

## Documentação

- [Produto e limites](docs/product/PRODUCT.md)
- [Arquitetura](docs/architecture/ARCHITECTURE.md)
- [Fundação de banco de dados](docs/architecture/DATABASE.md)
- [Pipeline de ingestão](docs/architecture/INGESTION.md)
- [Modelo de segurança](docs/security/SECURITY-MODEL.md)
- [Release 0.1](docs/roadmap/RELEASE-0.1.md)
- [Catálogo de ADRs](docs/adr/README.md)
- [Contribuição](CONTRIBUTING.md)
- [Política de segurança](SECURITY.md)
- [Política de privacidade do repositório](PRIVACY.md)

## Governança da implementação

A documentação de fundação e o bootstrap técnico já foram revisados e mergeados. A implementação segue uma issue atômica por PR, sem antecipar domínio financeiro, contratos, Android ou MCP antes das respectivas dependências e decisões arquiteturais.

## Licença

MIT. A licença cobre o código e a documentação pública, não concede direito de reutilizar dados financeiros pessoais ou documentos bancários reais.

## Aviso

A plataforma será uma ferramenta de consolidação, cálculo e apoio à decisão. Não substitui contador, advogado, planejador financeiro certificado nem análise oficial de instituição financeira.
