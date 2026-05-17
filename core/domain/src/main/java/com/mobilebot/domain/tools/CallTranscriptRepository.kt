package com.mobilebot.domain.tools

data class CallTranscript(
    val audioRef: String,
    val contact: String,
    val durationSeconds: Int,
    val transcript: String,
    val tasks: List<CallTranscriptTask> = emptyList(),
)

data class CallTranscriptTask(
    val title: String,
    val priority: String,
    val items: List<String> = emptyList(),
)

interface CallTranscriptRepository {
    suspend fun findTranscript(
        audioRef: String,
        contact: String,
    ): CallTranscript?
}
