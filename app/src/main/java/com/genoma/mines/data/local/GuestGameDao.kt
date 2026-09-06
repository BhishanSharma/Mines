package com.genoma.mines.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GuestGameDao {

    @Insert
    suspend fun insert(game: GuestGame): Long

    @Query("SELECT * FROM guest_games ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GuestGame>>

    @Query("SELECT * FROM guest_games ORDER BY createdAt DESC")
    suspend fun getAll(): List<GuestGame>

    @Query("DELETE FROM guest_games")
    suspend fun clearAll()
}