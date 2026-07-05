package com.volna.app.booking.presentation

import com.volna.app.booking.BookingRepository
import com.volna.app.booking.IdempotencyKey
import com.volna.app.booking.IdempotencyKeyFactory
import com.volna.app.catalog.Page
import com.volna.app.catalog.PageRequest
import com.volna.app.domain.model.Booking
import com.volna.app.domain.model.BookingDraft
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
import com.volna.app.notifications.PushPermissionPromptCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BookingFormStoreTest {
    @Test
    fun submitSuccessStoresPushReminderHintAndClearsOnDismiss() = runTest {
        val slot = testSlot()
        val booking = testBooking(slot).copy(
            isFirstBooking = true,
            reminderHours = listOf(24, 2),
        )
        val repository = FakeBookingRepository(Result.success(booking))
        val coordinator = FakePushPermissionPromptCoordinator(reminderHours = listOf(24, 2))
        val store = BookingFormStore(
            bookingRepository = repository,
            keyFactory = FixedIdempotencyKeyFactory,
            pushPermissionPromptCoordinator = coordinator,
            scope = CoroutineScope(coroutineContext),
        )

        store.accept(BookingFormIntent.Open(slot))
        store.accept(BookingFormIntent.Submit)
        yield()

        val stateAfterSubmit = store.state.value
        assertNotNull(stateAfterSubmit.createdBooking)
        assertEquals(listOf(24, 2), stateAfterSubmit.pushPromptReminderHours)
        assertEquals(1, coordinator.calls)

        store.accept(BookingFormIntent.SuccessDismissed)
        val stateAfterDismiss = store.state.value
        assertNull(stateAfterDismiss.createdBooking)
        assertNull(stateAfterDismiss.pushPromptReminderHours)
    }

    @Test
    fun submitSuccessWithoutPromptKeepsHintEmpty() = runTest {
        val slot = testSlot()
        val booking = testBooking(slot).copy(
            isFirstBooking = false,
            reminderHours = listOf(24, 2),
        )
        val repository = FakeBookingRepository(Result.success(booking))
        val coordinator = FakePushPermissionPromptCoordinator(reminderHours = null)
        val store = BookingFormStore(
            bookingRepository = repository,
            keyFactory = FixedIdempotencyKeyFactory,
            pushPermissionPromptCoordinator = coordinator,
            scope = CoroutineScope(coroutineContext),
        )

        store.accept(BookingFormIntent.Open(slot))
        store.accept(BookingFormIntent.Submit)
        yield()

        val state = store.state.value
        assertNotNull(state.createdBooking)
        assertNull(state.pushPromptReminderHours)
        assertEquals(1, coordinator.calls)
    }

    private class FakeBookingRepository(
        private val createResult: Result<Booking>,
    ) : BookingRepository {
        override suspend fun createBooking(draft: BookingDraft, idempotencyKey: IdempotencyKey): Result<Booking> =
            createResult

        override suspend fun listBookings(status: BookingStatus?, page: PageRequest): Result<Page<Booking>> =
            Result.failure(UnsupportedOperationException())

        override suspend fun getBooking(bookingId: BookingId): Result<Booking> =
            Result.failure(UnsupportedOperationException())

        override suspend fun cancelBooking(bookingId: BookingId): Result<Booking> =
            Result.failure(UnsupportedOperationException())
    }

    private class FakePushPermissionPromptCoordinator(
        private val reminderHours: List<Int>?,
    ) : PushPermissionPromptCoordinator {
        var calls: Int = 0
            private set

        override suspend fun requestIfNeeded(booking: Booking): List<Int>? {
            calls += 1
            return reminderHours
        }
    }

    private object FixedIdempotencyKeyFactory : IdempotencyKeyFactory {
        override fun next(): IdempotencyKey = IdempotencyKey("00000000-0000-0000-0000-000000000001")
    }

    private fun testSlot(): Slot = Slot(
        id = SlotId("slot-1"),
        startAt = Instant.parse("2026-07-01T10:00:00Z"),
        route = Route(
            id = RouteId("route-1"),
            name = "Острова и каналы",
            type = RouteType.Novice,
            capacityCap = 8,
            durationMin = 90,
        ),
        instructor = Instructor(
            id = InstructorId("instructor-1"),
            name = "Мария",
        ),
        totalSeats = 8,
        freeSeats = 3,
        freeRentalBoards = 2,
        price = MoneyRub(2500),
        rentalPrice = MoneyRub(800),
        meetingPoint = MeetingPoint(
            title = "Лодочная станция",
            coordinates = GeoPoint(59.978, 30.262),
        ),
        status = SlotStatus.Scheduled,
    )

    private fun testBooking(slot: Slot): Booking = Booking(
        id = BookingId("booking-1"),
        slotId = slot.id,
        clientId = null,
        seatsCount = 2,
        rentalCount = 1,
        status = BookingStatus.Active,
        priceTotal = MoneyRub(5800),
        createdAt = Instant.parse("2026-07-01T09:00:00Z"),
        cancelledAt = null,
        slot = slot,
    )
}
