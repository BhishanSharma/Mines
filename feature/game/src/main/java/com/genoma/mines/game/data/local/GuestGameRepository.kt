package com.genoma.mines.game.data.local
import com.genoma.mines.game.data.GameResult
import kotlinx.coroutines.flow.Flow

class GuestGameRepository(
    private val dao: GuestGameDao
) {

    suspend fun saveGameResult(gameResult: GameResult) {
        dao.insert(
            GuestGame(
                difficulty = gameResult.difficulty,
                score = gameResult.score,
                result = gameResult.result,
                duration = gameResult.durationSeconds,
                createdAt = gameResult.createdAt
            )
        )
    }

    fun observeHistory(): Flow<List<GuestGame>> = dao.observeAll()

    suspend fun getHistory(): List<GuestGame> = dao.getAll()

    suspend fun clearHistory() = dao.clearAll()
}