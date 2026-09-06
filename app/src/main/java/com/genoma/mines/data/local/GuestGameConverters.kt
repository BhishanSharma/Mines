package com.genoma.mines.data.local

import androidx.room.TypeConverter
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameResultType

class GuestGameConverters {

    @TypeConverter
    fun fromDifficulty(value: Difficulty): String = value.name

    @TypeConverter
    fun toDifficulty(value: String): Difficulty = Difficulty.valueOf(value)

    @TypeConverter
    fun fromResult(value: GameResultType): String = value.name

    @TypeConverter
    fun toResult(value: String): GameResultType = GameResultType.valueOf(value)
}