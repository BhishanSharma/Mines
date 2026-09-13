package com.genoma.mines.game.domain
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sound + haptic feedback for game events.
 *
 * Sounds are built from [ToneGenerator] (no bundled audio assets needed).
 * A single tone reads as a flat "beep", so tap/flag/win/loss are each a
 * short *sequence* of tones instead of one note — a rising run for a win,
 * a descending buzz for a loss — which reads as an actual sound effect
 * rather than a UI blip.
 *
 * Haptics use [VibrationEffect] waveforms (timing + amplitude pairs) so a
 * win feels like a couple of light taps building up, and a loss feels like
 * one sharp hit followed by a rumble — rather than every event producing
 * the same single buzz.
 */
class GameFeedback(
    private val context: Context
) {

    private val toneGenerator = ToneGenerator(
        AudioManager.STREAM_MUSIC,
        80
    )

    /** Backs the short tone sequences below; cancelled in [release]. */
    private val feedbackScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                        as android.os.VibratorManager

            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun tap(
        soundEnabled: Boolean = true,
        hapticsEnabled: Boolean = true
    ) {
        if (soundEnabled) {
            toneGenerator.startTone(
                ToneGenerator.TONE_PROP_BEEP2,
                40
            )
        }

        if (hapticsEnabled) {
            vibratePredefined(
                effect = VibrationEffect.EFFECT_TICK,
                fallbackDurationMs = 20
            )
        }
    }

    fun flag(
        soundEnabled: Boolean = true,
        hapticsEnabled: Boolean = true
    ) {
        if (soundEnabled) {
            toneGenerator.startTone(
                ToneGenerator.TONE_PROP_ACK,
                70
            )
        }

        if (hapticsEnabled) {
            vibratePredefined(
                effect = VibrationEffect.EFFECT_CLICK,
                fallbackDurationMs = 35
            )
        }
    }

    fun explosion(
        soundEnabled: Boolean = true,
        hapticsEnabled: Boolean = true
    ) {
        if (soundEnabled) {
            feedbackScope.launch {
                // A sharp crack followed by a lower, longer rumble tone —
                // reads much more like an impact than one flat beep.
                toneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 120)
                delay(120)
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_LOW_L, 260)
            }
        }

        if (hapticsEnabled) {
            vibrateWaveform(
                timings = longArrayOf(0, 90, 60, 220),
                amplitudes = intArrayOf(0, 255, 40, 160)
            )
        }
    }

    fun win(
        soundEnabled: Boolean = true,
        hapticsEnabled: Boolean = true
    ) {
        if (soundEnabled) {
            feedbackScope.launch {
                // A short rising run (DTMF tones roughly increase in pitch
                // with digit) reads as a little victory chime rather than
                // a single acknowledgement beep.
                val notes = intArrayOf(
                    ToneGenerator.TONE_DTMF_1,
                    ToneGenerator.TONE_DTMF_4,
                    ToneGenerator.TONE_DTMF_7,
                    ToneGenerator.TONE_DTMF_9
                )

                notes.forEach { note ->
                    toneGenerator.startTone(note, 90)
                    delay(85)
                }
            }
        }

        if (hapticsEnabled) {
            vibrateWaveform(
                timings = longArrayOf(0, 40, 70, 40, 70, 70, 140),
                amplitudes = intArrayOf(0, 110, 0, 160, 0, 255, 0)
            )
        }
    }

    /**
     * Extra celebration layered on top of [win] when the finished game is
     * also a new personal-best time for its difficulty — a brighter,
     * longer fanfare than the regular win chime so a record feels like a
     * bigger deal than an ordinary clear.
     */
    fun newBestTime(
        soundEnabled: Boolean = true,
        hapticsEnabled: Boolean = true
    ) {
        if (soundEnabled) {
            feedbackScope.launch {
                // Same rising run as `win`, but climbing further and
                // finishing on a held high note instead of stopping short.
                val notes = intArrayOf(
                    ToneGenerator.TONE_DTMF_1,
                    ToneGenerator.TONE_DTMF_4,
                    ToneGenerator.TONE_DTMF_7,
                    ToneGenerator.TONE_DTMF_9,
                    ToneGenerator.TONE_DTMF_S,
                    ToneGenerator.TONE_DTMF_S
                )

                notes.forEach { note ->
                    toneGenerator.startTone(note, 90)
                    delay(85)
                }
            }
        }

        if (hapticsEnabled) {
            vibrateWaveform(
                timings = longArrayOf(0, 40, 60, 40, 60, 40, 60, 200),
                amplitudes = intArrayOf(0, 120, 0, 170, 0, 220, 0, 255)
            )
        }
    }

    /**
     * Played when the game that just finished pushes the player's total XP
     * into a new level (see `LevelCalculator`). Distinct from [win] so a
     * level-up reads as its own milestone even when it lands on an
     * otherwise ordinary win or loss.
     */
    fun levelUp(
        soundEnabled: Boolean = true,
        hapticsEnabled: Boolean = true
    ) {
        if (soundEnabled) {
            feedbackScope.launch {
                // Two quick ascending pairs — reads as a distinct "level
                // up" jingle rather than the single rising run used for
                // `win`.
                val notes = intArrayOf(
                    ToneGenerator.TONE_DTMF_5,
                    ToneGenerator.TONE_DTMF_8,
                    ToneGenerator.TONE_DTMF_8,
                    ToneGenerator.TONE_DTMF_S
                )

                notes.forEach { note ->
                    toneGenerator.startTone(note, 80)
                    delay(75)
                }
            }
        }

        if (hapticsEnabled) {
            vibrateWaveform(
                timings = longArrayOf(0, 50, 50, 50, 50, 120),
                amplitudes = intArrayOf(0, 150, 0, 150, 0, 255)
            )
        }
    }

    /**
     * Played when the game that just finished unlocks a new achievement
     * tier (see `AchievementCalculator`). A single bright chime plus a
     * light double-tap haptic — deliberately lighter than [levelUp] since
     * several achievement tiers can unlock from one game and shouldn't
     * stack into a wall of sound.
     */
    fun achievementUnlocked(
        soundEnabled: Boolean = true,
        hapticsEnabled: Boolean = true
    ) {
        if (soundEnabled) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }

        if (hapticsEnabled) {
            vibrateWaveform(
                timings = longArrayOf(0, 40, 40, 80),
                amplitudes = intArrayOf(0, 200, 0, 255)
            )
        }
    }

    fun release() {
        toneGenerator.release()
        feedbackScope.cancel()
    }

    private fun vibrateWaveform(
        timings: LongArray,
        amplitudes: IntArray
    ) {
        if (!vibrator.hasVibrator()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    /**
     * Prefers the platform's tuned "tick"/"click" haptic (a short, crisp
     * pulse shaped by the device's own haptic engine) and falls back to a
     * plain timed buzz on older API levels where those aren't available.
     */
    private fun vibratePredefined(
        effect: Int,
        fallbackDurationMs: Long
    ) {
        if (!vibrator.hasVibrator()) {
            return
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                vibrator.vibrate(VibrationEffect.createPredefined(effect))
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        fallbackDurationMs,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            }

            else -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(fallbackDurationMs)
            }
        }
    }
}