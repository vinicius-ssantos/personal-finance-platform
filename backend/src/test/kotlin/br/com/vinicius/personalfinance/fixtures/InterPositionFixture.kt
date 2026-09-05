package br.com.vinicius.personalfinance.fixtures

/**
 * Synthetic Banco Inter consolidated-position reports.
 *
 * ## Provisional structure
 *
 * The section names come from `docs/roadmap/RELEASE-0.1.md`; the column
 * headers, ordering and number formatting below are **invented**, because
 * ADR 0029 keeps real reports out of the repository and no observed layout is
 * recorded anywhere in it.
 *
 * That is deliberate and it has a cost: these fixtures exercise the pipeline
 * faithfully, but they do not yet prove the parser will read a real statement.
 * Replacing the invented structure with the observed one is a change to this
 * file alone — the generator, goldens, checks and tests do not move.
 *
 * ## Invented by construction
 *
 * Every identity, code, date and amount here is made up. Amounts are chosen so
 * the arithmetic is checkable by hand, and no value corresponds to a real
 * holding.
 */
object InterPositionFixture {
    /** Test-only password. Never a real document password. */
    const val TEST_PASSWORD: String = "fixture-only-password"

    const val INSTITUTION: String = "BANCO_INTER"

    const val DOCUMENT_FAMILY: String = "POSITION_CONSOLIDATED"

    const val LAYOUT_VERSION: String = "provisional-2026.1"

    /** Distinct on purpose: ADR 0031 forbids collapsing these into one date. */
    const val POSITION_DATE: String = "31/01/2026"

    const val GENERATED_AT: String = "02/02/2026 08:15"

    const val MARKET_REFERENCE_DATE: String = "30/01/2026"

    /** A case the pipeline should handle, with the text it is built from. */
    data class Case(
        val name: String,
        val description: String,
        val lines: List<String>,
        val protected: Boolean = true,
    )

    private fun header(): List<String> =
        listOf(
            "BANCO INTER S.A.",
            "POSICAO CONSOLIDADA DE INVESTIMENTOS",
            "Data da posicao: $POSITION_DATE",
            "Documento gerado em: $GENERATED_AT",
            "Referencia de mercado: $MARKET_REFERENCE_DATE",
            "Titular: FULANO DE TAL DA SILVA",
            "Conta: 00000000-0",
            "",
        )

    private fun treasury(): List<String> =
        listOf(
            "TESOURO DIRETO",
            "Titulo                        Quantidade      Valor bruto (BRL)",
            "Tesouro Selic 2029              1,50000              15.000,00",
            "Tesouro IPCA+ 2035              0,75000               8.250,00",
            "Subtotal Tesouro Direto                              23.250,00",
            "",
        )

    private fun brazilianEquity(): List<String> =
        listOf(
            "RENDA VARIAVEL - BOLSA NACIONAL",
            "Ativo         Quantidade    Preco medio    Valor bruto (BRL)",
            "AAAA3               100,00          25,00           2.500,00",
            "BBBB11               50,00          40,00           2.000,00",
            "Subtotal Bolsa Nacional                              4.500,00",
            "",
        )

    private fun fixedIncome(): List<String> =
        listOf(
            "RENDA FIXA",
            "Emissor            Indexador   Vencimento    Valor bruto (BRL)",
            "Emissor Ficticio A  CDI 105%   15/06/2027           10.000,00",
            "Emissor Ficticio B  IPCA+5,00% 20/12/2029            7.500,00",
            "Subtotal Renda Fixa                                 17.500,00",
            "",
        )

    private fun international(): List<String> =
        listOf(
            "INTERNACIONAL",
            "Ativo      Quantidade    Preco medio    Valor bruto (USD)",
            "ZZZZ            10,00         150,00           1.500,00",
            "Subtotal Internacional (USD)                     1.500,00",
            "",
        )

    private fun funds(): List<String> =
        listOf(
            "FUNDOS DE INVESTIMENTO",
            "Fundo                        Cotas        Valor bruto (BRL)",
            "Fundo Ficticio Multimercado  1.000,000000        4.750,00",
            "Subtotal Fundos                                   4.750,00",
            "",
        )

    /** BRL and USD are totalled separately: ADR 0034 forbids implicit conversion. */
    private fun totals(brl: String = "50.000,00"): List<String> =
        listOf(
            "TOTAIS POR MOEDA",
            "Total BRL                                          $brl",
            "Total USD                                           1.500,00",
        )

    /** Every section present, totals consistent with the subtotals. */
    val complete: Case =
        Case(
            name = "complete",
            description = "all observed sections, BRL and USD, totals reconcile",
            lines =
                header() + treasury() + brazilianEquity() + fixedIncome() +
                    international() + funds() + totals(),
        )

    /**
     * The declared BRL total is 100,00 above the sum of subtotals.
     *
     * Deliberately larger than the one-cent tolerance, so reconciliation must
     * block rather than round it away.
     */
    val controlledMismatch: Case =
        Case(
            name = "controlled-mismatch",
            description = "declared BRL total exceeds the subtotals by 100,00",
            lines =
                header() + treasury() + brazilianEquity() + fixedIncome() +
                    international() + funds() + totals(brl = "50.100,00"),
        )

    /** A section no parser knows, to prove unknown content is not silently dropped. */
    val unknownSection: Case =
        Case(
            name = "unknown-section",
            description = "carries a section the parser does not know",
            lines =
                header() + treasury() +
                    listOf(
                        "PRODUTO ESTRUTURADO DESCONHECIDO",
                        "Descricao                              Valor (BRL)",
                        "Estrutura ficticia XYZ                    1.000,00",
                        "",
                    ) + totals(brl = "24.250,00"),
        )

    /** Not a position report at all, so detection must fail closed. */
    val unsupportedLayout: Case =
        Case(
            name = "unsupported-layout",
            description = "a document no detector should claim",
            lines =
                listOf(
                    "COMPROVANTE DE OPERACAO",
                    "Este documento nao e um extrato de posicao.",
                    "Numero da operacao: 000000",
                ) + (1..20).map { index -> "Linha irrelevante $index" },
            protected = false,
        )

    val all: List<Case> =
        listOf(complete, controlledMismatch, unknownSection, unsupportedLayout)
}
