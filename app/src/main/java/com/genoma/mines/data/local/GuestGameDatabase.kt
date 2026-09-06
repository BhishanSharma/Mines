package com.genoma.mines.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [GuestGame::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(GuestGameConverters::class)
abstract class GuestGameDatabase : RoomDatabase() {

    abstract fun guestGameDao(): GuestGameDao

    companion object {
        @Volatile
        private var instance: GuestGameDatabase? = null

        fun getInstance(context: Context): GuestGameDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GuestGameDatabase::class.java,
                    "guest_games.db"
                ).build().also { instance = it }
            }
        }
    }
}