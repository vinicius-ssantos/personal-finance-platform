package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.Money
import br.com.vinicius.personalfinance.shared.ValueQuality

/**
 * Renders a canonical document as reviewable text.
 *
 * Money is rendered in minor units with its currency, so a golden diff shows an
 * amount change rather than a formatting change. Unknown values render as an
 * explicit marker instead of a blank, which keeps `INV-003` visible in the
 * artefact a reviewer actually reads.
 */
internal fun renderCanonicalSnapshot(document: CanonicalPositionDocument): String =
    buildString {
        val descriptor = document.identity.descriptor
        appendLine(
            listOf(
                "document",
                "${descriptor.institution}/${descriptor.documentFamily}@${descriptor.layoutVersion}",
                document.identity.parserId,
                document.identity.parserVersion,
                document.declaredPositionTotal.renderMoney(),
            ).joinToString("|"),
        )
        appendLine(
            listOf(
                "temporality",
                document.temporality.positionDate.renderQuality { date -> date.value.toString() },
                document.temporality.generatedAt.renderQuality { at -> at.value.toString() },
                document.temporality.marketReferenceDate.renderQuality { date -> date.value.toString() },
            ).joinToString("|"),
        )
        document.summaries.forEach { summary -> appendSummary(summary) }
        document.sections.forEach { section -> appendSection(section) }
        document.unknownSections.forEach { unknown ->
            appendLine("unknown|p${unknown.pageNumber}|${unknown.headingRaw}|lines=${unknown.lineCount}")
        }
        document.issues.forEach { issue ->
            appendLine(
                "issue|${issue.code}|${issue.code.severity}|${issue.fieldPath}|" +
                    (issue.pageNumber?.let { page -> "p$page" } ?: "<no page>"),
            )
        }
    }.trimEnd()

private fun StringBuilder.appendSummary(summary: CanonicalSummary) {
    appendLine("summary|${summary.currency}|p${summary.pageNumber}|${summary.declaredTotal.renderMoney()}")
    summary.allocations.forEach { allocation ->
        appendLine("allocation|${allocation.labelRaw}|${allocation.currency}|${allocation.amount.renderMoney()}")
    }
}

private fun StringBuilder.appendSection(section: CanonicalSection) {
    appendLine(
        "section|${section.category}|${section.currency}|p${section.pageNumber}|" +
            section.declaredGross.renderMoney(),
    )
    section.declaredSubtotal?.let { subtotal ->
        appendLine(
            "subtotal|${subtotal.applied.renderMoney()}|${subtotal.gross.renderMoney()}|" +
                subtotal.net.renderMoney(),
        )
    }
    section.candidates.forEach { candidate -> appendCandidate(candidate) }
}

private fun StringBuilder.appendCandidate(candidate: PositionCandidate) {
    appendLine(
        listOf(
            "candidate",
            candidate.category.name,
            candidate.identity.descriptionRaw,
            candidate.identity.assetCodeRaw ?: "<none>",
            candidate.identity.indexerRaw ?: "<none>",
            "p${candidate.identity.pageNumber}",
        ).joinToString("|"),
    )
    appendLine(
        "holding|${candidate.holding.quantity.render { quantity -> quantity.value.toPlainString() }}|" +
            candidate.holding.unitPrice.render { price -> price.value.toPlainString() },
    )
    appendAmounts(candidate.amounts)
    candidate.schedule?.let { schedule ->
        appendLine(
            listOf(
                "schedule",
                schedule.applicationDate.render { date -> date.toString() },
                schedule.maturityDate.render { date -> date.toString() },
                schedule.rate.render { rate -> rate.value.toPlainString() },
            ).joinToString("|"),
        )
    }
}

private fun StringBuilder.appendAmounts(amounts: PositionAmounts) {
    appendLine(
        listOf(
            "amounts",
            amounts.gross.renderMoney(),
            amounts.applied.renderMoney(),
            amounts.net.renderMoney(),
            amounts.market.renderMoney(),
            amounts.redemptionAvailability.renderMoney(),
        ).joinToString("|"),
    )
    amounts.taxes?.let { taxes ->
        appendLine("taxes|${taxes.expectedIof.renderMoney()}|${taxes.expectedIr.renderMoney()}")
    }
}

private fun EvidencedValue<Money>?.renderMoney(): String = render { money -> "${money.amountMinor}${money.currency}" }

private fun <T : Any> EvidencedValue<T>?.render(value: (T) -> String): String =
    this?.let { evidenced -> "${evidenced.value.renderQuality(value)}@p${evidenced.evidence.pageNumber}" }
        ?: "<none>"

private fun <T : Any> ValueQuality<T>.renderQuality(render: (T) -> String): String =
    when (this) {
        ValueQuality.Unknown -> "<unknown>"
        is ValueQuality.Estimated -> "~${render(value)}"
        is ValueQuality.Exact -> render(value)
    }
