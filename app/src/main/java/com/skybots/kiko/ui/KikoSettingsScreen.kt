package com.skybots.kiko.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skybots.kiko.assistant.language.LanguageStyle
import com.skybots.kiko.assistant.language.ReplyStyle
import com.skybots.kiko.assistant.language.languageStyleFrom
import com.skybots.kiko.assistant.language.replyStyleFrom
import com.skybots.kiko.creator.CreatorIdentity
import com.skybots.kiko.memory.UserPreferenceEntity
import com.skybots.kiko.permissions.KikoPermission
import com.skybots.kiko.permissions.PermissionStatus
import com.skybots.kiko.ui.theme.KikoAccent
import com.skybots.kiko.ui.theme.KikoBackground
import com.skybots.kiko.ui.theme.KikoBorder
import com.skybots.kiko.ui.theme.KikoMutedText
import com.skybots.kiko.ui.theme.KikoSurface
import com.skybots.kiko.wake.WakeWordConfig
import com.skybots.kiko.wake.WakeWordEngineState
import com.skybots.kiko.wake.WakeWordSensitivity
import com.skybots.kiko.wake.opensource.WakeEngineHealth
import com.skybots.kiko.wake.wakeWordSensitivityFrom

@Composable
fun KikoSettingsScreen(
    preferences: UserPreferenceEntity,
    permissionStatuses: List<PermissionStatus>,
    exportedJson: String,
    importJson: String,
    systemBrightnessControlAllowed: Boolean,
    wakeWordStatus: WakeWordEngineState,
    wakeModelHealth: WakeEngineHealth,
    showWakeWordTestControls: Boolean,
    onBackClick: () -> Unit,
    onVoiceEnabledChange: (Boolean) -> Unit,
    onLanguageStyleChange: (LanguageStyle) -> Unit,
    onReplyStyleChange: (ReplyStyle) -> Unit,
    onPersonalizationEnabledChange: (Boolean) -> Unit,
    onSaveInteractionSummariesChange: (Boolean) -> Unit,
    onWakeWordEnabledChange: (Boolean) -> Unit,
    onWakeWordEngineChange: (String) -> Unit,
    onWakeWordSensitivityChange: (WakeWordSensitivity) -> Unit,
    onTestWakeWordClick: () -> Unit,
    onClearMemoryClick: () -> Unit,
    onExportMemoryClick: () -> Unit,
    onImportJsonChange: (String) -> Unit,
    onImportMemoryClick: () -> Unit,
    onAllowBrightnessControlClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KikoBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        SettingsHeader(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreferenceSection(title = "Voice and replies") {
                ToggleRow(
                    title = "Voice replies",
                    subtitle = "Speak Kiko responses with Android TextToSpeech.",
                    checked = preferences.voiceEnabled,
                    onCheckedChange = onVoiceEnabledChange,
                )
                ChipGroup(
                    title = "Reply language",
                    options = listOf(
                        LanguageStyle.AUTO to "Auto mirror user",
                        LanguageStyle.ENGLISH to "English",
                        LanguageStyle.HINGLISH to "Hinglish",
                        LanguageStyle.HINDI to "Hindi",
                    ),
                    selected = languageStyleFrom(preferences.preferredLanguageStyle),
                    onSelected = onLanguageStyleChange,
                )
                ChipGroup(
                    title = "Reply style",
                    options = listOf(
                        ReplyStyle.SHORT to "Short",
                        ReplyStyle.FRIENDLY to "Friendly",
                        ReplyStyle.PROFESSIONAL to "Professional",
                    ),
                    selected = replyStyleFrom(preferences.replyStyle),
                    onSelected = onReplyStyleChange,
                )
            }

            PreferenceSection(title = "Wake word") {
                ToggleRow(
                    title = "Enable \"Hey Kiko\"",
                    subtitle = "Optional foreground wake-word listening. Off by default.",
                    checked = preferences.wakeWordEnabled,
                    onCheckedChange = onWakeWordEnabledChange,
                )
                PermissionLine(
                    label = "Status",
                    value = wakeWordStatus.label,
                )
                PermissionLine(
                    label = "Wake phrase",
                    value = WakeWordConfig.DEFAULT_PHRASE,
                )
                ChipGroup(
                    title = "Engine",
                    options = listOf(
                        WakeWordConfig.ENGINE_OPEN_SOURCE to "Open-source local",
                        WakeWordConfig.ENGINE_FAKE to "Fake/Test",
                    ),
                    selected = preferences.wakeWordEngine.ifBlank { WakeWordConfig.ENGINE_OPEN_SOURCE },
                    onSelected = onWakeWordEngineChange,
                )
                PermissionLine(
                    label = "Model status",
                    value = wakeModelHealth.status.label,
                )
                ChipGroup(
                    title = "Sensitivity",
                    options = listOf(
                        WakeWordSensitivity.LOW to "Low",
                        WakeWordSensitivity.BALANCED to "Balanced",
                        WakeWordSensitivity.HIGH to "High",
                    ),
                    selected = wakeWordSensitivityFrom(preferences.wakeWordSensitivity),
                    onSelected = onWakeWordSensitivityChange,
                )
                Text(
                    text = wakeWordHelpText(wakeModelHealth),
                    color = KikoMutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (showWakeWordTestControls) {
                    Button(
                        onClick = onTestWakeWordClick,
                        enabled = preferences.wakeWordEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KikoAccent),
                    ) {
                        Text("Test wake flow / Simulate Hey Kiko")
                    }
                }
            }

            PreferenceSection(title = "Personalization") {
                ToggleRow(
                    title = "Personalization",
                    subtitle = "Remember useful aliases after you approve them.",
                    checked = preferences.personalizationEnabled,
                    onCheckedChange = onPersonalizationEnabledChange,
                )
                ToggleRow(
                    title = "Interaction summaries",
                    subtitle = "Save structured summaries, not raw conversations.",
                    checked = preferences.saveInteractionSummaries,
                    onCheckedChange = onSaveInteractionSummariesChange,
                )
                OutlinedButton(
                    onClick = onClearMemoryClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Clear local memory")
                }
            }

            PreferenceSection(title = "Permissions") {
                PermissionGuidance(
                    title = "Microphone",
                    body = "Needed only when you tap the mic for manual voice input.",
                )
                PermissionGuidance(
                    title = "Contacts",
                    body = "Requested only when a call command needs contact lookup.",
                )
                PermissionGuidance(
                    title = "Phone calls",
                    body = "If direct call permission is missing, Kiko opens the dialer instead.",
                )
                PermissionGuidance(
                    title = "Notifications",
                    body = "Reminders can be saved without notification permission, but alerts need permission.",
                )
                PermissionLine(
                    label = "System brightness",
                    value = if (systemBrightnessControlAllowed) "Allowed" else "Needs extra permission",
                )
                if (!systemBrightnessControlAllowed) {
                    Text(
                        text = "System-wide brightness needs extra permission. Without it, Kiko can adjust only its own screen brightness.",
                        color = KikoMutedText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = onAllowBrightnessControlClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KikoAccent),
                    ) {
                        Text("Allow system brightness control")
                    }
                }
                permissionStatuses
                    .filter { it.permission == KikoPermission.POST_NOTIFICATIONS }
                    .forEach { status ->
                        PermissionLine(
                            label = status.permission.displayName,
                            value = status.stateLabel,
                        )
                    }
            }

            PreferenceSection(title = "Memory JSON") {
                Button(
                    onClick = onExportMemoryClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = KikoAccent),
                ) {
                    Text("Export memory JSON")
                }
                if (exportedJson.isNotBlank()) {
                    JsonBox(
                        label = "Exported JSON",
                        value = exportedJson,
                        onValueChange = {},
                        readOnly = true,
                    )
                }
                JsonBox(
                    label = "Import JSON",
                    value = importJson,
                    onValueChange = onImportJsonChange,
                    readOnly = false,
                )
                OutlinedButton(
                    onClick = onImportMemoryClick,
                    enabled = importJson.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Import memory JSON")
                }
            }

            PreferenceSection(title = "About Kiko") {
                Text(
                    text = "Kiko is an offline-first Android voice assistant.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Created by ${CreatorIdentity.NAME}.",
                    color = KikoMutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Portfolio: aboutsid.vercel.app",
                    color = KikoMutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "No login. Local-first privacy.",
                    color = KikoMutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }
        Text(
            text = "Settings",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PreferenceSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = KikoSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, KikoBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                color = KikoMutedText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun <T> ChipGroup(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = KikoMutedText,
            style = MaterialTheme.typography.labelMedium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelected(value) },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PermissionGuidance(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KikoBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = body,
            color = KikoMutedText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PermissionLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KikoBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = KikoMutedText,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            color = KikoAccent,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun wakeWordHelpText(health: WakeEngineHealth): String =
    if (health.isReady) {
        "Kiko uses a foreground service for wake-word listening. Full voice recognition starts only after \"Hey Kiko\" is detected."
    } else {
        "${health.message} Use Fake/Test for development or manual mic until a trained local model is added."
    }

@Composable
private fun JsonBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        label = { Text(label) },
        minLines = 4,
        maxLines = 8,
        modifier = Modifier.fillMaxWidth(),
    )
}
