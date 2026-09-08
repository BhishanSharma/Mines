package com.genoma.mines.game
object XpAwards {
    const val WIN_XP = 100
    const val LOSS_XP = 10
}


data class LevelProgress(
    val level: Int,
    val currentXp: Int,
    val xpForNextLevel: Int,
    val totalXp: Int
)
object LevelCalculator {

    private const val BASE_XP = 100.0
    private const val GROWTH_EXPONENT = 1.5

    fun xpRequiredForLevel(level: Int): Int {
        val raw = BASE_XP * Math.pow(level.toDouble(), GROWTH_EXPONENT)
        return (Math.round(raw / 10.0) * 10).toInt().coerceAtLeast(10)
    }

    fun calculateTotalXp(wins: Int, losses: Int): Int {
        return wins * XpAwards.WIN_XP + losses * XpAwards.LOSS_XP
    }

    fun calculateProgress(totalXp: Int): LevelProgress {
        var level = 1
        var remainingXp = totalXp.coerceAtLeast(0)

        while (true) {
            val required = xpRequiredForLevel(level)

            if (remainingXp < required) {
                return LevelProgress(
                    level = level,
                    currentXp = remainingXp,
                    xpForNextLevel = required,
                    totalXp = totalXp
                )
            }

            remainingXp -= required
            level += 1
        }
    }
}