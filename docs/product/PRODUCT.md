# Produto e limites

## Problema

Os dados de investimentos estão distribuídos entre telas e documentos do Banco Inter. Responder perguntas como patrimônio atual, liquidez, aportes, rendimento estimado e capacidade de entrada em um imóvel exige cruzar fontes com temporalidade e qualidade diferentes.

## Objetivo

Criar uma fonte de verdade pessoal, auditável e reproduzível que:

- importe relatórios financeiros de forma segura;
- preserve documento, layout, parser, data e evidência;
- mantenha posições históricas imutáveis;
- calcule indicadores por código determinístico;
- exponha resultados por REST e, futuramente, MCP somente leitura;
- ofereça experiência diária pelo Android sem duplicar regras oficiais.

## Usuário

O MVP é de usuário único. O proprietário importa, revisa, confirma e consulta os próprios dados.

## Perguntas que o produto deve responder

- Quanto existe investido em uma data de referência?
- Qual parte possui valor líquido conhecido ou estimado?
- Como a carteira está distribuída por classe, emissor, indexador, moeda e vencimento?
- Qual é a idade e a cobertura dos dados?
- Quais fluxos representam aportes externos confirmados?
- Qual foi o rendimento estimado, separado de fluxos externos?
- Quanto do patrimônio pode ser usado em uma entrada de imóvel sem consumir a reserva definida?
- Qual aporte mensal seria necessário para atingir uma meta sob premissas explícitas?

## Superfícies

### Backend

Fonte de verdade, responsável por ingestão, reconciliação, domínio, persistência, analytics, metas, autenticação, REST, MCP e integrações.

### Android

Cliente Kotlin/Compose para consulta, upload, revisão e confirmação. Não interpreta PDF nem calcula patrimônio oficial.

### MCP

Adaptador read-only para consultas e simulações estruturadas. Não recebe arquivo bruto, senha, credencial bancária ou capacidade de movimentar dinheiro.

## Não objetivos

A plataforma não será banco, corretora, robô de investimento, motor de crédito, planejador tributário completo, sistema de execução de ordens ou substituto de aconselhamento profissional.

Ficam fora do MVP inicial:

- Pix, transferências, compras, vendas ou resgates;
- multiusuário;
- conta PJ;
- criptomoedas, previdência e COE;
- cotação intraday;
- OCR generalista como caminho principal;
- microserviços, Kafka, Redis e Kubernetes;
- iOS no primeiro ciclo;
- integração Open Finance antes de o domínio documental estar validado.

## Regras de produto

1. Importar não significa confirmar.
2. Dados ausentes permanecem desconhecidos.
3. Resultados exibem data-base, qualidade, cobertura e premissas.
4. Correções preservam histórico e auditoria.
5. Nenhum cliente externo comanda o domínio.
6. O sistema nunca recomenda usar todo o patrimônio como entrada.
7. Estimativas não são apresentadas como valores oficiais.

## Fases

1. Release 0.1: posição patrimonial local.
2. Histórico de movimentações e notas de renda fixa.
3. Analytics, liquidez, vencimentos e concentração.
4. Metas, projeções e simulação do imóvel.
5. REST autenticado e MCP read-only.
6. Android read-only, depois importação e alertas.
7. Open Finance opcional por adapter.
8. KMP e iOS somente após necessidade comprovada.

## Critério de sucesso inicial

A Release 0.1 será bem-sucedida quando uma fixture sintética equivalente ao extrato real protegido puder ser importada, reconciliada, revisada, confirmada e consultada sem expor senha ou dados pessoais.
