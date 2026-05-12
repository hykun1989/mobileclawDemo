package com.mobilebot.bridge.virtual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualCatalogDataTest {
    @Test
    fun configuredCatalogsReturnCoreActionResponses() {
        val actionsByService = mapOf(
            "geico" to listOf("getPolicy", "fileClaim", "getClaimStatus", "uploadEvidence"),
            "tesla_fleet" to listOf("getVehicleData", "getCollisionReport", "getDashcamFootage", "getLocation", "flashLights"),
            "ctrip" to listOf("searchFlights", "bookFlight", "searchHotels", "bookHotel", "getFlightStatus"),
            "aaa_roadside" to listOf("requestTow", "checkMembership", "findServiceCenter", "getTowStatus"),
            "opentable" to listOf("searchRestaurants", "getRestaurant", "checkAvailability", "makeReservation", "cancelReservation"),
            "marriott" to listOf("searchHotels", "getHotelDetails", "checkAvailability", "bookRoom", "getBonvoyBalance"),
            "visa_checker" to listOf("checkRequirements", "checkStatus"),
            "hotel_search" to listOf("search", "getDetails", "book"),
        )

        actionsByService.forEach { (serviceId, actions) ->
            actions.forEach { action ->
                assertNotNull("Expected response for $serviceId/$action", VirtualMockData.lookup(serviceId, action))
            }
        }
    }

    @Test
    fun dynamicFlightSearchReflectsRequestedRouteAndDate() {
        val response = requireNotNull(
            VirtualMockData.lookup(
                serviceId = "ctrip",
                action = "searchFlights",
                params = mapOf(
                    "from" to "上海",
                    "to" to "东京",
                    "date" to "2027-04-25",
                ),
            ),
        )

        @Suppress("UNCHECKED_CAST")
        val results = response["results"] as List<Map<String, Any>>
        assertEquals("上海", response["from"])
        assertEquals("东京", response["to"])
        assertEquals("2027-04-25", response["searchDate"])
        assertTrue(results.any { it["departure"].toString().startsWith("上海") })
        assertTrue(results.any { it["arrival"].toString().startsWith("东京") })
    }
}
