package com.genoma.mines.core.remoteconfig

import com.genoma.mines.core.remoteconfig.RemoteConfigParser.Keys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigParserTest {

    private fun parse(vararg values: Pair<String, String?>) =
        RemoteConfigParser.parse { key -> values.toMap()[key] }

    @Test
    fun `nothing fetched gives the agreed safe defaults`() {
        val parsed = parse()

        assertEquals(RemoteConfigSnapshot.DEFAULT, parsed.snapshot)
        assertTrue(parsed.issues.isEmpty())

        with(parsed.snapshot) {
            assertEquals(50, easyWinReward)
            assertEquals(20, lossCoinFee)
            assertEquals(20, minimumStartCoins)
            assertEquals(3, conversionLimitDaily)
            assertEquals(50, rewardedAdBonus)
            assertFalse(maintenanceMode)
            assertFalse(isWinAdBonusAvailable)
            assertFalse(isLuckAdsAvailable)
            assertFalse(lifeRecoveryAdEnabled)
        }
    }

    @Test
    fun `defaults registered with Firebase parse back to the default snapshot`() {
        val parsed = RemoteConfigParser.parse { key -> RemoteConfigParser.DEFAULTS[key]?.toString() }
        assertEquals(RemoteConfigSnapshot.DEFAULT, parsed.snapshot)
        assertTrue(parsed.issues.isEmpty())
    }

    @Test
    fun `valid values are used`() {
        val snapshot = parse(
            Keys.EASY_WIN_REWARD to "60",
            Keys.LOSS_COIN_FEE to "25",
            Keys.CONVERSION_LIMIT_DAILY to "5",
            Keys.REWARDED_AD_BONUS_ENABLED to "true",
            Keys.MAINTENANCE_MODE to "TRUE",
            Keys.MAINTENANCE_MESSAGE to "  Back at 5pm  "
        ).snapshot

        assertEquals(60, snapshot.easyWinReward)
        assertEquals(25, snapshot.lossCoinFee)
        assertEquals(5, snapshot.conversionLimitDaily)
        assertTrue(snapshot.isWinAdBonusAvailable)
        assertTrue(snapshot.maintenanceMode)
        assertEquals("Back at 5pm", snapshot.maintenanceMessage)
    }

    @Test
    fun `malformed numbers fall back to defaults instead of zero`() {
        val parsed = parse(
            Keys.EASY_WIN_REWARD to "fifty",
            Keys.LOSS_COIN_FEE to "",
            Keys.LUCK_AD_COIN_PROBABILITY to "NaN"
        )

        assertEquals(50, parsed.snapshot.easyWinReward)
        assertEquals(20, parsed.snapshot.lossCoinFee)
        assertEquals(0.0, parsed.snapshot.luckAdCoinProbability, 0.0)
        assertEquals(3, parsed.issues.size)
    }

    @Test
    fun `out of range values are clamped`() {
        val parsed = parse(
            Keys.HARD_WIN_REWARD to "1000000",
            Keys.LOSS_COIN_FEE to "-20",
            Keys.CONVERSION_LIMIT_DAILY to "999",
            Keys.LUCK_AD_GEM_PROBABILITY to "250.5"
        )

        assertEquals(1_000, parsed.snapshot.hardWinReward)
        assertEquals(0, parsed.snapshot.lossCoinFee)
        assertEquals(20, parsed.snapshot.conversionLimitDaily)
        assertEquals(100.0, parsed.snapshot.luckAdGemProbability, 0.0)
        assertEquals(4, parsed.issues.size)
    }

    @Test
    fun `decimal strings for integer parameters are accepted`() {
        assertEquals(40, parse(Keys.MEDIUM_WIN_REWARD to "40.0").snapshot.mediumWinReward)
    }

    @Test
    fun `invalid booleans keep the default`() {
        val parsed = parse(Keys.MAINTENANCE_MODE to "maybe", Keys.LIFE_RECOVERY_AD_ENABLED to "1")
        assertFalse(parsed.snapshot.maintenanceMode)
        assertTrue(parsed.snapshot.lifeRecoveryAdEnabled)
        assertEquals(1, parsed.issues.size)
    }

    @Test
    fun `blank maintenance message keeps the default message`() {
        assertEquals(
            RemoteConfigSnapshot.DEFAULT_MAINTENANCE_MESSAGE,
            parse(Keys.MAINTENANCE_MESSAGE to "   ").snapshot.maintenanceMessage
        )
    }

    @Test
    fun `long banner text is truncated and long item ids are rejected`() {
        val parsed = parse(
            Keys.EVENT_BANNER_TEXT to "x".repeat(500),
            Keys.STORE_FEATURED_ITEM to "y".repeat(500)
        )
        assertEquals(RemoteConfigParser.Ranges.MAX_BANNER_LENGTH, parsed.snapshot.eventBannerText.length)
        assertEquals("", parsed.snapshot.storeFeaturedItem)
    }

    @Test
    fun `event banner needs both the flag and text`() {
        assertFalse(parse(Keys.EVENT_BANNER_ENABLED to "true").snapshot.isEventBannerVisible)
        assertTrue(
            parse(Keys.EVENT_BANNER_ENABLED to "true", Keys.EVENT_BANNER_TEXT to "Diwali week!")
                .snapshot.isEventBannerVisible
        )
    }

    @Test
    fun `luck ads need both flags`() {
        assertFalse(parse(Keys.LUCK_AD_ENABLED to "true").snapshot.isLuckAdsAvailable)
        assertTrue(
            parse(Keys.LUCK_AD_ENABLED to "true", Keys.LUCK_ADS_FEATURE_ENABLED to "true")
                .snapshot.isLuckAdsAvailable
        )
    }

    @Test
    fun `all-zero luck probabilities are reported`() {
        val parsed = parse(
            Keys.LUCK_AD_ENABLED to "true",
            Keys.LUCK_AD_NO_REWARD_PROBABILITY to "0"
        )
        assertTrue(parsed.issues.any { it.problem.contains("zero") })
    }
}
