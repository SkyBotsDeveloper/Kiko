package com.skybots.kiko

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.skybots.kiko.orbit.FloatingOrbitCommand
import com.skybots.kiko.orbit.FloatingOrbitController
import com.skybots.kiko.orbit.FloatingOrbitRuntime
import com.skybots.kiko.orbit.FloatingOrbitService
import com.skybots.kiko.permissions.KikoPermission
import com.skybots.kiko.permissions.PermissionManager
import com.skybots.kiko.ui.KikoHomeScreen
import com.skybots.kiko.ui.KikoHomeUiState
import com.skybots.kiko.ui.KikoSettingsScreen
import com.skybots.kiko.ui.theme.KikoTheme
import com.skybots.kiko.utils.DiagnosticsLogger
import com.skybots.kiko.voice.SpeechRecognizerManager
import com.skybots.kiko.voice.TtsManager
import com.skybots.kiko.voice.VoiceInputState
import com.skybots.kiko.voice.VoiceOutputState
import com.skybots.kiko.wake.WakeWordControlResult
import com.skybots.kiko.wake.WakeDebugSettings
import com.skybots.kiko.wake.WakeDebugSettingsStore
import com.skybots.kiko.wake.WakeWordEngineState
import com.skybots.kiko.wake.WakeWordEvent
import com.skybots.kiko.wake.WakeMicArbitration
import com.skybots.kiko.wake.WakeWordRuntime
import com.skybots.kiko.wake.WakeWordService
import com.skybots.kiko.wake.WakeWordServiceController
import com.skybots.kiko.wake.WakeWordSensitivity
import com.skybots.kiko.wake.opensource.OpenSourceWakeConfig
import com.skybots.kiko.wake.opensource.TfliteWakeModelRunner
import com.skybots.kiko.wake.opensource.WakeModelAssetManager
import com.skybots.kiko.wake.opensource.WakeModelSelfTest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra(WakeWordService.EXTRA_WAKE_DETECTED, false) == true) {
            WakeWordRuntime.markPendingWakeLaunch()
        }
        if (intent?.getBooleanExtra(EXTRA_START_MANUAL_MIC, false) == true) {
            FloatingOrbitRuntime.requestManualMic()
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true) {
            FloatingOrbitRuntime.requestOpenSettings()
        }
        setContent {
            KikoTheme {
                KikoApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(WakeWordService.EXTRA_WAKE_DETECTED, false)) {
            WakeWordRuntime.markPendingWakeLaunch()
            WakeWordRuntime.publish(WakeWordEvent.WakeDetected)
        }
        if (intent.getBooleanExtra(EXTRA_START_MANUAL_MIC, false)) {
            FloatingOrbitRuntime.requestManualMic()
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false)) {
            FloatingOrbitRuntime.requestOpenSettings()
        }
    }

    companion object {
        const val EXTRA_START_MANUAL_MIC = "com.skybots.kiko.orbit.EXTRA_START_MANUAL_MIC"
        const val EXTRA_OPEN_SETTINGS = "com.skybots.kiko.orbit.EXTRA_OPEN_SETTINGS"
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
    val wakeWordController = remember(context) {
        WakeWordServiceController(
            context = context,
            permissionManager = permissionManager,
            memoryRepository = memoryRepository,
        )
    }
    val floatingOrbitController = remember(context) {
        FloatingOrbitController(context)
    }
    val mainHandler = remember {
        Handler(Looper.getMainLooper())
    }
    val wakeDebugSettingsStore = remember(context) {
        WakeDebugSettingsStore(context)
    }
    val wakeModelAssetManager = remember(context) {
        WakeModelAssetManager(context)
    }
    var wakeWordStatus by remember {
        mutableStateOf(wakeWordController.getStatus())
    }
    var wakeModelHealth by remember {
        mutableStateOf(TfliteWakeModelRunner.inspectModelHealth(wakeModelAssetManager, OpenSourceWakeConfig()))
    }
    var wakeDebugSettings by remember {
        mutableStateOf(wakeDebugSettingsStore.read())
    }
    var wakeScoreSnapshot by remember {
        mutableStateOf(WakeWordRuntime.currentScoreSnapshot())
    }
    var wakeSelfTestResult by remember {
        mutableStateOf("")
    }
    var pendingWakeEnableRequest by remember {
        mutableStateOf(false)
    }
    var floatingOrbitEnabled by remember {
        mutableStateOf(floatingOrbitController.settings().enabled)
    }
    var floatingOrbitPermissionGranted by remember {
        mutableStateOf(floatingOrbitController.hasOverlayPermission())
    }
    val showWakeDebugControls = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun refreshPermissionStatuses() {
        permissionStatuses = permissionManager.getPermissionStatuses()
    }

    fun refreshPreferences() {
        preferences = memoryRepository.getUserPreferences()
    }

    fun refreshWakeWordStatus() {
        wakeWordStatus = wakeWordController.getStatus()
        wakeModelHealth = TfliteWakeModelRunner.inspectModelHealth(wakeModelAssetManager, OpenSourceWakeConfig())
        wakeScoreSnapshot = WakeWordRuntime.currentScoreSnapshot()
        wakeDebugSettings = wakeDebugSettingsStore.read()
        floatingOrbitEnabled = floatingOrbitController.settings().enabled
        floatingOrbitPermissionGranted = floatingOrbitController.hasOverlayPermission()
    }

    fun updatePreferences(transform: (com.skybots.kiko.memory.UserPreferenceEntity) -> com.skybots.kiko.memory.UserPreferenceEntity) {
        memoryRepository.updateUserPreferences(transform(preferences))
        refreshPreferences()
        refreshWakeWordStatus()
    }

    fun applyWakeResult(result: WakeWordControlResult) {
        uiState = uiState.copy(
            runtimeState = if (result.state == WakeWordEngineState.Error) {
                AssistantRuntimeState.ERROR
            } else {
                uiState.runtimeState
            },
            statusMessage = result.message,
            kikoResponse = result.message,
        )
        refreshPreferences()
        refreshPermissionStatuses()
        refreshWakeWordStatus()
    }

    fun restartWakeIfEnabled() {
        if (memoryRepository.getUserPreferences().wakeWordEnabled) {
            applyWakeResult(wakeWordController.startService())
        }
    }

    fun refreshFloatingOrbit() {
        floatingOrbitEnabled = floatingOrbitController.settings().enabled
        floatingOrbitPermissionGranted = floatingOrbitController.hasOverlayPermission()
    }

    fun showFloatingListeningIfEnabled() {
        if (floatingOrbitController.settings().enabled && floatingOrbitController.hasOverlayPermission()) {
            context.startService(FloatingOrbitService.showListeningIntent(context))
        }
    }

    val wakeMicArbitration = remember(wakeWordController, memoryRepository) {
        WakeMicArbitration(
            isWakeEnabled = { memoryRepository.getUserPreferences().wakeWordEnabled },
            currentWakeState = { wakeWordController.getStatus() },
            stopWakeService = { wakeWordController.stopService() },
            startWakeService = { wakeWordController.startService() },
        )
    }

    val ttsManager = remember(context) {
        TtsManager(context) { outputState ->
            uiState = when (outputState) {
                VoiceOutputState.Idle -> {
                    if (uiState.runtimeState == AssistantRuntimeState.SPEAKING) {
                        if (wakeMicArbitration.resumeIfNeeded()) {
                            refreshWakeWordStatus()
                        }
                        floatingOrbitController.startIfEnabled()
                        refreshFloatingOrbit()
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
                ).also {
                    if (wakeMicArbitration.resumeIfNeeded()) {
                        refreshWakeWordStatus()
                    }
                    floatingOrbitController.startIfEnabled()
                    refreshFloatingOrbit()
                }
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
                KikoPermission.POST_NOTIFICATIONS.androidPermission,
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
        } else if (wakeMicArbitration.resumeIfNeeded()) {
            refreshWakeWordStatus()
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
                    ).also {
                        if (wakeMicArbitration.resumeIfNeeded()) {
                            refreshWakeWordStatus()
                        }
                    }
                }
            },
            onFinalResult = ::processTranscript,
        )
    }

    fun startManualVoiceInput() {
        ttsManager.stop()
        showFloatingListeningIfEnabled()
        val pausedWake = wakeMicArbitration.pauseForManualMic()
        refreshWakeWordStatus()
        val startListening = {
            if (permissionManager.hasRecordAudioPermission()) {
                speechRecognizerManager.startListening()
            }
        }
        if (pausedWake) {
            mainHandler.postDelayed(startListening, WAKE_MIC_RELEASE_DELAY_MS)
        } else {
            startListening()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        refreshPermissionStatuses()
        if (pendingWakeEnableRequest) {
            pendingWakeEnableRequest = false
            if (granted) {
                applyWakeResult(wakeWordController.enableWakeWord())
            } else {
                wakeWordStatus = WakeWordEngineState.PermissionMissing
                uiState = uiState.copy(
                    runtimeState = AssistantRuntimeState.ERROR,
                    kikoResponse = "Microphone permission is needed before Hey Kiko can be enabled.",
                    statusMessage = "Microphone permission denied.",
                )
            }
        } else if (granted) {
            startManualVoiceInput()
        } else {
            uiState = uiState.copy(
                runtimeState = AssistantRuntimeState.ERROR,
                kikoResponse = "Microphone permission is needed for manual voice input.",
                statusMessage = "Microphone permission denied.",
            )
        }
    }

    fun startListeningFromWakeWord() {
        refreshPermissionStatuses()
        if (uiState.runtimeState == AssistantRuntimeState.LISTENING ||
            uiState.runtimeState == AssistantRuntimeState.PROCESSING
        ) {
            uiState = uiState.copy(statusMessage = "Wake heard while Kiko is already listening.")
            return
        }
        if (permissionManager.hasRecordAudioPermission()) {
            ttsManager.stop()
            val pausedWake = wakeMicArbitration.pauseAfterWakeDetected()
            refreshWakeWordStatus()
            uiState = uiState.copy(
                runtimeState = AssistantRuntimeState.LISTENING,
                statusMessage = "Hey Kiko heard. Listening.",
            )
            val startListening = {
                if (permissionManager.hasRecordAudioPermission()) {
                    speechRecognizerManager.startListeningFromWakeWord()
                }
            }
            if (pausedWake) {
                mainHandler.postDelayed(startListening, WAKE_MIC_RELEASE_DELAY_MS)
            } else {
                startListening()
            }
        } else {
            DiagnosticsLogger.permissionMissing(KikoPermission.RECORD_AUDIO)
            wakeWordStatus = WakeWordEngineState.PermissionMissing
            uiState = uiState.copy(
                runtimeState = AssistantRuntimeState.ERROR,
                kikoResponse = "Microphone permission is needed for Hey Kiko.",
                statusMessage = "Microphone permission missing.",
            )
        }
    }

    DisposableEffect(speechRecognizerManager) {
        val unsubscribe = WakeWordRuntime.subscribe { event ->
            when (event) {
                WakeWordEvent.Started -> {
                    wakeWordStatus = WakeWordEngineState.Listening
                    uiState = uiState.copy(statusMessage = "Hey Kiko wake word is listening.")
                }
                WakeWordEvent.Stopped -> {
                    wakeWordStatus = wakeWordController.getStatus()
                    uiState = uiState.copy(statusMessage = "Wake-word service stopped.")
                }
                WakeWordEvent.PausedLocked -> {
                    wakeWordStatus = WakeWordEngineState.PausedLocked
                    uiState = uiState.copy(statusMessage = "Wake paused while phone is locked.")
                }
                WakeWordEvent.WakeDetected -> {
                    wakeWordStatus = WakeWordEngineState.WakeDetected
                    startListeningFromWakeWord()
                }
                is WakeWordEvent.ScoreDebug -> {
                    wakeScoreSnapshot = event.snapshot
                }
                is WakeWordEvent.Error -> {
                    wakeWordStatus = WakeWordEngineState.Error
                    uiState = uiState.copy(
                        runtimeState = AssistantRuntimeState.ERROR,
                        kikoResponse = event.message,
                        statusMessage = event.message,
                    )
                }
            }
        }
        onDispose { unsubscribe() }
    }

    DisposableEffect(speechRecognizerManager, floatingOrbitController) {
        val unsubscribe = FloatingOrbitRuntime.subscribe { command ->
            when (command) {
                FloatingOrbitCommand.StartManualMic -> {
                    if (permissionManager.hasRecordAudioPermission()) {
                        startManualVoiceInput()
                    } else {
                        DiagnosticsLogger.permissionMissing(KikoPermission.RECORD_AUDIO)
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                FloatingOrbitCommand.OpenSettings -> {
                    refreshPreferences()
                    refreshPermissionStatuses()
                    refreshWakeWordStatus()
                    systemBrightnessControlAllowed = Settings.System.canWrite(context)
                    showSettings = true
                }
            }
        }
        onDispose { unsubscribe() }
    }

    LaunchedEffect(Unit) {
        floatingOrbitController.startIfEnabled()
        refreshFloatingOrbit()
        if (FloatingOrbitRuntime.consumePendingOpenSettings() ||
            activity.intent?.getBooleanExtra(MainActivity.EXTRA_OPEN_SETTINGS, false) == true
        ) {
            activity.intent?.removeExtra(MainActivity.EXTRA_OPEN_SETTINGS)
            refreshPreferences()
            refreshPermissionStatuses()
            refreshWakeWordStatus()
            systemBrightnessControlAllowed = Settings.System.canWrite(context)
            showSettings = true
        }
        if (FloatingOrbitRuntime.consumePendingManualMic() ||
            activity.intent?.getBooleanExtra(MainActivity.EXTRA_START_MANUAL_MIC, false) == true
        ) {
            activity.intent?.removeExtra(MainActivity.EXTRA_START_MANUAL_MIC)
            if (permissionManager.hasRecordAudioPermission()) {
                startManualVoiceInput()
            } else {
                DiagnosticsLogger.permissionMissing(KikoPermission.RECORD_AUDIO)
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        if (WakeWordRuntime.consumePendingWakeLaunch() ||
            activity.intent?.getBooleanExtra(WakeWordService.EXTRA_WAKE_DETECTED, false) == true
        ) {
            activity.intent?.removeExtra(WakeWordService.EXTRA_WAKE_DETECTED)
            startListeningFromWakeWord()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizerManager.destroy()
            ttsManager.shutdown()
        }
    }

    if (showSettings) {
        BackHandler {
            refreshPermissionStatuses()
            systemBrightnessControlAllowed = Settings.System.canWrite(context)
            refreshWakeWordStatus()
            showSettings = false
        }
        KikoSettingsScreen(
            preferences = preferences,
            permissionStatuses = permissionStatuses,
            exportedJson = exportedMemoryJson,
            importJson = importMemoryJson,
            systemBrightnessControlAllowed = systemBrightnessControlAllowed,
            wakeWordStatus = wakeWordStatus,
            wakeModelHealth = wakeModelHealth,
            wakeDebugSettings = wakeDebugSettings,
            wakeScoreSnapshot = wakeScoreSnapshot,
            wakeSelfTestResult = wakeSelfTestResult,
            floatingOrbitEnabled = floatingOrbitEnabled,
            floatingOrbitPermissionGranted = floatingOrbitPermissionGranted,
            showWakeWordTestControls = showWakeDebugControls,
            showWakeDebugControls = showWakeDebugControls,
            onBackClick = {
                refreshPermissionStatuses()
                systemBrightnessControlAllowed = Settings.System.canWrite(context)
                refreshWakeWordStatus()
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
            onWakeWordEnabledChange = { enabled ->
                if (enabled) {
                    refreshPermissionStatuses()
                    val result = wakeWordController.enableWakeWord()
                    if (result.state == WakeWordEngineState.PermissionMissing) {
                        pendingWakeEnableRequest = true
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    applyWakeResult(result)
                } else {
                    applyWakeResult(wakeWordController.disableWakeWord())
                }
            },
            onWakeWordEngineChange = { engine ->
                updatePreferences { it.copy(wakeWordEngine = engine) }
                if (preferences.wakeWordEnabled) {
                    applyWakeResult(wakeWordController.startService())
                }
            },
            onWakeWordSensitivityChange = { sensitivity: WakeWordSensitivity ->
                updatePreferences { it.copy(wakeWordSensitivity = sensitivity.name) }
                if (preferences.wakeWordEnabled) {
                    applyWakeResult(wakeWordController.startService())
                }
            },
            onWakeDebugEnabledChange = { enabled ->
                wakeDebugSettings = wakeDebugSettings.copy(enabled = enabled)
                wakeDebugSettingsStore.update(wakeDebugSettings)
                restartWakeIfEnabled()
            },
            onWakeDebugThresholdChange = { threshold ->
                wakeDebugSettings = wakeDebugSettings.copy(threshold = threshold)
                wakeDebugSettingsStore.update(wakeDebugSettings)
                if (wakeDebugSettings.enabled) {
                    restartWakeIfEnabled()
                }
            },
            onAllowUnsafeCalibrationChange = { allow ->
                wakeDebugSettings = wakeDebugSettings.copy(allowUnsafeCalibration = allow)
                wakeDebugSettingsStore.update(wakeDebugSettings)
                if (wakeDebugSettings.enabled) {
                    restartWakeIfEnabled()
                }
            },
            onFloatingOrbitEnabledChange = { enabled ->
                val result = floatingOrbitController.setEnabled(enabled)
                refreshFloatingOrbit()
                uiState = uiState.copy(statusMessage = result.message, kikoResponse = result.message)
            },
            onOpenOverlayPermissionClick = {
                floatingOrbitController.openOverlaySettings()
            },
            onStartFloatingOrbitClick = {
                val result = floatingOrbitController.start()
                refreshFloatingOrbit()
                uiState = uiState.copy(statusMessage = result.message, kikoResponse = result.message)
            },
            onStopFloatingOrbitClick = {
                val result = floatingOrbitController.stop()
                refreshFloatingOrbit()
                uiState = uiState.copy(statusMessage = result.message, kikoResponse = result.message)
            },
            onUseInAppOrbitFallbackClick = {
                val result = floatingOrbitController.setEnabled(false)
                refreshFloatingOrbit()
                uiState = uiState.copy(statusMessage = result.message, kikoResponse = "Using in-app orbit fallback.")
            },
            onResetWakeScoreClick = {
                WakeWordRuntime.resetScore()
                wakeScoreSnapshot = WakeWordRuntime.currentScoreSnapshot()
                restartWakeIfEnabled()
                uiState = uiState.copy(statusMessage = "Wake score max reset.")
            },
            onRunWakeSelfTestClick = {
                wakeSelfTestResult = "Running wake score check..."
                Thread {
                    val result = WakeModelSelfTest(context).run()
                    mainHandler.post {
                        wakeSelfTestResult = result.summary()
                        uiState = uiState.copy(statusMessage = "Wake score check complete.")
                    }
                }.start()
            },
            onCopyWakeDebugSummaryClick = {
                val summary = "status=${wakeWordStatus.label} model=${wakeModelHealth.status.label} " +
                    wakeScoreSnapshot.summary()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Kiko wake debug", summary))
                uiState = uiState.copy(statusMessage = "Wake debug summary copied.")
            },
            onTestWakeWordClick = {
                applyWakeResult(wakeWordController.simulateWakeDetection())
            },
            onClearMemoryClick = {
                memoryRepository.clearAllMemory()
                clarificationManager.clear()
                exportedMemoryJson = ""
                importMemoryJson = ""
                refreshPreferences()
                refreshWakeWordStatus()
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
                    refreshWakeWordStatus()
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
            wakeWordState = wakeWordStatus,
            wakeModelHealth = wakeModelHealth,
            wakeScoreSnapshot = wakeScoreSnapshot,
            onMicClick = {
                refreshPermissionStatuses()
                if (permissionManager.hasRecordAudioPermission()) {
                    startManualVoiceInput()
                } else {
                    DiagnosticsLogger.permissionMissing(KikoPermission.RECORD_AUDIO)
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onSettingsClick = {
                refreshPreferences()
                refreshPermissionStatuses()
                refreshWakeWordStatus()
                systemBrightnessControlAllowed = Settings.System.canWrite(context)
                showSettings = true
            },
            onWakeStatusClick = {
                refreshPreferences()
                refreshPermissionStatuses()
                refreshWakeWordStatus()
                showSettings = true
            },
        )
    }
}

private const val WAKE_MIC_RELEASE_DELAY_MS = 350L
