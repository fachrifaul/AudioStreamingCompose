package com.fachri.audiostreamingcompose.network.model

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import com.fachri.audiostreamingcompose.core.bgOrange
import com.fachri.audiostreamingcompose.core.bgPink
import com.fachri.audiostreamingcompose.core.borderOrange
import com.fachri.audiostreamingcompose.core.borderPink
import kotlinx.parcelize.Parcelize

import java.util.UUID

@Parcelize
data class VoiceOption(
    val id: UUID = UUID.randomUUID(),
    val voiceId: Int,
    val sampleId: Int,
    val name: String
) : Parcelable {

    val imageUrlString: String
        get() = "https://static.dailyfriend.ai/images/voices/${name.lowercase()}.svg"

    val soundUrlString: String
        get() = "https://static.dailyfriend.ai/conversations/samples/$voiceId/$sampleId/audio.mp3"
}

fun Int.borderColor(): Color {
    return if (this % 2 == 0) Color.borderPink() else Color.borderOrange()
}

fun Int.bgColor(): Color {
    return if (this % 2 == 0) Color.bgPink() else Color.bgOrange()
}