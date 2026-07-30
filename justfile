set shell := ["bash", "-eu", "-o", "pipefail", "-c"]

compose := "docker compose --project-name personal-finance-local --file infra/compose.yml"

default:
    @just --list

local-up:
    {{compose}} up --detach --wait postgres

local-down:
    {{compose}} down --remove-orphans

local-status:
    {{compose}} ps

local-logs:
    {{compose}} logs --follow postgres

local-reset:
    bash infra/scripts/reset-local-database.sh

dev: backend-run

backend-check:
    cd backend && bash ./gradlew --no-daemon check

backend-test:
    cd backend && bash ./gradlew --no-daemon test

backend-run: local-up
    cd backend && SPRING_PROFILES_ACTIVE=local bash ./gradlew bootRun

backend-format:
    cd backend && bash ./gradlew ktlintFormat

backend-clean:
    cd backend && bash ./gradlew clean
