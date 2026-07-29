# Infraestrutura local

A infraestrutura da Release 0.1 contém somente um PostgreSQL local e reproduzível. Não há deployment remoto, dados reais, serviços de terceiros, Redis, Kafka ou Kubernetes neste corte.

## Requisitos

- Docker Engine ou Docker Desktop com Docker Compose v2;
- `just` para os comandos abreviados, embora todos possam ser executados diretamente;
- JDK 25 para iniciar o backend.

## PostgreSQL local

O arquivo `infra/compose.yml` cria o projeto fixo `personal-finance-local` com:

- PostgreSQL `18.1-alpine`;
- porta publicada somente em `127.0.0.1:5432`;
- volume nomeado `personal-finance-local-postgres`;
- health check por `pg_isready`;
- usuário e senha exclusivos para desenvolvimento local;
- nenhum seed ou dado financeiro.

As credenciais locais são deliberadamente óbvias e não devem ser reutilizadas em nenhum ambiente remoto:

```text
database: personal_finance
username: personal_finance_local
password: personal_finance_local_only
```

Ambientes diferentes do perfil `local` devem fornecer suas próprias propriedades `spring.datasource.*` por um mecanismo externo de secrets.

## Comandos

A partir da raiz do repositório:

```bash
just local-up       # inicia o PostgreSQL e aguarda readiness
just local-status   # mostra o estado do serviço
just local-logs     # acompanha logs do PostgreSQL
just local-down     # encerra o serviço e preserva o volume
just backend-run    # inicia PostgreSQL e executa o backend no perfil local
just backend-test   # executa testes com PostgreSQL real via Testcontainers
just backend-check  # lint, análise estática, migrações e testes
```

Sem `just`:

```bash
docker compose --project-name personal-finance-local --file infra/compose.yml up --detach --wait postgres
cd backend
SPRING_PROFILES_ACTIVE=local bash ./gradlew bootRun
```

O backend fica em `127.0.0.1:8080`. A readiness de banco e migrações fica em `/actuator/health/readiness`.

## Reset protegido

O reset exclui somente o volume do projeto Compose local. O script:

- não aceita URL de banco nem argumentos;
- exige uma confirmação literal;
- recusa contextos Docker com endpoint TCP ou SSH;
- usa nomes fixos de projeto, arquivo e serviço;
- recria o PostgreSQL e aguarda o health check.

Execução:

```bash
CONFIRM_LOCAL_RESET=RESET_PERSONAL_FINANCE_LOCAL just local-reset
```

O comando destrói todo o conteúdo do banco local. Ele não é ferramenta de administração para bancos remotos.

## Migrações

Flyway é a única autoridade para criar ou alterar o schema `personal_finance`. A migração `V1__initialize_database_foundation.sql` cria apenas a fundação técnica e não contém tabelas de domínio nem dados de exemplo.

Migrações aplicadas nunca devem ser editadas. Uma mudança posterior exige um novo arquivo versionado em `backend/src/main/resources/db/migration/`.
