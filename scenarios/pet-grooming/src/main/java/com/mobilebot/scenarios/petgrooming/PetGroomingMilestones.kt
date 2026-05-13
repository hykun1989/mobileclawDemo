package com.mobilebot.scenarios.petgrooming

import org.json.JSONObject

enum class PetGroomingMilestone {
    HOME_CONFIRMED,
    PAYMENT_COMPLETED,
    EXPENSE_RECORDED,
}

data class PetGroomingMilestoneUpdate(
    val milestones: Set<PetGroomingMilestone>,
    val paymentAmount: String?,
)

object PetGroomingMilestoneDetector {
    fun fromSystemRuntimeData(data: JSONObject): PetGroomingMilestoneUpdate {
        val milestones = mutableSetOf<PetGroomingMilestone>()
        var paymentAmount: String? = null

        val smsEventType = data.optJSONObject("sms")?.optString("eventType").orEmpty()
        when (smsEventType) {
            "driver_home_arrival" -> milestones += PetGroomingMilestone.HOME_CONFIRMED
        }

        val paymentStatus = data.optJSONObject("payment")?.optString("status").orEmpty()
        if (paymentStatus == "completed") {
            milestones += PetGroomingMilestone.PAYMENT_COMPLETED
            paymentAmount = data.optJSONObject("payment")
                ?.optString("amount")
                ?.takeIf { it.isNotBlank() }
                ?.let(PetGroomingConversationRules::normalizeAmountText)
        }

        val expenseStatus = data.optJSONObject("expense")?.optString("status").orEmpty()
        if (expenseStatus == "recorded") {
            milestones += PetGroomingMilestone.EXPENSE_RECORDED
            paymentAmount = data.optJSONObject("expense")
                ?.optString("amount")
                ?.takeIf { it.isNotBlank() }
                ?.let(PetGroomingConversationRules::normalizeAmountText)
                ?: paymentAmount
        }

        return PetGroomingMilestoneUpdate(
            milestones = milestones,
            paymentAmount = paymentAmount,
        )
    }
}
