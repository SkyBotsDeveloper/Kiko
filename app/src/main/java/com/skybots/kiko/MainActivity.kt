package com.skybots.kiko

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.skybots.kiko.actions.apps.AndroidAppLauncher
import com.skybots.kiko.actions.apps.AppMatcher
import com.skybots.kiko.actions.apps.PackageManagerInstalledAppRepository
import com.skybots.kiko.actions.apps.RealAppActionHandler
import com.skybots.kiko.actions.contacts.AndroidContactsRepository
import com.skybots.kiko.actions.contacts.AndroidPhoneActionLauncher
import com.skybots.kiko.actions.contacts.ContactMatcher
import com.skybots.kiko.actions.contacts.RealContactActionHandler
import com.skybots.kiko.actions.device.AlarmActionHandler
import com.skybots.kiko.actions.device.AlarmParser
import com.skybots.kiko.actions.device.AndroidAlarmLauncher
import com.skybots.kiko.actions.device.AndroidBrightnessController
import com.skybots.kiko.actions.device.AndroidFlashlightController
import com.skybots.kiko.actions.device.AndroidLocalReminderScheduler
import com.skybots.kiko.actions.device.AndroidVolumeController
import com.skybots.kiko.actions.device.BrightnessActionHandler
import com.skybots.kiko.actions.device.FlashlightActionHandler
import com.skybots.kiko.actions.device.RealDeviceActionHandler
import com.skybots.kiko.actions.device.ReminderActionHandler
import com.skybots.kiko.actions.device.ReminderParser
import com.skybots.kiko.actions.device.VolumeActionHandler
import com.skybots.kiko.assistant.AssistantOrchestrator
import com.skybots.kiko.assistant.AssistantRuntimeState
import com.skybots.kiko.assistant.clarification.ClarificationManager
import com.skybots.kiko.assistant.language.LanguageStyle
import com.skybots.kiko.assistant.language.ReplyStyle
import com.skybots.kiko.memory.KikoDatabase
import com.skybots.kiko.memory.RoomMemoryRepository
import com.skybots.kiko.permissions.KikoPermission
import com.skybots.kiko.permissions.PermissionManager
import com.skybots.kiko.ui.KikoHomeScreen
import com.skybots.kiko.ui.KikoHomeUiState
import com.skybots.kiko.ui.KikoSettingsScreen
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
    val activity = context as Activity
    val permissionManager = remember(context) {
        PermissionManager(context)
    }
    val memoryRepository = remember(context) {
        RoomMemoryRepository(KikoDatabase.create(context))
    }
    val clarificationManager = remember(memoryRepository) {
        ClarificationManager(memoryRepository = memoryRepository)
    }
    val contactsRepository = remember(context) {
        AndroidContactsRepository(
            context = context,
            permissionChecker = permissionManager,
        )
    }
    val orchestrator = remember(context) {
        AssistantOrchestrator(
            appActionHandler = RealAppActionHandler(
                installedAppRepository = PackageManagerInstalledAppRepository(context),
                appMatcher = AppMatcher(),
                appLauncher = AndroidAppLauncher(context),
                clarificationManager = clarificationManager,
                memoryRepository = memoryRepository,
            ),
            contactActionHandler = RealContactActionHandler(
                contactsRepository = contactsRepository,
                contactMatcher = ContactMatcher(),
                permissionChecker = permissionManager,
                phoneActionLauncher = AndroidPhoneActionLauncher(context),
                clarificationManager = clarificationManager,
                memoryRepository = memoryRepository,
            ),
            deviceActionHandler = RealDeviceActionHandler(
                flashlightActionHandler = FlashlightActionHandler(
                    flashlightController = AndroidFlashlightController(context),
                ),
                volumeActionHandler = VolumeActionHandler(
                    volumeController = AndroidVolumeController(context),
                ),
                brightnessActionHandler = BrightnessActionHandler(
                    brightnessController = AndroidBrightnessController(activity),
                ),
                alarmActionHandler = AlarmActionHandler(
                    alarmParser = AlarmParser(),
                    alarmLauncher = AndroidAlarmLauncher(context),
                ),
                reminderActionHandler = ReminderActionHandler(
                    reminderParser = ReminderParser(),
                    reminderRepository = memoryRepository,
                    reminderScheduler = AndroidLocalReminderScheduler(
                        context = context,
                        permissionChecker = permissionManager,
                    ),
                ),
            ),
            clarificationManager = clarificationManager,
            memoryRepository = memoryRepository,
        )
    }

    var permissionStatuses by remember {
        mutableStateOf(permissionManager.getPermissionStatuses())
    }
    var uiState by remember {
        mutableStateOf(KikoHomeUiState())
    }
    var preferences by remember {
        mutableStateOf(memoryRepository.getUserPreferences())
    }
    var showSettings by remember {
        mutableStateOf(false)
    }
    var exportedMemoryJson by remember {
        mutableStateOf("")
    }
    var importMemoryJson by remember {
        mutableStateOf("")
    }
    var systemBrightnessControlAllowed by remember {
        mutableStateOf(Settings.System.canWrite(context))
    }

    fun refreshPermissionStatuses() {
        permissionStatuses = permissionManager.getPermissionStatuses()
    }

    fun refreshPreferences() {
        preferences = memoryRepository.getUserPreferences()
    }

    fun updatePreferences(transform: (com.skybots.kiko.memory.UserPreferenceEntity) -> com.skybots.kiko.memory.UserPreferenceEntity) {
        memoryRepository.updateUserPreferences(transform(preferences))
        refreshPreferences()
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

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        refreshPermissionStatuses()
        if (granted) {
            contactsRepository.refresh()
            uiState = uiState.copy(
                runtimeState = AssistantRuntimeState.IDLE,
                kikoResponse = "Contacts permission granted. Please say the call command again.",
                statusMessage = "Contacts permission granted.",
            )
        } else {
            uiState = uiState.copy(
                runtimeState = AssistantRuntimeState.ERROR,
                kikoResponse = "Contacts permission is needed to find and call people.",
                statusMessage = "Contacts permission denied.",
            )
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        refreshPermissionStatuses()
        uiState = if (granted) {
            uiState.copy(
                runtimeState = AssistantRuntimeState.IDLE,
                statusMessage = "Notification permission granted.",
            )
        } else {
            uiState.copy(
                runtimeState = AssistantRuntimeState.ERROR,
                statusMessage = "Notification permission denied.",
            )
        }
    }

    fun requestActionPermission(permission: KikoPermission?) {
        when (permission) {
            KikoPermission.READ_CONTACTS -> contactsPermissionLauncher.launch(
                Manifest.permission.READ_CONTACTS,
            )
            KikoPermission.POST_NOTIFICATIONS -> notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS,
            )
            null,
            KikoPermission.RECORD_AUDIO,
            KikoPermission.CALL_PHONE,
            KikoPermission.CAMERA -> Unit
        }
    }

    fun processTranscript(transcript: String) {
        uiState = uiState.copy(
            transcript = transcript,
            runtimeState = AssistantRuntimeState.EXECUTING,
            statusMessage = "Executing locally.",
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
        refreshPreferences()
        if (memoryRepository.getUserPreferences().voiceEnabled) {
            ttsManager.speak(result.response, result.intent.languageHint)
        }
        requestActionPermission(result.requestedPermission)
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

    if (showSettings) {
        KikoSettingsScreen(
            preferences = preferences,
            permissionStatuses = permissionStatuses,
            exportedJson = exportedMemoryJson,
            importJson = importMemoryJson,
            systemBrightnessControlAllowed = systemBrightnessControlAllowed,
            onBackClick = {
                refreshPermissionStatuses()
                systemBrightnessControlAllowed = Settings.System.canWrite(context)
                showSettings = false
            },
            onVoiceEnabledChange = { enabled ->
                updatePreferences { it.copy(voiceEnabled = enabled) }
            },
            onLanguageStyleChange = { style: LanguageStyle ->
                updatePreferences { it.copy(preferredLanguageStyle = style.name) }
            },
            onReplyStyleChange = { style: ReplyStyle ->
                updatePreferences { it.copy(replyStyle = style.name) }
            },
            onPersonalizationEnabledChange = { enabled ->
                updatePreferences { it.copy(personalizationEnabled = enabled) }
            },
            onSaveInteractionSummariesChange = { enabled ->
                updatePreferences { it.copy(saveInteractionSummaries = enabled) }
            },
            onClearMemoryClick = {
                memoryRepository.clearAllMemory()
                clarificationManager.clear()
                exportedMemoryJson = ""
                importMemoryJson = ""
                uiState = uiState.copy(statusMessage = "Local memory cleared.")
            },
            onExportMemoryClick = {
                exportedMemoryJson = memoryRepository.exportMemoryJson()
                uiState = uiState.copy(statusMessage = "Memory JSON exported locally.")
            },
            onImportJsonChange = { importMemoryJson = it },
            onImportMemoryClick = {
                runCatching {
                    memoryRepository.importMemoryJson(importMemoryJson)
                    refreshPreferences()
                    exportedMemoryJson = ""
                    uiState = uiState.copy(statusMessage = "Memory JSON imported.")
                }.getOrElse { error ->
                    uiState = uiState.copy(
                        runtimeState = AssistantRuntimeState.ERROR,
                        statusMessage = error.message ?: "Memory import failed.",
                    )
                }
            },
            onAllowBrightnessControlClick = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                }
            },
        )
    } else {
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
            onSettingsClick = {
                refreshPreferences()
                refreshPermissionStatuses()
                systemBrightnessControlAllowed = Settings.System.canWrite(context)
                showSettings = true
            },
        )
    }
}
