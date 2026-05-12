package com.mobilebot.domain.tools

import com.mobilebot.model.ToolDefinition
import com.mobilebot.model.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class QueryServiceTool
    @Inject
    constructor() : Tool {
        override val name: String = "query_service"

        override val definition: ToolDefinition = ToolDefinition(
            name = name,
            description = "Query an authorized service endpoint or MCP tool declared by the active skill. Use this for service discovery, shop details, product lookup, price lookup, or availability context.",
            parametersSchema = """
            {
              "type": "object",
              "properties": {
                "endpoint": {"type": "string", "description": "MCP or service endpoint URL from the active skill."},
                "serviceId": {"type": "string", "description": "Service id from the skill, such as pet_salon_search or grocery_search."},
                "action": {"type": "string", "description": "Tool/action name to call on the service."},
                "arguments": {"type": "object", "description": "Action arguments."}
              },
              "required": ["endpoint", "action"]
            }
            """.trimIndent(),
        )

        override val executionPolicy: ToolExecutionPolicy =
            ToolExecutionPolicy(requiresConnectivity = true, hasSideEffects = false, timeoutMs = 45_000L)

        override suspend fun execute(argumentsJson: String): ToolResult {
            return try {
                val args = JSONObject(argumentsJson)
                val endpoint = args.optString("endpoint").trim()
                val action = args.optString("action").trim()
                if (endpoint.isBlank()) return ToolResult(false, "endpoint is required")
                if (action.isBlank()) return ToolResult(false, "action is required")
                val payload = JSONObject()
                    .put("jsonrpc", "2.0")
                    .put("id", System.currentTimeMillis())
                    .put(
                        "method",
                        "tools/call",
                    )
                    .put(
                        "params",
                        JSONObject()
                            .put("name", action)
                            .put("arguments", args.optJSONObject("arguments") ?: args.optJSONObject("params") ?: JSONObject()),
                    )
                val response = postJson(endpoint, payload)
                val parsed = parseMcpResult(response)
                ToolResult(
                    ok = true,
                    message = "Service response returned: ${args.optString("serviceId").ifBlank { action }}",
                    dataJson = JSONObject(parsed).toString(),
                )
            } catch (e: Exception) {
                ToolResult(false, e.message ?: "query_service failed")
            }
        }
    }

class ResolvePlaceTool
    @Inject
    constructor() : Tool {
        override val name: String = "resolve_place"

        override val definition: ToolDefinition = ToolDefinition(
            name = name,
            description = "Resolve a semantic place mentioned by the user, memory, transcript, or skill into a structured place. This is for place understanding, not GPS.",
            parametersSchema = """
            {
              "type": "object",
              "properties": {
                "label": {"type": "string", "description": "Place phrase, for example home, downstairs, a preferred service location, a usual store, or a place mentioned in conversation."},
                "context": {"type": "string", "description": "Optional surrounding text."}
              },
              "required": ["label"]
            }
            """.trimIndent(),
        )

        override val executionPolicy: ToolExecutionPolicy = ToolExecutionPolicy(hasSideEffects = false)

        override suspend fun execute(argumentsJson: String): ToolResult {
            return try {
                val args = JSONObject(argumentsJson)
                val label = args.optString("label").trim()
                val place = AgentContextResolver.resolvePlace(label, args.optString("context"))
                ToolResult(
                    ok = true,
                    message = "Place resolved: ${place["label"]}",
                    dataJson = JSONObject(place).toString(),
                )
            } catch (e: Exception) {
                ToolResult(false, e.message ?: "resolve_place failed")
            }
        }
    }

class TranscribeCallTool
    @Inject
    constructor() : Tool {
        override val name: String = "transcribe_call"

        override val definition: ToolDefinition = ToolDefinition(
            name = name,
            description = "Transcribe a call audio reference supplied by the system runtime and extract actionable items.",
            parametersSchema = """
            {
              "type": "object",
              "properties": {
                "audioRef": {"type": "string", "description": "Audio reference from CallEndedEvent."},
                "contact": {"type": "string", "description": "Caller name."},
                "taskFocus": {"type": "string", "description": "Optional task domain to extract."}
              },
              "required": ["audioRef"]
            }
            """.trimIndent(),
        )

        override val executionPolicy: ToolExecutionPolicy = ToolExecutionPolicy(hasSideEffects = false)

        override suspend fun execute(argumentsJson: String): ToolResult {
            return try {
                val args = JSONObject(argumentsJson)
                val contact = args.optString("contact").ifBlank { "caller" }
                val transcript = AgentContextResolver.transcribeCall(
                    audioRef = args.optString("audioRef"),
                    contact = contact,
                    taskFocus = args.optString("taskFocus"),
                )
                ToolResult(
                    ok = true,
                    message = "Call transcript returned: $contact",
                    dataJson = JSONObject(transcript).toString(),
                )
            } catch (e: Exception) {
                ToolResult(false, e.message ?: "transcribe_call failed")
            }
        }
    }

class CompletePaymentTool
    @Inject
    constructor() : Tool {
        override val name: String = "complete_payment"

        override val definition: ToolDefinition = ToolDefinition(
            name = name,
            description = "Complete a routine payment after the active skill has verified that the service is finished and the amount is normal.",
            parametersSchema = """
            {
              "type": "object",
              "properties": {
                "recipient": {"type": "string", "description": "Merchant or person to pay."},
                "amount": {"type": "string", "description": "Human-readable amount, such as 316元."},
                "reason": {"type": "string", "description": "Payment reason."},
                "date": {"type": "string", "description": "Scenario date or service date."}
              },
              "required": ["recipient", "amount"]
            }
            """.trimIndent(),
        )

        override val executionPolicy: ToolExecutionPolicy =
            ToolExecutionPolicy(hasSideEffects = true, requiresUserApproval = false)

        override suspend fun execute(argumentsJson: String): ToolResult {
            return try {
                val args = JSONObject(argumentsJson)
                val recipient = args.optString("recipient").ifBlank { args.optString("merchant") }
                val amount = normalizeAmount(args.optString("amount").ifBlank { args.optString("amountCny") })
                if (recipient.isBlank()) return ToolResult(false, "recipient is required")
                if (amount.isBlank()) return ToolResult(false, "amount is required")
                val item = linkedMapOf<String, Any?>(
                    "id" to "payment-${System.currentTimeMillis()}",
                    "recipient" to recipient,
                    "amount" to amount,
                    "reason" to args.optString("reason"),
                    "date" to args.optString("date"),
                    "status" to "completed",
                )
                ToolResult(
                    ok = true,
                    message = "Payment completed: $amount to $recipient",
                    dataJson = JSONObject(mapOf("payment" to item, "source" to "agent_tool")).toString(),
                )
            } catch (e: Exception) {
                ToolResult(false, e.message ?: "complete_payment failed")
            }
        }
    }

class RecordExpenseTool
    @Inject
    constructor() : Tool {
        override val name: String = "record_expense"

        override val definition: ToolDefinition = ToolDefinition(
            name = name,
            description = "Record an expense after payment or when the active skill needs a ledger entry.",
            parametersSchema = """
            {
              "type": "object",
              "properties": {
                "merchant": {"type": "string", "description": "Merchant name."},
                "amount": {"type": "string", "description": "Human-readable amount."},
                "category": {"type": "string", "description": "Expense category."},
                "description": {"type": "string", "description": "Short ledger note."},
                "date": {"type": "string", "description": "Scenario date or service date."}
              },
              "required": ["merchant", "amount"]
            }
            """.trimIndent(),
        )

        override val executionPolicy: ToolExecutionPolicy =
            ToolExecutionPolicy(hasSideEffects = true, requiresUserApproval = false)

        override suspend fun execute(argumentsJson: String): ToolResult {
            return try {
                val args = JSONObject(argumentsJson)
                val merchant = args.optString("merchant").ifBlank { args.optString("recipient") }
                val amount = normalizeAmount(args.optString("amount").ifBlank { args.optString("amountCny") })
                if (merchant.isBlank()) return ToolResult(false, "merchant is required")
                if (amount.isBlank()) return ToolResult(false, "amount is required")
                val item = linkedMapOf<String, Any?>(
                    "id" to "expense-${System.currentTimeMillis()}",
                    "merchant" to merchant,
                    "amount" to amount,
                    "category" to args.optString("category").ifBlank { "家庭服务" },
                    "description" to args.optString("description").ifBlank { args.optString("note") },
                    "date" to args.optString("date"),
                    "status" to "recorded",
                )
                ToolResult(
                    ok = true,
                    message = "Expense recorded: $amount at $merchant",
                    dataJson = JSONObject(mapOf("expense" to item, "source" to "agent_tool")).toString(),
                )
            } catch (e: Exception) {
                ToolResult(false, e.message ?: "record_expense failed")
            }
        }
    }

private suspend fun postJson(
    endpoint: String,
    payload: JSONObject,
): String =
    withContext(Dispatchers.IO) {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 35_000
            doOutput = true
            setRequestProperty("Accept", "application/json, text/event-stream")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("Service HTTP ${conn.responseCode}: ${body.take(180)}")
            }
            body
        } finally {
            conn.disconnect()
        }
    }

private fun parseMcpResult(response: String): Map<String, Any?> {
    val root = JSONObject(response)
    root.optJSONObject("error")?.let {
        throw IllegalStateException(it.optString("message").ifBlank { "Service error" })
    }
    val result = root.optJSONObject("result") ?: return mapOf("raw" to response)
    val content = result.optJSONArray("content")
    val text = content?.optJSONObject(0)?.optString("text").orEmpty()
    if (text.isBlank()) return jsonObjectToMap(result)
    val parsed = runCatching { JSONTokener(text).nextValue() }.getOrNull() ?: return mapOf("text" to text)
    return when (parsed) {
        is JSONObject -> jsonObjectToMap(parsed)
        is JSONArray -> mapOf("items" to jsonArrayToList(parsed))
        else -> mapOf("text" to parsed.toString())
    }
}

private object AgentContextResolver {
    fun resolvePlace(
        rawLabel: String,
        context: String,
    ): Map<String, Any?> {
        val label = "$rawLabel $context".lowercase()
        val normalizedLabel = rawLabel.ifBlank { "unknown" }
        return when {
            label.contains("楼下") || label.contains("downstairs") ->
                mapOf("id" to "place-home-downstairs", "label" to "家楼下", "address" to "常用家庭楼下临停点", "type" to "home_pickup")
            label.contains("home") || label.contains("家") ->
                mapOf("id" to "place-home", "label" to "家", "address" to "家庭地址", "type" to "home")
            label.contains("超市") || label.contains("商场") || label.contains("store") || label.contains("mall") ->
                mapOf("id" to stablePlaceId(normalizedLabel), "label" to normalizedLabel, "address" to normalizedLabel, "type" to "retail")
            else ->
                mapOf("id" to stablePlaceId(normalizedLabel), "label" to normalizedLabel, "address" to normalizedLabel, "type" to "semantic")
        }
    }

    fun transcribeCall(
        audioRef: String,
        contact: String,
        taskFocus: String,
    ): Map<String, Any?> {
        val title = taskFocus.ifBlank { "通话待办" }
        return mapOf(
            "audioRef" to audioRef,
            "contact" to contact,
            "durationSeconds" to 60,
            "transcript" to "通话中提到一个需要后续跟进的事项。",
            "tasks" to listOf(
                mapOf("title" to title, "priority" to "normal"),
            ),
        )
    }

    private fun stablePlaceId(label: String): String =
        "place-${label.lowercase().replace(Regex("""\s+"""), "-")}"
}

private fun normalizeAmount(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    val number = Regex("""\d+(?:\.\d+)?""").find(trimmed)?.value ?: return trimmed
    return "${number}元"
}

private fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> {
    val out = linkedMapOf<String, Any?>()
    for (key in obj.keys()) out[key] = jsonValue(obj.opt(key))
    return out
}

private fun jsonArrayToList(arr: JSONArray): List<Any?> =
    (0 until arr.length()).map { index -> jsonValue(arr.opt(index)) }

private fun jsonValue(value: Any?): Any? =
    when (value) {
        is JSONObject -> jsonObjectToMap(value)
        is JSONArray -> jsonArrayToList(value)
        JSONObject.NULL -> null
        else -> value
    }
