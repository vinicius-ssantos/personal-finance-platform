package br.com.vinicius.personalfinance.ingestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportErrorCatalogTests {
    @Test
    fun `every code follows the PF_AREA_ERROR convention`() {
        val pattern = Regex("^PF_[A-Z]+_[A-Z_]+$")

        ImportErrorCode.entries.forEach { entry ->
            assertTrue(pattern.matches(entry.code), "${entry.code} breaks the convention")
        }
    }

    @Test
    fun `codes are unique and resolvable`() {
        val codes = ImportErrorCode.entries.map { it.code }

        assertEquals(codes.size, codes.toSet().size, "duplicate error codes: $codes")
        codes.forEach { code -> assertNotNull(ImportErrorCode.ofCode(code)) }
    }

    @Test
    fun `an unknown code resolves to null instead of a guess`() {
        assertEquals(null, ImportErrorCode.ofCode("PF_NOT_A_REAL_CODE"))
    }

    @Test
    fun `severity is independent from the code`() {
        val severities = ImportErrorCode.entries.map { it.severity }.toSet()

        assertTrue(severities.size > 1, "a catalogue with one severity collapses the concepts")
    }

    @Test
    fun `retry class is declared and never inferred from the HTTP status`() {
        val byStatus = ImportErrorCode.entries.groupBy { it.httpStatus }
        val statusWithSeveralRetryClasses =
            byStatus.filterValues { entries -> entries.map { it.retryClass }.toSet().size > 1 }

        assertTrue(
            statusWithSeveralRetryClasses.isNotEmpty(),
            "if every status implied one retry class, the catalogue would be redundant",
        )
    }

    @Test
    fun `secrets never travel in error details`() {
        val secretCarrying =
            listOf(
                ImportErrorCode.PDF_PASSWORD_REQUIRED,
                ImportErrorCode.PDF_INVALID_PASSWORD,
                ImportErrorCode.PDF_PASSWORD_ATTEMPTS_EXCEEDED,
            )

        secretCarrying.forEach { entry ->
            assertEquals(DetailExposure.NONE, entry.detailExposure, "${entry.code} may leak")
        }
    }

    @Test
    fun `internal failures stay opaque to the client`() {
        assertEquals(DetailExposure.NONE, ImportErrorCode.INTERNAL_ERROR.detailExposure)
        assertEquals(500, ImportErrorCode.INTERNAL_ERROR.httpStatus)
    }
}
