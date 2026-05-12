package com.mobilebot.scenarios.petgrooming

object PetGroomingContacts {
    const val PREFERRED_SHOP = "PetSmart"
    const val ALTERNATIVE_SHOP = "Harbor Paws Salon"

    fun selectedShopName(useAlternative: Boolean): String =
        if (useAlternative) ALTERNATIVE_SHOP else PREFERRED_SHOP

    fun isGroomingShopContact(contact: String): Boolean =
        shopNameIn(contact) != null ||
            contact.contains("pet", ignoreCase = true) ||
            contact.contains("salon", ignoreCase = true) ||
            contact.contains("groom", ignoreCase = true) ||
            contact.contains("宠物") ||
            contact.contains("洗护") ||
            contact.contains("洗澡")

    fun shopNameIn(text: String): String? =
        when {
            text.contains("Harbor Paws", ignoreCase = true) ||
                text.contains("harbor-paws-salon", ignoreCase = true) ||
                text.contains("+86-756-888-1111") ||
                text.contains("756-888-1111") -> ALTERNATIVE_SHOP
            text.contains("PetSmart", ignoreCase = true) ||
                text.contains("Pet Smart", ignoreCase = true) ||
                text.contains("+86-756-888-0001") ||
                text.contains("756-888-0001") -> PREFERRED_SHOP
            else -> null
        }

    fun displayDriverReminderBody(raw: String): String {
        val shopName = shopNameIn(raw) ?: return raw
        if (!raw.contains("司机") && !raw.contains("Driver")) return raw

        val normalized = raw
            .replace("5:00前送达 PetSmart", "17:00前送达 PetSmart")
            .replace("5:00前送到 PetSmart", "17:00前送达 PetSmart")

        return when {
            normalized.contains("13:30") ||
                normalized.contains("14:00") ||
                normalized.contains("下午2") ||
                normalized.contains("下午两点") ->
                "13:30 Driver 到家接 Kylin，14:00前送达 $shopName。"
            normalized.contains("16:30") ||
                normalized.contains("17:00") ->
                "16:30 Driver 到家接 Kylin，17:00前送达 $shopName。"
            normalized.contains("8:30") ||
                normalized.contains("08:30") ||
                normalized.contains("9:00") ||
                normalized.contains("09:00") ||
                normalized.contains("9点") ->
                "08:30 Driver 到家接 Kylin，09:00前送达 $shopName。"
            else -> normalized
        }
    }
}
