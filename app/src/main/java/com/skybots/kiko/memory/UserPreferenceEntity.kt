package com.skybots.kiko.memory

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.skybots.kiko.assistant.language.LanguageStyle
import com.skybots.kiko.assistant.language.ReplyStyle
import com.skybots.kiko.wake.WakeWordConfig
import com.skybots.kiko.wake.WakeWordEngineState
import com.skybots.kiko.wake.WakeWordSensitivity

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val id: Int = DEFAULT_ID,
    val preferredLanguageStyle: String = LanguageStyle.AUTO.name,
    val replyStyle: String = ReplyStyle.FRIENDLY.name,
    val voiceEnabled: Boolean = true,
    val personalizationEnabled: Boolean = true,
    val saveInteractionSummaries: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val wakeWordEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "'Hey Kiko'")
    val wakeWordPhrase: String = WakeWordConfig.DEFAULT_PHRASE,
    @ColumnInfo(defaultValue = "'fake'")
    val wakeWordEngine: String = WakeWordConfig.ENGINE_FAKE,
    @ColumnInfo(defaultValue = "'BALANCED'")
    val wakeWordSensitivity: String = WakeWordSensitivity.BALANCED.name,
    @ColumnInfo(defaultValue = "'Disabled'")
    val wakeWordStatus: String = WakeWordEngineState.Disabled.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val DEFAULT_ID = 1
    }
}
