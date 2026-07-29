set shell := ["bash", "-eu", "-o", "pipefail", "-c"]

default:
    @just --list

backend-check:
    cd backend && bash ./gradlew check

backend-test:
    cd backend && bash ./gradlew test

backend-run:
    cd backend && bash ./gradlew bootRun

backend-format:
    cd backend && bash ./gradlew ktlintFormat

backend-clean:
    cd backend && bash ./gradlew clean
