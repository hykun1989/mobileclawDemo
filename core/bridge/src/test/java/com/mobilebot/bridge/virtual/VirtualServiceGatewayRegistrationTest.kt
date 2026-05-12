package com.mobilebot.bridge.virtual

import com.mobilebot.bridge.ServiceActionDescriptor
import com.mobilebot.bridge.ServiceDescriptor
import com.mobilebot.bridge.ServiceRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualServiceGatewayRegistrationTest {
    @Test
    fun registeredCatalogServiceReturnsKnownDataWithMetadata() = runBlocking {
        val gateway = VirtualServiceGateway()
        gateway.registerService(ctripDescriptor())

        val response = gateway.call(ServiceRequest("ctrip", "bookFlight"))

        assertTrue(response.ok)
        assertEquals("ctrip", response.data["_serviceId"])
        assertEquals("bookFlight", response.data["_action"])
        assertEquals(true, response.data["_virtual"])
        assertTrue(response.data.containsKey("bookingId"))
    }

    @Test
    fun registeredActionWithoutCatalogDataEchoesParams() = runBlocking {
        val gateway = VirtualServiceGateway()
        gateway.registerService(
            ServiceDescriptor(
                id = "local_service",
                name = "Local Service",
                category = "utility",
                baseUrl = "https://service.local",
                authType = "none",
                actions = listOf(ServiceActionDescriptor("reserve", "POST", "/reserve", "Reserve resource")),
            ),
        )

        val response = gateway.call(
            ServiceRequest(
                serviceId = "local_service",
                action = "reserve",
                params = mapOf("slot" to "14:00"),
            ),
        )

        assertTrue(response.ok)
        assertEquals("local_service", response.data["serviceId"])
        assertEquals("reserve", response.data["action"])
        assertEquals(mapOf("slot" to "14:00"), response.data["params"])
    }

    @Test
    fun unknownServiceAndUnknownActionFailClearly() = runBlocking {
        val gateway = VirtualServiceGateway()
        gateway.registerService(ctripDescriptor())

        val unknownService = gateway.call(ServiceRequest("missing", "bookFlight"))
        val unknownAction = gateway.call(ServiceRequest("ctrip", "missingAction"))

        assertFalse(unknownService.ok)
        assertTrue(unknownService.message.contains("Unknown service"))
        assertFalse(unknownAction.ok)
        assertTrue(unknownAction.message.contains("Unknown action"))
    }

    @Test
    fun registrationControlsAvailability() {
        val gateway = VirtualServiceGateway()

        assertFalse(gateway.isServiceAuthorized("ctrip"))
        gateway.registerService(ctripDescriptor())

        assertTrue(gateway.isServiceAuthorized("ctrip"))
        assertEquals(listOf("ctrip"), gateway.listAvailableServices().map { it.id })
    }

    private fun ctripDescriptor() = ServiceDescriptor(
        id = "ctrip",
        name = "Ctrip",
        category = "travel",
        baseUrl = "https://ctrip.local/api/v1",
        authType = "none",
        actions = listOf(
            ServiceActionDescriptor("searchFlights", "GET", "/flights", "Search flights"),
            ServiceActionDescriptor("bookFlight", "POST", "/flights/book", "Book flight"),
        ),
    )
}
