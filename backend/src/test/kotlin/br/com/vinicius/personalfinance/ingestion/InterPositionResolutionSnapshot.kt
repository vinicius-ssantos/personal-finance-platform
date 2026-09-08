package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.portfolio.AssetResolution
import br.com.vinicius.personalfinance.portfolio.StrongIdentifier

/**
 * Renders identity decisions as reviewable text.
 *
 * The golden records the decision and its reason, not only the outcome: a change
 * from "resolved" to "needs review" and a change in *why* it needs review are
 * different regressions, and a diff should say which one happened.
 */
internal fun renderResolutionSnapshot(document: ResolvedPositionDocument): String =
    buildString {
        document.candidates.forEach { entry ->
            appendLine(
                listOf(
                    "claim",
                    entry.fieldPath,
                    entry.claim.nameRaw,
                    entry.claim.type?.name ?: "<untyped>",
                    entry.claim.currency.name,
                    entry.claim.strongIdentifiers.joinToString(",", "[", "]", transform = ::renderIdentifier),
                ).joinToString("|"),
            )
            appendLine("decision|${entry.fieldPath}|${renderResolution(entry.resolution)}")
        }
        document.reviewRequired.forEach { item ->
            appendLine("review|${item.fieldPath}|p${item.pageNumber}|${item.reason}|${item.confidence}")
        }
    }.trimEnd()

private fun renderIdentifier(identifier: StrongIdentifier): String =
    when (identifier) {
        is StrongIdentifier.Ticker -> "ticker(${identifier.symbol}@${identifier.exchange})"
        is StrongIdentifier.InstitutionalCode -> "code(${identifier.institution}:${identifier.code})"
        is StrongIdentifier.FundTaxId -> "taxid(${identifier.taxId})"
    }

private fun renderResolution(resolution: AssetResolution): String =
    when (resolution) {
        is AssetResolution.Resolved ->
            "RESOLVED|${resolution.confidence}|${resolution.type}|" +
                "${renderIdentifier(resolution.identifier)}|${resolution.fingerprint.value}"

        is AssetResolution.ReviewRequired ->
            "REVIEW|${resolution.confidence}|${resolution.reason}|${resolution.fingerprint.value}"
    }
