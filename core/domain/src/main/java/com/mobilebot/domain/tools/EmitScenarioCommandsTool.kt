package com.mobilebot.domain.tools

import com.mobilebot.model.ToolDefinition
import com.mobilebot.model.ToolResult
import com.mobilebot.scenarios.runtime.ScenarioCommandCodec
import javax.inject.Inject

class EmitScenarioCommandsTool
    @Inject
    constructor() : Tool {
        override val name: String = NAME

        override val definition: ToolDefinition =
            ToolDefinition(
                name = NAME,
                description = "Emit a validated batch of scenario orchestration commands for the host app to execute.",
                parametersSchema = ScenarioCommandCodec.toolParametersSchema(),
            )

        override suspend fun execute(argumentsJson: String): ToolResult {
            val parsed = ScenarioCommandCodec.parse(argumentsJson)
            return if (parsed.isOk) {
                ToolResult(
                    ok = true,
                    message = "Scenario commands accepted.",
                    dataJson = parsed.batch?.let(ScenarioCommandCodec::toJson),
                )
            } else {
                ToolResult(ok = false, message = parsed.error ?: "Scenario command validation failed.")
            }
        }

        companion object {
            const val NAME = "emit_scenario_commands"
        }
    }
