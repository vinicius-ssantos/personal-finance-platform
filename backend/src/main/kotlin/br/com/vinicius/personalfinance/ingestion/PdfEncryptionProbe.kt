package br.com.vinicius.personalfinance.ingestion

import org.springframework.stereotype.Component

/**
 * Cheap check for whether a PDF is encrypted.
 *
 * An encrypted PDF carries an `/Encrypt` entry in its trailer. Looking for it
 * is enough to route the import into `PASSWORD_REQUIRED` before a real parser
 * exists (`FR-PASSWORD-001`).
 *
 * This is a routing hint, not an authority: the extraction step with PDFBox is
 * what actually decides whether the password opens the document. A false
 * positive costs one unnecessary password prompt; a false negative surfaces as
 * a normal extraction failure. Neither is a security boundary.
 */
@Component
class PdfEncryptionProbe {
    fun looksEncrypted(content: ByteArray): Boolean = String(content, Charsets.ISO_8859_1).contains(ENCRYPT_MARKER)

    private companion object {
        const val ENCRYPT_MARKER = "/Encrypt"
    }
}
