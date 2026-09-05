package br.com.vinicius.personalfinance.ingestion

/**
 * A PDF password, held in memory and never anywhere else.
 *
 * `FR-PASSWORD-003` forbids persisting it and `FR-PASSWORD-004` forbids it
 * appearing in logs, traces, metrics or responses. Two habits enforce that
 * here:
 *
 * - [toString] is redacted, so an accidental string interpolation in a log
 *   statement cannot leak it;
 * - [clear] zeroes the backing array, so the value stops existing in the heap
 *   once the continuation is done with it.
 *
 * A `String` would defeat both: it is immutable, interned in places, and prints
 * itself.
 */
class DocumentPassword(
    value: CharArray,
) {
    private val chars: CharArray = value.copyOf()

    private var cleared: Boolean = false

    init {
        require(value.isNotEmpty()) { "document password must not be empty" }
    }

    /** Runs [block] with the raw characters. The array must not escape. */
    fun <T> use(block: (CharArray) -> T): T {
        check(!cleared) { "document password was already cleared" }
        return block(chars.copyOf())
    }

    fun clear() {
        chars.fill('\u0000')
        cleared = true
    }

    /** Never the value. */
    override fun toString(): String = REDACTED

    companion object {
        const val REDACTED: String = "[REDACTED]"

        fun of(raw: String): DocumentPassword = DocumentPassword(raw.toCharArray())
    }
}
