# Pipeline de ingestão documental

## Escopo validado

A descoberta privada confirmou três famílias textuais do Banco Inter:

- extrato consolidado de posição;
- extrato de movimentações;
- notas de negociação de renda fixa.

Os PDFs observados são protegidos por senha e possuem texto digital extraível. OCR não faz parte do caminho principal. Os documentos reais não entram no repositório.

## Estados

```text
UPLOADED
  -> PASSWORD_REQUIRED -> DECRYPTED
  -> SCANNED
  -> EXTRACTED
  -> PARSED
  -> ENRICHED
  -> VALIDATED
  -> PREVIEW_READY
  -> COMMITTED

Estados terminais alternativos: REJECTED, FAILED e DUPLICATE.
```

Somente `PREVIEW_READY` pode virar `COMMITTED`. Estados finais não retornam a estados anteriores.

## Recepção

- MIME inicial: `application/pdf`;
- limite inicial: 15 MB, configurável;
- limite de páginas e tempo de parsing;
- `importId` gerado antes do processamento;
- nome original apenas para exibição sanitizada;
- armazenamento temporário com nome aleatório;
- assinatura mágica validada, não apenas extensão;
- PDF com JavaScript, anexos ou conteúdo ativo é rejeitado quando detectável.

## Senha do PDF

A senha do documento é segredo efêmero:

- enviada separadamente do arquivo;
- máximo de tentativas e tamanho limitado;
- nunca persistida, registrada, medida ou reenviada;
- usada somente em memória ou diretório temporário restrito;
- referências ao segredo eliminadas tão cedo quanto possível;
- cópia descriptografada removida imediatamente;
- ausência gera `PDF_PASSWORD_REQUIRED`;
- valor inválido gera `INVALID_PDF_PASSWORD` sem detalhes adicionais.

O endpoint de continuidade deverá aceitar a senha por uso único e TTL curto, vinculado ao `importId`. O corpo não entra em tracing, access log ou métricas.

## Idempotência

Duas chaves complementares:

```text
raw_file_key = owner + institution + document_type + sha256_do_arquivo_recebido
semantic_key = owner + institution + document_type + data_ou_periodo + hash_das_linhas_canonicas
```

- arquivo idêntico retorna `DUPLICATE`;
- reexport com bytes diferentes é detectado semanticamente;
- importação em andamento retorna o estado atual;
- retry de falha exige intenção explícita e motivo;
- reimportação nunca duplica posições, movimentos ou notas.

## Detecção e parsers

Cada parser declara:

- instituição;
- família documental;
- `layoutVersion`;
- `parserVersion`;
- cabeçalhos, seções, colunas e marcadores esperados;
- evidências presentes e ausentes;
- limiar de confiança;
- motivo estável de rejeição.

Tipos iniciais:

```text
INTER_POSITION_CONSOLIDATED
INTER_MOVEMENT_STATEMENT
INTER_FIXED_INCOME_NEGOTIATION_NOTES
```

A Release 0.1 implementa apenas `INTER_POSITION_CONSOLIDATED`.

## Temporalidade

O modelo separa:

- data da posição;
- início e fim do período;
- data de geração;
- data de referência de mercado;
- data da operação;
- data de liquidação, quando disponível.

A data de geração não substitui automaticamente a data financeira.

## Normalização

- números pt-BR;
- BRL e USD explícitos;
- datas e quantidades;
- ticker, código, emissor, indexador e taxa;
- tipo de ativo;
- fingerprint canônico;
- valor original redigido e referência de página;
- evidência, qualidade e confiança;
- ausências como desconhecidas, nunca zero.

## Reconciliação de posições

1. reconciliar cada moeda separadamente;
2. comparar soma das posições com subtotal da categoria;
3. comparar subtotais com o total declarado quando comparável;
4. usar tolerância padrão de 1 centavo para BRL;
5. nunca tratar conversão implícita como cotação declarada;
6. bloquear commit em divergência acima da tolerância;
7. registrar diferença, seção, moeda e evidência sem copiar PII.

## Prévia

A prévia inclui:

- família, layout e confiança;
- parser e versão;
- datas e moedas;
- posições candidatas;
- totais declarados e calculados;
- campos desconhecidos;
- ativos novos ou ambíguos;
- possíveis duplicatas;
- avisos bloqueantes e não bloqueantes;
- `previewVersion` para controle otimista.

A prévia não altera o domínio.

## Confirmação

O commit:

- exige estado `PREVIEW_READY` e `previewVersion` atual;
- registra ator, instante, IP resumido e correlation ID;
- persiste dados canônicos, evidências e auditoria na mesma transação;
- marca a versão efetiva sem apagar versões anteriores;
- não guarda senha nem arquivo descriptografado;
- é idempotente.

Na Release 0.1, o snapshot efetivo é atualizado na mesma transação. Reconstruções assíncronas futuras exigem outbox transacional e jobs duráveis no PostgreSQL.

## Retenção

Padrão local:

- descriptografado: somente durante processamento;
- original criptografado: até 24 horas após confirmação;
- retenção opcional prolongada: desabilitada por padrão;
- hash e fingerprint: mantidos para idempotência;
- prévias e erros: sem texto financeiro integral;
- purge: verificável e auditado.

## Fixtures

Fixtures públicas serão sintéticas e incluirão:

- PDF protegido com senha de teste;
- seções de Tesouro, bolsa, renda fixa, internacional e fundos;
- BRL e USD;
- datas de posição, geração e mercado distintas;
- total válido e divergência controlada;
- seção desconhecida;
- texto extraído, JSON bruto, JSON canônico, evidências e issues esperadas.

Toda mudança semântica de parser exige versão nova e regressão completa das golden files.
