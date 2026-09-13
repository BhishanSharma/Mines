package com.genoma.mines.game.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.genoma.mines.game.domain.Difficulty
import com.genoma.mines.game.domain.GameResultType
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class GuestGameDao_Impl(
  __db: RoomDatabase,
) : GuestGameDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfGuestGame: EntityInsertAdapter<GuestGame>

  private val __guestGameConverters: GuestGameConverters = GuestGameConverters()
  init {
    this.__db = __db
    this.__insertAdapterOfGuestGame = object : EntityInsertAdapter<GuestGame>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `guest_games` (`id`,`difficulty`,`score`,`result`,`duration`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: GuestGame) {
        statement.bindLong(1, entity.id)
        val _tmp: String = __guestGameConverters.fromDifficulty(entity.difficulty)
        statement.bindText(2, _tmp)
        statement.bindLong(3, entity.score.toLong())
        val _tmp_1: String = __guestGameConverters.fromResult(entity.result)
        statement.bindText(4, _tmp_1)
        statement.bindLong(5, entity.duration)
        statement.bindLong(6, entity.createdAt)
      }
    }
  }

  public override suspend fun insert(game: GuestGame): Long = performSuspending(__db, false, true) {
      _connection ->
    val _result: Long = __insertAdapterOfGuestGame.insertAndReturnId(_connection, game)
    _result
  }

  public override fun observeAll(): Flow<List<GuestGame>> {
    val _sql: String = "SELECT * FROM guest_games ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("guest_games")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDifficulty: Int = getColumnIndexOrThrow(_stmt, "difficulty")
        val _columnIndexOfScore: Int = getColumnIndexOrThrow(_stmt, "score")
        val _columnIndexOfResult: Int = getColumnIndexOrThrow(_stmt, "result")
        val _columnIndexOfDuration: Int = getColumnIndexOrThrow(_stmt, "duration")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<GuestGame> = mutableListOf()
        while (_stmt.step()) {
          val _item: GuestGame
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDifficulty: Difficulty
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfDifficulty)
          _tmpDifficulty = __guestGameConverters.toDifficulty(_tmp)
          val _tmpScore: Int
          _tmpScore = _stmt.getLong(_columnIndexOfScore).toInt()
          val _tmpResult: GameResultType
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfResult)
          _tmpResult = __guestGameConverters.toResult(_tmp_1)
          val _tmpDuration: Long
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = GuestGame(_tmpId,_tmpDifficulty,_tmpScore,_tmpResult,_tmpDuration,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<GuestGame> {
    val _sql: String = "SELECT * FROM guest_games ORDER BY createdAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDifficulty: Int = getColumnIndexOrThrow(_stmt, "difficulty")
        val _columnIndexOfScore: Int = getColumnIndexOrThrow(_stmt, "score")
        val _columnIndexOfResult: Int = getColumnIndexOrThrow(_stmt, "result")
        val _columnIndexOfDuration: Int = getColumnIndexOrThrow(_stmt, "duration")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<GuestGame> = mutableListOf()
        while (_stmt.step()) {
          val _item: GuestGame
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDifficulty: Difficulty
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfDifficulty)
          _tmpDifficulty = __guestGameConverters.toDifficulty(_tmp)
          val _tmpScore: Int
          _tmpScore = _stmt.getLong(_columnIndexOfScore).toInt()
          val _tmpResult: GameResultType
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfResult)
          _tmpResult = __guestGameConverters.toResult(_tmp_1)
          val _tmpDuration: Long
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = GuestGame(_tmpId,_tmpDifficulty,_tmpScore,_tmpResult,_tmpDuration,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAll() {
    val _sql: String = "DELETE FROM guest_games"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
