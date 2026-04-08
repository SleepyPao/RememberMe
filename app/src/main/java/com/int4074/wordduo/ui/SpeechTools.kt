package com.int4074.wordduo.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

interface RecognizerHandle {
    fun start(): Boolean
    fun release()
}

class SpeechPlayer(context: Context) : TextToSpeech.OnInitListener {
    private var engine: TextToSpeech? = TextToSpeech(context, this)
    private var ready = false
    private var initialized = false
    private var pendingText: String? = null

    override fun onInit(status: Int) {
        initialized = true
        ready = false
        if (status == TextToSpeech.SUCCESS) {
            val selectedLocale = listOf(Locale.US, Locale.ENGLISH, Locale.getDefault()).firstOrNull { locale ->
                val result = engine?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
            ready = selectedLocale != null
            if (ready) {
                engine?.setSpeechRate(0.92f)
                pendingText?.let {
                    engine?.speak(it, TextToSpeech.QUEUE_FLUSH, null, it)
                    pendingText = null
                }
            }
        }
    }

    fun speak(text: String): Boolean {
        return when {
            ready -> {
                engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
                true
            }
            initialized -> false
            else -> {
                pendingText = text
                true
            }
        }
    }

    fun release() {
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}

class AndroidRecognizerHandle(
    context: Context,
    private val onRecognized: (String) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onErrorMessage: (String) -> Unit
) : RecognizerHandle {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningChanged(true)
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                onListeningChanged(false)
            }

            override fun onError(error: Int) {
                onListeningChanged(false)
                onErrorMessage(errorMessage(error))
                onRecognized("")
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                onRecognized(text)
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    override fun start(): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        return runCatching {
            recognizer.startListening(intent)
            true
        }.getOrElse {
            onListeningChanged(false)
            onErrorMessage("当前设备无法启动语音识别")
            false
        }
    }

    override fun release() {
        recognizer.destroy()
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "麦克风音频异常，请重试"
        SpeechRecognizer.ERROR_CLIENT -> "语音识别客户端异常，请重试"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少麦克风权限"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "语音识别网络不可用"
        SpeechRecognizer.ERROR_NO_MATCH -> "没有识别到有效发音，请再读一遍"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音识别服务正忙，请稍后再试"
        SpeechRecognizer.ERROR_SERVER -> "语音识别服务暂时不可用"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "等待说话超时，请点开始跟读后立即发音"
        else -> "语音识别失败，请稍后再试"
    }
}

class NoOpRecognizerHandle : RecognizerHandle {
    override fun start(): Boolean = false
    override fun release() = Unit
}

@Composable
fun rememberSpeechPlayer(): SpeechPlayer {
    val context = LocalContext.current
    val player = remember { SpeechPlayer(context) }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}

fun isSpeechRecognitionAvailable(context: Context): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

@Composable
fun rememberSpeechRecognizer(
    enabled: Boolean,
    onRecognized: (String) -> Unit,
    onListeningChanged: (Boolean) -> Unit,
    onErrorMessage: (String) -> Unit
): RecognizerHandle {
    val context = LocalContext.current
    val handle = remember(enabled) {
        if (enabled && SpeechRecognizer.isRecognitionAvailable(context)) {
            AndroidRecognizerHandle(context, onRecognized, onListeningChanged, onErrorMessage)
        } else {
            NoOpRecognizerHandle()
        }
    }
    DisposableEffect(handle) {
        onDispose { handle.release() }
    }
    return handle
}
