package com.example.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.R
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * SoundManager provides low-latency game audio effects using direct PCM AudioTrack playback.
 * By parsing the raw 16-bit 44.1kHz PCM samples directly from resources instead of using SoundPool/MediaCodec,
 * it avoids querying hardware media decoders (which triggers "Failed to query component interface for
 * required system resources" on virtualized environments) while delivering instant, zero-latency feedback.
 */
class SoundManager(private val context: Context) {

    var isSoundEnabled: Boolean = true

    private val executor = Executors.newSingleThreadExecutor()
    private val tracks = ConcurrentHashMap<Int, AudioTrack>()

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val SOUND_DICE_ROLL = 1
        private const val SOUND_TOKEN_STEP = 2
        private const val SOUND_ROLL_SIX = 3
        private const val SOUND_TOKEN_CAPTURE = 4
        private const val SOUND_VICTORY = 5
    }

    init {
        // Load PCM buffers in background to keep UI thread silky smooth and responsive
        executor.execute {
            preloadTrack(SOUND_DICE_ROLL, R.raw.dice_roll)
            preloadTrack(SOUND_TOKEN_STEP, R.raw.token_step)
            preloadTrack(SOUND_ROLL_SIX, R.raw.roll_six)
            preloadTrack(SOUND_TOKEN_CAPTURE, R.raw.token_capture)
            preloadTrack(SOUND_VICTORY, R.raw.victory)
        }
    }

    private fun preloadTrack(soundKey: Int, resId: Int) {
        try {
            val pcmBytes = readPcmFromRaw(resId) ?: return
            val track = createStaticTrack(pcmBytes) ?: return
            tracks[soundKey] = track
        } catch (_: Throwable) {
            // Graceful fallback if audio device is unavailable in virtual environment
        }
    }

    private fun readPcmFromRaw(resId: Int): ByteArray? {
        return try {
            val inputStream: InputStream = context.resources.openRawResource(resId)
            val allBytes = inputStream.use { it.readBytes() }
            // Find "data" chunk in WAV file
            var dataIndex = -1
            for (i in 0 until (allBytes.size - 8)) {
                if (allBytes[i] == 'd'.code.toByte() &&
                    allBytes[i + 1] == 'a'.code.toByte() &&
                    allBytes[i + 2] == 't'.code.toByte() &&
                    allBytes[i + 3] == 'a'.code.toByte()
                ) {
                    dataIndex = i + 8 // skip "data" (4 bytes) + chunk size (4 bytes)
                    break
                }
            }

            if (dataIndex in 0 until allBytes.size) {
                allBytes.copyOfRange(dataIndex, allBytes.size)
            } else if (allBytes.size > 44) {
                // Fallback: standard 44-byte WAV header skip
                allBytes.copyOfRange(44, allBytes.size)
            } else {
                allBytes
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun createStaticTrack(pcmData: ByteArray): AudioTrack? {
        return try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, pcmData.size)
            track
        } catch (_: Throwable) {
            null
        }
    }

    private fun playSound(soundKey: Int) {
        if (!isSoundEnabled) return
        executor.execute {
            try {
                val track = tracks[soundKey] ?: return@execute
                synchronized(track) {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.stop()
                    }
                    track.reloadStaticData()
                    track.play()
                }
            } catch (_: Throwable) {
                // Safe handling in headless or restricted audio environments
            }
        }
    }

    fun playDiceRoll() {
        playSound(SOUND_DICE_ROLL)
    }

    fun playTokenStep() {
        playSound(SOUND_TOKEN_STEP)
    }

    fun playRollSix() {
        playSound(SOUND_ROLL_SIX)
    }

    fun playTokenCapture() {
        playSound(SOUND_TOKEN_CAPTURE)
    }

    fun playVictory() {
        playSound(SOUND_VICTORY)
    }

    fun release() {
        executor.execute {
            for ((_, track) in tracks) {
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {
                }
            }
            tracks.clear()
        }
        try {
            executor.shutdown()
        } catch (_: Throwable) {
        }
    }
}
