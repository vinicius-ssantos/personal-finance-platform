# Fixtures - extrato consolidado de posição

Pacote sintético que reproduz a **estrutura observada** de um extrato consolidado de posição do Banco Inter sem conter dado real.

## Fonte da estrutura

Um relatório real da família alvo foi inspecionado somente em ambiente privado. Nenhum PDF real, texto extraído integral, identidade, conta, posição, código de ativo ou valor foi copiado para o repositório.

Foram registrados apenas elementos não sensíveis da forma:

- nomes e ordem das seções;
- cabeçalhos de coluna;
- convenções de data, número e moeda;
- posição dos subtotais e totais declarados;
- quebras de página relevantes à proveniência;
- marcadores de duas outras famílias do próprio Inter para casos negativos.

Somente **uma emissão da família `POSITION_CONSOLIDATED`** foi observada até agora. Portanto `2026_07` identifica o primeiro layout suportado, mas não afirma compatibilidade com toda variação histórica ou futura do Banco Inter.

## Forma observada do documento alvo

A sequência relevante é:

```text
capa: Posição Consolidada
-> identificação / Posição Total
-> Seu patrimônio atual
-> Distribuição da carteira
   -> Tesouro Direto
   -> Renda Variável
   -> Renda Fixa
   -> Renda Variável Internacional
   -> Fundos de Investimentos
-> material legal / fechamento
```

O cabeçalho das páginas de conteúdo repete a família e a data de posição. A data da posição usa `DD/MM/YYYY`; o instante de solicitação usa `DD/MM/YYYY HH:mm`.

### Resumo

`Seu patrimônio atual` lista as categorias na mesma ordem acima. Valores locais usam `R$`; a categoria internacional usa `US$`.

O relatório também declara uma `Posição Total R$`, mas **não expõe a taxa de câmbio usada para incorporar a parcela internacional**. Essa declaração pode ser preservada como valor bruto da fonte, porém a aplicação não pode reconstruí-la nem reconciliá-la por conversão implícita (ADR 0034 / `INV-002`).

### Tesouro Direto

Cabeçalhos observados:

```text
Aplicação
Vencimento
Quantidade
Valor Aplicado (R$)
Valor Bruto (R$)
```

### Renda Variável

Cada ativo aparece como um bloco, seguido de:

```text
Quantidade
Valor Bruto (R$)
```

A seção pode continuar em páginas seguintes sem repetir o título da categoria.

### Renda Fixa

Cada produto possui nome próprio e uma tabela com:

```text
Código Ativo
Vencimento
Aplicação
Taxa
Indexador
Valor Aplicado (R$)
IOF Previsto (R$)
IR Previsto (R$)
Valor Bruto (R$)
Valor Mercado (DD/MM/YYYY)
Valor Líquido (R$)
```

O `Valor Mercado` carrega uma data no próprio cabeçalho. Campos indisponíveis podem aparecer como `-`. A seção encerra com `Subtotal`, preenchendo somente as colunas financeiras aplicáveis.

### Renda Variável Internacional

Cada ativo aparece como bloco com:

```text
Quantidade
Valor Bruto (US$)
```

Quantidades fracionárias usam vírgula decimal.

### Fundos de Investimentos

Cada fundo aparece com:

```text
Quantidade de cotas
Preço mercado (R$)
Valor aplicado (R$)
Disp. Resgate (R$)
Valor Bruto (R$)
Valor Líquido (R$)
Valor IOF (R$)
Valor IR (R$)
```

Quantidade de cotas pode possuir muitas casas decimais; campos não disponíveis também podem aparecer como `-`.

## Formatação observada

- BRL: `R$ 1.234,56`;
- USD: `US$ 1.234,56`;
- percentual: vírgula decimal e quantidade variável de casas;
- datas financeiras: `DD/MM/YYYY`;
- instante de solicitação: `DD/MM/YYYY HH:mm`;
- valores ausentes: `-` onde o layout prevê coluna mas não há valor.

## Famílias negativas do mesmo Banco Inter

Dois outros relatórios reais foram inspecionados apenas para evitar falso positivo do detector:

- `Extrato de movimentações`, com marcadores `Visão geral` e `Movimentações`;
- `Notas de renda fixa`, com marcadores `Nota de Negociação`, `Características do Título` e `Características de Operação`.

As fixtures `unsupported-movements` e `unsupported-fixed-income-notes` são totalmente sintéticas e reproduzem somente esses marcadores estruturais. O detector de posição deve recusá-las mesmo sendo documentos do Banco Inter.

## Casos versionados

| Caso | O que exercita |
|---|---|
| `complete` | layout observado completo, BRL + USD, documento protegido e páginas explícitas |
| `summary-only` | resumo dedicado |
| `treasury-only` | Tesouro Direto dedicado |
| `brazilian-equity-only` | renda variável nacional dedicada |
| `fixed-income-only` | renda fixa dedicada, subtotal e data no cabeçalho de valor de mercado |
| `international-only` | renda variável internacional em USD |
| `funds-only` | fundos dedicados |
| `missing-optional-field` | campo opcional indisponível representado por `-` |
| `malformed-value` | token monetário pt-BR deliberadamente malformado |
| `controlled-mismatch` | subtotal de renda fixa divergente do total declarado da seção |
| `unknown-section` | seção nova que nenhum parser conhece |
| `unsupported-movements` | mesma instituição, família de movimentações |
| `unsupported-fixed-income-notes` | mesma instituição, família de notas de renda fixa |

## Por que os PDFs não são versionados

Os PDFs são gerados em tempo de teste por `FixturePdfBuilder`:

- o conteúdo sintético permanece revisável em Kotlin;
- páginas são explícitas, preservando proveniência;
- nada que tenha encostado num documento real pode entrar no Git por acidente;
- metadados são controlados pelo gerador.

O que fica em `fixtures/` são os goldens da forma canônica do texto extraído.

## Regenerar os goldens

```bash
cd backend
./gradlew test -DupdateGoldenFiles=true --tests '*FixtureGoldenTests'
```

O diff resultante deve ser revisado. Golden que muda sem a fixture ter mudado é regressão de extração ou alteração semântica do layout.

## Regra para futuras observações privadas

Ao inspecionar outra emissão do relatório de posição, registrar somente diferenças de forma: seção ausente/nova, cabeçalho alterado, coluna extra, formato diferente ou quebra de página relevante.

Nunca registrar: identidade do titular, CPF, número de conta, endereço, telefone, saldos, quantidades, códigos de ativos reais, senha ou trechos financeiros copiados do documento.

`FixturePrivacyTests` é um piso automatizado, não um certificado de anonimização.
