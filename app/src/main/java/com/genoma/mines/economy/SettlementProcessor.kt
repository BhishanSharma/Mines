package com.genoma.mines.economy

import android.util.Log
import com.genoma.mines.core.economy.PendingSettlement
import com.genoma.mines.life.data.LifeDataStore
import com.genoma.mines.session.SessionManager
import com.genoma.mines.session.UserSession
import com.genoma.mines.wallet.data.CoinWalletDataStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SettlementResult {
    /** Applied by this call. */
    data object Applied : SettlementResult

    /** Already applied earlier (duplicate callback or retry); nothing changed. */
    data object AlreadyApplied : SettlementResult

    /** Saved and waiting: offline, a transient error, or a different account is signed in. */
    data object Queued : SettlementResult
}

/**
 * Applies game results and rewards exactly once, for guests and accounts.
 *
 * Every settlement is first persisted in [PendingSettlementStore], then
 * applied with its idempotency key, then removed from the queue. Anything
 * that fails stays queued and is retried by [processPending] on app start,
 * sign-in, return to the foreground and network reconnect.
 */
class SettlementProcessor(
    private val sessionManager: SessionManager,
    private val queue: PendingSettlementStore,
    private val guestWallet: CoinWalletDataStore,
    private val guestLife: LifeDataStore,
    private val firestoreSettlements: FirestoreSettlementRepository
) {

    private val mutex = Mutex()

    /** Owner id for a settlement earned right now. */
    fun currentOwner(): String =
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> session.firebaseUid
            UserSession.Guest -> PendingSettlement.GUEST_OWNER
        }

    suspend fun submit(settlement: PendingSettlement): SettlementResult = mutex.withLock {
        queue.add(settlement)
        applyAndDequeue(settlement)
    }

    /** Retries everything that is still queued. Returns how many were applied. */
    suspend fun processPending(): Int = mutex.withLock {
        var applied = 0
        for (settlement in queue.all()) {
            if (applyAndDequeue(settlement) == SettlementResult.Applied) applied++
        }
        applied
    }

    private suspend fun applyAndDequeue(settlement: PendingSettlement): SettlementResult {
        val result = try {
            apply(settlement)
        } catch (exception: Exception) {
            Log.w(TAG, "Settlement ${settlement.id} not applied yet; will retry", exception)
            SettlementResult.Queued
        }

        if (result != SettlementResult.Queued) {
            queue.remove(settlement.id)
        }
        return result
    }

    private suspend fun apply(settlement: PendingSettlement): SettlementResult {
        if (settlement.owner == PendingSettlement.GUEST_OWNER) {
            val wallet = guestWallet.applySettlement(settlement)
            val life = if (settlement.plan.lifeDebit > 0 || settlement.plan.lifeCredit > 0) {
                guestLife.applyIdempotent(
                    operationId = settlement.id,
                    lifeDebit = settlement.plan.lifeDebit,
                    lifeCredit = settlement.plan.lifeCredit
                )
            } else {
                false
            }
            return if (wallet || life) SettlementResult.Applied else SettlementResult.AlreadyApplied
        }

        val session = sessionManager.currentSession
        if (session !is UserSession.Authenticated || session.firebaseUid != settlement.owner) {
            // Earned by an account that is not signed in right now; keep it for later.
            return SettlementResult.Queued
        }

        val appliedNow = firestoreSettlements.apply(session.firebaseUid, settlement)

        return if (appliedNow) SettlementResult.Applied else SettlementResult.AlreadyApplied
    }

    private companion object {
        const val TAG = "SettlementProcessor"
    }
}
