package com.genoma.mines.data

import com.genoma.mines.data.local.GuestGameRepository
import com.genoma.mines.data.remote.FirestoreGameRepository
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameResultType
import com.genoma.mines.session.SessionManager
import com.genoma.mines.session.UserSession

class GameRepositoryImpl(
    private val sessionManager: SessionManager,
    private val guestRepository: GuestGameRepository,
    private val firestoreRepository: FirestoreGameRepository
) : GameRepository {

    override suspend fun saveGameResult(gameResult: GameResult) {
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> {
                firestoreRepository.saveGameResult(session.firebaseUid, gameResult)
            }

            UserSession.Guest -> {
                guestRepository.saveGameResult(gameResult)
            }
        }
    }

    override suspend fun getGameHistory(): List<GameHistoryItem> {
        return when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> {
                firestoreRepository.getGameHistory(session.firebaseUid).map { entry ->
                    GameHistoryItem(
                        difficulty = Difficulty.valueOf(entry.difficulty),
                        score = entry.score,
                        result = GameResultType.valueOf(entry.result),
                        durationSeconds = entry.duration,
                        createdAtMillis = entry.createdAt?.time
                            ?: System.currentTimeMillis()
                    )
                }
            }

            UserSession.Guest -> {
                guestRepository.getHistory().map { game ->
                    GameHistoryItem(
                        difficulty = game.difficulty,
                        score = game.score,
                        result = game.result,
                        durationSeconds = game.duration,
                        createdAtMillis = game.createdAt
                    )
                }
            }
        }
    }

    override suspend fun getStatistics(): UserStatistics {
        val session = sessionManager.currentSession

        if (session !is UserSession.Authenticated) {
            return UserStatistics.EMPTY
        }

        val stats = firestoreRepository.getStatistics(session.firebaseUid)

        return UserStatistics(
            totalGames = stats.totalGames,
            totalWins = stats.totalWins,
            totalLosses = stats.totalLosses,
            totalScore = stats.totalScore,
            easy = DifficultyStatistics(stats.easyGames, stats.easyWins, stats.easyScore),
            medium = DifficultyStatistics(stats.mediumGames, stats.mediumWins, stats.mediumScore),
            hard = DifficultyStatistics(stats.hardGames, stats.hardWins, stats.hardScore)
        )
    }
}