package com.genoma.mines.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

class GameFeedback(
    private val context: Context
) {

    private val toneGenerator = ToneGenerator(
        AudioManager.STREAM_MUSIC,
        80
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
                ToneGenerator.TONE_PROP_BEEP,
                50
            )
        }

        if (hapticsEnabled) {
            vibrate(30)
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
            vibrate(40)
        }
    }

    fun explosion(
        soundEnabled: Boolean = true,
        hapticsEnabled: Boolean = true
    ) {
        if (soundEnabled) {
            toneGenerator.startTone(
                ToneGenerator.TONE_PROP_NACK,
                200
            )
        }

        if (hapticsEnabled) {
            vibrate(300)
        }
    }

    fun win(
        soundEnabled: Boolean = true,
        hapticsEnabled: Boolean = true
    ) {
        if (soundEnabled) {
            toneGenerator.startTone(
                ToneGenerator.TONE_PROP_ACK,
                150
            )
        }

        if (hapticsEnabled) {
            vibrate(150)
        }
    }

    fun release() {
        toneGenerator.release()
    }

    private fun vibrate(duration: Long) {
        if (!vibrator.hasVibrator()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    duration,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}