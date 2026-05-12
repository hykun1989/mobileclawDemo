package com.mobilebot.scenarios.familyshopping

enum class FamilyShoppingSurfaceStatus {
    RUNNING,
    DONE,
    BLOCKED,
}

enum class FamilyShoppingSurfaceRole {
    AGENT,
    USER,
}

data class FamilyShoppingSurfaceConversation(
    val role: FamilyShoppingSurfaceRole,
    val text: String,
)

data class FamilyShoppingSurfaceLog(
    val text: String,
)

data class FamilyShoppingSurfaceParticipant(
    val id: String,
    val label: String,
    val displayName: String,
    val role: String,
)

data class FamilyShoppingSurfaceProgress(
    val label: String,
    val detail: String,
    val completed: Int,
    val total: Int,
)

data class FamilyShoppingTaskSeed(
    val title: String,
    val subtitle: String,
    val status: FamilyShoppingSurfaceStatus,
    val conversations: List<FamilyShoppingSurfaceConversation>,
    val logs: List<FamilyShoppingSurfaceLog>,
    val participants: List<FamilyShoppingSurfaceParticipant>,
    val progress: FamilyShoppingSurfaceProgress,
)

data class FamilyShoppingTaskUpdate(
    val status: FamilyShoppingSurfaceStatus = FamilyShoppingSurfaceStatus.RUNNING,
    val subtitle: String,
    val conversations: List<FamilyShoppingSurfaceConversation> = emptyList(),
    val logs: List<FamilyShoppingSurfaceLog> = emptyList(),
    val participants: List<FamilyShoppingSurfaceParticipant>? = null,
    val progress: FamilyShoppingSurfaceProgress,
)

object FamilyShoppingTaskSurface {
    const val TASK_ID = "family-shopping-live"

    fun fromEllaCall(): FamilyShoppingTaskSeed =
        FamilyShoppingTaskSeed(
            title = "家庭采购",
            subtitle = "Ella 电话交代的待办",
            status = FamilyShoppingSurfaceStatus.RUNNING,
            conversations = listOf(
                FamilyShoppingSurfaceConversation(
                    FamilyShoppingSurfaceRole.AGENT,
                    "刚才 Ella 电话里提到周末家庭采购，我已经帮你建立任务跟踪，会继续整理需要确认的事项。",
                ),
            ),
            logs = listOf(
                FamilyShoppingSurfaceLog("通话转写完成：识别到 Ella 交代的家庭采购待办。"),
                FamilyShoppingSurfaceLog("新建家庭采购任务。"),
            ),
            participants = listOf(ELLA),
            progress = FamilyShoppingSurfaceProgress(
                label = "进行中",
                detail = "整理待办事项",
                completed = 1,
                total = 4,
            ),
        )

    fun priorityFollowup(messageBody: String): FamilyShoppingTaskUpdate =
        FamilyShoppingTaskUpdate(
            subtitle = "采购优先级已更新",
            conversations = listOf(
                FamilyShoppingSurfaceConversation(
                    FamilyShoppingSurfaceRole.AGENT,
                    "Ella 又补充了采购优先级：低脂牛奶和洗衣液优先，水果顺路再买。我已经同步到任务里。",
                ),
            ),
            logs = listOf(
                FamilyShoppingSurfaceLog("收到 Ella 的短信：$messageBody"),
                FamilyShoppingSurfaceLog("更新采购优先级：低脂牛奶、洗衣液优先，水果可选。"),
            ),
            progress = FamilyShoppingSurfaceProgress(
                label = "进行中",
                detail = "匹配采购方案",
                completed = 2,
                total = 4,
            ),
        )

    fun marketDeliveryCandidate(messageBody: String): FamilyShoppingTaskUpdate =
        FamilyShoppingTaskUpdate(
            subtitle = "已找到可配送渠道",
            conversations = listOf(
                FamilyShoppingSurfaceConversation(
                    FamilyShoppingSurfaceRole.AGENT,
                    "附近超市有低脂牛奶和常用洗衣液，45 分钟内可送达。我先把它作为家庭采购候选，不打断你。",
                ),
            ),
            logs = listOf(
                FamilyShoppingSurfaceLog("收到 Ole 通知：$messageBody"),
                FamilyShoppingSurfaceLog("加入采购候选：低脂牛奶、常用洗衣液，预计 45 分钟内送达。"),
            ),
            participants = listOf(ELLA, OLE),
            progress = FamilyShoppingSurfaceProgress(
                label = "进行中",
                detail = "等待清单确认",
                completed = 3,
                total = 4,
            ),
        )

    fun clarifiedList(messageBody: String): FamilyShoppingTaskUpdate =
        FamilyShoppingTaskUpdate(
            subtitle = "采购清单已收敛",
            conversations = listOf(
                FamilyShoppingSurfaceConversation(
                    FamilyShoppingSurfaceRole.AGENT,
                    "Ella 又调整了清单：洗衣液买常用款，猫粮不用买。我已经把候选清单收敛好了。",
                ),
            ),
            logs = listOf(
                FamilyShoppingSurfaceLog("收到 Ella 的短信：$messageBody"),
                FamilyShoppingSurfaceLog("更新采购清单：保留低脂牛奶、常用洗衣液；移除猫粮。"),
            ),
            progress = FamilyShoppingSurfaceProgress(
                label = "进行中",
                detail = "清单已收敛",
                completed = 4,
                total = 4,
            ),
        )

    private val ELLA = FamilyShoppingSurfaceParticipant(
        id = "ella",
        label = "E",
        displayName = "Ella",
        role = "family",
    )

    private val OLE = FamilyShoppingSurfaceParticipant(
        id = "ole",
        label = "O",
        displayName = "Ole",
        role = "market",
    )
}
