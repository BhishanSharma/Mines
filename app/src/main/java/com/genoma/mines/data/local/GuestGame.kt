package com.genoma.mines.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameResultType

@Entity(tableName = "guest_games")
data class GuestGame(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val difficulty: Difficulty,
    val score: Int,
    val result: GameResultType,
    val duration: Long,
    val createdAt: Long
)