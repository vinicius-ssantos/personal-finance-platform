# Contributing

## Antes de começar

1. Leia `README.md`, `PRIVACY.md`, `SECURITY.md` e os documentos de arquitetura.
2. Confirme que existe issue aprovada para a mudança.
3. Não use dados financeiros reais em código, testes, screenshots ou exemplos.
4. Mudanças arquiteturais relevantes exigem ADR.

## Fluxo

- crie branch a partir de `main`;
- use nomes como `feat/issue-123-short-name`, `fix/issue-123-short-name` ou `docs/issue-123-short-name`;
- mantenha commits pequenos e convencionais;
- abra PR vinculada à issue;
- descreva escopo, riscos, testes e impacto em segurança/privacidade;
- não faça merge com checks falhando;
- prefira squash merge para manter histórico legível.

## Padrão de commits

```text
feat: add position parser registry
fix: reject stale import preview
refactor: isolate portfolio persistence adapter
test: cover protected pdf password lifecycle
docs: record multi-currency reconciliation decision
chore: update build tooling
```

## Definition of Done por mudança

- critério da issue atendido;
- testes adequados adicionados;
- lint e build verdes;
- contratos e documentação atualizados;
- logs e erros sanitizados;
- nenhuma regressão de privacidade;
- fixtures exclusivamente sintéticas;
- ADR criado ou atualizado quando necessário;
- migração reversível ou runbook de correção quando houver banco.

## Kotlin

- injeção por construtor;
- nulabilidade explícita;
- sem `!!` em produção, salvo interoperabilidade encapsulada;
- `data class` para valores e DTOs, não automaticamente para entidades;
- `sealed interface` para estados fechados;
- dinheiro em minor units;
- `BigDecimal` para quantidades e taxas;
- corrotinas somente quando a cadeia inteira se beneficiar;
- nenhuma regra financeira em controller ou adapter.

## Testes

Mudanças no parser exigem:

- fixture sintética;
- texto extraído esperado;
- JSON bruto e canônico esperado;
- reconciliação esperada;
- evidência e confiança esperadas;
- incremento de versão em mudança semântica.

Mudanças financeiras exigem unitários e property tests. Mudanças de persistência exigem Testcontainers. Mudanças de contrato exigem OpenAPI lint e contract tests.

## Segurança

Não publique vulnerabilidade ou dado sensível em issue/PR. Use Security Advisories. Secret scan local é obrigatório antes do push.

## Revisão

Uma PR deve ser pequena o suficiente para responder claramente:

- o que muda;
- por que muda;
- como foi testado;
- quais dados toca;
- qual falha evita;
- como reverter.
