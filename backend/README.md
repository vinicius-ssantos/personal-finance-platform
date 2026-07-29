# Backend

Projeto Kotlin/JVM independente que concentra a fonte de verdade da Personal Finance Platform.

## Requisitos

- JDK 25;
- Docker Engine ou Docker Desktop com Docker Compose v2;
- acesso ao Maven Central na primeira execução;
- Gradle Wrapper versionado no diretório `backend/`.

## Execução local

A partir da raiz do monorepo:

```bash
just backend-run
```

Esse comando inicia o PostgreSQL local, aguarda o health check e executa o backend com `SPRING_PROFILES_ACTIVE=local`.

Sem `just`:

```bash
docker compose --project-name personal-finance-local --file infra/compose.yml up --detach --wait postgres
cd backend
SPRING_PROFILES_ACTIVE=local bash ./gradlew bootRun
```

No Windows, recomenda-se executar os comandos pelo WSL. Para rodar somente o Gradle pelo PowerShell:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "local"
.\gradlew.bat bootRun
```

A aplicação inicia em `127.0.0.1:8080`.

- health geral: `/actuator/health`;
- readiness de PostgreSQL e Flyway: `/actuator/health/readiness`.

## Verificação

```bash
just backend-check
just backend-test
```

Os testes iniciam um PostgreSQL `18.1-alpine` real via Testcontainers. Eles verificam:

- inicialização completa do contexto;
- conectividade JDBC;
- aplicação da migração em banco vazio;
- readiness condicionada à validade das migrações;
- falha fechada quando uma migração é inválida;
- reconstrução limpa do schema apenas com os arquivos Flyway.

## Perfis e credenciais

O perfil `local` usa somente `127.0.0.1:5432` e credenciais explícitas de desenvolvimento. Fora desse perfil, o projeto não fornece URL, usuário ou senha padrão: o ambiente deve injetar suas próprias propriedades `spring.datasource.*`.

O `clean` do Flyway permanece desabilitado na aplicação. Somente os testes contra containers efêmeros habilitam a operação para provar o rebuild completo.

## Migrações

Arquivos versionados ficam em:

```text
src/main/resources/db/migration/
```

Convenção:

```text
V<numero>__<descricao_em_snake_case>.sql
```

Migrações aplicadas são imutáveis. Não edite um arquivo já executado; crie a próxima versão. Não inclua dados reais ou seeds financeiros.

## Escopo atual

A fundação contém aplicação Spring Boot, PostgreSQL, Flyway, readiness, lint e testes de integração. Domínio financeiro, importação de PDF, API de negócio, MCP e Android pertencem a issues posteriores.
