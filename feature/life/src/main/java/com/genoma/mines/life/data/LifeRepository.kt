package com.genoma.mines.life.data
import com.genoma.mines.life.domain.LifeSnapshot
import com.genoma.mines.life.domain.LifeStatus
import kotlinx.coroutines.flow.Flow

interface LifeRepository {
    /** Raw persisted hearts; turn into a live count with LifeCalculator / rememberLifeStatus. */
    val lifeSnapshot: Flow<LifeSnapshot>

    suspend fun getStatus(): LifeStatus

    /** Spends one heart. Returns false (and changes nothing) when there are none left. */
    suspend fun consumeHeart(): Boolean

    suspend fun refundHeart()

    suspend fun refillHearts()
}
