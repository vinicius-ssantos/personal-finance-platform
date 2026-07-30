package br.com.vinicius.personalfinance.shared

sealed interface ValueQuality<out T : Any> {
    data object Unknown : ValueQuality<Nothing>

    data class Estimated<T : Any>(
        val value: T,
    ) : ValueQuality<T>

    data class Exact<T : Any>(
        val value: T,
    ) : ValueQuality<T>
}

fun <T : Any> ValueQuality<T>.valueOrNull(): T? =
    when (this) {
        ValueQuality.Unknown -> null
        is ValueQuality.Estimated -> value
        is ValueQuality.Exact -> value
    }

inline fun <T : Any, R : Any> ValueQuality<T>.map(
    transform: (T) -> R,
): ValueQuality<R> =
    when (this) {
        ValueQuality.Unknown -> ValueQuality.Unknown
        is ValueQuality.Estimated -> ValueQuality.Estimated(transform(value))
        is ValueQuality.Exact -> ValueQuality.Exact(transform(value))
    }
