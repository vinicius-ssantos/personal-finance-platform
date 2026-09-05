package br.com.vinicius.personalfinance.audit

/**
 * Keeps secrets and financial values out of the audit trail.
 *
 * The Release 0.1 security model forbids the PDF password from reaching logs,
 * traces, metrics, the database or responses, and the audit trail must not
 * become a side channel for balances either. This object is deliberately
 * conservative: an unknown key that *looks* sensitive is redacted rather than
 * stored, because a false redaction costs debuggability while a false negative
 * costs a leak.
 */
object AuditRedaction {
    const val PLACEHOLDER: String = "[REDACTED]"

    const val MAX_VALUE_LENGTH: Int = 512

    private val SENSITIVE_KEY_FRAGMENTS =
        listOf(
            "password",
            "senha",
            "secret",
            "token",
            "credential",
            "authorization",
            "cookie",
            "apikey",
            "api_key",
            "amount",
            "valor",
            "balance",
            "saldo",
            "quantity",
            "quantidade",
            "price",
            "preco",
            "total",
            "cpf",
            "cnpj",
        )

    /** True when [key] names something that must never be stored verbatim. */
    fun isSensitive(key: String): Boolean {
        val normalized = key.lowercase().replace("-", "").replace("_", "")
        return SENSITIVE_KEY_FRAGMENTS.any { fragment ->
            normalized.contains(fragment.replace("_", ""))
        }
    }

    /** Returns [details] with sensitive values replaced and long values truncated. */
    fun redact(details: Map<String, String>): Map<String, String> =
        details.entries.associate { (key, value) ->
            key to
                when {
                    isSensitive(key) -> PLACEHOLDER
                    value.length > MAX_VALUE_LENGTH -> value.take(MAX_VALUE_LENGTH)
                    else -> value
                }
        }

    /**
     * Fails when [details] still carries a sensitive value.
     *
     * [AuditEvent.of] redacts before constructing, so this guards the direct
     * constructor and any future call site that forgets to redact.
     */
    fun requireSafe(details: Map<String, String>) {
        val leaked =
            details.entries
                .filter { (key, value) -> isSensitive(key) && value != PLACEHOLDER }
                .map { it.key }
        require(leaked.isEmpty()) {
            "audit details must not carry sensitive values for keys: ${leaked.sorted()}"
        }
        val oversized = details.entries.filter { it.value.length > MAX_VALUE_LENGTH }.map { it.key }
        require(oversized.isEmpty()) {
            "audit detail values must not exceed $MAX_VALUE_LENGTH characters: ${oversized.sorted()}"
        }
    }
}
