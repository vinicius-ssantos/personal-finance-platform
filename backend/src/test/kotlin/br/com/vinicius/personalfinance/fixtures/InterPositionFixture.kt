package br.com.vinicius.personalfinance.fixtures

/**
 * Synthetic Banco Inter consolidated-position reports.
 *
 * The structure below is based on a privately inspected real export. Only the
 * non-sensitive shape was carried over: section names/order, column labels,
 * numeric/date/currency conventions, subtotal placement and page boundaries.
 * Every identity, code, date and amount in this file is invented.
 *
 * The two unsupported cases model other Banco Inter document families observed
 * privately. They exist to prove that a same-institution document is not enough
 * evidence for the position detector to claim a layout.
 */
object InterPositionFixture {
    /** Test-only password. Never a real document password. */
    const val TEST_PASSWORD: String = "fixture-only-password"

    const val INSTITUTION: String = "BANCO_INTER"

    const val DOCUMENT_FAMILY: String = "POSITION_CONSOLIDATED"

    /** First consolidated-position layout observed privately. */
    const val LAYOUT_VERSION: String = "2024_07"

    /** Distinct on purpose: financial position date and export instant are not one concept. */
    const val POSITION_DATE: String = "31/01/2026"

    const val GENERATED_DATE: String = "02/02/2026"

    const val GENERATED_AT: String = "$GENERATED_DATE 08:15"

    /**
     * The source exposes this date inside the fixed-income "Valor Mercado" column header,
     * not as a standalone document field.
     */
    const val MARKET_REFERENCE_DATE: String = GENERATED_DATE

    /**
     * Deliberately cannot be derived from the section totals without FX evidence.
     *
     * The observed report declares one BRL "Posição Total" while the international
     * category is denominated in US$. No exchange rate is printed. ADR 0034 therefore
     * forbids the application from reconstructing this value by an implicit conversion.
     */
    const val DECLARED_POSITION_TOTAL_BRL: String = "12.000,00"

    data class Case(
        val name: String,
        val description: String,
        val pages: List<List<String>>,
        val protected: Boolean = true,
    ) {
        val lines: List<String>
            get() = pages.flatten()
    }

    private fun pageHeader(): List<String> =
        listOf(
            "CPF XXX.XXX.XXX-XX / Conta 00000000",
            "Extrato de posição em $POSITION_DATE",
        )

    private fun coverPage(): List<String> =
        listOf(
            "POSIÇÃO CONSOLIDADA",
            "Extrato de posição referente a $POSITION_DATE",
        )

    private fun identityPage(positionTotal: String = DECLARED_POSITION_TOTAL_BRL): List<String> =
        pageHeader() +
            listOf(
                "TITULAR FICTÍCIO",
                "Solicitado no dia $GENERATED_AT",
                "Posição Total R$ $positionTotal",
            )

    private fun summaryPage(fixedIncomeTotal: String = "3.000,00"): List<String> =
        pageHeader() +
            listOf(
                "Seu patrimônio atual",
                "R$ $DECLARED_POSITION_TOTAL_BRL",
                "Tesouro Direto R$ 1.000,00",
                "Renda Variável R$ 2.000,00",
                "Renda Fixa R$ $fixedIncomeTotal",
                "Renda Variável Internacional US$ 400,00",
                "Fundos de Investimentos R$ 4.000,00",
                "Extrato de posição em $POSITION_DATE",
            )

    private fun distributionIntro(): List<String> =
        listOf(
            "Distribuição da carteira",
            "Aqui você acompanha a organização da sua carteira, visualizando a",
            "distribuição das posições por ativo.",
        )

    private fun treasurySection(): List<String> =
        listOf(
            "10,00% Tesouro Direto Valor Bruto R$ 1.000,00",
            "Tesouro Fictício 2029",
            "Aplicação Vencimento Quantidade Valor Aplicado (R$) Valor Bruto (R$)",
            "10/01/2026 01/03/2029 0,50 R$ 900,00 R$ 1.000,00",
        )

    private fun brazilianEquitySection(): List<String> =
        listOf(
            "20,00% Renda Variável Valor Bruto R$ 2.000,00",
            "AAAA3",
            "Quantidade Valor Bruto (R$)",
            "10 R$ 1.000,00",
            "BBBB11",
            "Quantidade Valor Bruto (R$)",
            "5 R$ 1.000,00",
        )

    private fun fixedIncomeHeader(total: String = "3.000,00"): List<String> =
        listOf(
            "30,00% Renda Fixa Valor Bruto R$ $total",
            "LCI FICTÍCIA 3 ANOS",
            "Código Ativo Vencimento Aplicação Taxa Indexador",
            "Valor Aplicado (R$) IOF Previsto (R$) IR Previsto (R$) Valor Bruto (R$)",
            "Valor Mercado ($MARKET_REFERENCE_DATE) Valor Líquido (R$)",
        )

    private fun fixedIncomeRows(
        subtotalGross: String = "3.000,00",
        malformedGross: String? = null,
    ): List<String> {
        val firstGross = malformedGross ?: "1.250,00"
        return listOf(
            "FICTICIO001 20/01/2029 16/01/2026 100,00% IPCA R$ 1.200,00 0,00 0,00 R$ $firstGross - R$ 1.250,00",
            "CDB FICTÍCIO",
            "Código Ativo Vencimento Aplicação Taxa Indexador",
            "Valor Aplicado (R$) IOF Previsto (R$) IR Previsto (R$) Valor Bruto (R$)",
            "Valor Mercado ($MARKET_REFERENCE_DATE) Valor Líquido (R$)",
            "FICTICIO002 07/07/2029 16/07/2026 100,00% CDI R$ 1.700,00 0,00 0,00 R$ 1.750,00 - R$ 1.750,00",
            "Subtotal R$ 2.900,00 R$ $subtotalGross R$ $subtotalGross",
        )
    }

    private fun internationalSection(): List<String> =
        listOf(
            "4,00% Renda Variável Internacional Valor Bruto US$ 400,00",
            "ZZZZ",
            "Quantidade Valor Bruto (US$)",
            "0,50000 US$ 200,00",
            "YYYY",
            "Quantidade Valor Bruto (US$)",
            "0,25000 US$ 200,00",
        )

    private fun fundsSection(missingOptional: Boolean = false): List<String> {
        val redemption = if (missingOptional) "-" else "R$ 100,00"
        return listOf(
            "40,00% Fundos de Investimentos Valor Bruto R$ 4.000,00",
            "FUNDO FICTÍCIO MULTIMERCADO",
            "Quantidade de cotas Preço mercado (R$) Valor aplicado (R$) Disp. Resgate (R$)",
            "Valor Bruto (R$) Valor Líquido (R$) Valor IOF (R$) Valor IR (R$)",
            "1.000,00000000 R$ 2,00 R$ 1.800,00 $redemption R$ 2.000,00 R$ 1.990,00 - R$ 10,00",
            "FUNDO FICTÍCIO RENDA FIXA",
            "Quantidade de cotas Preço mercado (R$) Valor aplicado (R$) Disp. Resgate (R$)",
            "Valor Bruto (R$) Valor Líquido (R$) Valor IOF (R$) Valor IR (R$)",
            "500,00000000 R$ 4,00 R$ 1.900,00 - R$ 2.000,00 R$ 2.000,00 - R$ 0,00",
        )
    }

    private fun legalPage(): List<String> =
        pageHeader() +
            listOf(
                "Material informativo sintético para fixture de teste.",
                "Nenhuma informação desta página corresponde a cliente, ativo ou saldo real.",
            )

    private fun completePages(
        fixedIncomeSummaryTotal: String = "3.000,00",
        fixedIncomeSectionTotal: String = "3.000,00",
        fixedIncomeSubtotalGross: String = "3.000,00",
        malformedGross: String? = null,
    ): List<List<String>> =
        listOf(
            coverPage(),
            identityPage(),
            summaryPage(fixedIncomeSummaryTotal),
            pageHeader() + distributionIntro() + treasurySection() + brazilianEquitySection(),
            pageHeader() +
                fixedIncomeHeader(fixedIncomeSectionTotal) +
                fixedIncomeRows(fixedIncomeSubtotalGross, malformedGross),
            pageHeader() + internationalSection(),
            pageHeader() + fundsSection(),
            legalPage(),
            pageHeader(),
        )

    val complete: Case =
        Case(
            name = "complete",
            description = "observed position layout with all supported sections and BRL plus USD",
            pages = completePages(),
        )

    val summaryOnly: Case =
        Case(
            name = "summary-only",
            description = "dedicated summary-page fixture",
            pages = listOf(coverPage(), identityPage(), summaryPage()),
        )

    val treasuryOnly: Case =
        Case(
            name = "treasury-only",
            description = "dedicated Treasury section fixture",
            pages = listOf(pageHeader() + distributionIntro() + treasurySection()),
        )

    val brazilianEquityOnly: Case =
        Case(
            name = "brazilian-equity-only",
            description = "dedicated Brazilian equity section fixture",
            pages = listOf(pageHeader() + distributionIntro() + brazilianEquitySection()),
        )

    val fixedIncomeOnly: Case =
        Case(
            name = "fixed-income-only",
            description = "dedicated fixed-income section fixture",
            pages = listOf(pageHeader() + distributionIntro() + fixedIncomeHeader() + fixedIncomeRows()),
        )

    val internationalOnly: Case =
        Case(
            name = "international-only",
            description = "dedicated international USD section fixture",
            pages = listOf(pageHeader() + distributionIntro() + internationalSection()),
        )

    val fundsOnly: Case =
        Case(
            name = "funds-only",
            description = "dedicated investment-funds section fixture",
            pages = listOf(pageHeader() + distributionIntro() + fundsSection()),
        )

    val missingOptionalField: Case =
        Case(
            name = "missing-optional-field",
            description = "observed dash placeholder for an unavailable optional fund field",
            pages = listOf(pageHeader() + distributionIntro() + fundsSection(missingOptional = true)),
        )

    val malformedValue: Case =
        Case(
            name = "malformed-value",
            description = "known layout with a deliberately malformed pt-BR money token",
            pages = completePages(malformedGross = "1.25X,00"),
        )

    val controlledMismatch: Case =
        Case(
            name = "controlled-mismatch",
            description = "fixed-income subtotal disagrees with its declared section total",
            pages = completePages(fixedIncomeSubtotalGross = "2.900,00"),
        )

    val unknownSection: Case =
        Case(
            name = "unknown-section",
            description = "known position document carrying a section no parser knows",
            pages =
                listOf(
                    coverPage(),
                    identityPage(),
                    summaryPage(),
                    pageHeader() +
                        distributionIntro() +
                        treasurySection() +
                        listOf(
                            "PRODUTO ESTRUTURADO DESCONHECIDO",
                            "Descrição Valor Bruto (R$)",
                            "Estrutura fictícia XYZ R$ 500,00",
                        ),
                ),
        )

    val unsupportedMovements: Case =
        Case(
            name = "unsupported-layout",
            description = "same institution, but movement-statement family",
            pages =
                listOf(
                    listOf(
                        "EXTRATO DE MOVIMENTAÇÕES",
                        "Extrato de movimentações de 01/01/2026 a 31/01/2026",
                    ),
                    listOf(
                        "Visão geral",
                        "Seu patrimônio inicial R$ 10.000,00",
                        "Seu patrimônio final R$ 11.000,00",
                        "Entradas",
                        "Renda Fixa +R$ 1.000,00",
                    ),
                    listOf(
                        "Movimentações",
                        "Produto Ativo Código Ativo Quantidade Valor IOF Previsto IR Previsto Valor Líquido",
                        "Renda Fixa CDB FICTÍCIO FICTICIO001 100,00 +R$ 1.000,00 0,00% 0,00% +R$ 1.000,00",
                    ),
                ),
            protected = false,
        )

    val unsupportedFixedIncomeNotes: Case =
        Case(
            name = "unsupported-fixed-income-notes",
            description = "same institution, but fixed-income negotiation-note family",
            pages =
                listOf(
                    listOf(
                        "NOTAS DE RENDA FIXA",
                        "Notas de renda fixa do período de 01/01/2026 a 31/01/2026",
                    ),
                    listOf(
                        "Nota de Negociação: 000000000",
                        "Tipo de Operação: Aplicação",
                        "Data da Operação 15/01/2026",
                        "Dados Cliente",
                        "Características do Título",
                        "Ativo Emissão Vencimento Indexador Taxa Nominal Local de Custódia",
                        "Características de Operação",
                        "Quantidade/Valor Nominal PU da Operação Indexador/Taxa negociada Forma de Liquidação",
                    ),
                ),
            protected = false,
        )

    val all: List<Case> =
        listOf(
            complete,
            summaryOnly,
            treasuryOnly,
            brazilianEquityOnly,
            fixedIncomeOnly,
            internationalOnly,
            fundsOnly,
            missingOptionalField,
            malformedValue,
            controlledMismatch,
            unknownSection,
            unsupportedMovements,
            unsupportedFixedIncomeNotes,
        )
}
