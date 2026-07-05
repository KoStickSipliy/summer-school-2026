package com.volna.app.notifications

import com.volna.app.domain.model.Booking
import com.volna.app.domain.model.BookingId
import com.volna.app.domain.model.BookingStatus
import com.volna.app.domain.model.GeoPoint
import com.volna.app.domain.model.Instructor
import com.volna.app.domain.model.InstructorId
import com.volna.app.domain.model.MeetingPoint
import com.volna.app.domain.model.MoneyRub
import com.volna.app.domain.model.Route
import com.volna.app.domain.model.RouteId
import com.volna.app.domain.model.RouteType
import com.volna.app.domain.model.Slot
import com.volna.app.domain.model.SlotId
import com.volna.app.domain.model.SlotStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultPushPermissionPromptCoordinatorTest {
    @Test
    fun firstBookingRequestsPermissionAndPersistsFlag() = runTest {
        val storage = FakePushPromptStateStorage(initialRequested = false)
        val requester = FakePushPermissionRequester()
        val coordinator = DefaultPushPermissionPromptCoordinator(storage, requester)

        val reminderHours = coordinator.requestIfNeeded(testBooking().copy(
            isFirstBooking = true,
            reminderHours = listOf(24, 2, 24, -1),
        ))

        assertEquals(listOf(24, 2), reminderHours)
        assertTrue(storage.requested)
        assertEquals(listOf(24, 2), requester.lastReminderHours)
    }

    @Test
    fun alreadyRequestedSkipsPermissionFlow() = runTest {
        val storage = FakePushPromptStateStorage(initialRequested = true)
        val requester = FakePushPermissionRequester()
        val coordinator = DefaultPushPermissionPromptCoordinator(storage, requester)

        val reminderHours = coordinator.requestIfNeeded(testBooking().copy(
            isFirstBooking = true,
            reminderHours = listOf(24, 2),
        ))

        assertNull(reminderHours)
        assertEquals(null, requester.lastReminderHours)
    }

    @Test
    fun nonFirstBookingDoesNotRequestPermission() = runTest {
        val storage = FakePushPromptStateStorage(initialRequested = false)
        val requester = FakePushPermissionRequester()
        val coordinator = DefaultPushPermissionPromptCoordinator(storage, requester)

        val reminderHours = coordinator.requestIfNeeded(testBooking().copy(
            isFirstBooking = false,
            reminderHours = listOf(24, 2),
        ))

        assertNull(reminderHours)
        assertEquals(null, requester.lastReminderHours)
        assertEquals(false, storage.requested)
    }

    private class FakePushPromptStateStorage(
        initialRequested: Boolean,
    ) : PushPermissionPromptStateStorage {
        var requested: Boolean = initialRequested

        override suspend fun wasPromptRequested(): Boolean = requested

        override suspend fun markPromptRequested() {
            requested = true
        }
    }

    private class FakePushPermissionRequester : PushPermissionRequester {
        var lastReminderHours: List<Int>? = null

        override suspend fun requestPermission(reminderHours: List<Int>) {
            lastReminderHours = reminderHours
        }
    }

    private fun testBooking(): Booking = Booking(
        id = BookingId("booking-1"),
        slotId = SlotId("slot-1"),
        clientId = null,
        seatsCount = 2,
        rentalCount = 1,
        status = BookingStatus.Active,
        priceTotal = MoneyRub(5800),
        createdAt = Instant.parse("2026-07-01T09:00:00Z"),
        cancelledAt = null,
        slot = Slot(
            id = SlotId("slot-1"),
            startAt = Instant.parse("2026-07-02T10:00:00Z"),
            route = Route(
                id = RouteId("route-1"),
                name = "Острова и каналы",
                type = RouteType.Novice,
                capacityCap = 8,
                durationMin = 90,
            ),
            instructor = Instructor(InstructorId("instructor-1"), "Мария"),
            totalSeats = 8,
            freeSeats = 4,
            freeRentalBoards = 4,
            price = MoneyRub(2500),
            rentalPrice = MoneyRub(800),
            meetingPoint = MeetingPoint("Лодочная станция", GeoPoint(59.978, 30.262)),
            status = SlotStatus.Scheduled,
        ),
    )
}
