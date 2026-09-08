package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.audit.AuditActor
import br.com.vinicius.personalfinance.fixtures.FixturePdfBuilder
import br.com.vinicius.personalfinance.fixtures.InterPositionFixture
import br.com.vinicius.personalfinance.portfolio.DeterministicAssetResolver
import br.com.vinicius.personalfinance.shared.DomainClock
import br.com.vinicius.personalfinance.shared.ImportBatchId
import br.com.vinicius.personalfinance.shared.ValueQuality
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestConstructor
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.security.MessageDigest
import java.time.Instant

@Testcontainers
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ImportPreviewPersistenceTests(
    private val lifecycle: ImportBatchLifecycleService,
    private val batches: ImportBatchRepository,
    private val previews: ImportPreviewRepository,
    private val jdbcTemplate: JdbcTemplate,
) {
    private val extractor = PdfBoxTextExtractor()
    private val parser = InterPositionDocumentParser()
    private val normalizer = InterPositionNormalizer()
    private val resolutionService = PositionAssetResolutionService(DeterministicAssetResolver())
    private val reconciler = PositionReconciler()
    private val builder = ImportPreviewBuilder(DomainClock.fixed(Instant.parse("2026-02-03T10:00:00Z")))

    private fun digestOf(seed: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(seed.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun batchAtPreviewReady(seed: String): ImportBatch {
        var batch =
            lifecycle.receive(
                id = ImportBatchId.random(),
                rawSha256 = digestOf(seed),
                actor = AuditActor.LOCAL_OWNER,
            )
        listOf(
            ImportBatchStatus.FINGERPRINTED,
            ImportBatchStatus.EXTRACTING,
            ImportBatchStatus.LAYOUT_DETECTED,
            ImportBatchStatus.PARSING,
            ImportBatchStatus.NORMALIZING,
            ImportBatchStatus.RECONCILING,
        ).forEach { status ->
            batch = lifecycle.transition(batch.id, status, AuditActor.LOCAL_OWNER)
        }
        return lifecycle.transition(batch.id, ImportBatchStatus.PREVIEW_READY, AuditActor.LOCAL_OWNER)
    }

    private fun buildPreview(
        batch: ImportBatch,
        case: InterPositionFixture.Case = InterPositionFixture.complete,
    ): ImportPreview {
        val password = if (case.protected) DocumentPassword.of(InterPositionFixture.TEST_PASSWORD) else null
        val document = normalizer.normalize(parser.parse(extractor.extract(FixturePdfBuilder.build(case), password)))
        return builder.build(
            importBatchId = batch.id,
            previewVersion = batch.previewVersion,
            resolved = resolutionService.resolve(document),
            reconciliation = reconciler.reconcile(document),
        )
    }

    /**
     * The only legal way to withdraw an offer and mint the next one.
     *
     * Re-reconciling is what makes the previous version detectably stale: while
     * the batch is back in `RECONCILING` nothing can commit, so there is no
     * window where a superseded proposal looks current.
     */
    private fun supersede(batch: ImportBatch): ImportBatch {
        lifecycle.transition(batch.id, ImportBatchStatus.RECONCILING, AuditActor.LOCAL_OWNER)
        return lifecycle.transition(batch.id, ImportBatchStatus.PREVIEW_READY, AuditActor.LOCAL_OWNER)
    }

    @Test
    fun `a stored preview reads back with its positions, findings and evidence`() {
        val batch = batchAtPreviewReady("preview-roundtrip")
        val stored = buildPreview(batch)

        previews.insert(stored)
        val loaded = previews.find(batch.id, batch.previewVersion)

        assertNotNull(loaded)
        assertEquals(stored.positions.size, loaded!!.positions.size)
        assertEquals(stored.findings.size, loaded.findings.size)
        assertEquals(stored.source, loaded.source)
        assertEquals(stored.temporality.positionDate, loaded.temporality.positionDate)
        assertEquals(stored.declaredPositionTotal, loaded.declaredPositionTotal)
        assertEquals(stored.canCommit, loaded.canCommit)
    }

    @Test
    fun `an unknown amount survives the round trip as unknown and not as zero`() {
        val batch = batchAtPreviewReady("preview-unknown")
        val stored = buildPreview(batch, InterPositionFixture.malformedValue)

        previews.insert(stored)
        val loaded = previews.find(batch.id, batch.previewVersion)!!
        val unreadable = loaded.positions.first { position -> position.gross is ValueQuality.Unknown }

        assertEquals("R$ 1.25X,00", unreadable.evidence.tokenRaw)
        assertFalse(loaded.canCommit)
        assertEquals(
            0L,
            jdbcTemplate.queryForObject(
                """
                select count(*) from import_preview_position
                where gross_quality = 'UNKNOWN' and gross_minor is not null
                """.trimIndent(),
                Long::class.java,
            ),
            "the schema must refuse an unknown amount that carries a value",
        )
    }

    @Test
    fun `a preview version is written once and never rewritten`() {
        val batch = batchAtPreviewReady("preview-immutable")
        val stored = buildPreview(batch)

        previews.insert(stored)

        // FR-PREVIEW-002: a version identifies one proposal. Offering a different
        // one under the same number would make a confirmed decision unexplainable.
        assertThrows(Exception::class.java) { previews.insert(stored.copy(id = java.util.UUID.randomUUID())) }
    }

    @Test
    fun `a superseded preview stays readable while only the latest is on offer`() {
        val first = batchAtPreviewReady("preview-superseded")
        previews.insert(buildPreview(first))

        val refreshed = supersede(first)
        previews.insert(buildPreview(refreshed, InterPositionFixture.controlledMismatch))

        assertEquals(1, first.previewVersion)
        assertEquals(2, refreshed.previewVersion)
        assertNotNull(previews.find(first.id, 1))
        assertEquals(2, previews.findLatest(first.id)?.previewVersion)
    }

    @Test
    fun `committing a superseded preview version is refused as stale`() {
        val first = batchAtPreviewReady("preview-stale")
        previews.insert(buildPreview(first))
        val refreshed = supersede(first)

        val failure =
            assertThrows(ImportLifecycleException::class.java) {
                refreshed.startCommit(first.previewVersion, DomainClock.systemUtc())
            }

        assertEquals(ImportErrorCode.PREVIEW_VERSION_CONFLICT, failure.code)
        assertEquals(ImportBatchStatus.PREVIEW_READY, batches.findById(first.id)?.status)
        assertEquals(refreshed.previewVersion, batches.findById(first.id)?.previewVersion)
    }

    @Test
    fun `storing a preview writes no portfolio state`() {
        val batch = batchAtPreviewReady("preview-no-portfolio")
        previews.insert(buildPreview(batch))

        // FR-PREVIEW-004: a proposal is not a snapshot. Release 0.1 has no
        // confirmed position table yet, and this test fails the moment building
        // a preview starts writing into one.
        val portfolioTables =
            jdbcTemplate.queryForObject(
                """
                select count(*) from information_schema.tables
                where table_schema = 'public'
                  and table_name in ('position_snapshot', 'asset', 'financial_account')
                """.trimIndent(),
                Long::class.java,
            )

        assertEquals(0L, portfolioTables)
        assertTrue(previews.findLatest(batch.id) != null)
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
