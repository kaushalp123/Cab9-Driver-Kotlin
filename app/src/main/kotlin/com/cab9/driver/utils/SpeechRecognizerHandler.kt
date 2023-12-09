package com.cab9.driver.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.cab9.driver.ext.isPermissionGranted
import com.cab9.driver.ui.booking.offers.BookingOfferActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class SpeechAction {
    ACCEPT, REJECT, UNKNOWN;
}

@ActivityScoped
class SpeechRecognizerHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityRef: Activity
) : RecognitionListener, DefaultLifecycleObserver {

    private val activityContext: BookingOfferActivity
        get() = activityRef as BookingOfferActivity

    private val isAvailable: Boolean
        get() = activityRef.isPermissionGranted(Manifest.permission.RECORD_AUDIO)
                && SpeechRecognizer.isRecognitionAvailable(activityRef.applicationContext)

    private val speechIntent: Intent
        get() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activityRef.packageName)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            //putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30 * 1000)
        }

    private var isRecognized = false

    private var speechRecognizer: SpeechRecognizer? = null

    private val _speechListenerUiVisibility = MutableStateFlow(false)
    val speechListenerUiVisibility = _speechListenerUiVisibility.asStateFlow()

    private val _speechListenerAction = MutableStateFlow(SpeechAction.UNKNOWN)
    val speechListenerAction = _speechListenerAction.asStateFlow()

    init {
        activityContext.lifecycle.addObserver(this)
        if (isAvailable) {
            SpeechRecognizer.createSpeechRecognizer(context)
                .apply { setRecognitionListener(this@SpeechRecognizerHandler) }
                .also { speechRecognizer = it }
        } else Timber.w("Speech related permissions not provided!")
    }

    private fun startListening(isRestart: Boolean = false) {
        speechRecognizer?.run {
            Timber.w("${if (isRestart) "Restart" else "Start"} listening...".uppercase())
            if (isRestart) {
                //speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            }
            speechRecognizer?.startListening(speechIntent)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        startListening()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
        super.onDestroy(owner)
    }

    fun release() {
        speechRecognizer?.run {
            Timber.w("Releasing speech recognizer".uppercase())
            stopListening()
            cancel()
            destroy()
        }
        speechRecognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Timber.i("onReadyForSpeech")
        _speechListenerUiVisibility.value = true
    }

    override fun onBeginningOfSpeech() {
        Timber.i("onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Timber.i("onEndOfSpeech")
    }

    override fun onError(error: Int) {
        Timber.w("Speech error: ${getErrorText(error)}")
        if (error == SpeechRecognizer.ERROR_NO_MATCH) {
            activityContext.lifecycleScope.launch {
                delay(1000)
                startListening(true)
            }
        } //else _speechListenerUiVisibility.value = false
    }

    override fun onResults(results: Bundle?) {
        Timber.i("onResults")
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        onResultFound(matches)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        Timber.i("onPartialResults")
        val targets = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val sources = partialResults?.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
        targets?.map { Timber.d("Partial target: $it") }
        sources?.map { Timber.d("Partial source: $it") }
        onResultFound(targets)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Timber.i("onEvent: $eventType")
    }

    private fun onResultFound(matches: List<String>?) {
        if (matches.isNullOrEmpty() || isRecognized) return
        for (text in matches) {
            Timber.i("Matches: $text")
            if (text.equals("Accept", ignoreCase = true)
                || text.equals("Except", ignoreCase = true)
            ) {
                Timber.w("Offer accepted via SpeechRecognizer...".uppercase())
                isRecognized = true
                release()
                _speechListenerAction.value = SpeechAction.ACCEPT
                break
            } else if (text.equals("Reject", ignoreCase = true)) {
                Timber.d("Offer rejected via SpeechRecognizer...".uppercase())
                isRecognized = true
                release()
                _speechListenerAction.value = SpeechAction.REJECT
                break
            }
        }
    }

    private fun getErrorText(errorCode: Int) = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
        SpeechRecognizer.ERROR_SERVER -> "error from server"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
        else -> "Didn't understand, please try again."
    }

    //    private var speechRecognizer: SpeechRecognizer? = null
//
//    private var isRecognized = false
//    private var offerTimeoutInSecs = 0
//    private var startedListeningAtTimeInMillis = 0L
//
//    init {
//        (activityRef as AppCompatActivity).lifecycle.addObserver(this)
//    }
//
//    private val _speechListenerUiVisibility = MutableStateFlow(false)
//    val speechListenerUiVisibility = _speechListenerUiVisibility.asStateFlow()
//
//    private val _speechListenerAction = MutableStateFlow(SpeechAction.UNKNOWN)
//    val speechListenerAction = _speechListenerAction.asStateFlow()
//
//    private val isAvailable: Boolean
//        get() = activityRef.isPermissionGranted(Manifest.permission.RECORD_AUDIO)
//                && SpeechRecognizer.isRecognitionAvailable(activityRef.applicationContext)
//
//    fun startListening(timeoutInSecs: Int) {
//        if (isAvailable) {
//            offerTimeoutInSecs = timeoutInSecs - 1
//            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activityRef)
//            speechRecognizer?.setRecognitionListener(this)
//            _speechListenerAction.value = SpeechAction.UNKNOWN
//            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
//                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activityRef.packageName)
//                putExtra(EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 100)
//                //putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
//                putExtra(EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, offerTimeoutInSecs * 1000)
//            }.also {
//                startedListeningAtTimeInMillis = System.currentTimeMillis()
//                speechRecognizer?.startListening(it)
//            }
//        } else Timber.w("Missing permission to initialize Speech Recognizer!!!")
//    }
//
//    fun stopListening() {
//        _speechListenerUiVisibility.value = false
//        speechRecognizer?.let {
//            Timber.w("SpeechRecognizer stopListening...")
//            it.stopListening()
//            it.destroy()
//        }
//        speechRecognizer = null
//    }
//
//    override fun onStop(owner: LifecycleOwner) {
//        stopListening()
//        super.onStop(owner)
//    }
//
//    override fun onReadyForSpeech(bundle: Bundle?) {
//        Timber.i("onReadyForSpeech")
//        _speechListenerUiVisibility.value = true
//    }
//
//    override fun onBeginningOfSpeech() {
//        Timber.i("onBeginningOfSpeech")
//    }
//
//    override fun onRmsChanged(p0: Float) {}
//    override fun onBufferReceived(p0: ByteArray?) {}
//
//    override fun onEndOfSpeech() {
//        Timber.i("onEndOfSpeech")
//    }
//
//    override fun onError(errCode: Int) {
//        Timber.w("Speech error: ${getErrorText(errCode)}")
//        // Handling(stopping and starting listening again) ERROR_NO_MATCH
//        // scenario which occurs in some devices due to google speech recognizer issue
//        if (errCode == SpeechRecognizer.ERROR_NO_MATCH) {
//            val diffInMillis = System.currentTimeMillis() - startedListeningAtTimeInMillis
//            val actualDiff = offerTimeoutInSecs - (diffInMillis / 1000)
//            // Start only if timer is 3 secs or more
//            if (actualDiff > 2) {
//                Timber.w("Restarting speech listener again...".uppercase())
//                // Restart listening again
//                stopListening()
//                startListening(timeoutInSecs = offerTimeoutInSecs)
//            } else _speechListenerUiVisibility.value = false
//        } else _speechListenerUiVisibility.value = false
//    }
//
//    override fun onResults(results: Bundle?) {
//        Timber.i("onResults")
//        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//        onResultFound(matches)
//    }
//
//    override fun onPartialResults(partialResults: Bundle?) {
//        Timber.i("onPartialResults")
//        val targets = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//        val sources = partialResults?.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
//        targets?.map { Timber.d("Partial target: $it") }
//        sources?.map { Timber.d("Partial source: $it") }
//        onResultFound(targets)
//    }
//
//    override fun onEvent(eventType: Int, p1: Bundle?) {
//        Timber.i("onEvent: $eventType")
//    }
//
//    private fun onResultFound(matches: List<String>?) {
//        if (matches.isNullOrEmpty() || isRecognized) return
//        for (text in matches) {
//            Timber.i("Matches: $text")
//            if (text.equals("Accept", ignoreCase = true)
//                || text.equals("Except", ignoreCase = true)
//            ) {
//                Timber.d("Offer accepted via SpeechRecognizer...")
//                isRecognized = true
//                stopListening()
//                // Sending changes to activity
//                _speechListenerUiVisibility.value = false
//                _speechListenerAction.value = SpeechAction.ACCEPT
//                break
//            } else if (text.equals("Reject", ignoreCase = true)) {
//                Timber.d("Offer rejected via SpeechRecognizer...")
//                isRecognized = true
//                stopListening()
//                // Sending changes to activity
//                _speechListenerUiVisibility.value = false
//                _speechListenerAction.value = SpeechAction.REJECT
//                break
//            }
//        }
//    }
//
//    private fun getErrorText(errorCode: Int) = when (errorCode) {
//        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
//        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
//        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
//        SpeechRecognizer.ERROR_NETWORK -> "Network error"
//        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
//        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
//        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
//        SpeechRecognizer.ERROR_SERVER -> "error from server"
//        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
//        else -> "Didn't understand, please try again."
//    }
}