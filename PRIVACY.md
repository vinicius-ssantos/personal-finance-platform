# Política de privacidade do repositório

## Escopo

Este documento descreve quais dados podem aparecer no repositório público e quais devem permanecer apenas em ambiente privado de desenvolvimento ou produção pessoal.

## Dados permitidos

- código-fonte;
- documentação técnica e de produto;
- schemas OpenAPI e JSON;
- exemplos completamente fictícios;
- fixtures sintéticas equivalentes ao formato dos documentos;
- métricas técnicas sem identificadores ou valores financeiros;
- screenshots com dados gerados e revisados.

## Dados proibidos

- PDFs ou CSVs reais de instituição financeira;
- texto extraído de relatório real;
- nome completo associado a saldo ou posição;
- CPF, endereço, telefone, agência, conta ou identificador bancário;
- patrimônio, aportes, movimentações, metas ou ativos reais do usuário;
- senha de PDF, token, cookie, chave, secret ou credencial;
- backup, dump ou log de ambiente com dados reais;
- arquivos temporários descriptografados.

## Descoberta documental privada

Documentos reais podem ser usados localmente para validar viabilidade e identificar layouts. O processo deve:

1. ocorrer em ambiente privado;
2. evitar retenção desnecessária;
3. registrar apenas família, layout, campos e diferenças sem PII;
4. gerar fixture sintética equivalente;
5. remover cópias temporárias descriptografadas;
6. nunca anexar o documento real a issue, PR, CI ou artifact.

## Fixtures e exemplos

Fixtures públicas devem usar nomes, códigos, valores, datas e identificadores inventados. A revisão de PR deve verificar que nenhuma combinação permita reconstruir dados reais.

## Telemetria futura

A aplicação não deve enviar saldos, ativos, documentos, CPF ou conta para analytics ou crash reporting. Eventos permitidos são técnicos e sanitizados, como versão, duração, categoria de falha e correlation ID opaco.

## Retenção da aplicação

A política padrão será:

- descriptografado somente durante processamento;
- original criptografado por até 24 horas após confirmação;
- retenção longa desabilitada por padrão;
- hash e fingerprint mantidos para idempotência;
- exclusão verificável e auditada.

## Responsabilidade

O mantenedor é responsável por revisar toda publicação. Contribuidores devem interromper a submissão e solicitar canal privado ao perceber qualquer dado potencialmente real ou sensível.
