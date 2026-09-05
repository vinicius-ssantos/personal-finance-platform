package br.com.vinicius.personalfinance.ingestion

import java.security.MessageDigest
import java.util.Locale

/**
 * Digest of what a document *says*, as opposed to the bytes it is made of.
 *
 * The raw digest changes when the same report is exported twice, because
 * producers embed timestamps and object ids. This one survives that: it
 * canonicalises the text first, so two exports of the same statement collapse
 * to the same value while a genuinely different statement does not.
 *
 * Canonicalisation is deliberately blunt — case folded, whitespace collapsed,
 * blank lines dropped. `NFR-PARSER-DET-003` forbids depending on the JVM
 * default locale, so [Locale.ROOT] is explicit rather than implied.
 */
object SemanticFingerprint {
    const val ALGORITHM: String = "SHA-256"

    fun of(document: ExtractedDocument): String = ofText(document.fullText())

    fun ofText(text: String): String =
        MessageDigest
            .getInstance(ALGORITHM)
            .digest(canonicalize(text).toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    /** The canonical form the digest is taken over. Exposed so tests can pin it. */
    fun canonicalize(text: String): String =
        text
            .lowercase(Locale.ROOT)
            .lineSequence()
            .map { line -> line.replace(WHITESPACE_RUN, " ").trim() }
            .filter { line -> line.isNotEmpty() }
            .joinToString(separator = "\n")

    private val WHITESPACE_RUN = Regex("""\s+""")
}
