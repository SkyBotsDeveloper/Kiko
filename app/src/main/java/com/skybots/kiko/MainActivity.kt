package com.skybots.kiko

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.skybots.kiko.assistant.AssistantOrchestrator
import com.skybots.kiko.assistant.AssistantRuntimeState
import com.skybots.kiko.permissions.PermissionManager
import com.skybots.kiko.ui.KikoHomeScreen
import com.skybots.kiko.ui.KikoHomeUiState
import com.skybots.kiko.ui.theme.KikoTheme
import com.skybots.kiko.voice.SpeechRecognizerManager
import com.skybots.kiko.voice.TtsManager
import com.skybots.kiko.voice.VoiceInputState
import com.skybots.kiko.voice.VoiceOutputState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KikoTheme {
                KikoApp()
            }
        }
    }
}

@Composable
private fun KikoApp() {
    val context = LocalContext.current
    val permissionManager = remember(context) {
        PermissionManager(context)
    }
    val orchestrator = remember {
        AssistantOrchestrator()
    }

    var permissionStatuses by remember {
        mutableStateOf(permissionManager.getPermissionStatuses())
    }
    var uiState by remember {
        mutableStateOf(KikoHomeUiState())
    }

    fun refreshPermissionStatuses() {
        permissionStatuses = permissionManager.getPermissionStatuses()
    }

    val ttsManager = remember(context) {
        TtsManager(context) { outputState ->
            uiState = when (outputState) {
                VoiceOutputState.Idle -> {
                    if (uiState.runtimeState == AssistantRuntimeState.SPEAKING) {
                        uiState.copy(
                            runtimeState = AssistantRuntimeState.IDLE,
                            statusMessage = "Ready for manual voice input.",
                        )
                    } else {
                        uiState
                    }
                }
                VoiceOutputState.Initializing -> uiState.copy(
                    statusMessage = "Preparing voice output.",
                )
                VoiceOutputState.Speaking -> uiState.copy(
                    runtimeState = AssistantRuntimeState.SPEAKING,
                    statusMessage = "Speaking response.",
                )
                is VoiceOutputState.Error -> uiState.copy(
                    runtimeState = AssistantRuntimeState.ERROR,
                    statusMessage = outputState.message,
                )
            }
        }
    }

    fun processTranscript(transcript: String) {
        uiState = uiState.copy(
            transcript = transcript,
            runtimeState = AssistantRuntimeState.PROCESSING,
            statusMessage = "Processing locally.",
        )

        val result = orchestrator.processTranscript(transcript)
        uiState = uiState.copy(
            kikoResponse = result.response,
            runtimeState = if (result.runtimeState == AssistantRuntimeState.ERROR) {
                AssistantRuntimeState.ERROR
            } else {
                AssistantRuntimeState.SPEAKING
            },
            statusMessage = result.errorMessage ?: "Response ready.",
        )
        ttsManager.speak(result.response, result.intent.languageHint)
    }

    val speechRecognizerManager = remember(context) {
        SpeechRecognizerManager(
            context = context,
            onStateChange = { inputState ->
                uiState = when (inputState) {
                    VoiceInputState.Idle -> uiState.copy(
                        runtimeState = AssistantRuntimeState.IDLE,
                        statusMessage = "Ready for manual voice input.",
                    )
                    VoiceInputState.Listening -> uiState.copy(
                        runtimeState = AssistantRuntimeState.LISTENING,
                        statusMessage = "Listening.",
                    )
                    VoiceInputState.Processing -> uiState.copy(
                        runtimeState = AssistantRuntimeState.PROCESSING,
                        statusMessage = "Processing speech.",
                    )
                    is VoiceInputState.Error -> uiState.copy(
                        runtimeState = AssistantRuntimeState.ERROR,
                        kikoResponse = inputState.message,
                        statusMessage = inputState.message,
                    )
                }
            },
            onFinalResult = ::processTranscript,
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        refreshPermissionStatuses()
        if (granted) {
            speechRecognizerManager.startListening()
        } else {
            uiState = uiState.copy(
                runtimeState = AssistantRuntimeState.ERROR,
                kikoResponse = "Microphone permission is needed for manual voice input.",
                statusMessage = "Microphone permission denied.",
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizerManager.destroy()
            ttsManager.shutdown()
        }
    }

    KikoHomeScreen(
        uiState = uiState,
        permissionStatuses = permissionStatuses,
        onMicClick = {
            refreshPermissionStatuses()
            if (permissionManager.hasRecordAudioPermission()) {
                ttsManager.stop()
                speechRecognizerManager.startListening()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
    )
}
