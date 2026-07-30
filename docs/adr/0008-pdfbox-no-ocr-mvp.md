# ADR 0008 — PDFBox sem OCR no MVP
Status: Accepted
Date: 2026-07-29

## Context

Os relatórios consolidados observados possuem texto digital extraível. OCR amplia superfície de ataque, custo, variação e necessidade de avaliação de qualidade, enquanto a Release 0.1 precisa provar um caminho determinístico e local.

## Decision

PDFBox será usado para abrir PDFs protegidos e extrair texto nativo com proveniência de página. A Release 0.1 rejeita documentos sem texto estruturado suficiente e não executa OCR como fallback.

O detector de layout e o parser recebem apenas a saída extraída dentro dos limites definidos. PDF, senha e texto bruto não são enviados a LLMs ou serviços externos.

## Consequences

- extração permanece local, reproduzível e testável;
- documentos escaneados ou layouts baseados em imagem não são suportados;
- limites de páginas, tamanho e conteúdo ativo precisam ser aplicados antes do parsing;
- uma futura adoção de OCR exige ADR, threat model e fixtures próprias.

## Alternatives considered

OCR local automático foi adiado por variabilidade e custo operacional. OCR em nuvem foi rejeitado por privacidade e dependência externa. Leitura manual foi rejeitada por não oferecer um fluxo automatizável e regressivo.
