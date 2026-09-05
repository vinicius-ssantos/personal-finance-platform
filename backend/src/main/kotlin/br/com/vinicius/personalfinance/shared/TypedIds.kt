package br.com.vinicius.personalfinance.shared

import java.util.UUID

sealed interface TypedId {
    val value: UUID
}

@JvmInline
value class ImportBatchId(
    override val value: UUID,
) : TypedId,
    Comparable<ImportBatchId> {
    override fun compareTo(other: ImportBatchId): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()

    companion object {
        fun random(): ImportBatchId = ImportBatchId(UUID.randomUUID())

        fun parse(raw: String): ImportBatchId = ImportBatchId(UUID.fromString(raw))
    }
}

@JvmInline
value class FinancialAccountId(
    override val value: UUID,
) : TypedId,
    Comparable<FinancialAccountId> {
    override fun compareTo(other: FinancialAccountId): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()

    companion object {
        fun random(): FinancialAccountId = FinancialAccountId(UUID.randomUUID())

        fun parse(raw: String): FinancialAccountId = FinancialAccountId(UUID.fromString(raw))
    }
}

@JvmInline
value class AssetId(
    override val value: UUID,
) : TypedId,
    Comparable<AssetId> {
    override fun compareTo(other: AssetId): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()

    companion object {
        fun random(): AssetId = AssetId(UUID.randomUUID())

        fun parse(raw: String): AssetId = AssetId(UUID.fromString(raw))
    }
}

@JvmInline
value class PositionSnapshotId(
    override val value: UUID,
) : TypedId,
    Comparable<PositionSnapshotId> {
    override fun compareTo(other: PositionSnapshotId): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()

    companion object {
        fun random(): PositionSnapshotId = PositionSnapshotId(UUID.randomUUID())

        fun parse(raw: String): PositionSnapshotId = PositionSnapshotId(UUID.fromString(raw))
    }
}
