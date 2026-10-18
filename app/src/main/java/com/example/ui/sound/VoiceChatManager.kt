package com.example.ui.sound

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class VoiceChatManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) {
    private val _isMicEnabled = MutableStateFlow(false)
    val isMicEnabled: StateFlow<Boolean> = _isMicEnabled.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f) // 0.0 to 1.0 amplitude
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun toggleMic() {
        if (_isMicEnabled.value) {
            disableMic()
        } else {
            enableMic()
        }
    }

    fun enableMic() {
        if (!hasPermission()) return
        _isMicEnabled.value = true
        startAudioCapture()
    }

    fun disableMic() {
        _isMicEnabled.value = false
        stopAudioCapture()
    }

    fun setPushToTalk(active: Boolean) {
        if (active) {
            enableMic()
        } else {
            disableMic()
        }
    }

    private fun startAudioCapture() {
        stopAudioCapture()
        if (!hasPermission()) return

        try {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(2048)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()

            recordJob = scope.launch(Dispatchers.IO) {
                val buffer = ShortArray(bufferSize / 2)
                var wasSpeaking = false
                while (isActive && _isMicEnabled.value) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0L
                        for (i in 0 until read) {
                            sum += abs(buffer[i].toLong())
                        }
                        val avg = sum.toFloat() / read.toFloat()
                        val normalized = (avg / 3000f).coerceIn(0f, 1f)
                        _audioLevel.value = normalized

                        val speaking = normalized > 0.15f
                        if (speaking != wasSpeaking) {
                            wasSpeaking = speaking
                            _isSpeaking.value = speaking
                            onSpeakingStateChanged(speaking)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            stopAudioCapture()
        }
    }

    private fun stopAudioCapture() {
        recordJob?.cancel()
        recordJob = null
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _audioLevel.value = 0f
        if (_isSpeaking.value) {
            _isSpeaking.value = false
            onSpeakingStateChanged(false)
        }
    }

    fun release() {
        disableMic()
    }
}
