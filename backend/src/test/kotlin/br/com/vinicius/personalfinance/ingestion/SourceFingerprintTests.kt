package br.com.vinicius.personalfinance.ingestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SourceFingerprintTests {
    @Test
    fun `identical bytes always produce the same digest`() {
        val content = "%PDF-1.7 same bytes".toByteArray()

        assertEquals(SourceFingerprint.of(content), SourceFingerprint.of(content.copyOf()))
    }

    @Test
    fun `a single changed byte changes the digest`() {
        val first = SourceFingerprint.of("%PDF-1.7 a".toByteArray())
        val second = SourceFingerprint.of("%PDF-1.7 b".toByteArray())

        assertNotEquals(first, second)
    }

    @Test
    fun `the digest is lowercase hex of the expected width`() {
        val digest = SourceFingerprint.of("%PDF-1.7".toByteArray())

        assertTrue(Regex("^[0-9a-f]{64}$").matches(digest), "unexpected digest shape: $digest")
    }

    @Test
    fun `the digest matches the published SHA-256 of an empty input`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            SourceFingerprint.of(ByteArray(0)),
        )
    }
}
