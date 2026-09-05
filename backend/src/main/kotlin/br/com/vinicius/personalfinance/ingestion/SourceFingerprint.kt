package br.com.vinicius.personalfinance.ingestion

import java.security.MessageDigest

/**
 * Content digest of a received document.
 *
 * `FR-IDEMP-001` requires the fingerprint to survive a restart, so it is
 * derived from the bytes alone — never from a filename, an upload timestamp or
 * anything else that changes between runs. Identical bytes always produce the
 * same digest.
 */
object SourceFingerprint {
    const val ALGORITHM: String = "SHA-256"

    fun of(content: ByteArray): String =
        MessageDigest
            .getInstance(ALGORITHM)
            .digest(content)
            .joinToString("") { byte -> "%02x".format(byte) }
}
