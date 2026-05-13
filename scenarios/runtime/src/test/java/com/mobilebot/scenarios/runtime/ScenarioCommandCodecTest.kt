package com.mobilebot.scenarios.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioCommandCodecTest {
    @Test
    fun parsesValidCommandBatch() {
        val result = ScenarioCommandCodec.parse(
            """
            {
              "commands": [
                {
                  "type": "create_task",
                  "taskId": "task-1",
                  "title": "麒麟洗护",
                  "subtitle": "等待用户确认",
                  "status": "BLOCKED",
                  "conversations": [{"role": "AGENT", "text": "要改到 14:00 吗？"}],
                  "logs": [{"text": "收到宠物店短信。"}],
                  "participants": [{"id": "nt", "label": "NT", "displayName": "NT", "role": "agent"}],
                  "progress": {"label": "等待", "detail": "需要用户确认", "completed": 0, "total": 3},
                  "decision": {
                    "text": "是否改到 14:00？",
                    "actions": [{"label": "可以", "key": "accept"}, {"label": "不改了", "key": "decline"}]
                  }
                },
                {
                  "type": "send_sms",
                  "taskId": "task-1",
                  "to": "PetSmart",
                  "displayName": "PetSmart",
                  "message": "好的，14:00 准时到。"
                }
              ]
            }
            """.trimIndent(),
        )

        assertTrue(result.isOk)
        val commands = result.batch?.commands.orEmpty()
        assertEquals(2, commands.size)
        val create = commands[0] as ScenarioAgentCommand.CreateTask
        assertEquals("task-1", create.taskId)
        assertEquals(ScenarioSurfaceStatus.BLOCKED, create.seed.status)
        assertEquals(listOf("可以", "不改了"), create.seed.decision?.actions?.map { it.label })
        val sms = commands[1] as ScenarioAgentCommand.SendSms
        assertEquals("PetSmart", sms.to)
        assertEquals("好的，14:00 准时到。", sms.message)
    }

    @Test
    fun rejectsUnknownCommandType() {
        val result = ScenarioCommandCodec.parse(
            """{"commands":[{"type":"unknown","taskId":"task-1"}]}""",
        )

        assertFalse(result.isOk)
        assertTrue(result.error.orEmpty().contains("不支持"))
    }

    @Test
    fun rejectsMissingRequiredField() {
        val result = ScenarioCommandCodec.parse(
            """{"commands":[{"type":"send_sms","taskId":"task-1","to":"PetSmart"}]}""",
        )

        assertFalse(result.isOk)
        assertTrue(result.error.orEmpty().contains("message"))
    }

    @Test
    fun encodesRoundTrippableBatch() {
        val batch = ScenarioCommandBatch(
            listOf(
                ScenarioAgentCommand.WaitSms(
                    taskId = "task-1",
                    contact = "Driver",
                    reason = "等待确认接送时间",
                ),
                ScenarioAgentCommand.CompleteTask(
                    taskId = "task-1",
                    summary = "已完成。",
                ),
            ),
        )

        val parsed = ScenarioCommandCodec.parse(ScenarioCommandCodec.toJson(batch))

        assertTrue(parsed.isOk)
        assertEquals(batch.commands, parsed.batch?.commands)
    }
}
