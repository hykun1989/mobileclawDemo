package com.mobilebot.scenarios.runtime

enum class ScenarioSurfaceStatus {
    RUNNING,
    DONE,
    BLOCKED,
}

enum class ScenarioSurfaceRole {
    AGENT,
    USER,
}

data class ScenarioConversation(
    val role: ScenarioSurfaceRole,
    val text: String,
)

data class ScenarioLog(
    val text: String,
)

data class ScenarioParticipant(
    val id: String,
    val label: String,
    val displayName: String,
    val role: String,
)

data class ScenarioProgress(
    val label: String,
    val detail: String,
    val completed: Int,
    val total: Int,
)

data class ScenarioDecision(
    val text: String,
    val actions: List<ScenarioAction>,
)

data class ScenarioAction(
    val label: String,
    val key: String,
)

data class ScenarioTimeline(
    val title: String,
    val detail: String,
    val status: ScenarioSurfaceStatus,
)

data class ScenarioTaskSeed(
    val taskId: String,
    val title: String,
    val subtitle: String,
    val status: ScenarioSurfaceStatus,
    val conversations: List<ScenarioConversation>,
    val logs: List<ScenarioLog>,
    val participants: List<ScenarioParticipant>,
    val progress: ScenarioProgress,
    val decision: ScenarioDecision? = null,
    val timeline: List<ScenarioTimeline> = emptyList(),
)

data class ScenarioTaskUpdate(
    val taskId: String,
    val subtitle: String,
    val status: ScenarioSurfaceStatus = ScenarioSurfaceStatus.RUNNING,
    val conversations: List<ScenarioConversation> = emptyList(),
    val logs: List<ScenarioLog> = emptyList(),
    val participants: List<ScenarioParticipant>? = null,
    val participantsToAdd: List<ScenarioParticipant> = emptyList(),
    val participantsToRemove: List<String> = emptyList(),
    val progress: ScenarioProgress,
    val decision: ScenarioDecision? = null,
    val activeActionValue: String? = null,
    val timeline: List<ScenarioTimeline> = emptyList(),
    val finalSummary: String? = null,
)

sealed interface ScenarioEffect {
    val taskId: String

    data class CreateTask(
        val seed: ScenarioTaskSeed,
    ) : ScenarioEffect {
        override val taskId: String = seed.taskId
    }

    data class UpdateTask(
        val update: ScenarioTaskUpdate,
    ) : ScenarioEffect {
        override val taskId: String = update.taskId
    }

    data class SwitchTask(
        override val taskId: String,
    ) : ScenarioEffect
}

// 场景模块只输出这些通用结构，UI 层不关心具体场景类型。
interface ScenarioTaskSurface {
    val taskId: String
}
