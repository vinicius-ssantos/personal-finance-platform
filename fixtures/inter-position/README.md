# Fixtures — extrato consolidado de posição

Pacote sintético que reproduz a **estrutura** de um extrato consolidado de posição,
sem conter nenhum dado real.

## Aviso: a estrutura é provisória

Os nomes das seções vêm de `docs/roadmap/RELEASE-0.1.md`. **Os cabeçalhos de coluna,
a ordem e a formatação numérica foram inventados**, porque a ADR 0029 mantém
relatórios reais fora do repositório e nenhum layout observado está registrado nele.

Consequência prática: estas fixtures exercitam o pipeline com fidelidade de
comportamento, mas **ainda não provam que o parser lerá um extrato real**. Elas
provam que a detecção, a extração e a reconciliação funcionam sobre um documento
com essa forma.

Trocar a estrutura inventada pela observada é uma alteração em
`InterPositionFixture.kt` e uma regeneração dos goldens. Nada mais se move.

## Casos

| Caso | O que exercita |
|---|---|
| `complete` | todas as seções, BRL e USD, totais que reconciliam |
| `controlled-mismatch` | total BRL declarado 100,00 acima dos subtotais, acima da tolerância |
| `unknown-section` | uma seção que nenhum parser conhece |
| `unsupported-layout` | documento que nenhum detector deve reivindicar |

Os três primeiros são protegidos por senha; o último não, para que a recusa por
layout seja testada sem depender da senha.

Conferência do caso `complete`, feita à mão de propósito:

```text
23.250,00  Tesouro Direto
 4.500,00  Bolsa nacional
17.500,00  Renda fixa
 4.750,00  Fundos
---------
50.000,00  Total BRL declarado

 1.500,00  Total USD declarado, sem conversão
```

## Por que os PDFs não são versionados

Os PDFs são **gerados em tempo de teste** por `FixturePdfBuilder`, não commitados:

- um blob binário não pode ser revisado, e a ADR 0029 alerta que é justamente por
  metadado residual que dado pessoal escapa;
- a fonte de cada fixture permanece legível em Kotlin, então quem revisa vê que o
  conteúdo é inventado;
- nada que tenha encostado num documento real pode entrar aqui por acidente.

O que é versionado são os **goldens em texto**: a forma canônica do texto extraído,
que é sobre o que o pipeline raciocina.

Os bytes de uma fixture protegida **não** são reprodutíveis entre execuções — a
cifragem deriva um salt aleatório, por design. O texto extraído é.

## Regenerar os goldens

```bash
cd backend
./gradlew test -DupdateGoldenFiles=true --tests '*FixtureGoldenTests'
```

O diff resultante é revisado como qualquer outra mudança. Um golden que muda sem
que a fixture tenha mudado é sinal de regressão na extração, não ruído.

## Processo de descoberta privada

A ADR 0029 permite inspecionar relatórios reais **apenas** em ambiente privado e
temporário, para descobrir estrutura, campos e variações.

Ao observar um relatório real, registre aqui somente a **forma**:

- nomes de seção e a ordem em que aparecem;
- cabeçalhos de coluna e alinhamento;
- formato de número, data e moeda;
- onde aparecem subtotais e totais;
- variações entre emissões (seção ausente, coluna extra, quebra de página no meio
  de uma seção).

Nunca registre: identidade do titular, número de conta, saldos, quantidades,
códigos de ativo reais, nem trechos copiados do texto extraído.

O arquivo real não é anexado a issue, PR ou artifact de CI, e não é copiado para
dentro do repositório em nenhuma etapa.

## Verificação automatizada

`FixturePrivacyTests` varre este diretório e as próprias definições em busca de
CPF, CNPJ, e-mail, telefone, cartão, chave PIX aleatória e segredo atribuído, e
falha se encontrar qualquer um. Também recusa PDF, ZIP, XLSX, OFX e CSV
commitados aqui.

Passar nesse check é piso, não certificado: os padrões cobrem as formas que dado
pessoal costuma tomar, não todas as possíveis.
