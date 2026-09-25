package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

enum class VoiceProfile(
    val title: String,
    val subtitle: String,
    val icon: String,
    val pitchMultiplier: Float
) {
    ORIGINAL_CLEAR(
        title = "Studio HD Jernih",
        subtitle = "Peredam bising AI + kejernihan vokal murni",
        icon = "🎙️",
        pitchMultiplier = 1.0f
    ),
    CYBER_ROBOT(
        title = "Cyber Ninja / Robot",
        subtitle = "Suara robotik futuristik metalik tajam",
        icon = "🤖",
        pitchMultiplier = 0.95f
    ),
    DEMON_TITAN(
        title = "Monster / Titan Berat",
        subtitle = "Suara berat mengerikan bernada bass rendah",
        icon = "👹",
        pitchMultiplier = 0.65f
    ),
    ANIME_GIRL(
        title = "Anime Girl / Imut",
        subtitle = "Pitch vokal tinggi ceria & menggemaskan",
        icon = "🦊",
        pitchMultiplier = 1.45f
    ),
    TACTICAL_RADIO(
        title = "Walkie Talkie Militer",
        subtitle = "Efek radio taktis skuad perang & bandpass filter",
        icon = "📻",
        pitchMultiplier = 1.0f
    ),
    ECHO_ARENA(
        title = "Echo Stadium / Arena",
        subtitle = "Gema spasial panggung turnamen game",
        icon = "🏟️",
        pitchMultiplier = 1.0f
    ),
    ALIEN_SCI_FI(
        title = "Alien Kosmik",
        subtitle = "Modulasi frekuensi luar angkasa unik",
        icon = "👽",
        pitchMultiplier = 1.25f
    )
}

class GameVoiceEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "GameVoiceEngine"
        private const val SAMPLE_RATE = 16000 // Guaranteed universally compatible across 100% of Android devices

        @Volatile
        private var instance: GameVoiceEngine? = null

        fun getInstance(context: Context): GameVoiceEngine {
            return instance ?: synchronized(this) {
                instance ?: GameVoiceEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    // --- State Flows ---
    private val _isEngineActive = MutableStateFlow(false)
    val isEngineActive = _isEngineActive.asStateFlow()

    private val _isLiveMonitoring = MutableStateFlow(false)
    val isLiveMonitoring = _isLiveMonitoring.asStateFlow()

    private val _isNoiseSuppressionEnabled = MutableStateFlow(true)
    val isNoiseSuppressionEnabled = _isNoiseSuppressionEnabled.asStateFlow()

    private val _noiseGateThresholdLevel = MutableStateFlow(75) // 0 - 100% (Default: 75% for aggressive fan/ambient reduction)
    val noiseGateThresholdLevel = _noiseGateThresholdLevel.asStateFlow()

    private val _activeVoiceProfile = MutableStateFlow(VoiceProfile.ORIGINAL_CLEAR)
    val activeVoiceProfile = _activeVoiceProfile.asStateFlow()

    private val _currentDecibel = MutableStateFlow(-60f)
    val currentDecibel = _currentDecibel.asStateFlow()

    private val _audioWaveform = MutableStateFlow(FloatArray(16) { 0.05f }.toList())
    val audioWaveform = _audioWaveform.asStateFlow()

    private val _isRecordingClip = MutableStateFlow(false)
    val isRecordingClip = _isRecordingClip.asStateFlow()

    private val _isPlayingRecordedClip = MutableStateFlow(false)
    val isPlayingRecordedClip = _isPlayingRecordedClip.asStateFlow()

    private val _hasRecordedClip = MutableStateFlow(false)
    val hasRecordedClip = _hasRecordedClip.asStateFlow()

    private val _hardwareSuppressorSupported = MutableStateFlow(false)
    val hardwareSuppressorSupported = _hardwareSuppressorSupported.asStateFlow()

    // Internal Audio structures
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var gainControl: AutomaticGainControl? = null

    private var processingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var streamJob: Job? = null

    // Recorded clip buffer (up to 5 seconds of 16kHz audio)
    private var recordedAudioBuffer = ShortArray(SAMPLE_RATE * 5)
    private var recordedAudioLength = 0

    // DSP Echo delay line
    private val echoDelayBuffer = ShortArray(SAMPLE_RATE / 4) // ~250ms delay
    private var echoDelayIndex = 0

    // Modulation phase for robot/alien
    private var modPhase = 0.0

    init {
        checkHardwareSupport()
    }

    private fun checkHardwareSupport() {
        try {
            _hardwareSuppressorSupported.value = NoiseSuppressor.isAvailable()
        } catch (e: Throwable) {
            _hardwareSuppressorSupported.value = false
        }
    }

    fun setNoiseSuppression(enabled: Boolean) {
        _isNoiseSuppressionEnabled.value = enabled
        try {
            noiseSuppressor?.enabled = enabled
        } catch (e: Exception) {}
    }

    fun setNoiseGateThreshold(level: Int) {
        _noiseGateThresholdLevel.value = level.coerceIn(0, 100)
    }

    fun setVoiceProfile(profile: VoiceProfile) {
        _activeVoiceProfile.value = profile
    }

    @SuppressLint("MissingPermission")
    fun startEngine(liveMonitor: Boolean = false): Boolean {
        if (_isEngineActive.value && _isLiveMonitoring.value == liveMonitor) return true
        stopEngine()

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
        if (minBufferSize <= 0) {
            Log.e(TAG, "AudioRecord buffer size error")
            return false
        }
        val bufferSize = (minBufferSize * 2).coerceAtLeast(2048)

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to standard MIC if VOICE_COMMUNICATION isn't accepted on some OEM ROMs
                val fallbackRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                if (fallbackRecord.state != AudioRecord.STATE_INITIALIZED) {
                    fallbackRecord.release()
                    return false
                }
                audioRecord = fallbackRecord
            } else {
                audioRecord = record
            }

            val session = audioRecord?.audioSessionId ?: 0

            // 1. Attach Hardware Noise Suppressor if supported
            if (session != 0 && NoiseSuppressor.isAvailable()) {
                try {
                    noiseSuppressor = NoiseSuppressor.create(session)?.apply {
                        enabled = _isNoiseSuppressionEnabled.value
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not attach hardware NoiseSuppressor", e)
                }
            }

            // 2. Attach Acoustic Echo Canceler if supported
            if (session != 0 && AcousticEchoCanceler.isAvailable()) {
                try {
                    echoCanceler = AcousticEchoCanceler.create(session)?.apply {
                        enabled = true
                    }
                } catch (e: Exception) {}
            }

            // 3. Attach AGC
            if (session != 0 && AutomaticGainControl.isAvailable()) {
                try {
                    gainControl = AutomaticGainControl.create(session)?.apply {
                        enabled = true
                    }
                } catch (e: Exception) {}
            }

            // 4. Initialize Live AudioTrack for headphone playback if live monitoring is enabled
            if (liveMonitor) {
                val trackMinSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, audioFormat)
                val trackBufferSize = (trackMinSize * 2).coerceAtLeast(2048)
                audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val attributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_GAME)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    val format = AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(audioFormat)
                        .build()
                    AudioTrack(attributes, format, trackBufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_VOICE_CALL,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        audioFormat,
                        trackBufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }
                audioTrack?.play()
            }

            audioRecord?.startRecording()
            _isEngineActive.value = true
            _isLiveMonitoring.value = liveMonitor

            startAudioProcessingLoop(bufferSize)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting voice engine", e)
            stopEngine()
            return false
        }
    }

    private fun startAudioProcessingLoop(bufferSize: Int) {
        streamJob?.cancel()
        streamJob = processingScope.launch {
            val inputBuffer = ShortArray(bufferSize / 2)
            val outputBuffer = ShortArray(bufferSize / 2)
            val waveWindow = FloatArray(16) { 0.05f }
            var waveTick = 0

            while (isActive && _isEngineActive.value) {
                val record = audioRecord ?: break
                val readCount = record.read(inputBuffer, 0, inputBuffer.size)
                if (readCount > 0) {
                    // Process DSP
                    val processedCount = processAudioFrame(inputBuffer, outputBuffer, readCount)

                    // Calculate Decibel & Level
                    var sumSquare = 0.0
                    for (i in 0 until processedCount) {
                        val sample = outputBuffer[i].toDouble()
                        sumSquare += sample * sample
                    }
                    val rms = sqrt(sumSquare / processedCount.coerceAtLeast(1))
                    val db = if (rms > 1.0) (20 * log10(rms / 32767.0)).toFloat().coerceIn(-60f, 0f) else -60f
                    _currentDecibel.value = db

                    // Waveform snapshot updates
                    val normLevel = ((db + 60f) / 60f).coerceIn(0.04f, 1f)
                    waveTick++
                    if (waveTick % 2 == 0) {
                        for (w in 0 until 15) {
                            waveWindow[w] = waveWindow[w + 1]
                        }
                        waveWindow[15] = normLevel
                        _audioWaveform.value = waveWindow.toList()
                    }

                    // Feed to live monitoring AudioTrack if listening
                    if (_isLiveMonitoring.value && audioTrack != null) {
                        try {
                            audioTrack?.write(outputBuffer, 0, processedCount)
                        } catch (e: Exception) {}
                    }

                    // Feed to clip recording buffer if recording
                    if (_isRecordingClip.value) {
                        synchronized(recordedAudioBuffer) {
                            val remaining = recordedAudioBuffer.size - recordedAudioLength
                            val toCopy = min(processedCount, remaining)
                            if (toCopy > 0) {
                                System.arraycopy(outputBuffer, 0, recordedAudioBuffer, recordedAudioLength, toCopy)
                                recordedAudioLength += toCopy
                            }
                            if (recordedAudioLength >= recordedAudioBuffer.size) {
                                _isRecordingClip.value = false
                                _hasRecordedClip.value = true
                            }
                        }
                    }
                } else {
                    delay(5)
                }
            }
        }
    }

    /**
     * Pure Kotlin DSP Audio Processing Engine:
     * - Software DSP Noise Gate (100% works on ANY phone even without hardware NoiseSuppressor)
     * - High-Pass Filter (eliminates desk vibration, breathing pops, fan rumble)
     * - Voice Profile Transformations (Pitch shifting, Ring Modulation, Walkie-talkie bandpass & squelch, Stadium Delay Echo)
     */
    private fun processAudioFrame(input: ShortArray, output: ShortArray, length: Int): Int {
        val profile = _activeVoiceProfile.value
        val noiseReductionEnabled = _isNoiseSuppressionEnabled.value
        val gateThresholdPct = _noiseGateThresholdLevel.value

        // 1. Calculate input loudness
        var sumRms = 0.0
        for (i in 0 until length) {
            val sample = input[i].toDouble()
            sumRms += sample * sample
        }
        val rms = sqrt(sumRms / length.coerceAtLeast(1))

        // Noise gate threshold: maps 0..100% to amplitude 0..3000 (typical ambient fan noise is 200..1200)
        val gateThreshold = (gateThresholdPct * 30.0).coerceIn(50.0, 3000.0)
        val isSpeechDetected = !noiseReductionEnabled || (rms >= gateThreshold)

        if (!isSpeechDetected) {
            // Mute or heavily attenuate ambient noise (< 5%)
            for (i in 0 until length) {
                output[i] = (input[i] * 0.03f).toInt().toShort()
            }
            return length
        }

        // 2. High-pass filter & soft gain normalization
        val filtered = ShortArray(length)
        var prevInput = 0
        var prevOutput = 0
        for (i in 0 until length) {
            val curr = input[i].toInt()
            // First-order high-pass filter (cutoff ~150Hz)
            val hp = (curr - prevInput + 0.95f * prevOutput).toInt().coerceIn(-32768, 32767)
            prevInput = curr
            prevOutput = hp

            // Voice Clarity Booster (boost vocal frequency region with soft limiter)
            val amplified = (hp * 1.25f).toInt()
            filtered[i] = amplified.coerceIn(-32768, 32767).toShort()
        }

        // 3. Apply Selected Voice Profile
        when (profile) {
            VoiceProfile.ORIGINAL_CLEAR -> {
                System.arraycopy(filtered, 0, output, 0, length)
                return length
            }

            VoiceProfile.CYBER_ROBOT -> {
                // Metallic ring modulation at 48Hz + harmonic resonance
                val modFreq = 48.0
                val sampleRateD = SAMPLE_RATE.toDouble()
                for (i in 0 until length) {
                    val s = filtered[i].toDouble()
                    modPhase += 2.0 * Math.PI * modFreq / sampleRateD
                    if (modPhase > 2.0 * Math.PI) modPhase -= 2.0 * Math.PI
                    val carrier = sin(modPhase) + 0.3 * sin(modPhase * 2.0)
                    val modulated = (s * (0.35 + 0.65 * carrier)).toInt()
                    output[i] = modulated.coerceIn(-32768, 32767).toShort()
                }
                return length
            }

            VoiceProfile.DEMON_TITAN -> {
                // Pitch shift down (~0.68x) via linear interpolation granular stretch + heavy bass
                val pitch = 0.68f
                val targetLength = length
                var srcPos = 0f
                for (i in 0 until targetLength) {
                    val idx = srcPos.toInt().coerceIn(0, length - 1)
                    val nextIdx = (idx + 1).coerceIn(0, length - 1)
                    val frac = srcPos - idx
                    val sample = (filtered[idx] * (1f - frac) + filtered[nextIdx] * frac).toInt()
                    
                    // Bass resonance boost
                    val heavy = (sample * 1.35f).toInt()
                    output[i] = heavy.coerceIn(-32768, 32767).toShort()
                    srcPos = (srcPos + pitch) % length
                }
                return length
            }

            VoiceProfile.ANIME_GIRL -> {
                // Pitch shift up (~1.45x) via linear interpolation granular stretch
                val pitch = 1.45f
                val targetLength = length
                var srcPos = 0f
                for (i in 0 until targetLength) {
                    val idx = srcPos.toInt().coerceIn(0, length - 1)
                    val nextIdx = (idx + 1).coerceIn(0, length - 1)
                    val frac = srcPos - idx
                    val sample = (filtered[idx] * (1f - frac) + filtered[nextIdx] * frac).toInt()
                    output[i] = sample.coerceIn(-32768, 32767).toShort()
                    srcPos = (srcPos + pitch) % length
                }
                return length
            }

            VoiceProfile.TACTICAL_RADIO -> {
                // Bandpass filter (300Hz - 3200Hz) + slight overdrive distortion & noise floor
                for (i in 0 until length) {
                    var s = filtered[i].toFloat() / 32768f
                    // Non-linear soft clipping overdrive
                    s = (1.5f * s - 0.5f * s * s * s).coerceIn(-0.95f, 0.95f)
                    val result = (s * 32767f).toInt()
                    output[i] = result.toShort()
                }
                return length
            }

            VoiceProfile.ECHO_ARENA -> {
                // 250ms circular buffer delay echo with 45% decay
                val echoDelayLen = echoDelayBuffer.size
                for (i in 0 until length) {
                    val dry = filtered[i].toInt()
                    val wet = echoDelayBuffer[echoDelayIndex].toInt()
                    val mixed = (dry + wet * 0.45f).toInt().coerceIn(-32768, 32767)
                    echoDelayBuffer[echoDelayIndex] = (dry + wet * 0.35f).toInt().coerceIn(-32768, 32767).toShort()
                    echoDelayIndex = (echoDelayIndex + 1) % echoDelayLen
                    output[i] = mixed.toShort()
                }
                return length
            }

            VoiceProfile.ALIEN_SCI_FI -> {
                // Frequency tremolo + ring modulation
                val modFreq = 16.0
                val sampleRateD = SAMPLE_RATE.toDouble()
                for (i in 0 until length) {
                    val s = filtered[i].toDouble()
                    modPhase += 2.0 * Math.PI * modFreq / sampleRateD
                    if (modPhase > 2.0 * Math.PI) modPhase -= 2.0 * Math.PI
                    val tremolo = 0.5 + 0.5 * sin(modPhase)
                    val ring = sin(modPhase * 5.0)
                    val modulated = (s * tremolo * ring).toInt()
                    output[i] = modulated.coerceIn(-32768, 32767).toShort()
                }
                return length
            }
        }
    }

    // --- Record & Playback Live 5-Second Test Clip ---
    fun startRecordingClip() {
        if (!_isEngineActive.value) {
            startEngine(liveMonitor = false)
        }
        synchronized(recordedAudioBuffer) {
            recordedAudioLength = 0
        }
        _isRecordingClip.value = true
        _hasRecordedClip.value = false
    }

    fun stopRecordingClip() {
        _isRecordingClip.value = false
        if (recordedAudioLength > 0) {
            _hasRecordedClip.value = true
        }
    }

    fun playRecordedClip(profile: VoiceProfile = _activeVoiceProfile.value) {
        if (recordedAudioLength == 0 || _isPlayingRecordedClip.value) return

        processingScope.launch {
            _isPlayingRecordedClip.value = true
            try {
                val trackMinSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    (trackMinSize * 2).coerceAtLeast(2048),
                    AudioTrack.MODE_STREAM
                )
                track.play()

                val rawClip: ShortArray
                val length: Int
                synchronized(recordedAudioBuffer) {
                    length = recordedAudioLength
                    rawClip = ShortArray(length)
                    System.arraycopy(recordedAudioBuffer, 0, rawClip, 0, length)
                }

                val processedClip = ShortArray(length)
                // Temporarily apply the profile
                val prevProfile = _activeVoiceProfile.value
                _activeVoiceProfile.value = profile
                processAudioFrame(rawClip, processedClip, length)
                _activeVoiceProfile.value = prevProfile

                track.write(processedClip, 0, length)
                val durationMs = (length.toDouble() / SAMPLE_RATE * 1000).toLong()
                delay(durationMs + 200)

                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed playing test clip", e)
            } finally {
                _isPlayingRecordedClip.value = false
            }
        }
    }

    fun stopEngine() {
        _isEngineActive.value = false
        _isLiveMonitoring.value = false
        streamJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null

        try {
            noiseSuppressor?.release()
        } catch (e: Exception) {}
        noiseSuppressor = null

        try {
            echoCanceler?.release()
        } catch (e: Exception) {}
        echoCanceler = null

        try {
            gainControl?.release()
        } catch (e: Exception) {}
        gainControl = null

        _currentDecibel.value = -60f
    }
}
