package com.mobilebot.scenarios.petgrooming

import com.mobilebot.domain.agent.AgentDecisionIntent

data class PetGroomingActionCandidate(
    val label: String,
    val value: String,
)

object PetGroomingConversationRules {
    fun isTransientNarration(text: String): Boolean =
        text.contains("正在发送") ||
            text.contains("正在等待") ||
            text.contains("等待 PetSmart 回复") ||
            text.contains("稍后将等待") ||
            text.contains("稍后将自动处理") ||
            text.contains("已向 PetSmart 发送") ||
            text.contains("已向 Harbor Paws Salon 发送") ||
            text.contains("接下来将等待") ||
            text.contains("之后立即联系司机") ||
            text.contains("发送预约咨询短信") ||
            text.contains("详细信息已加载") ||
            (text.contains("Driver 已识别") && text.contains("PetSmart")) ||
            (text.contains("已确认：") && text.contains("PetSmart")) ||
            text.contains("现在向 PetSmart 发送短信") ||
            text.contains("是否现在就联系司机") ||
            text.contains("接下来将联系您的私人司机") ||
            text.contains("已向司机发送指令") ||
            text.contains("等待司机回复确认") ||
            text.contains("正在监听司机") ||
            text.contains("pickup 计划") ||
            text.contains("已启动监听") ||
            text.contains("仍在监听") ||
            text.contains("当前状态为") ||
            text.contains("按流程优先级") ||
            text.contains("首个缺失") ||
            text.contains("实际属于") ||
            text.contains("下一步：") ||
            text.contains("4:45前送达") ||
            (
                text.contains("预约已确认") &&
                    text.contains("接下来") &&
                    text.contains("司机") &&
                    text.contains("PetSmart")
            )

    fun isCompletionText(text: String): Boolean {
        val lower = text.lowercase()
        return !lower.contains("pending") &&
            !lower.contains("home confirmation pending") &&
            !text.contains("到家确认待") &&
            !text.contains("等待司机确认") &&
            (
                lower.contains("all steps complete") ||
                    lower.contains("paid and accounted") ||
                    lower.contains("payment completed") ||
                    lower.contains("closed loop") ||
                    text.contains("流程闭环") ||
                    text.contains("全流程已完成") ||
                    text.contains("全流程完成") ||
                    text.contains("全流程顺利完成") ||
                    text.contains("全部完成") ||
                    (text.contains("支付") && (text.contains("记账") || text.contains("账务") || text.contains("支付与记账") || text.contains("支付及记账") || text.contains("费用已记入") || text.contains("已记入")))
            ) &&
            (text.contains("到家") || text.contains("安全到家") || text.contains("回家") || text.contains("已接回") || text.contains("接回") || lower.contains("home")) &&
            (lower.contains("kylin") || text.contains("麒麟"))
    }

    fun isRoutineReminderQuestion(
        text: String,
        looksLikeDecisionRequest: Boolean,
    ): Boolean {
        val lower = text.lowercase()
        return (text.contains("需要我") || lower.contains("do you want me")) &&
            (text.contains("提醒") || lower.contains("reminder")) &&
            !looksLikeDecisionRequest
    }

    fun compactCompletionText(
        text: String,
        knownAmount: String?,
    ): String {
        val amount = knownAmount
            ?: Regex("""(?:¥|￥)\s?\d+(?:\.\d+)?|(?:cny|rmb|yuan)\s*\d+(?:\.\d+)?|\d+(?:\.\d+)?\s*(?:yuan|rmb|cny|元)""", RegexOption.IGNORE_CASE)
                .find(text)
                ?.value
                ?.replace(Regex("""\s+"""), "")
                ?.let(::normalizeAmountText)
        val feeText = amount?.let { "洗护费用 $it" } ?: "洗护费用"
        return "麒麟已到家，$feeText 已支付并完成记账。"
    }

    fun normalizeAmountText(value: String): String {
        val number = Regex("""\d+(?:\.\d+)?""").find(value)?.value ?: return value
        return "${number}元"
    }

    fun actionCandidates(promptText: String): List<PetGroomingActionCandidate> {
        val lower = promptText.lowercase()
        if (isAfternoonBathOnlyTradeoff(promptText)) {
            return listOf(
                PetGroomingActionCandidate("约下午5点", "USER_INTENT:pet_grooming.book_afternoon_bath_only"),
                PetGroomingActionCandidate("约9点", "USER_INTENT:pet_grooming.book_0900"),
                PetGroomingActionCandidate("换一家", "USER_INTENT:pet_grooming.find_alternative_shop"),
            )
        }
        if (isGroomingTimeTradeoff(promptText)) {
            return listOf(
                PetGroomingActionCandidate("约9点", "USER_INTENT:pet_grooming.book_0900"),
                PetGroomingActionCandidate("问下午", "USER_INTENT:pet_grooming.ask_afternoon"),
                PetGroomingActionCandidate("换一家", "USER_INTENT:pet_grooming.find_alternative_shop"),
            )
        }
        val asksToKeepGrooming =
            (lower.contains("grooming") || promptText.contains("美容") || promptText.contains("洗澡") || promptText.contains("洗护")) &&
                (lower.contains("appointment") || lower.contains("sunday") || lower.contains("周日")) &&
                (
                    lower.contains("regular") ||
                        lower.contains("weekly") ||
                        lower.contains("keep kylin") ||
                        lower.contains("proceed") ||
                        lower.contains("defer") ||
                        promptText.contains("按计划") ||
                        promptText.contains("请选择") ||
                        promptText.contains("继续吗") ||
                        lower.contains("照常") ||
                        lower.contains("改天")
                )
        if (!asksToKeepGrooming) return emptyList()
        return listOf(
            PetGroomingActionCandidate("好的", "好的"),
            PetGroomingActionCandidate("改天再说", "改天再说"),
        )
    }

    fun shouldSuppressResolvedPrompt(
        text: String,
        latestIntent: AgentDecisionIntent?,
    ): Boolean {
        val resolvedInitialTradeoff =
            latestIntent in setOf(
                PetGroomingDecisionIntents.BookNine,
                PetGroomingDecisionIntents.AskAfternoon,
                PetGroomingDecisionIntents.BookAfternoonBathOnly,
                PetGroomingDecisionIntents.FindAlternative,
            )
        val resolvedAfternoonTradeoff =
            latestIntent in setOf(
                PetGroomingDecisionIntents.BookNine,
                PetGroomingDecisionIntents.BookAfternoonBathOnly,
                PetGroomingDecisionIntents.FindAlternative,
            )
        return when {
            isAfternoonBathOnlyTradeoff(text) -> resolvedAfternoonTradeoff
            isGroomingTimeTradeoff(text) -> resolvedInitialTradeoff
            else -> false
        }
    }

    fun compactActionLabel(
        label: String,
        value: String,
    ): String {
        val combined = "$label $value"
        val lower = combined.lowercase()
        val timeLabel = Regex("""\b\d{1,2}(?::\d{2})?\s*(?:am|pm)?\b""", RegexOption.IGNORE_CASE)
            .find(combined)
            ?.value
            ?.replace(Regex("""\s+"""), " ")
            ?.trim()
        return when {
            combined.contains("好的") ->
                "好的"
            lower.contains("defer") || lower.contains("later") || lower.contains("next week") || combined.contains("改天") ->
                "改天再说"
            lower.contains("book_afternoon_bath_only") || combined.contains("下午5点") || combined.contains("下午五点") ->
                "约下午5点"
            lower.contains("book_0900") ->
                "约9点"
            lower.contains("book") || lower.contains("booking") || lower.contains("appointment") ->
                timeLabel?.let { "约${normalizeTimeLabel(it)}" } ?: "预约"
            lower.contains("modify") || lower.contains("change") || combined.contains("修改") ->
                "修改计划"
            lower.contains("afternoon") || combined.contains("下午") ->
                "问下午"
            lower.contains("another shop") || lower.contains("other shop") || combined.contains("换一家") ->
                "换一家"
            lower.contains("cancel") || combined.contains("取消") ->
                "取消"
            else -> label.substringBefore("（")
                .substringBefore("(")
                .substringBefore("->")
                .substringBefore("=>")
                .substringBefore("→")
                .trim()
                .ifBlank { label }
        }.let { it.take(24).trim() }
    }

    fun compactDecisionPromptText(text: String): String? =
        when {
            isAfternoonBathOnlyTradeoff(text) ->
                "PetSmart说下午5点后可以，只够洗澡，不含除毛。要改约下午5点吗？"
            isGroomingTimeTradeoff(text) ->
                "PetSmart说明天上午9点可以洗澡和除毛，下午5点后只够洗澡。要约9点吗？"
            actionCandidates(text).isNotEmpty() ->
                "明天周日了，还是照常给麒麟约洗澡么？"
            else -> null
        }

    fun isNonActionFact(value: String): Boolean {
        val lower = value.lowercase()
        return lower.contains("booking secured") ||
            lower.contains("medium dog") ||
            lower.contains("basic bath") ||
            lower.contains("de-shedding care") ||
            lower.contains("time changed")
    }

    // 场景会话规则只识别宠物洗护相关取舍，不承担通用意图判断。
    private fun isGroomingTimeTradeoff(text: String): Boolean {
        val lower = text.lowercase()
        val hasPetSmart = lower.contains("petsmart") || text.contains("宠物店")
        val hasMorning = lower.contains("9:00") || lower.contains("9am") || text.contains("上午九点") || text.contains("上午9点")
        val hasAfternoon = lower.contains("afternoon") || lower.contains("5 pm") || lower.contains("17:00") ||
            text.contains("下午") || text.contains("五点") || text.contains("5点")
        val asksChoice = lower.contains("would you like") || lower.contains("which option") ||
            lower.contains("tradeoff") || text.contains("要约") || text.contains("选择")
        return hasPetSmart && hasMorning && hasAfternoon && asksChoice
    }

    private fun isAfternoonBathOnlyTradeoff(text: String): Boolean {
        val lower = text.lowercase()
        val hasPetSmart = lower.contains("petsmart") || text.contains("宠物店")
        val hasAfternoon = lower.contains("afternoon") || lower.contains("17:00") || lower.contains("5 pm") ||
            text.contains("下午") || text.contains("五点") || text.contains("5点")
        val hasBathOnly = lower.contains("bath-only") || lower.contains("bath only") ||
            text.contains("只洗澡") || text.contains("不能除毛") || text.contains("不含除毛")
        return hasPetSmart && hasAfternoon && hasBathOnly
    }

    private fun normalizeTimeLabel(value: String): String =
        value
            .replace(Regex(""":00\b"""), "点")
            .replace(Regex("""\s*(am|AM)\b"""), "")
            .replace(Regex("""\s*(pm|PM)\b"""), "")
            .trim()
}
