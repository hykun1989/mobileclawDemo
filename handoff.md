# MobileClaw AIOS Agent Handoff

## 1. Current State

- Workspace: `E:\MyCodeSpace\mobileclaw`
- Current branch: `llm-controlled-orchestration`
- Repo state before this handoff: clean
- Current package/activity for device testing: `com.mobilebot/.MainActivity`
- Last verified device: `481QFGDR222MJ`
- Do not clear app data during verification. The API key is configured in app settings.

Recent commits on this branch:

```text
442f51f Allow active call to advance internally
f04f3db Stop fast clock at first due event
19a36e1 Prevent fast clock while interaction layer is active
b7dc262 Pause scenario clock while awaiting interaction
d11a712 Reset scenario clock anchor when screen is ready
a40fd91 Anchor live scenario clock to elapsed time
fa01bee Stabilize scenario fast-forward clock
152e9eb Keep scenario command tool scoped to runner
4daef4f Hold dependent events until scenario decision
9660e1f Remove duplicate interaction skill asset
61bd3a8 Execute scenario reminder commands
39161aa Route scenario decisions through LLM commands
```

## 2. Product Direction

The product direction has moved from a long single pet-grooming story to a dense one-hour AIOS flow from 13:00 to 14:00.

The goal is to show that the assistant is busy but low-interruption:

- SystemRuntime emits external facts such as incoming SMS, calls, reminders, notifications, and device/system events.
- Agent understands events, applies user memory and skill instructions, calls tools, creates or updates tasks, and decides when user input is truly needed.
- UI presents a multi-task AIOS surface with task detail, conversation stream, operational blueprint, progress bar, interaction actions, call layer, reminder layer, and task sidebar.

Important wording rule:

- User-visible copy and repo-visible names should not expose staging/trial wording.
- This is still a staged experience internally, but it must present as real in UI and resources.

## 3. Architecture Boundaries

Keep these boundaries strict. The user has repeatedly corrected boundary leaks.

### SystemRuntime

SystemRuntime only plays the phone system and external world.

Allowed responsibilities:

- Incoming SMS
- SMS sending channel
- Incoming call and call state
- Reminder firing
- Notification arrival
- Contacts
- Current positioning/device state where needed

Not SystemRuntime responsibilities:

- Understanding event meaning
- Scenario decision-making
- Writing task copy
- Service/MCP reasoning
- Payment/accounting decision
- Call transcript interpretation beyond emitting or carrying raw call facts

### Agent/Core Domain

Agent owns generic orchestration and LLM/tool protocols.

Allowed responsibilities:

- Multi-session task orchestration
- Controlled LLM turn running
- Tool calling and tool result handling
- User memory consumption
- Intent normalization for action clicks and free text
- Guardrails such as repeated tool-call protection

Do not put concrete scenario entities or copy in `core/domain`.

### Feature Chat

`feature/chat` is the Android UI and multi-session carrier.

Allowed responsibilities:

- Rendering task surfaces
- Handling user gestures
- Holding task/session state for UI
- Calling scenario/runtime interfaces

Do not put concrete scenario rules in `feature/chat`.

### Scenarios and Skills

Concrete scenario content belongs here:

- `scenarios/*`
- `core/data/src/main/assets/skills/md/*`
- memory/config resources

Current scenario modules include:

- `scenarios:runtime`
- `scenarios:one-hour-flow`
- `scenarios:pet-grooming`
- `scenarios:family-shopping`
- `scenarios:coldchain-delivery`
- `scenarios:health-supply`

## 4. Key Code Paths

Core LLM/control protocol:

- `scenarios/runtime/src/main/java/com/mobilebot/scenarios/runtime/ScenarioAgentCommand.kt`
- `scenarios/runtime/src/test/java/com/mobilebot/scenarios/runtime/ScenarioCommandCodecTest.kt`
- `core/domain/src/main/java/com/mobilebot/domain/agent/ScenarioAgentTurnRunner.kt`
- `core/domain/src/main/java/com/mobilebot/domain/tools/EmitScenarioCommandsTool.kt`
- `core/domain/src/test/java/com/mobilebot/domain/agent/ScenarioAgentTurnRunnerTest.kt`

One-hour event flow:

- `scenarios/one-hour-flow/src/main/java/com/mobilebot/scenarios/onehour/OneHourScenarioFlow.kt`
- `scenarios/one-hour-flow/src/main/java/com/mobilebot/scenarios/onehour/OneHourScenarioPolicy.kt`
- `scenarios/one-hour-flow/src/test/java/com/mobilebot/scenarios/onehour/OneHourScenarioFlowTest.kt`
- `core/data/src/main/assets/skills/md/one-hour-aio/SYSTEM_RUNTIME.json`
- `core/data/src/main/assets/skills/md/one-hour-aio/AGENT_CONTEXT.json`

UI and task state:

- `feature/chat/src/main/java/com/mobilebot/chat/AgentExperienceViewModel.kt`
- `feature/chat/src/main/java/com/mobilebot/chat/AgentExperienceScreen.kt`
- `feature/chat/src/main/java/com/mobilebot/chat/AgentExperienceModels.kt`

Skill resources:

- `core/data/src/main/assets/skills/md/pet-grooming/SKILL.md`
- `core/data/src/main/assets/skills/md/family-errand/SKILL.md`
- `core/data/src/main/assets/skills/md/coldchain-delivery/SKILL.md`
- `core/data/src/main/assets/skills/md/health-supply/SKILL.md`
- `core/data/src/main/assets/skills/md/messaging/SKILL.md`
- `core/data/src/main/assets/skills/md/contacts/SKILL.md`
- `core/data/src/main/assets/skills/md/alarm-timer/SKILL.md`

## 5. Current Verified Behavior

The following has been verified on device after the latest commits.

### Main Accept Branch

1. App starts around `04/25/2027 Sat 13:00`.
2. No initial conversation should appear.
3. Tapping the time enters fast-forward-to-next-event.
4. At 13:05, the PetSmart message triggers a pet grooming task.
5. User chooses `可以`.
6. Agent replies to PetSmart, contacts driver, and waits for driver confirmation.
7. Next fast-forward stops at 13:08 driver reply.
8. Agent creates the 13:20 reminder.
9. Next fast-forward stops at 13:09 Ella incoming call.
10. User taps answer.
11. Call overlay covers the conversation and blueprint areas.
12. After several seconds the call ends automatically.
13. A family shopping task is created and selected.
14. Next relevant fast-forward can reach 13:20.
15. At 13:20, reminder layer appears for sending Kylin downstairs.
16. Reminder layer closes automatically after about 20 seconds.

### Decline Branch

1. App starts and fast-forwards to 13:05.
2. User chooses `不改了`.
3. Pet grooming flow completes or keeps original arrangement.
4. Driver SMS and 13:20 pet reminder must not be triggered.
5. Next fast-forward goes to Ella call, not to driver/pet dependent events.

### Clock/Interaction Fixes

- Live clock pauses while waiting for user interaction.
- Manual time fast-forward is blocked while an interaction layer, active call, reminder layer, or busy state is active.
- Internal call auto-advance is allowed, so the active call can finish.
- Fast-forward now stops at the first due event instead of precomputing a later target and swallowing intermediate events.

## 6. UI Rules Already Implemented

- Top-left hamburger opens the task sidebar.
- Sidebar contains task cards and supports pinning.
- Clicking a task card should not reorder cards.
- `NT` is the fixed first participant.
- Other participants are added only when they become part of the task.
- AI work icon sits inside the progress area and opens the blueprint.
- Time area no longer expands blueprint. It is a test helper for fast-forward-to-next-event.
- Blueprint logs append in arrival order only. Do not sort or deduplicate.
- Blueprint should auto-expand on new operational log and auto-scroll to latest.
- Expanded blueprint should not permanently hide new conversation progress.
- Action buttons preserve visible labels while showing selected-action loading affordance.

## 7. Build and Test Commands

Use PowerShell.

```powershell
git status --short --branch
.\gradlew :feature:chat:compileDebugKotlin
.\gradlew :scenarios:runtime:testDebugUnitTest :core:domain:testDebugUnitTest :scenarios:one-hour-flow:testDebugUnitTest assembleDebug
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Static boundary scans:

```powershell
rg -n "f[a]ke|F[a]ke|d[e]mo|D[e]mo" core feature scenarios app -S
rg -n "Kylin|PetSmart|Ella|Driver|冷链|健康补给|麒麟" feature\chat\src\main\java core\domain\src\main\java -S
```

Expected scan behavior:

- First scan should have no matches.
- Second scan should have no matches in `feature/chat/src/main/java` and `core/domain/src/main/java`.
- Scenario-specific names are allowed in `scenarios/*` and skill resources.

## 8. Device Test Shortcuts

Do not clear app data.

Common launch:

```powershell
adb shell am force-stop com.mobilebot
adb shell am start -n com.mobilebot/.MainActivity
```

Useful taps from prior device runs:

```text
Tap time area: around 684,210
Tap outside blueprint near bottom: around 735,1830
Tap first positive action: around 746,1095
Tap answer call: around 684,1880
```

Coordinates may drift with device density and current UI state. Prefer visual inspection when possible.

## 9. Known Constraints and Risks

- The current branch has partial LLM-controlled orchestration. It is not yet the final fully open-ended LLM planner.
- Deterministic scenario flow still exists as fallback and test baseline.
- Action clicks and free text should converge into the same LLM intent path; do not reintroduce keyword enumeration for open-ended user replies.
- Do not modify the external pet salon MCP project. The user owns that project and explicitly told us not to touch it.
- Do not clear app data because API key settings are stored there.
- Keep concrete scenario content out of `feature/chat` and `core/domain`.
- Keep SystemRuntime from making decisions or writing scenario-specific task copy.
- If changing pet grooming closure, remember the preferred product rule: the chain should stay open through driver pickup, salon progress, return, and driver-confirmed home arrival.

## 10. Suggested Next Steps

Recommended next thread plan:

1. Start by running `git status --short --branch` and confirm branch is `llm-controlled-orchestration`.
2. Re-run the static boundary scans.
3. Run the core test/build command.
4. Install to device without clearing data.
5. Re-test accept and decline branches quickly.
6. Continue LLM orchestration in small commits:
   - Move more of the one-hour flow from deterministic routing into `ScenarioAgentTurnRunner`.
   - Ensure `ScenarioAgentCommand` stays the only LLM output protocol.
   - Make action clicks and typed input share one intent normalization path.
   - Add tests for repeated tool-call guardrails and multi-task session isolation.
7. Only after the LLM loop is stable, push the branch and prepare PR notes.

## 11. Last Verified Commands

The last successful validation before this handoff:

```powershell
.\gradlew :scenarios:runtime:testDebugUnitTest :core:domain:testDebugUnitTest :scenarios:one-hour-flow:testDebugUnitTest assembleDebug
```

The last successful install:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The latest verified commits were clean after removing temporary screenshots and XML dumps.
