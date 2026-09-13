package com.genoma.mines.game.data
interface GameRepository {
    suspend fun saveGameResult(gameResult: GameResult)
    suspend fun getGameHistory(): List<GameHistoryItem>
    suspend fun getStatistics(): UserStatistics
}