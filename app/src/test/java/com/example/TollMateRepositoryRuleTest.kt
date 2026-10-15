package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repository.TollMateRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TollMateRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createJourney_authenticatedUser_success() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = TollMateRepository(firestore)

        val jrnId = "JRN-20261015-" + UUID.randomUUID().toString().take(5).uppercase()
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createJourney(
                journeyId = jrnId,
                vehicleNumber = "MH12AB1234",
                startLocation = "Pune",
                destination = "Mumbai"
            )
        }

        assertTrue("Expected createJourney to succeed", result.isSuccess)
        val created = result.getOrThrow()
        assertEquals(jrnId, created.journeyId)
        assertEquals("MH12AB1234", created.vehicleNumber)
        assertEquals("ACTIVE", created.status)
    }

    @Test
    fun observeUserJourneys_authenticatedUser_returnsJourneys() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = TollMateRepository(firestore)

        val jrnId = "JRN-20261015-" + UUID.randomUUID().toString().take(5).uppercase()
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createJourney(
                journeyId = jrnId,
                vehicleNumber = "DL01A1234",
                startLocation = "Delhi",
                destination = "Jaipur"
            ).getOrThrow()
        }

        val journeys = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserJourneys(uid).first { list -> list.any { it.journeyId == jrnId } }
        }

        assertTrue(journeys.any { it.journeyId == jrnId })
    }

    @Test
    fun unauthenticatedAccess_failsGracefully() = runBlocking {
        auth.signOut()
        val repository = TollMateRepository(firestore)

        var failedWithSecurityOrAuth = false
        try {
            repository.createJourney(
                journeyId = "JRN-UNAUTH",
                vehicleNumber = "MH12AB1234",
                startLocation = "A",
                destination = "B"
            )
        } catch (e: Exception) {
            failedWithSecurityOrAuth = true
        }

        assertTrue("Expected unauthenticated write to be rejected", failedWithSecurityOrAuth)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@tollmate.com"
        const val BOB_EMAIL = "bob@tollmate.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
