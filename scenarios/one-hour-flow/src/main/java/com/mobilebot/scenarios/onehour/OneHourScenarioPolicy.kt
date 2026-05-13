package com.mobilebot.scenarios.onehour

import com.mobilebot.domain.agent.AgentDecisionIntent
import com.mobilebot.scenarios.petgrooming.PetGroomingContacts
import com.mobilebot.scenarios.petgrooming.PetGroomingConversationRules
import com.mobilebot.scenarios.petgrooming.PetGroomingDecisionIntents
import com.mobilebot.scenarios.petgrooming.PetGroomingMilestone
import com.mobilebot.scenarios.petgrooming.PetGroomingMilestoneDetector
import com.mobilebot.scenarios.petgrooming.PetGroomingScenarioSpec
import com.mobilebot.scenarios.petgrooming.PetGroomingTaskSurface
import com.mobilebot.scenarios.runtime.ScenarioConversation
import com.mobilebot.scenarios.runtime.ScenarioDecision
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

data class OneHourScenarioConfig(
    val scenarioId: String,
    val title: String,
    val skillName: String,
    val expectedSignals: List<String>,
    val triggerText: String,
)

data class OneHourActionCandidate(
    val label: String,
    val value: String,
)

object OneHourScenarioPolicy {
    fun config(): OneHourScenarioConfig =
        PetGroomingScenarioSpec.config().let {
            OneHourScenarioConfig(
                scenarioId = it.scenarioId,
                title = it.title,
                skillName = it.skillName,
                expectedSignals = it.expectedSignals,
                triggerText = it.triggerText,
            )
        }

    fun matches(scenarioId: String): Boolean =
        PetGroomingScenarioSpec.matches(scenarioId)

    fun precheckDecision(): ScenarioDecision =
        PetGroomingScenarioSpec.precheckDecision()

    fun initialTaskLogText(): String =
        PetGroomingScenarioSpec.initialTaskLogText()

    fun triggerText(clock: LocalDateTime): String =
        PetGroomingScenarioSpec.triggerText(clock)

    fun initialDecisionInstruction(agentText: String): String =
        PetGroomingScenarioSpec.initialDecisionInstruction(agentText)

    fun deferredCompletionMessage(): String =
        PetGroomingScenarioSpec.deferredCompletionMessage()

    fun workflowStoppedError(): String =
        PetGroomingScenarioSpec.workflowStoppedError()

    fun nextMilestoneDetail(): String =
        PetGroomingScenarioSpec.nextMilestoneDetail()

    fun closureRequiredDetail(): String =
        PetGroomingScenarioSpec.closureRequiredDetail()

    fun continuationTrace(): String =
        PetGroomingScenarioSpec.continuationTrace()

    fun continuationPrompt(
        date: LocalDate,
        useAlternativeService: Boolean,
    ): String =
        PetGroomingScenarioSpec.continuationPrompt(
            groomingDate = date,
            selectedShop = PetGroomingContacts.selectedShopName(useAlternativeService),
        )

    fun decisionIntents(scenarioId: String): List<AgentDecisionIntent> =
        PetGroomingDecisionIntents.forScenario(scenarioId)

    fun isKeepCurrentWeek(intent: AgentDecisionIntent?): Boolean =
        intent == PetGroomingDecisionIntents.KeepCurrentWeek

    fun isDeferCurrentWeek(intent: AgentDecisionIntent?): Boolean =
        intent == PetGroomingDecisionIntents.DeferCurrentWeek

    fun isAcceptOpenSlot(intent: AgentDecisionIntent?): Boolean =
        intent == PetGroomingDecisionIntents.AcceptOpenSlot

    fun isKeepOriginalSlot(intent: AgentDecisionIntent?): Boolean =
        intent == PetGroomingDecisionIntents.KeepOriginalSlot

    fun isAfternoonBathOnly(intent: AgentDecisionIntent?): Boolean =
        intent == PetGroomingDecisionIntents.BookAfternoonBathOnly

    fun isAlternativeService(intent: AgentDecisionIntent?): Boolean =
        intent == PetGroomingDecisionIntents.FindAlternative

    fun actionCandidates(promptText: String): List<OneHourActionCandidate> =
        PetGroomingConversationRules.actionCandidates(promptText)
            .map { OneHourActionCandidate(it.label, it.value) }

    fun shouldSuppressResolvedPrompt(
        text: String,
        latestIntent: AgentDecisionIntent?,
    ): Boolean =
        PetGroomingConversationRules.shouldSuppressResolvedPrompt(text, latestIntent)

    fun compactActionLabel(
        label: String,
        value: String,
    ): String =
        PetGroomingConversationRules.compactActionLabel(label, value)

    fun compactDecisionPromptText(text: String): String? =
        PetGroomingConversationRules.compactDecisionPromptText(text)

    fun isTransientNarration(text: String): Boolean =
        PetGroomingConversationRules.isTransientNarration(text)

    fun isRoutineReminderQuestion(
        text: String,
        looksLikeDecisionRequest: Boolean,
    ): Boolean =
        PetGroomingConversationRules.isRoutineReminderQuestion(text, looksLikeDecisionRequest)

    fun isCompletionText(text: String): Boolean =
        PetGroomingConversationRules.isCompletionText(text)

    fun compactCompletionText(
        text: String,
        amount: String?,
    ): String =
        PetGroomingConversationRules.compactCompletionText(text, amount)

    fun normalizeAmountText(value: String): String =
        PetGroomingConversationRules.normalizeAmountText(value)

    fun isNonActionFact(value: String): Boolean =
        PetGroomingConversationRules.isNonActionFact(value)

    fun defaultServiceName(): String =
        PetGroomingContacts.defaultShopName()

    fun displayContactName(contact: String): String =
        PetGroomingContacts.displayContactName(contact)

    fun roleForContact(contact: String): String =
        PetGroomingContacts.roleForContact(contact)

    fun participantRoleForContact(contact: String): String =
        when (PetGroomingContacts.roleForContact(contact)) {
            "private_driver" -> "private_driver"
            "grooming_service" -> "service_provider"
            else -> "service"
        }

    fun labelForContact(contact: String): String =
        PetGroomingContacts.labelForContact(contact)

    fun isServiceContact(contact: String): Boolean =
        PetGroomingContacts.isGroomingShopContact(contact)

    fun displayReminderBody(raw: String): String =
        PetGroomingContacts.displayDriverReminderBody(raw)

    fun serviceTaskLogText(
        serviceId: String,
        action: String,
        namedEntity: String,
    ): String? =
        when {
            serviceId == "pet_salon_search" && action.lowercase().contains("detail") -> {
                val serviceName = namedEntity.ifBlank { PetGroomingContacts.defaultShopName() }
                "添加 $serviceName 到参与方。"
            }
            serviceId == "pet_salon_search" -> "查询附近服务方。"
            else -> null
        }

    fun openSlotClarification(userText: String): Pair<List<ScenarioConversation>, ScenarioDecision> =
        PetGroomingTaskSurface.openSlotClarification(userText)
}

class OneHourScenarioRunTracker {
    private val milestones = mutableSetOf<PetGroomingMilestone>()
    var paymentAmount: String? = null
        private set

    fun clear() {
        milestones.clear()
        paymentAmount = null
    }

    fun recordSystemRuntimeData(data: JSONObject) {
        val update = PetGroomingMilestoneDetector.fromSystemRuntimeData(data)
        milestones += update.milestones
        paymentAmount = update.paymentAmount ?: paymentAmount
    }

    fun closureSatisfied(): Boolean =
        milestones.containsAll(
            setOf(
                PetGroomingMilestone.HOME_CONFIRMED,
                PetGroomingMilestone.PAYMENT_COMPLETED,
                PetGroomingMilestone.EXPENSE_RECORDED,
            ),
        )
}
