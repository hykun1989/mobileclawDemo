package com.mobilebot.domain.agent

import com.mobilebot.scenarios.runtime.ScenarioAgentCommand
import com.mobilebot.scenarios.runtime.ScenarioCommandAuthorization
import com.mobilebot.scenarios.runtime.ScenarioReminderAuthorization
import com.mobilebot.scenarios.runtime.ScenarioSmsAuthorization

object ScenarioCommandGuard {
    fun validate(
        commands: List<ScenarioAgentCommand>,
        knownTaskIds: Set<String>,
        authorization: ScenarioCommandAuthorization,
    ): String? {
        val known = knownTaskIds.toMutableSet()
        commands.forEachIndexed { index, command ->
            if (command.taskId.isBlank()) return "Command ${index + 1} is missing taskId."
            if (command.taskId !in authorization.taskIds) {
                return "Command ${index + 1} is not authorized for taskId: ${command.taskId}."
            }
            when (command) {
                is ScenarioAgentCommand.CreateTask -> known += command.taskId
                else -> if (command.taskId !in known) {
                    return "Command ${index + 1} references unknown taskId: ${command.taskId}."
                }
            }
            sideEffectError(command, authorization)?.let { return it }
        }
        return null
    }

    private fun sideEffectError(
        command: ScenarioAgentCommand,
        authorization: ScenarioCommandAuthorization,
    ): String? =
        when (command) {
            is ScenarioAgentCommand.SendSms -> {
                val allowed = ScenarioSmsAuthorization(command.taskId, command.to) in authorization.sms
                if (allowed) null else "Unauthorized SMS command: ${command.taskId} -> ${command.to}."
            }
            is ScenarioAgentCommand.CreateReminder -> {
                val allowed =
                    ScenarioReminderAuthorization(command.taskId, command.scheduledFor) in authorization.reminders
                if (allowed) null else "Unauthorized reminder command: ${command.taskId} @ ${command.scheduledFor}."
            }
            else -> null
        }
}
