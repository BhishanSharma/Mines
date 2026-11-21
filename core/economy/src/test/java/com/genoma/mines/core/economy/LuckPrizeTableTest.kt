package com.genoma.mines.core.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LuckPrizeTableTest {

    private val json = """
        [
          {"type":"COINS","amount":10,"weight":40},
          {"type":"GEMS","amount":1,"weight":10},
          {"type":"NONE","amount":0,"weight":50}
        ]
    """.trimIndent()

    private fun build(
        json: String = "",
        coin: Int = 0,
        gem: Int = 0,
        coinW: Double = 0.0,
        gemW: Double = 0.0,
        noneW: Double = 100.0
    ) = LuckPrizeTable.build(json, coin, gem, coinW, gemW, noneW)

    @Test
    fun `parses configured slots`() {
        val table = build(json)
        assertEquals(
            listOf(
                LuckPrize(LuckPrizeType.COINS, 10, 40.0),
                LuckPrize(LuckPrizeType.GEMS, 1, 10.0),
                LuckPrize(LuckPrizeType.NONE, 0, 50.0)
            ),
            table
        )
        assertEquals(0.4, table[0].probabilityIn(table), 1e-9)
    }

    @Test
    fun `invalid slots are skipped`() {
        val table = build(
            """
            [
              {"type":"COINS","amount":10,"weight":40},
              {"type":"COINS","amount":0,"weight":10},
              {"type":"GEMS","amount":5,"weight":-1},
              {"type":"NONE","amount":3,"weight":10},
              {"type":"JACKPOT","amount":9,"weight":10},
              {"type":"GEMS","amount":5000,"weight":10},
              "not an object"
            ]
            """.trimIndent()
        )
        assertEquals(listOf(LuckPrize(LuckPrizeType.COINS, 10, 40.0)), table)
    }

    @Test
    fun `malformed json falls back to legacy values`() {
        val table = build("{oops", coin = 25, coinW = 30.0, noneW = 70.0)
        assertEquals(
            listOf(LuckPrize(LuckPrizeType.COINS, 25, 30.0), LuckPrize(LuckPrizeType.NONE, 0, 70.0)),
            table
        )
    }

    @Test
    fun `a table with only no-reward slots is unavailable`() {
        assertTrue(build().isEmpty())
        assertTrue(build("""[{"type":"NONE","amount":0,"weight":1}]""").isEmpty())
    }

    @Test
    fun `selection follows the weights`() {
        val table = build(json)
        val random = Random(42)
        val counts = IntArray(table.size)
        repeat(100_000) { counts[LuckPrizeTable.selectIndex(table, random)!!]++ }

        assertEquals(0.40, counts[0] / 100_000.0, 0.01)
        assertEquals(0.10, counts[1] / 100_000.0, 0.01)
        assertEquals(0.50, counts[2] / 100_000.0, 0.01)
    }

    @Test
    fun `every slot is reachable and boundaries are handled`() {
        val table = build(json)
        val low = object : Random() {
            override fun nextBits(bitCount: Int) = 0
            override fun nextDouble() = 0.0
        }
        val high = object : Random() {
            override fun nextBits(bitCount: Int) = 0
            override fun nextDouble() = 0.9999999999
        }
        assertEquals(0, LuckPrizeTable.selectIndex(table, low))
        assertEquals(2, LuckPrizeTable.selectIndex(table, high))
        assertNull(LuckPrizeTable.selectIndex(emptyList()))
    }

    @Test
    fun `prizes map to the exact wallet change`() {
        assertEquals(SettlementPlan(coinCredit = 10), LuckPrize(LuckPrizeType.COINS, 10, 1.0).toSettlementPlan())
        assertEquals(SettlementPlan(gemCredit = 2), LuckPrize(LuckPrizeType.GEMS, 2, 1.0).toSettlementPlan())
        assertTrue(LuckPrize(LuckPrizeType.NONE, 0, 1.0).toSettlementPlan().isEmpty)
    }
}
