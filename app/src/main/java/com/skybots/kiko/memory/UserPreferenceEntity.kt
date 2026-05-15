package com.skybots.kiko.memory

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skybots.kiko.assistant.language.LanguageStyle
import com.skybots.kiko.assistant.language.ReplyStyle

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val id: Int = DEFAULT_ID,
    val preferredLanguageStyle: String = LanguageStyle.AUTO.name,
    val replyStyle: String = ReplyStyle.FRIENDLY.name,
    val voiceEnabled: Boolean = true,
    val personalizationEnabled: Boolean = true,
    val saveInteractionSummaries: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val DEFAULT_ID = 1
    }
}
