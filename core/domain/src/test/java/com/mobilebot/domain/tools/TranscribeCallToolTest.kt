package com.mobilebot.domain.tools

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class TranscribeCallToolTest {
    @Test
    fun returnsRepositoryTranscriptForAudioRef() = runBlocking {
        val tool = TranscribeCallTool(
            object : CallTranscriptRepository {
                override suspend fun findTranscript(
                    audioRef: String,
                    contact: String,
                ): CallTranscript? =
                    CallTranscript(
                        audioRef = audioRef,
                        contact = contact,
                        durationSeconds = 126,
                        transcript = "Ella 说下午如果方便，帮家里补低脂牛奶、常用洗衣液和一点水果。",
                        tasks = listOf(
                            CallTranscriptTask(
                                title = "家庭采购",
                                priority = "normal",
                                items = listOf("低脂牛奶", "常用洗衣液", "水果可选"),
                            ),
                        ),
                    )
            },
        )

        val result = tool.execute("""{"audioRef":"ella-call-ended","contact":"Ella"}""")
        val data = JSONObject(result.dataJson.orEmpty())

        assertTrue(result.ok)
        assertEquals("ella-call-ended", data.getString("audioRef"))
        assertEquals("Ella", data.getString("contact"))
        assertTrue(data.getString("transcript").contains("低脂牛奶"))
        assertEquals("水果可选", data.getJSONArray("tasks").getJSONObject(0).getJSONArray("items").getString(2))
    }

    @Test
    fun fallsBackWhenRepositoryHasNoTranscript() = runBlocking {
        val tool = TranscribeCallTool(
            object : CallTranscriptRepository {
                override suspend fun findTranscript(
                    audioRef: String,
                    contact: String,
                ): CallTranscript? = null
            },
        )

        val result = tool.execute("""{"audioRef":"unknown-call","contact":"Ella","taskFocus":"家庭采购"}""")
        val data = JSONObject(result.dataJson.orEmpty())

        assertTrue(result.ok)
        assertEquals("通话中提到一个需要后续跟进的事项。", data.getString("transcript"))
        assertEquals("家庭采购", data.getJSONArray("tasks").getJSONObject(0).getString("title"))
    }
}
