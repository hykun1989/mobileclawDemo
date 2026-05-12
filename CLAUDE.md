# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MobileBot is an Android multi-module Agent application. It runs a local AI agent on Android devices that communicates with LLMs via the OpenAI `tool_calls` protocol. The agent can invoke tools (browser, maps, contacts, SMS, etc.) and execute skills (SKILL.md-based task definitions) to accomplish complex multi-step tasks on the device.

## Build System & Commands

- **Gradle Kotlin DSL** (`settings.gradle.kts`, `build.gradle.kts`)
- **JDK 17** required
- **SDK**: compileSdk/targetSdk 35, minSdk 26
- **Kotlin 2.0.21**, AGP 8.7.2, Compose BOM 2024.10.01

### Essential commands

```bash
# Full build (all platforms)
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Run JVM unit tests (no Android device needed)
./gradlew test

# Run unit tests for a specific module
./gradlew :core:domain:testDebugUnitTest
./gradlew :core:bridge:testDebugUnitTest
./gradlew :core:data:testDebugUnitTest

# Run Android Lint
./gradlew lintDebug

# Run a single test class
./gradlew :core:domain:testDebugUnitTest --tests "com.mobilebot.domain.skill.EnhancedSkillParserTest" --no-daemon

# Run all tests without daemon (helps on memory-constrained machines)
$env:GRADLE_OPTS="-Xmx1g -XX:+UseSerialGC"
./gradlew test --no-daemon
```

## Module Structure

| Module | Responsibility | Key files |
|---|---|---|
| `:app` | Application entry, Hilt app-level DI, foreground service, notification listener | `MobileBotApplication`, `MainActivity`, `AgentForegroundService` |
| `:feature:chat` | Compose UI - chat screen, settings screen, navigation | `ChatScreen`, `ChatViewModel`, `SettingsScreen`, `MobileBotNavHost` |
| `:core:model` | Shared DTOs - messages, tools, stream events | `Messages.kt`, `ToolModels.kt`, `StreamEvents.kt` |
| `:core:bus` | Agent-to-UI message bus (`OutboundMessage`/`InboundMessage`) | `MessageBus.kt` |
| `:core:network` | OpenAI-compatible LLM client, SSE streaming | `OpenAiCompatibleClient.kt`, `NanobotStreamClient.kt` |
| `:core:bridge` | Android system capability abstraction (browser, maps, contacts, SMS, location, notifications, files, etc.) + virtual bridge for testing | `DeviceCapabilityBridge`, `Android*Bridge`, `Virtual*Bridge`, `SwitchableDeviceCapabilityBridge` |
| `:core:domain` | **Agent loop** (`ToolCallAgentLoop`), tool registry, skill system, plan mode, subtask execution | `AgentLoop.kt`, `ToolCallAgentLoop.kt`, `ToolRegistry.kt`, `SkillExecutor.kt`, `PlanManager.kt`, `SubtaskExecutor.kt`, all tools in `tools/` |
| `:core:data` | Room database, settings storage, skill asset loading, WorkManager heartbeats, virtual data bootstrapper | `SessionRepositoryImpl`, `UserSettingsRepository`, `SkillAssetLoader`, `UserProfileStoreImpl` |

### Dependency direction

```
app → feature:chat, core:data, core:domain, core:bridge
feature:chat → core:model, core:bus, core:domain, core:data, core:network
core:domain → core:model, core:bus, core:bridge, core:network
core:data → core:model, core:bridge, core:domain, core:network
core:bridge, core:model — bottom-level, cross-cutting
```

**Do not** add UI logic to `:app` or Android-specific implementations to `:core:domain`.

## Agent Architecture

### Core execution flow

1. `ChatViewModel.send()` → `ForegroundController` → `AgentLoop.processUserMessage()` (per-chat mutex in AgentLoop, delegates to ToolCallAgentLoop)
2. `ToolCallAgentLoop` builds system prompt (with skill catalog from `SkillRegistry`) + tool definitions from `ToolRegistry`
3. `LlmConfigurator.beforeRequest()` syncs API key / base URL / model to the LLM client
4. Sends to LLM via OpenAI `tool_calls` protocol via `LlmClient.chat()` (blocking) or `.chatStream()` (SSE)
5. If LLM returns `tool_calls`: execute each tool (through `ToolPolicyEngine` capability/foreground/connectivity checks + `ToolPermissionGate` user approval) → append results → loop
6. If LLM calls `use_skill`: `SkillTool` routes to `SkillExecutor` → load SKILL.md → inline (inject guidance into context) or fork (spawn SubAgentRunner)
7. If LLM calls `create_plan`: `CreatePlanTool` stores in `PlanManager`, loop pauses, sends `TodoListCard` + `ActionPrompt` to UI, waits for user approve/edit/cancel
8. Tool results flow back to UI via `MessageBus.outbound` SharedFlow

### Key components

| Component | File | Role |
|---|---|---|
| `AgentLoop` | `core/domain/AgentLoop.kt` | Top-level entry with per-session mutex; delegates to ToolCallAgentLoop |
| `ToolCallAgentLoop` | `core/domain/agent/ToolCallAgentLoop.kt` | Main tool_calls iteration: build prompt → LLM → execute tools → loop |
| `ToolRegistry` | `core/domain/tools/ToolRegistry.kt` | Collects all tools via Hilt `@IntoSet`, filters by device capabilities, executes |
| `ToolPolicyEngine` | `core/domain/tools/ToolPolicyEngine.kt` | Checks capability, foreground, and connectivity constraints before execution |
| `ToolPermissionGate` | `core/domain/tools/ToolPermissionGate.kt` | User approval gate for tools with `requiresUserApproval: true` |
| `SkillTool` | `core/domain/tools/SkillTool.kt` | The `use_skill` tool — LLM routes to skills through this |
| `SkillExecutor` | `core/domain/skill/SkillExecutor.kt` | Executes SKILL.md skills: inline (context injection) or fork (sub-agent) |
| `SkillRegistry` | `core/domain/skill/SkillRegistry.kt` | Multi-source skill registry with priority override (Bundled < Cloud < User) |
| `PlanManager` | `core/domain/agent/PlanManager.kt` | Plan state machine: NONE → PENDING → EXECUTING → DONE |
| `SubtaskExecutor` | `core/domain/subtask/SubtaskExecutor.kt` | Creates/manages subtasks with independent sessions and shared facts |
| `MessageBus` | `core/bus/MessageBus.kt` | SharedFlow-based Agent→UI channel (`inbound`/`outbound` flows) |
| `MemoryFacade` | `core/domain/memory/MemoryFacade.kt` | Working memory, session summaries, fact retrieval |

### Data persistence

- Chat messages stored in **Room** database (`AppDatabase` with SessionDao, MessageDao)
- Primary chat session: `chatId = "main"` → session key `mobile:main`
- User settings (API key, base URL, model) stored via DataStore/SharedPreferences
- Working memory and facts persisted in Room tables (through `MemoryFacade` + `MemoryFileRepository`)

## Key Patterns

### Hilt DI / Module Binding

Two primary DI modules wire up the agent's tool and bridge layers:

| Module | File | Pattern |
|---|---|---|
| `DomainToolModule` | `core/domain/DomainModule.kt` | `@Binds @IntoSet Tool` — 30+ tool bindings |
| `BridgeModule` | `core/bridge/di/BridgeModule.kt` | `@Provides @Singleton DeviceCapabilityBridge` — switches between real/virtual |
| `NetworkModule` | `core/network/NetworkModule.kt` | Binds `LlmClient` to `OpenAiCompatibleClient` |

The `BridgeModule` companion decides at startup whether to use `SwitchableDeviceCapabilityBridge` (if any bridge is virtual) or `AndroidDeviceCapabilityBridge` directly. The `virtual_bridge_config.json` controls which bridges are virtual.

### Tool definition pattern

Tools implement the `DomainToolModule` Hilt multi-binding pattern. Each tool declares:
- `name` (e.g., "open_url") — unique string identifier the LLM uses to call it
- `definition: ToolDefinition` — JSON schema (`name`, `description`, `parametersSchema` as raw JSON string)
- `requiredCapabilities` — set of Android permissions/features needed
- `executionPolicy: ToolExecutionPolicy` — `requiresUserApproval`, `requiresForeground`, `requiresConnectivity`, `hasSideEffects`
- `risk: ToolRisk` — `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- `suspend fun execute(argumentsJson: String): ToolResult` — returns `ToolResult(ok, message, dataJson?)`

Tools access Android APIs through `DeviceCapabilityBridge` interface (property-per-capability: `.files`, `.contacts`, `.browser`, etc.), never directly through Android SDK calls.

### Skill system

Skills are defined as SKILL.md files (YAML frontmatter + Markdown body) in `core/data/src/main/assets/skills/md/`. Additional bundled skills use JSON format in `skills/bundled/` and `skills/scenarios/`. At startup, `SkillAssetLoader.loadAllSkills()` loads and registers all skills into `SkillRegistry`.

SKILL.md YAML frontmatter fields (parsed by `SkillMdParser`):

| Field | Type | Description |
|---|---|---|
| `name` | string | Human-readable name |
| `description` | string | What the skill does (shown to LLM) |
| `category` | string | Grouping category |
| `allowed-tools` | list | Tool names the skill can use |
| `context` | `inline`/`fork` | Execution mode |
| `effort` | `low`/`medium`/`high` | Estimated effort |
| `risk` | `low`/`medium`/`high`/`critical` | Risk level |
| `requires` | map | `permissions`, `connectivity`, `apps`, `minApi` |
| `conditions` | map | `time`, `location-type`, `device-state` |
| `composes-skills` | list | Sub-skills this skill can invoke |
| `required-services` | list | External service IDs needed |
| `always` | bool | Always inject this skill's guidance |
| `disable-model-invocation` | bool | Block LLM from invoking it directly |
| `prompt-summary` | string | Short summary for prompt (defaults to description) |

### Virtual Bridge System (for emulator testing)

Many tools depend on real Android hardware (contacts, SMS, location). The **Virtual Bridge** system provides mock data for testing on emulators.

- Config file: `app/src/main/assets/virtual_bridge_config.json`
- Format: `{ "defaultMode": "virtual", "bridges": { "contacts": "virtual", "browser": "real", ... } }`
- Available bridges: `telephony`, `contacts`, `location`, `notifications`, `files`, `services`, `accessibility`, `media`, `browser`, `maps`, `clipboard`, `share`, `system`, `appState`
- Each bridge can be independently set to `"virtual"` or `"real"`
- `SwitchableDeviceCapabilityBridge` routes calls based on config at runtime
- Virtual mock data: `VirtualMockData.kt` (contacts, location, notifications, etc.)
- `VirtualBridgeManager.hasAnyVirtual()` controls whether the switchable bridge is used at all

### LLM Client Interface

The `LlmClient` interface (`core/network/LlmClient.kt`) abstracts model communication:

```kotlin
interface LlmClient {
    var defaultModel: String
    suspend fun chat(messages, tools, model, maxTokens): LlmResponse
    fun chatStream(messages, tools, model, maxTokens): Flow<StreamEvent>
}
```

- `LlmMessage` has `role`, `content`, `toolCallId`, `name`, `toolCalls`
- `LlmResponse` has `content`, `toolCalls`, `finishReason`
- Implemented by `OpenAiCompatibleClient` (OpenAI protocol) and `NanobotStreamClient` (SSE streaming)
- `NetworkModule` binds the client via Hilt

## Testing

- **JUnit 4** for JVM unit tests, placed in `<module>/src/test/java/`
- 11 test files across `core:domain` (9), `core:bridge` (1), `core:data` (1)
- Integration tests use `RecordingDeviceCapabilityBridge` + `Recording*Bridge` test doubles in `core/domain/src/test/java/com/mobilebot/domain/testdoubles/`
- **Maestro** UI automation in `maestro/flows/` (requires separate installation)
- No instrumented tests currently

## Adding new code

### New tool: Create implementation in `core:domain/tools/`, add Android API in `core:bridge/` if needed, bind in `DomainModule`, add tests.

### New skill: Create `core/data/src/main/assets/skills/md/{name}/SKILL.md` with YAML frontmatter. Auto-registered on app startup.

### New bridge: Define interface in `core/bridge/`, implement in `core/bridge/impl/`, register in `BridgeModule`, configure `virtual_bridge_config.json`.

## `claude_code_src/` directory

Contains embedded Claude Code source files (TypeScript/TSX). This is the CLI tool source bundled into the repo for some purpose — treat as external/bundled code, not primary development target.
