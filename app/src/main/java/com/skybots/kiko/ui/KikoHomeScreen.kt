package com.skybots.kiko.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skybots.kiko.assistant.AssistantRuntimeState
import com.skybots.kiko.permissions.KikoPermission
import com.skybots.kiko.permissions.PermissionStatus
import com.skybots.kiko.ui.theme.KikoAccent
import com.skybots.kiko.ui.theme.KikoBackground
import com.skybots.kiko.ui.theme.KikoBorder
import com.skybots.kiko.ui.theme.KikoMutedText
import com.skybots.kiko.ui.theme.KikoSurface
import com.skybots.kiko.ui.theme.KikoTheme
import com.skybots.kiko.wake.WakeCalibrationStatus
import com.skybots.kiko.wake.WakeScoreSnapshot
import com.skybots.kiko.wake.WakeWordEngineState
import com.skybots.kiko.wake.opensource.WakeEngineHealth
import com.skybots.kiko.wake.opensource.WakeEngineHealthStatus

@Composable
fun KikoHomeScreen(
    uiState: KikoHomeUiState,
    permissionStatuses: List<PermissionStatus>,
    wakeWordState: WakeWordEngineState,
    wakeModelHealth: WakeEngineHealth,
    wakeScoreSnapshot: WakeScoreSnapshot,
    onMicClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onWakeStatusClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val orbitState = orbitVisualState(
        runtimeState = uiState.runtimeState,
        wakeWordState = wakeWordState,
        calibrationStatus = wakeScoreSnapshot.calibrationStatus,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KikoBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Open settings",
                tint = KikoMutedText,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OrbitWakeStatusPill(
                wakeWordState = wakeWordState,
                wakeModelHealth = wakeModelHealth,
                wakeScoreSnapshot = wakeScoreSnapshot,
                onClick = onWakeStatusClick,
            )

            KikoOrbit(
                orbitState = orbitState,
                onClick = onMicClick,
            )

            Text(
                text = orbitStatusText(
                    wakeWordState = wakeWordState,
                    wakeModelHealth = wakeModelHealth,
                    wakeScoreSnapshot = wakeScoreSnapshot,
                    runtimeState = uiState.runtimeState,
                ),
                color = if (orbitState == OrbitVisualState.NEEDS_MODEL) KikoWarning else Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Text(
                text = orbitSupportText(
                    wakeWordState = wakeWordState,
                    wakeScoreSnapshot = wakeScoreSnapshot,
                    uiState = uiState,
                ),
                color = KikoMutedText,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MicButton(
                    isListening = uiState.runtimeState == AssistantRuntimeState.LISTENING,
                    isEnabled = uiState.runtimeState != AssistantRuntimeState.PROCESSING &&
                        uiState.runtimeState != AssistantRuntimeState.SPEAKING,
                    onClick = onMicClick,
                )
                Surface(
                    color = KikoSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, KikoBorder),
                ) {
                    Text(
                        text = uiState.runtimeState.label,
                        color = KikoAccent,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
            }

            LastInteractionCompact(
                transcript = uiState.transcript,
                response = uiState.kikoResponse,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TrySayingStrip()
            PermissionStatusSection(permissionStatuses = permissionStatuses)
        }
    }
}

@Composable
private fun OrbitWakeStatusPill(
    wakeWordState: WakeWordEngineState,
    wakeModelHealth: WakeEngineHealth,
    wakeScoreSnapshot: WakeScoreSnapshot,
    onClick: () -> Unit,
) {
    val unsafe = wakeScoreSnapshot.calibrationStatus == WakeCalibrationStatus.NEEDS_BETTER_MODEL ||
        wakeScoreSnapshot.calibrationStatus == WakeCalibrationStatus.UNSAFE_BASELINE
    val text = when {
        wakeWordState == WakeWordEngineState.PausedLocked -> "Wake paused"
        unsafe -> "Wake trigger blocked"
        wakeWordState == WakeWordEngineState.Listening -> "Hey Kiko active"
        wakeWordState == WakeWordEngineState.Disabled -> "Wake off"
        else -> wakeWordState.label
    }
    val detail = when {
        unsafe -> "${wakeModelHealth.status.label} - ${wakeScoreSnapshot.calibrationStatus.label}"
        else -> wakeModelHealth.status.label
    }

    Surface(
        color = KikoSurface.copy(alpha = 0.86f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (unsafe) KikoWarning.copy(alpha = 0.55f) else KikoBorder),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                color = if (unsafe) KikoWarning else KikoAccent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = detail,
                color = KikoMutedText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun KikoOrbit(
    orbitState: OrbitVisualState,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "kiko-orbit")
    val pulse = transition.animateFloat(
        initialValue = orbitState.pulseStart,
        targetValue = orbitState.pulseEnd,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = orbitState.pulseDurationMillis),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbit-pulse",
    )
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
        ),
        label = "orbit-ring",
    )

    Canvas(
        modifier = Modifier
            .size(184.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val glowRadius = size.minDimension * 0.46f
        val coreRadius = size.minDimension * 0.13f
        val ringRadius = size.minDimension * 0.31f
        val ringColor = orbitState.color

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ringColor.copy(alpha = orbitState.glowAlpha * pulse.value),
                    ringColor.copy(alpha = 0.08f * pulse.value),
                    Color.Transparent,
                ),
                center = center,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = center,
        )
        drawCircle(
            color = ringColor.copy(alpha = 0.46f),
            radius = ringRadius,
            center = center,
            style = Stroke(width = 1.4.dp.toPx()),
        )
        if (orbitState == OrbitVisualState.PROCESSING) {
            drawArc(
                color = ringColor.copy(alpha = 0.75f),
                startAngle = rotation.value,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = Size(ringRadius * 2f, ringRadius * 2f),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (orbitState == OrbitVisualState.PAUSED) 0.5f else 0.95f),
                    ringColor.copy(alpha = if (orbitState == OrbitVisualState.PAUSED) 0.38f else 0.92f),
                    ringColor.copy(alpha = 0.16f),
                ),
                center = center,
                radius = coreRadius * 1.5f,
            ),
            radius = coreRadius * pulse.value,
            center = center,
        )
    }
}

@Composable
private fun MicButton(
    isListening: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.size(54.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isListening) Color.White else KikoAccent,
            contentColor = KikoBackground,
            disabledContainerColor = KikoSurface,
            disabledContentColor = KikoMutedText,
        ),
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = if (isListening) "Listening" else "Start voice input",
            modifier = Modifier.size(25.dp),
        )
    }
}

@Composable
private fun LastInteractionCompact(
    transcript: String,
    response: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = KikoSurface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, KikoBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = transcript,
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = response,
                color = KikoMutedText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TrySayingStrip() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Open Telegram", "Call mummy", "Torch jalao", "Volume 50", "Kal 6 baje alarm").forEach {
            Surface(
                color = KikoSurface.copy(alpha = 0.68f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, KikoBorder),
            ) {
                Text(
                    text = it,
                    color = KikoMutedText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusSection(permissionStatuses: List<PermissionStatus>) {
    val relevantStatuses = permissionStatuses.filter { status ->
        !status.isGranted && status.permission in setOf(
            KikoPermission.RECORD_AUDIO,
            KikoPermission.READ_CONTACTS,
            KikoPermission.POST_NOTIFICATIONS,
        )
    }
    if (relevantStatuses.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = KikoSurface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, KikoBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Needs",
                color = KikoAccent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = relevantStatuses.joinToString { it.permission.displayName },
                color = KikoMutedText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val AssistantRuntimeState.label: String
    get() = when (this) {
        AssistantRuntimeState.IDLE -> "Idle"
        AssistantRuntimeState.LISTENING -> "Listening"
        AssistantRuntimeState.PROCESSING -> "Processing"
        AssistantRuntimeState.EXECUTING -> "Executing"
        AssistantRuntimeState.SPEAKING -> "Speaking"
        AssistantRuntimeState.ERROR -> "Error"
    }

internal enum class OrbitVisualState(
    val color: Color,
    val glowAlpha: Float,
    val pulseStart: Float,
    val pulseEnd: Float,
    val pulseDurationMillis: Int,
) {
    IDLE(KikoAccent, 0.28f, 0.86f, 1f, 1800),
    LISTENING(KikoAccent, 0.48f, 0.78f, 1.12f, 850),
    PROCESSING(KikoAccent, 0.34f, 0.9f, 1.04f, 1200),
    SPEAKING(KikoAccent, 0.4f, 0.82f, 1.08f, 1050),
    NEEDS_MODEL(KikoWarning, 0.3f, 0.86f, 1.02f, 1700),
    ERROR(KikoDanger, 0.3f, 0.88f, 1.03f, 1700),
    PAUSED(KikoMutedText, 0.16f, 0.92f, 1f, 2200),
}

internal fun orbitVisualState(
    runtimeState: AssistantRuntimeState,
    wakeWordState: WakeWordEngineState,
    calibrationStatus: WakeCalibrationStatus,
): OrbitVisualState =
    when {
        runtimeState == AssistantRuntimeState.LISTENING -> OrbitVisualState.LISTENING
        runtimeState == AssistantRuntimeState.PROCESSING ||
            runtimeState == AssistantRuntimeState.EXECUTING -> OrbitVisualState.PROCESSING
        runtimeState == AssistantRuntimeState.SPEAKING -> OrbitVisualState.SPEAKING
        runtimeState == AssistantRuntimeState.ERROR -> OrbitVisualState.ERROR
        wakeWordState == WakeWordEngineState.PausedLocked -> OrbitVisualState.PAUSED
        calibrationStatus == WakeCalibrationStatus.NEEDS_BETTER_MODEL ||
            calibrationStatus == WakeCalibrationStatus.UNSAFE_BASELINE -> OrbitVisualState.NEEDS_MODEL
        else -> OrbitVisualState.IDLE
    }

internal fun shouldShowListeningOrbit(state: AssistantRuntimeState): Boolean =
    state == AssistantRuntimeState.LISTENING || state == AssistantRuntimeState.PROCESSING

internal fun orbitStatusText(
    wakeWordState: WakeWordEngineState,
    wakeModelHealth: WakeEngineHealth,
    wakeScoreSnapshot: WakeScoreSnapshot,
    runtimeState: AssistantRuntimeState,
): String =
    when {
        runtimeState == AssistantRuntimeState.LISTENING -> "Kiko is listening"
        runtimeState == AssistantRuntimeState.PROCESSING -> "Processing speech"
        runtimeState == AssistantRuntimeState.EXECUTING -> "Running local action"
        runtimeState == AssistantRuntimeState.SPEAKING -> "Kiko is speaking"
        wakeWordState == WakeWordEngineState.PausedLocked -> "Wake paused while phone is locked"
        wakeScoreSnapshot.calibrationStatus == WakeCalibrationStatus.NEEDS_BETTER_MODEL ||
            wakeScoreSnapshot.calibrationStatus == WakeCalibrationStatus.UNSAFE_BASELINE ->
            "Wake listening, model needs better training"
        wakeWordState == WakeWordEngineState.Listening &&
            wakeModelHealth.status in readyModelStates -> "Hey Kiko listening"
        wakeWordState == WakeWordEngineState.Disabled -> "Tap to talk"
        else -> wakeWordState.label
    }

private fun orbitSupportText(
    wakeWordState: WakeWordEngineState,
    wakeScoreSnapshot: WakeScoreSnapshot,
    uiState: KikoHomeUiState,
): String =
    when {
        wakeWordState == WakeWordEngineState.PausedLocked ->
            "Kiko will resume wake listening after unlock if Hey Kiko is still enabled."
        wakeScoreSnapshot.calibrationStatus == WakeCalibrationStatus.NEEDS_BETTER_MODEL ||
            wakeScoreSnapshot.calibrationStatus == WakeCalibrationStatus.UNSAFE_BASELINE ->
            "Wake trigger is blocked for safety. This sanity model is for pipeline testing; train a balanced/quality model for real wake use."
        uiState.runtimeState == AssistantRuntimeState.IDLE -> "Tap the orbit or mic for manual voice."
        else -> uiState.statusMessage
    }

private val readyModelStates = setOf(
    WakeEngineHealthStatus.READY,
    WakeEngineHealthStatus.RAW_AUDIO_COMPATIBLE,
    WakeEngineHealthStatus.LOG_MEL_COMPATIBLE,
)

private val KikoWarning = Color(0xFFFFC267)
private val KikoDanger = Color(0xFFFF6B6B)

@Preview(showBackground = true, backgroundColor = 0xFF06080D)
@Composable
private fun KikoHomeScreenPreview() {
    KikoTheme {
        KikoHomeScreen(
            uiState = KikoHomeUiState(),
            permissionStatuses = KikoPermission.entries.map { permission ->
                PermissionStatus(permission = permission, isGranted = false)
            },
            wakeWordState = WakeWordEngineState.Listening,
            wakeModelHealth = WakeEngineHealth.logMelCompatible(32, 118),
            wakeScoreSnapshot = WakeScoreSnapshot(
                calibrationStatus = WakeCalibrationStatus.NEEDS_BETTER_MODEL,
                baselineScore = 0.501f,
            ),
            onMicClick = {},
            onSettingsClick = {},
            onWakeStatusClick = {},
        )
    }
}
