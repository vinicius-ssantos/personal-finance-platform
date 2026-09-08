package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.audit.AuditActor
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestConstructor
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.io.ByteArrayOutputStream

private val TEST_LAYOUT = LayoutDescriptor("TEST_BANK", "POSITION_CONSOLIDATED", "2026.1")

private const val TEST_MARKER = "POSICAO CONSOLIDADA TESTE"

/**
 * Stands in for the Banco Inter detector, which arrives with its own issue.
 * Registering one here proves the wiring end to end without pre-empting that
 * work.
 */
@TestConfiguration
class StubLayoutConfiguration {
    @Bean
    fun stubDetector(): LayoutDetector =
        object : LayoutDetector {
            override val descriptor = TEST_LAYOUT

            override fun detect(document: ExtractedDocument): LayoutEvidence? =
                if (document.fullText().contains(TEST_MARKER)) {
                    LayoutEvidence(TEST_LAYOUT, confidence = 0.99, markers = listOf("header"))
                } else {
                    null
                }
        }

    @Bean
    fun stubParser(): DocumentParser =
        object : DocumentParser {
            override val descriptor = TEST_LAYOUT
            override val parserId = "test-position-parser"
            override val parserVersion = "1.4.2"

            override fun parse(document: ExtractedDocument): ParsedSourceDocument = object : ParsedSourceDocument {}
        }
}

@Testcontainers
@SpringBootTest
@Import(StubLayoutConfiguration::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class DocumentExtractionTests(
    private val intake: DocumentIntakeService,
    private val extraction: DocumentExtractionService,
    private val repository: ImportBatchRepository,
) {
    private fun pdfWith(lines: List<String>): ByteArray {
        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                stream.newLineAtOffset(40f, 750f)
                stream.setLeading(14f)
                lines.forEach { line ->
                    stream.showText(line)
                    stream.newLine()
                }
                stream.endText()
            }
            val out = ByteArrayOutputStream()
            document.save(out)
            return out.toByteArray()
        }
    }

    /** Enough text to clear the structured-text threshold. */
    private fun recognisableReport(): ByteArray =
        pdfWith(listOf(TEST_MARKER) + (1..30).map { index -> "Ativo $index quantidade $index" })

    @Test
    fun `a recognised document reaches LAYOUT_DETECTED with its parser persisted`() {
        val received = intake.receive(recognisableReport(), AuditActor.LOCAL_OWNER)

        val detected = extraction.extractAndSelect(received, AuditActor.LOCAL_OWNER)

        assertEquals(ImportBatchStatus.LAYOUT_DETECTED, detected.status)
        val reloaded = repository.findById(detected.id)
        assertNotNull(reloaded?.parser)
        assertEquals(TEST_LAYOUT, reloaded?.parser?.layout)
        assertEquals("test-position-parser", reloaded?.parser?.parserId)
        assertEquals("1.4.2", reloaded?.parser?.parserVersion)
    }

    @Test
    fun `the semantic fingerprint is persisted alongside the raw digest`() {
        val content = recognisableReport()
        val received = intake.receive(content, AuditActor.LOCAL_OWNER)

        val detected = extraction.extractAndSelect(received, AuditActor.LOCAL_OWNER)
        val reloaded = repository.findById(detected.id)

        assertEquals(SourceFingerprint.of(content), reloaded?.rawSha256)
        assertNotNull(reloaded?.semanticFingerprint)
        assertEquals(64, reloaded?.semanticFingerprint?.length)
    }

    @Test
    fun `an unrecognised document fails closed instead of picking a parser`() {
        val received =
            intake.receive(
                pdfWith((1..30).map { index -> "Documento desconhecido linha $index" }),
                AuditActor.LOCAL_OWNER,
            )

        val failure =
            assertThrows(ImportLifecycleException::class.java) {
                extraction.extractAndSelect(received, AuditActor.LOCAL_OWNER)
            }

        assertEquals(ImportErrorCode.LAYOUT_UNSUPPORTED, failure.code)
        assertEquals(ImportBatchStatus.FAILED, repository.findById(received.id)?.status)
    }

    @Test
    fun `a document without enough text fails safely rather than parsing as empty`() {
        val received = intake.receive(pdfWith(listOf("x")), AuditActor.LOCAL_OWNER)

        val failure =
            assertThrows(ImportLifecycleException::class.java) {
                extraction.extractAndSelect(received, AuditActor.LOCAL_OWNER)
            }

        assertEquals(ImportErrorCode.PDF_EXTRACTION_FAILED, failure.code)
        assertEquals(ImportBatchStatus.FAILED, repository.findById(received.id)?.status)
    }

    private companion object {
        private const val POSTGRES_IMAGE = "postgres:18.1-alpine"

        @Container
        @ServiceConnection
        @JvmField
        val postgres =
            PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE))
                .withDatabaseName("personal_finance_test")
                .withUsername("personal_finance_test")
                .withPassword("test-only")
    }
}
