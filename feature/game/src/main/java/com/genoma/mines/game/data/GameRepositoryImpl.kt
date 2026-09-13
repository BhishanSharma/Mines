package com.genoma.mines.game.data
import com.genoma.mines.game.data.local.GuestGame
import com.genoma.mines.game.data.local.GuestGameRepository
import com.genoma.mines.game.data.remote.FirestoreGameRepository
import com.genoma.mines.game.domain.Difficulty
import com.genoma.mines.game.domain.GameResultType
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
        return when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> {
                val stats = firestoreRepository.getStatistics(session.firebaseUid)

                UserStatistics(
                    totalGames = stats.totalGames,
                    totalWins = stats.totalWins,
                    totalLosses = stats.totalLosses,
                    // Score is floored at zero at write time now (see
                    // FirestoreGameRepository.saveGameResult), so this
                    // coerce is just a safety net for any older data
                    // written before that fix.
                    totalScore = stats.totalScore.coerceAtLeast(0),
                    easy = DifficultyStatistics(
                        stats.easyGames,
                        stats.easyWins,
                        stats.easyScore.coerceAtLeast(0)
                    ),
                    medium = DifficultyStatistics(
                        stats.mediumGames,
                        stats.mediumWins,
                        stats.mediumScore.coerceAtLeast(0)
                    ),
                    hard = DifficultyStatistics(
                        stats.hardGames,
                        stats.hardWins,
                        stats.hardScore.coerceAtLeast(0)
                    )
                )
            }

            UserSession.Guest -> {
                // Guest results live only in Room — nothing to fetch from
                // Firestore, so the totals are aggregated straight from
                // local history instead of always returning EMPTY.
                //
                // Score is floored chronologically (oldest game first),
                // not just on the final sum. That way, once a losing
                // streak has dragged the running total down to zero, the
                // next win starts counting up from zero immediately
                // instead of having to cancel out the earlier debt first.
                val history = guestRepository.getHistory()
                    .sortedBy { it.createdAt }

                fun runningClampedScore(games: List<GuestGame>): Int {
                    var running = 0
                    for (game in games) {
                        running = (running + game.score).coerceAtLeast(0)
                    }
                    return running
                }

                fun statsFor(difficulty: Difficulty): DifficultyStatistics {
                    val games = history.filter { it.difficulty == difficulty }
                    return DifficultyStatistics(
                        games = games.size,
                        wins = games.count { it.result == GameResultType.WIN },
                        score = runningClampedScore(games)
                    )
                }

                UserStatistics(
                    totalGames = history.size,
                    totalWins = history.count { it.result == GameResultType.WIN },
                    totalLosses = history.count { it.result == GameResultType.LOSS },
                    totalScore = runningClampedScore(history),
                    easy = statsFor(Difficulty.EASY),
                    medium = statsFor(Difficulty.MEDIUM),
                    hard = statsFor(Difficulty.HARD)
                )
            }
        }
    }
}