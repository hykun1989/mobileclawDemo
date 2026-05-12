package com.mobilebot.scenarios.petgrooming

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PetGroomingScenarioConfig(
    val scenarioId: String,
    val title: String,
    val skillName: String,
    val expectedSignals: List<String>,
    val triggerText: String,
)

object PetGroomingScenarioSpec {
    const val SCENARIO_ID = "pet-grooming"
    const val SKILL_NAME = "pet-grooming"

    fun config(): PetGroomingScenarioConfig =
        PetGroomingScenarioConfig(
            scenarioId = SCENARIO_ID,
            title = "Kylin Grooming Assistant",
            skillName = SKILL_NAME,
            expectedSignals = listOf(
                "User profile, places, and preferences are available.",
                "Service messages can be sent and received.",
                "Home, route, and salon locations are ready.",
                "Driver and salon contacts are available.",
                "Progress updates can be delivered quietly.",
            ),
            triggerText = """
                Start the `pet-grooming` scenario skill as the scheduled Saturday precheck for Kylin's recurring grooming.
                Use the current scenario clock supplied by the runtime. Treat the next day as the grooming date unless a trusted system result gives a more specific date.
                ${sharedRules()}
            """.trimIndent(),
        )

    fun triggerText(clock: LocalDateTime): String {
        val precheckDate = clock.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val groomingDate = clock.toLocalDate().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val morningReminder = clock.toLocalDate().plusDays(1).atTime(8, 30).format(BLUEPRINT_TIME_FORMATTER)
        val afternoonReminder = clock.toLocalDate().plusDays(1).atTime(16, 30).format(BLUEPRINT_TIME_FORMATTER)
        val dayName = scenarioDayName(clock, full = true)
        val groomingDayName = scenarioDayName(clock.plusDays(1), full = true)
        val time = clock.toLocalTime().format(CLOCK_TIME_FORMATTER)
        return """
            Start the `pet-grooming` scenario skill as the scheduled Saturday precheck for Kylin's recurring grooming.
            Current scenario clock: $dayName $precheckDate $time for precheck, with grooming expected on $groomingDayName $groomingDate. Do not invent another date. Use $groomingDate as the payment and accounting date for this run.
            ${sharedRules(morningReminder = morningReminder, afternoonReminder = afternoonReminder)}
            Do not send user-facing prose about loading memory, creating a plan, tool usage, or decision point ids. The first user-facing message should be only the weekly grooming precheck question with short action candidates.
        """.trimIndent()
    }

    fun continuationPrompt(
        groomingDate: java.time.LocalDate,
        selectedShop: String,
    ): String {
        val paymentDate = groomingDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val morningReminder = groomingDate.atTime(8, 30).format(BLUEPRINT_TIME_FORMATTER)
        val afternoonReminder = groomingDate.atTime(16, 30).format(BLUEPRINT_TIME_FORMATTER)
        return """
            Continue the pet-grooming workflow from the next missing operational milestone. The previous assistant answer stopped before closure.
            Do not summarize, audit, or explain the history in prose. Start by calling the next needed tool.
            All user-facing assistant messages, action candidates, plan titles, plan steps, SMS bodies, reminders, notifications, payment descriptions, and accounting descriptions must be Chinese. Proper nouns such as PetSmart, Driver, Kylin, CNY, and NT may stay as written. Do not write English prose on user-facing surfaces.
            Continue in this order, choosing the first missing milestone only: $selectedShop booking confirmation; Driver home-pickup confirmation; Driver delivery-to-shop update from Driver; $selectedShop arrival/progress/finish update; Driver pickup-from-shop and return update; Driver home confirmation; payment; accounting; one short final status.
            If Y selected the 9:00 option and $selectedShop has not yet replied with a booking-confirmed SMS, the next tool must be a $selectedShop confirmation SMS or a $selectedShop wait. Do not message Driver in that state.
            Driver is Y's private driver. For a 9:00 $selectedShop appointment, the first Driver leg is 8:30 home pickup, then arrival at $selectedShop by 9:00. For an accepted afternoon bath-only slot after 17:00, the first Driver SMS must explicitly say 16:30 home pickup and arrival at $selectedShop by 17:00; do not ask Driver to pick up at 17:00 or say only "before 17:00". For a 14:00 Harbor Paws Salon appointment, the first Driver SMS must explicitly say 13:30 home pickup and arrival at Harbor Paws Salon by 14:00. $selectedShop progress, delay, revised pickup time, and finish must come from $selectedShop. The second Driver leg is $selectedShop to home only after $selectedShop reports Kylin is finished, ready, or gives a revised pickup time after a delay.
            The first Driver SMS must only cover the selected appointment's home pickup and $selectedShop delivery. Do not include a predicted grooming finish time, return pickup time, or home-arrival instruction in that first Driver SMS.
            After $selectedShop confirms the selected slot, contact Driver directly without asking Y again. Driver SMS is addressed to Driver; start it with `司机您好` or `您好`, never `Y您好` or similar user-facing greetings.
            After sending the first Driver SMS, first wait for Driver's home-pickup confirmation using that SMS listener. A reply such as "收到，我8:30来接 Kylin" satisfies only pickup confirmation, not delivery. After Driver confirms the pickup plan, create a long_reminder for the selected grooming-day departure time ($morningReminder for a 9:00 appointment, $afternoonReminder for an afternoon 17:00 appointment, or 13:30 for a 14:00 Harbor Paws Salon appointment) with a Chinese title like `麒麟出发洗澡`; this is reminder creation and must not be treated as actual departure. After that, call a second system_wait_for_sms from Driver with context "Driver delivery-to-shop update for $selectedShop" and no old watchId. Only an inbound Driver message that says Kylin was delivered, arrived, 到店, 送到, or 送达 satisfies delivery-to-shop.
            After Driver confirms the first pickup plan, do not send Driver another SMS asking for future milestone reports. For Driver delivery-to-shop update, call system_wait_for_sms with Driver as the sender. Do not wait on $selectedShop for arrival or progress until Driver has reported Kylin was delivered to $selectedShop.
            For the normal selected scope, use the pet salon search service's published price for Kylin's extra-large Bernese Mountain Dog size and selected bath/de-shedding scope. Do not use small-dog pricing, full grooming/styling pricing, or pickup coordination fees unless Y or $selectedShop explicitly changes the scope.
            Do not include prices, totals, fee details, or CNY in normal outbound $selectedShop SMS. $selectedShop SMS should only confirm time, service scope, and booking status unless there is an abnormal price issue.
            For payment and accounting date, use $paymentDate for this run. Do not use the phone's real current year.
            If payment is already completed but no expense has been recorded, the next tool must be device_system with action "accounting" for $selectedShop, the same amount used for payment from the published service result, date $paymentDate, and a Chinese description for Kylin's grooming.
            A Driver promise to return Kylin later is not home confirmation. Do not call system_wait_for_sms for home confirmation until after Driver has been told to bring Kylin home or has reported Kylin is on the way home.
            When home confirmation is the next missing milestone, send Driver a short SMS asking him to reply once Kylin is home, then call system_wait_for_sms with the returned watchId. Do not pay or account until that inbound Driver SMS explicitly says Kylin is home.
            Do not ask Y whether to create routine reminders, and do not stop at a routine reminder question. Create routine reminders autonomously when useful, then continue to the next SMS signal.
            If payment or accounting already happened before Driver home confirmation, still wait for Driver home confirmation before final status.
            If a message was sent and no matching reply was received, call system_wait_for_sms for that contact.
            Ask Y only for a declared decision point, a material time/service tradeoff, safety issue, unusual fee, failed payment, or unclear instruction.
        """.trimIndent()
    }

    private fun sharedRules(
        morningReminder: String = "next-day 08:30",
        afternoonReminder: String = "next-day 16:30",
    ): String =
        """
            All user-facing assistant messages, action candidates, plan titles, plan steps, SMS bodies, reminders, notifications, payment descriptions, and accounting descriptions must be Chinese. Proper nouns such as PetSmart, Driver, Kylin, CNY, and NT may stay as written. Do not write English prose on user-facing surfaces.
            Invoke `use_skill` with skill_name `pet-grooming`, begin at the weekly precheck decision point, then load user memory, create a concise plan, resolve Y's preferred grooming shop PetSmart through device_system service_call with serviceId `pet_salon_search` and query `PetSmart`, consider another shop only if PetSmart cannot satisfy the requested timing or service scope, use system_send_sms and system_wait_for_sms for SMS conversations, and use device_system for remaining phone and OS capabilities.
            After Y keeps the appointment, call device_system service_call with serviceId `pet_salon_search` and action `get_pet_shop_detail` before sending any SMS to PetSmart. Use the service result for shop identity, address, contact details, service items, and published prices. Kylin is an extra-large Bernese Mountain Dog; use the published service price for Kylin's selected size and service scope as the expected fee for payment/accounting. Do not use full grooming/styling price, small-dog pricing, or pickup coordination fees unless the selected scope changes. Confirm available times, final service scope, and booking status by SMS with PetSmart. Never ask PetSmart for final price in the normal path, and do not include prices, totals, fee details, or CNY in normal PetSmart SMS.
            The first PetSmart SMS after Y keeps this week must ask for the regular Sunday 14:00 bath plus de-shedding slot. Do not ask for 9:00, 17:00, or broad morning/afternoon alternatives in that first PetSmart SMS; those alternatives should come from PetSmart's reply.
            A PetSmart message that a time is available is not the final booking confirmation. After Y chooses the 9:00 option, the next operational step is only: send PetSmart a confirmation SMS for that slot, then call system_wait_for_sms for PetSmart's booking confirmation. Do not call system_send_sms for Driver until PetSmart's inbound booking-confirmed SMS exists in history.
            If Y chooses another shop and Harbor Paws Salon is selected for the original 14:00 bath plus de-shedding slot, do not reuse PetSmart's 8:30/9:00 timing. Confirm the 14:00 Harbor Paws Salon booking by SMS first, then coordinate Driver for 13:30 home pickup and arrival at Harbor Paws Salon by 14:00.
            Do not resolve or message Driver until the selected grooming shop has confirmed the final booking slot by inbound SMS. Driver is Y's private driver: for a 9:00 PetSmart appointment, coordinate Driver to pick Kylin up from Y's home at 8:30 and deliver him to PetSmart by 9:00; for an accepted afternoon bath-only slot after 17:00, coordinate Driver to pick Kylin up at exactly 16:30 and deliver him to PetSmart by 17:00; for a 14:00 Harbor Paws Salon appointment, coordinate Driver to pick Kylin up at 13:30 and deliver him to Harbor Paws Salon by 14:00. The afternoon first Driver SMS must explicitly say `16:30 到家接 Kylin，17:00 前送到 PetSmart`; do not write `下午5点前到家接`. Do not include a predicted grooming finish time, return pickup time, or home-arrival instruction in this first Driver SMS. After Driver confirms the first pickup plan, do not send Driver another SMS asking for future milestone reports; the next listener must be system_wait_for_sms from Driver for Driver's delivery-to-shop update. Do not wait on the selected shop for arrival or progress until Driver has reported Kylin was delivered to that shop. Ask Driver to pick up from the selected shop only after that shop says Kylin is finished, ready, or gives a revised pickup time after a delay.
            After the selected grooming shop confirms the booking, contact Driver directly without asking Y again. Driver SMS is addressed to Driver; start it with `司机您好` or `您好`, never `Y您好` or similar Y-facing greetings.
            After sending the first Driver SMS, first wait for Driver's home-pickup confirmation using that SMS listener. A reply such as "收到，我8:30来接 Kylin" satisfies only pickup confirmation, not delivery. After Driver confirms the pickup plan, create a long_reminder for the selected grooming-day departure time ($morningReminder for a 9:00 appointment, or $afternoonReminder for an afternoon 17:00 appointment) with a Chinese title like `麒麟出发洗澡`; this is reminder creation and must not be treated as actual departure. After that, call a second system_wait_for_sms from Driver with context "Driver delivery-to-shop update for <selected shop name>" and no old watchId. Only an inbound Driver message that says Kylin was delivered, arrived, 到店, 送到, or 送达 satisfies delivery-to-shop.
            After Y answers a declared decision point, continue through routine downstream actions without asking again: booking confirmation, driver home pickup coordination, reminders, Driver delivery-to-shop monitoring, selected-shop progress/finish monitoring, Driver return coordination, home confirmation, payment, accounting, and final summary. Pause only at declared decision points or real blockers, and finish only after Kylin is confirmed home, payment is complete, and the expense is recorded. Do not ask Y whether to create routine reminders. For home confirmation, send Driver a short SMS asking him to confirm once Kylin is home, then wait on that returned SMS listener. Do not pay before an inbound Driver SMS explicitly confirms home arrival. Selected-shop progress and finish updates must be listened from the selected shop, not delegated to Driver.
            If Y selects a concrete grooming time or service option, treat that as confirmation to proceed with that option. Do not ask for a second confirmation of the same choice.
            Do not end with a promise to monitor later while the workflow is still open. If the next step is monitoring, immediately call system_wait_for_sms for the next expected PetSmart or Driver signal.
        """.trimIndent()

    private fun scenarioDayName(
        clock: LocalDateTime,
        full: Boolean,
    ): String {
        val dayOffset = Duration.between(INITIAL_CLOCK.toLocalDate().atStartOfDay(), clock.toLocalDate().atStartOfDay())
            .toDays()
            .let { Math.floorMod(it, SCENARIO_DAY_NAMES.size.toLong()) }
            .toInt()
        return if (full) SCENARIO_DAY_NAMES[dayOffset].first else SCENARIO_DAY_NAMES[dayOffset].second
    }

    private val INITIAL_CLOCK: LocalDateTime = LocalDateTime.of(2027, 4, 25, 13, 0)
    private val CLOCK_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val BLUEPRINT_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm", Locale.US)
    private val SCENARIO_DAY_NAMES = listOf(
        "Saturday" to "Sat",
        "Sunday" to "Sun",
        "Monday" to "Mon",
        "Tuesday" to "Tue",
        "Wednesday" to "Wed",
        "Thursday" to "Thu",
        "Friday" to "Fri",
    )
}
