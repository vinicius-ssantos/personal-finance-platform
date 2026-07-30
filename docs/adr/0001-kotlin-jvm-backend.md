# ADR 0001 — Kotlin/JVM no backend
Status: Accepted
Date: 2026-07-29

## Context

O backend precisa executar regras financeiras determinísticas, integrar-se ao ecossistema Spring e permanecer acessível a um desenvolvedor com experiência principal em Java. A futura aplicação Android também usa Kotlin, mas o compartilhamento de código entre servidor e dispositivo não é requisito da Release 0.1.

## Decision

Kotlin/JVM é a linguagem principal do backend. Java pode ser usado quando uma biblioteca, ferramenta ou limitação de interoperabilidade justificar. O código compila para JDK 25, trata warnings como erros e mantém o pacote raiz `br.com.vinicius.personalfinance`.

Entidades Spring, JDBC, persistência e contratos de transporte não serão compartilhados com o mobile. Qualquer compartilhamento futuro exige uma extração deliberada de tipos puros.

## Consequences

- acesso direto ao ecossistema Java e Spring;
- null-safety e tipos de valor mais expressivos;
- necessidade de convenções para interoperabilidade e reflexão;
- build e lint precisam validar Kotlin de forma reproduzível;
- conhecimento de Kotlin passa a ser requisito de manutenção do servidor.

## Alternatives considered

Java puro foi rejeitado por reduzir a expressividade sem ganho relevante para este projeto. Kotlin Multiplatform foi adiado porque anteciparia acoplamento entre backend e mobile antes de existir demanda real.