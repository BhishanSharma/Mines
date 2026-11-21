package com.genoma.mines.core.economy

/** Why a wallet balance changed. Persisted by name; never rename entries. */
enum class WalletTransactionType(val label: String) {
    GAME_WIN("Game won"),
    GAME_LOSS("Game lost"),
    WIN_AD_BONUS("Win bonus ad"),
    LIFE_RECOVERY_AD("Life recovery ad"),
    LUCK_SPIN("Luck Spin"),
    COINS_TO_GEMS("Coins to Gems"),
    GEMS_TO_COINS("Gems to Coins"),
    STORE_PURCHASE("Store purchase"),
    HEART_REFILL("Heart refill"),
    REAL_MONEY_PURCHASE("Purchase"),
    GUEST_MIGRATION("Guest progress synced");

    companion object {
        fun fromName(name: String): WalletTransactionType? =
            entries.firstOrNull { it.name == name }
    }
}

/** One entry of the wallet history shown to the player. */
data class WalletTransaction(
    val id: String,
    val type: WalletTransactionType,
    val coinsDelta: Int,
    val gemsDelta: Int,
    val livesDelta: Int,
    val createdAtMillis: Long
)

/**
 * A wallet/Life change that must be applied exactly once.
 *
 * [id] is the idempotency key (for example `game-<uuid>`). It is stored
 * with the applied change, so retrying after a crash, a lost connection or
 * a duplicate callback never applies the change twice.
 *
 * [owner] is the Firebase uid, or [GUEST_OWNER] for on-device play. A
 * settlement is only ever applied to the account that earned it.
 */
data class PendingSettlement(
    val id: String,
    val owner: String,
    val type: WalletTransactionType,
    val plan: SettlementPlan,
    val createdAtMillis: Long
) {
    init {
        require(id.isNotBlank() && id.none { it == FIELD_SEPARATOR || it == '\n' })
        require(owner.isNotBlank() && owner.none { it == FIELD_SEPARATOR || it == '\n' })
    }

    companion object {
        const val GUEST_OWNER = "guest"
        internal const val FIELD_SEPARATOR = '|'
    }
}

/**
 * Line-based codec for on-device storage of pending settlements and wallet
 * history. Corrupt lines are skipped rather than crashing the app.
 */
object EconomyCodec {

    private const val SEP = PendingSettlement.FIELD_SEPARATOR

    fun encodeSettlements(items: List<PendingSettlement>): String =
        items.joinToString("\n") { item ->
            listOf(
                item.id,
                item.owner,
                item.type.name,
                item.plan.coinCredit,
                item.plan.coinDebit,
                item.plan.gemCredit,
                item.plan.gemDebit,
                item.plan.lifeDebit,
                item.plan.lifeCredit,
                item.createdAtMillis
            ).joinToString(SEP.toString())
        }

    fun decodeSettlements(raw: String?): List<PendingSettlement> {
        if (raw.isNullOrBlank()) return emptyList()

        return raw.lineSequence().mapNotNull { line ->
            runCatching {
                val f = line.split(SEP)
                if (f.size != 10) return@runCatching null

                PendingSettlement(
                    id = f[0],
                    owner = f[1],
                    type = WalletTransactionType.fromName(f[2]) ?: return@runCatching null,
                    plan = SettlementPlan(
                        coinCredit = f[3].toInt(),
                        coinDebit = f[4].toInt(),
                        gemCredit = f[5].toInt(),
                        gemDebit = f[6].toInt(),
                        lifeDebit = f[7].toInt(),
                        lifeCredit = f[8].toInt()
                    ),
                    createdAtMillis = f[9].toLong()
                )
            }.getOrNull()
        }.toList()
    }

    fun encodeTransactions(items: List<WalletTransaction>): String =
        items.joinToString("\n") { item ->
            require(item.id.none { it == SEP || it == '\n' })
            listOf(
                item.id,
                item.type.name,
                item.coinsDelta,
                item.gemsDelta,
                item.livesDelta,
                item.createdAtMillis
            ).joinToString(SEP.toString())
        }

    fun decodeTransactions(raw: String?): List<WalletTransaction> {
        if (raw.isNullOrBlank()) return emptyList()

        return raw.lineSequence().mapNotNull { line ->
            runCatching {
                val f = line.split(SEP)
                if (f.size != 6) return@runCatching null

                WalletTransaction(
                    id = f[0],
                    type = WalletTransactionType.fromName(f[1]) ?: return@runCatching null,
                    coinsDelta = f[2].toInt(),
                    gemsDelta = f[3].toInt(),
                    livesDelta = f[4].toInt(),
                    createdAtMillis = f[5].toLong()
                )
            }.getOrNull()
        }.toList()
    }
}
