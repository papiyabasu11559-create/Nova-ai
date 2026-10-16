package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceSubsystem(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceSubsystem"
        private const val UTTERANCE_ID = "nova_tts_utterance"
    }

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _isHandsFreeActive = MutableStateFlow(false)
    val isHandsFreeActive: StateFlow<Boolean> = _isHandsFreeActive.asStateFlow()

    var onTtsFinished: (() -> Unit)? = null

    // Single warm reused SpeechRecognizer instance
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizerWarmedUp = false
    private var lastResultCallback: ((String) -> Unit)? = null
    private var lastErrorCallback: ((String) -> Unit)? = null

    private var sentenceBuffer = StringBuilder()

    init {
        // Initialize TTS at app start and keep it alive
        tts = TextToSpeech(context, this)
        warmUpSpeechRecognizer()
    }

    private fun warmUpSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    setupRecognitionListener()
                    isRecognizerWarmedUp = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error warming up recognizer: ${e.message}")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "TTS Language not supported or missing data")
            } else {
                isTtsInitialized = true
                tts?.setSpeechRate(1.1f) // Faster natural 1.1x playback
                setupUtteranceListener()
            }
        } else {
            Log.e(TAG, "TTS Initialization failed with status: $status")
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                requestAudioFocus()
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                releaseAudioFocus()
                mainHandler.post {
                    onTtsFinished?.invoke()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                releaseAudioFocus()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                releaseAudioFocus()
            }
        })
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .build()
            focusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    fun stripMarkdown(text: String): String {
        return text
            .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
            .replace(Regex("""\*(.*?)\*"""), "$1")
            .replace(Regex("""#{1,6}\s*"""), "")
            .replace(Regex("""`{1,3}[^`]*`{1,3}"""), "")
            .replace(Regex("""\[(.*?)\]\(.*?\)"""), "$1")
            .replace(Regex("""[#*`_~]"""), "")
            .trim()
    }

    fun resetStreamingSpeech() {
        sentenceBuffer.clear()
        stopSpeaking()
    }

    /**
     * Speak while streaming: queue each completed sentence immediately via QUEUE_ADD
     * without waiting for the full response to finish generating!
     */
    fun appendTokenAndSpeakStream(token: String, pitch: Float = 1.0f, speed: Float = 1.1f) {
        sentenceBuffer.append(token)
        val textSoFar = sentenceBuffer.toString()
        val sentenceEndIndex = textSoFar.indexOfAny(charArrayOf('.', '!', '?', '\n'))
        if (sentenceEndIndex != -1 && sentenceEndIndex >= 8) {
            val sentence = textSoFar.substring(0, sentenceEndIndex + 1).trim()
            val remaining = textSoFar.substring(sentenceEndIndex + 1)
            sentenceBuffer = StringBuilder(remaining)

            val cleanSentence = stripMarkdown(sentence)
            if (cleanSentence.isNotBlank()) {
                queueSentence(cleanSentence, pitch, speed)
            }
        }
    }

    fun finishStreamingSpeech(pitch: Float = 1.0f, speed: Float = 1.1f) {
        val remaining = sentenceBuffer.toString().trim()
        sentenceBuffer.clear()
        val cleanSentence = stripMarkdown(remaining)
        if (cleanSentence.isNotBlank()) {
            queueSentence(cleanSentence, pitch, speed)
        }
    }

    private fun queueSentence(sentence: String, pitch: Float, speed: Float) {
        if (!isTtsInitialized || tts == null) return
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)
        _isSpeaking.value = true
        val utteranceId = "${UTTERANCE_ID}_${System.currentTimeMillis()}"
        tts?.speak(sentence, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    fun speak(text: String, pitch: Float = 1.0f, speed: Float = 1.1f) {
        if (!isTtsInitialized) {
            Log.w(TAG, "TTS not ready yet")
            return
        }
        stopSpeaking()
        stopListening()
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)
        _isSpeaking.value = true
        val clean = stripMarkdown(text)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stopSpeaking() {
        if (tts != null && _isSpeaking.value) {
            tts?.stop()
            _isSpeaking.value = false
            releaseAudioFocus()
        }
        sentenceBuffer.clear()
    }

    fun setHandsFreeMode(enabled: Boolean) {
        _isHandsFreeActive.value = enabled
        if (!enabled) {
            stopListening()
        }
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
                // Barge-in: stop speaking immediately when user begins talking
                stopSpeaking()
            }

            override fun onRmsChanged(rmsdB: Float) {
                _rmsDb.value = rmsdB.coerceIn(0f, 10f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _rmsDb.value = 0f

                // In hands-free mode, restart recognition immediately without delay!
                if (_isHandsFreeActive.value &&
                    (error == SpeechRecognizer.ERROR_NO_MATCH ||
                     error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                     error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT)) {
                    restartRecognitionImmediately()
                    return
                }

                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking louder."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Recognition error: code $error"
                }
                lastErrorCallback?.invoke(message)
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _rmsDb.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _recognizedText.value = text
                    lastResultCallback?.invoke(text)
                } else if (_isHandsFreeActive.value) {
                    restartRecognitionImmediately()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _recognizedText.value = text
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition service is not available on this device.")
            return
        }

        lastResultCallback = onResult
        lastErrorCallback = onError

        stopSpeaking()

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                setupRecognitionListener()
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Prefer offline recognition for lowest latency
                putExtra("android.speech.extra.PREFER_OFFLINE", true)
                // Short silence timeouts: 800ms complete, 600ms possibly complete
                putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 800L)
                putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 600L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 800)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 600)
            }
            _isListening.value = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer", e)
            _isListening.value = false
            onError("Error starting microphone: ${e.localizedMessage}")
        }
    }

    fun restartRecognitionImmediately() {
        if (!_isHandsFreeActive.value) return
        mainHandler.post {
            if (_isHandsFreeActive.value && !_isSpeaking.value) {
                lastResultCallback?.let { res ->
                    lastErrorCallback?.let { err ->
                        startListening(res, err)
                    }
                }
            }
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping recognizer: ${e.message}")
        } finally {
            _isListening.value = false
            _rmsDb.value = 0f
        }
    }

    fun shutdown() {
        _isHandsFreeActive.value = false
        stopSpeaking()
        stopListening()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignored
        }
        speechRecognizer = null
        tts?.shutdown()
        tts = null
    }
}
