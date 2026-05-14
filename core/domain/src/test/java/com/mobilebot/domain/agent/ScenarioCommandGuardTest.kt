package com.mobilebot.domain.agent

import com.mobilebot.scenarios.runtime.ScenarioAgentCommand
import com.mobilebot.scenarios.runtime.ScenarioProgress
import com.mobilebot.scenarios.runtime.ScenarioSurfaceStatus
import com.mobilebot.scenarios.runtime.ScenarioTaskSeed
import com.mobilebot.scenarios.runtime.ScenarioTaskUpdate
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioCommandGuardTest {
    @Test
    fun allowsCreateThenUpdateNewTaskInSameTurn() {
        val error = ScenarioCommandGuard.validate(
            commands = listOf(createTask("new-task"), updateTask("new-task")),
            knownTaskIds = setOf("existing-task"),
            referenceCommands = emptyList(),
        )

        assertNull(error)
    }

    @Test
    fun rejectsUnknownTaskCommand() {
        val error = ScenarioCommandGuard.validate(
            commands = listOf(updateTask("missing-task")),
            knownTaskIds = setOf("existing-task"),
            referenceCommands = emptyList(),
        )

        assertNotNull(error)
        assertTrue(error.orEmpty().contains("未知任务"))
    }

    @Test
    fun rejectsBlankTaskId() {
        val error = ScenarioCommandGuard.validate(
            commands = listOf(ScenarioAgentCommand.SwitchTask("")),
            knownTaskIds = setOf("existing-task"),
            referenceCommands = emptyList(),
        )

        assertNotNull(error)
        assertTrue(error.orEmpty().contains("taskId"))
    }

    @Test
    fun rejectsUnauthorizedSmsCommand() {
        val command = ScenarioAgentCommand.SendSms(
            taskId = "existing-task",
            to = "contact-a",
            message = "hello",
        )
        val error = ScenarioCommandGuard.validate(
            commands = listOf(command),
            knownTaskIds = setOf("existing-task"),
            referenceCommands = emptyList(),
        )

        assertNotNull(error)
        assertTrue(error.orEmpty().contains("未授权短信"))
    }

    @Test
    fun allowsReferenceAuthorizedSmsCommand() {
        val command = ScenarioAgentCommand.SendSms(
            taskId = "existing-task",
            to = "contact-a",
            message = "hello",
        )
        val error = ScenarioCommandGuard.validate(
            commands = listOf(command),
            knownTaskIds = setOf("existing-task"),
            referenceCommands = listOf(command.copy(message = "reference")),
        )

        assertNull(error)
    }

    @Test
    fun rejectsUnauthorizedReminderCommand() {
        val command = ScenarioAgentCommand.CreateReminder(
            taskId = "existing-task",
            title = "Reminder",
            body = "Body",
            scheduledFor = "2027-04-25T13:20:00",
        )
        val error = ScenarioCommandGuard.validate(
            commands = listOf(command),
            knownTaskIds = setOf("existing-task"),
            referenceCommands = emptyList(),
        )

        assertNotNull(error)
        assertTrue(error.orEmpty().contains("未授权提醒"))
    }

    @Test
    fun allowsReferenceAuthorizedReminderCommand() {
        val command = ScenarioAgentCommand.CreateReminder(
            taskId = "existing-task",
            title = "Reminder",
            body = "Body",
            scheduledFor = "2027-04-25T13:20:00",
        )
        val reference = command.copy(title = "Reference", body = "Reference")
        val error = ScenarioCommandGuard.validate(
            commands = listOf(command),
            knownTaskIds = setOf("existing-task"),
            referenceCommands = listOf(reference),
        )

        assertNull(error)
    }

    private fun createTask(taskId: String): ScenarioAgentCommand.CreateTask =
        ScenarioAgentCommand.CreateTask(
            ScenarioTaskSeed(
                taskId = taskId,
                title = "Task",
                subtitle = "Subtitle",
                status = ScenarioSurfaceStatus.RUNNING,
                conversations = emptyList(),
                logs = emptyList(),
                participants = emptyList(),
                progress = progress(),
            ),
        )

    private fun updateTask(taskId: String): ScenarioAgentCommand.UpdateTask =
        ScenarioAgentCommand.UpdateTask(
            ScenarioTaskUpdate(
                taskId = taskId,
                subtitle = "Subtitle",
                progress = progress(),
            ),
        )

    private fun progress(): ScenarioProgress =
        ScenarioProgress(
            label = "Running",
            detail = "Working",
            completed = 0,
            total = 1,
        )
}
