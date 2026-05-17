package com.mobilebot.domain.agent

import com.mobilebot.scenarios.runtime.ScenarioAgentCommand
import com.mobilebot.scenarios.runtime.ScenarioCommandAuthorization
import com.mobilebot.scenarios.runtime.ScenarioProgress
import com.mobilebot.scenarios.runtime.ScenarioReminderAuthorization
import com.mobilebot.scenarios.runtime.ScenarioSmsAuthorization
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
            authorization = authorization("new-task"),
        )

        assertNull(error)
    }

    @Test
    fun rejectsUnknownTaskCommand() {
        val error = ScenarioCommandGuard.validate(
            commands = listOf(updateTask("missing-task")),
            knownTaskIds = setOf("existing-task"),
            authorization = authorization("missing-task"),
        )

        assertNotNull(error)
        assertTrue(error.orEmpty().contains("unknown taskId"))
    }

    @Test
    fun rejectsBlankTaskId() {
        val error = ScenarioCommandGuard.validate(
            commands = listOf(ScenarioAgentCommand.SwitchTask("")),
            knownTaskIds = setOf("existing-task"),
            authorization = authorization(""),
        )

        assertNotNull(error)
        assertTrue(error.orEmpty().contains("taskId"))
    }

    @Test
    fun rejectsTaskOutsideAuthorizedPolicyEvenWhenKnown() {
        val error = ScenarioCommandGuard.validate(
            commands = listOf(updateTask("pet-task")),
            knownTaskIds = setOf("pet-task", "family-task"),
            authorization = authorization("family-task"),
        )

        assertNotNull(error)
        assertTrue(error.orEmpty().contains("not authorized"))
    }

    @Test
    fun allowsEmptyCommandBatchWithEmptyAuthorization() {
        val error = ScenarioCommandGuard.validate(
            commands = emptyList(),
            knownTaskIds = setOf("existing-task"),
            authorization = ScenarioCommandAuthorization(),
        )

        assertNull(error)
    }

    @Test
    fun rejectsCommandsWhenNoPolicyTaskIdsAreDeclared() {
        val error = ScenarioCommandGuard.validate(
            commands = listOf(updateTask("existing-task")),
            knownTaskIds = setOf("existing-task"),
            authorization = ScenarioCommandAuthorization(),
        )

        assertNotNull(error)
        assertTrue(error.orEmpty().contains("not authorized"))
    }

    @Test
    fun doesNotRequireReferenceDecisionActions() {
        val error = ScenarioCommandGuard.validate(
            commands = listOf(createTask("pet-task")),
            knownTaskIds = emptySet(),
            authorization = authorization("pet-task"),
        )

        assertNull(error)
    }

    @Test
    fun doesNotRequireReferenceFlowCommands() {
        val error = ScenarioCommandGuard.validate(
            commands = listOf(updateTask("existing-task")),
            knownTaskIds = setOf("existing-task"),
            authorization = authorization("existing-task"),
        )

        assertNull(error)
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
            authorization = authorization("existing-task"),
        )

        assertNotNull(error)
        assertTrue(error.orEmpty().contains("Unauthorized SMS"))
    }

    @Test
    fun allowsPolicyAuthorizedSmsCommand() {
        val command = ScenarioAgentCommand.SendSms(
            taskId = "existing-task",
            to = "contact-a",
            message = "hello",
        )
        val error = ScenarioCommandGuard.validate(
            commands = listOf(command),
            knownTaskIds = setOf("existing-task"),
            authorization = authorization("existing-task").copy(
                sms = setOf(ScenarioSmsAuthorization("existing-task", "contact-a")),
            ),
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
            authorization = authorization("existing-task"),
        )

        assertNotNull(error)
        assertTrue(error.orEmpty().contains("Unauthorized reminder"))
    }

    @Test
    fun allowsPolicyAuthorizedReminderCommand() {
        val command = ScenarioAgentCommand.CreateReminder(
            taskId = "existing-task",
            title = "Reminder",
            body = "Body",
            scheduledFor = "2027-04-25T13:20:00",
        )
        val error = ScenarioCommandGuard.validate(
            commands = listOf(command),
            knownTaskIds = setOf("existing-task"),
            authorization = authorization("existing-task").copy(
                reminders = setOf(ScenarioReminderAuthorization("existing-task", "2027-04-25T13:20:00")),
            ),
        )

        assertNull(error)
    }

    private fun authorization(vararg taskIds: String): ScenarioCommandAuthorization =
        ScenarioCommandAuthorization(taskIds = taskIds.filter { it.isNotBlank() }.toSet())

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

    private fun updateTask(
        taskId: String,
        status: ScenarioSurfaceStatus = ScenarioSurfaceStatus.RUNNING,
    ): ScenarioAgentCommand.UpdateTask =
        ScenarioAgentCommand.UpdateTask(
            ScenarioTaskUpdate(
                taskId = taskId,
                subtitle = "Subtitle",
                status = status,
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
