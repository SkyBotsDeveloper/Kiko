package com.skybots.kiko.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skybots.kiko.app.KikoAppConfig
import com.skybots.kiko.assistant.AssistantRuntimeState
import com.skybots.kiko.creator.CreatorIdentity
import com.skybots.kiko.permissions.KikoPermission
import com.skybots.kiko.permissions.PermissionStatus
import com.skybots.kiko.ui.theme.KikoAccent
import com.skybots.kiko.ui.theme.KikoBackground
import com.skybots.kiko.ui.theme.KikoBorder
import com.skybots.kiko.ui.theme.KikoMutedText
import com.skybots.kiko.ui.theme.KikoSurface
import com.skybots.kiko.ui.theme.KikoTheme

@Composable
fun KikoHomeScreen(
    uiState: KikoHomeUiState,
    permissionStatuses: List<PermissionStatus>,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KikoBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KikoHeader(uiState = uiState)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                KikoOrb(isListening = uiState.runtimeState == AssistantRuntimeState.LISTENING)
                Spacer(modifier = Modifier.height(16.dp))
                MicButton(
                    isListening = uiState.runtimeState == AssistantRuntimeState.LISTENING,
                    isEnabled = uiState.runtimeState != AssistantRuntimeState.PROCESSING &&
                        uiState.runtimeState != AssistantRuntimeState.SPEAKING,
                    onClick = onMicClick,
                )
                Spacer(modifier = Modifier.height(20.dp))
                AssistantTextPlaceholders(
                    transcript = uiState.transcript,
                    response = uiState.kikoResponse,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            PermissionStatusSection(permissionStatuses = permissionStatuses)
        }
    }
}

@Composable
private fun KikoHeader(uiState: KikoHomeUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = KikoAppConfig.APP_NAME,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Offline-first Android assistant by ${CreatorIdentity.NAME}",
            color = KikoMutedText,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        RuntimeStatusLine(uiState = uiState)
    }
}

@Composable
private fun RuntimeStatusLine(uiState: KikoHomeUiState) {
    Surface(
        color = KikoSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, KikoBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = uiState.runtimeState.label,
                color = KikoAccent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = uiState.statusMessage,
                color = KikoMutedText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun KikoOrb(isListening: Boolean) {
    val transition = rememberInfiniteTransition(label = "kiko-orb")
    val pulse = transition.animateFloat(
        initialValue = if (isListening) 0.82f else 0.72f,
        targetValue = if (isListening) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 900 else 1600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orb-pulse",
    )

    Canvas(modifier = Modifier.size(132.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val glowRadius = size.minDimension * 0.46f
        val coreRadius = size.minDimension * 0.16f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    KikoAccent.copy(alpha = 0.34f * pulse.value),
                    KikoAccent.copy(alpha = 0.08f * pulse.value),
                    Color.Transparent,
                ),
                center = center,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = center,
        )
        drawCircle(
            color = KikoAccent.copy(alpha = 0.52f),
            radius = coreRadius * 1.9f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    KikoAccent.copy(alpha = 0.9f),
                    KikoAccent.copy(alpha = 0.18f),
                ),
                center = center,
                radius = coreRadius * 1.35f,
            ),
            radius = coreRadius,
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
        modifier = Modifier.size(58.dp),
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
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun AssistantTextPlaceholders(
    transcript: String,
    response: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PlaceholderLine(
            label = "Transcript",
            value = transcript,
        )
        PlaceholderLine(
            label = "Kiko",
            value = response,
        )
    }
}

@Composable
private fun PlaceholderLine(
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = KikoSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, KikoBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                text = label,
                color = KikoAccent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PermissionStatusSection(permissionStatuses: List<PermissionStatus>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(KikoSurface)
            .border(width = 1.dp, color = KikoBorder, shape = RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Permission status",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        permissionStatuses.forEach { status ->
            PermissionStatusRow(status = status)
        }
    }
}

@Composable
private fun PermissionStatusRow(status: PermissionStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = status.permission.displayName,
            color = KikoMutedText,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = status.stateLabel,
            color = if (status.isGranted) KikoAccent else Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelMedium,
        )
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

@Preview(showBackground = true, backgroundColor = 0xFF06080D)
@Composable
private fun KikoHomeScreenPreview() {
    KikoTheme {
        KikoHomeScreen(
            uiState = KikoHomeUiState(),
            permissionStatuses = KikoPermission.entries.map { permission ->
                PermissionStatus(permission = permission, isGranted = false)
            },
            onMicClick = {},
        )
    }
}
