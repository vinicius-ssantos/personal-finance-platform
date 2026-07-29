# Backend

Projeto Kotlin/JVM independente que concentra a fonte de verdade da Personal Finance Platform.

## Requisitos

- JDK 25;
- acesso ao Maven Central na primeira execução;
- Gradle Wrapper versionado no diretório `backend/`.

## Comandos

A partir da raiz do monorepo:

```bash
just backend-check
just backend-test
just backend-run
```

Sem `just`:

```bash
cd backend
./gradlew check
./gradlew test
./gradlew bootRun
```

No Windows:

```powershell
cd backend
.\gradlew.bat check
.\gradlew.bat bootRun
```

A aplicação inicia em `127.0.0.1:8080`. O health check fica disponível em `/actuator/health`.

## Escopo atual

Este bootstrap contém apenas a aplicação mínima, Actuator, Spring MVC, lint e teste de contexto. Banco de dados, domínio financeiro, importação de PDF, API de negócio, MCP e Android pertencem a issues posteriores.
