package com.mobilebot.domain.agent

import com.mobilebot.scenarios.runtime.ScenarioAgentCommand

object ScenarioCommandGuard {
    fun validate(
        commands: List<ScenarioAgentCommand>,
        knownTaskIds: Set<String>,
        referenceCommands: List<ScenarioAgentCommand>,
    ): String? {
        val known = knownTaskIds.toMutableSet()
        commands.forEachIndexed { index, command ->
            if (command.taskId.isBlank()) return "第 ${index + 1} 条命令缺少 taskId。"
            when (command) {
                is ScenarioAgentCommand.CreateTask -> known += command.taskId
                else -> if (command.taskId !in known) {
                    return "第 ${index + 1} 条命令引用未知任务：${command.taskId}。"
                }
            }
            sideEffectError(command, referenceCommands)?.let { return it }
        }
        return null
    }

    private fun sideEffectError(
        command: ScenarioAgentCommand,
        referenceCommands: List<ScenarioAgentCommand>,
    ): String? =
        when (command) {
            is ScenarioAgentCommand.SendSms -> {
                val allowed = referenceCommands.any {
                    it is ScenarioAgentCommand.SendSms &&
                        it.taskId == command.taskId &&
                        it.to == command.to
                }
                if (allowed) null else "未授权短信命令：${command.taskId} -> ${command.to}。"
            }
            is ScenarioAgentCommand.CreateReminder -> {
                val allowed = referenceCommands.any {
                    it is ScenarioAgentCommand.CreateReminder &&
                        it.taskId == command.taskId &&
                        it.scheduledFor == command.scheduledFor
                }
                if (allowed) null else "未授权提醒命令：${command.taskId} @ ${command.scheduledFor}。"
            }
            else -> null
        }
}
