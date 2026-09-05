package br.com.vinicius.personalfinance.ingestion

/**
 * Identity of a document layout (`FR-LAYOUT-001`).
 *
 * The triple is the registry key. A producer changing its report format means a
 * new [layoutVersion], never a silent reinterpretation of the old one.
 */
data class LayoutDescriptor(
    val institution: String,
    val documentFamily: String,
    val layoutVersion: String,
) {
    init {
        require(institution.isNotBlank()) { "institution must not be blank" }
        require(documentFamily.isNotBlank()) { "documentFamily must not be blank" }
        require(layoutVersion.isNotBlank()) { "layoutVersion must not be blank" }
    }

    override fun toString(): String = "$institution/$documentFamily@$layoutVersion"
}

/**
 * Why a detector believes a document has a given layout (`FR-LAYOUT-004`).
 *
 * [confidence] is a ratio in `0.0..1.0`. [markers] names the constructs that
 * matched — never document content, so evidence can be logged safely.
 */
data class LayoutEvidence(
    val descriptor: LayoutDescriptor,
    val confidence: Double,
    val markers: List<String>,
) {
    init {
        require(confidence in MINIMUM_CONFIDENCE..MAXIMUM_CONFIDENCE) {
            "confidence must be within $MINIMUM_CONFIDENCE..$MAXIMUM_CONFIDENCE, got $confidence"
        }
        require(markers.isNotEmpty()) { "evidence must name at least one marker" }
    }

    companion object {
        const val MINIMUM_CONFIDENCE: Double = 0.0

        const val MAXIMUM_CONFIDENCE: Double = 1.0
    }
}

/**
 * Recognises one layout.
 *
 * Implementations must be deterministic: same document, same verdict, on every
 * run and on every machine. `NFR-PARSER-DET-002` and `NFR-PARSER-DET-003`
 * forbid consulting the current time or the JVM default locale to decide, and
 * `NFR-PARSER-DET-004` forbids asking an unversioned external service.
 *
 * A detector returns null when it does not recognise the document. It must not
 * return low-confidence evidence to "have a go": the selector treats any
 * evidence as a claim.
 */
interface LayoutDetector {
    val descriptor: LayoutDescriptor

    fun detect(document: ExtractedDocument): LayoutEvidence?
}
