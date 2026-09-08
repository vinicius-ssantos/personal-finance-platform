package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.Money

/**
 * Renders a reconciliation report as reviewable text.
 *
 * Both sides of every comparison are recorded, not only the verdict: when a
 * golden changes, the diff has to show whether the declared value moved, the
 * computed one did, or only the conclusion drawn from them.
 */
internal fun renderReconciliationSnapshot(report: ReconciliationReport): String =
    buildString {
        appendLine("tolerance|${report.toleranceId}")
        report.results.forEach { result ->
            appendLine(
                listOf(
                    "recon",
                    result.rule.code,
                    result.rule.scope.name,
                    result.currency.name,
                    result.subject,
                    result.amounts.declared.render(),
                    result.amounts.calculated.render(),
                    result.amounts.differenceMinor?.toString() ?: "<none>",
                    "tol=${result.amounts.toleranceMinor}",
                    result.outcome.name,
                    result.reason?.name ?: "<none>",
                ).joinToString("|"),
            )
        }
        appendLine("blocking|${report.blockingResults.size}")
    }.trimEnd()

private fun Money?.render(): String = this?.let { money -> "${money.amountMinor}${money.currency}" } ?: "<none>"
