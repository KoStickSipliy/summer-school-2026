package com.volna.app.catalog.presentation

import com.volna.app.catalog.InstructorRepository
import com.volna.app.catalog.Page
import com.volna.app.catalog.PageRequest
import com.volna.app.catalog.SlotFilters
import com.volna.app.catalog.SlotRepository
import com.volna.app.core.error.AppFailure
import com.volna.app.core.error.AppFailureException
import com.volna.app.core.ui.EmptyReason
import com.volna.app.core.ui.Loadable
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.days

class SlotListStoreTest {
    @Test
    fun loadUsesDefaultSevenDayWindowAndSortsSlots() = runTest {
        val now = Instant.parse("2026-07-01T10:00:00Z")
        val slotRepository = FakeSlotRepository(
            results = listOf(
                Result.success(
                    Page(
                        items = listOf(
                            slot("slot-2", "2026-07-03T09:00:00Z"),
                            slot("slot-1", "2026-07-02T09:00:00Z"),
                        ),
                        limit = 20,
                        offset = 0,
                        total = 2,
                    ),
                ),
            ),
        )
        val store = SlotListStore(
            slotRepository = slotRepository,
            instructorRepository = FakeInstructorRepository(),
            nowProvider = { now },
            timeZoneProvider = { TimeZone.UTC },
            scope = CoroutineScope(coroutineContext),
        )

        store.accept(SlotListIntent.Load)
        yield()

        val state = store.state.value
        val content = assertIs<Loadable.Content<List<Slot>>>(state.slots)
        assertEquals(listOf("slot-1", "slot-2"), content.value.map { it.id.value })
        assertEquals(now, slotRepository.capturedFilters.single().dateFrom)
        assertEquals(now + 7.days, slotRepository.capturedFilters.single().dateTo)
    }

    @Test
    fun refreshFailureKeepsExistingSlotsAndShowsMessage() = runTest {
        val slot = slot("slot-1", "2026-07-02T09:00:00Z")
        val slotRepository = FakeSlotRepository(
            results = listOf(
                Result.success(Page(items = listOf(slot), limit = 20, offset = 0, total = 1)),
                Result.failure(AppFailureException(AppFailure.NetworkUnavailable)),
            ),
        )
        val store = SlotListStore(
            slotRepository = slotRepository,
            instructorRepository = FakeInstructorRepository(),
            scope = CoroutineScope(coroutineContext),
        )

        store.accept(SlotListIntent.Load)
        yield()
        store.accept(SlotListIntent.Refresh)
        yield()

        val state = store.state.value
        val content = assertIs<Loadable.Content<List<Slot>>>(state.slots)
        assertEquals(listOf(slot), content.value)
        assertEquals(false, content.refreshing)
        assertEquals("Не удалось обновить. Проверьте соединение и попробуйте снова.", state.message)
        assertEquals(2, slotRepository.calls)
    }

    @Test
    fun emptyResultWithAppliedFiltersUsesNoSlotsByFiltersReason() = runTest {
        val slotRepository = FakeSlotRepository(
            results = listOf(
                Result.success(Page(items = emptyList(), limit = 20, offset = 0, total = 0)),
            ),
        )
        val store = SlotListStore(
            slotRepository = slotRepository,
            instructorRepository = FakeInstructorRepository(),
            scope = CoroutineScope(coroutineContext),
        )

        store.accept(SlotListIntent.OpenFilters)
        store.accept(SlotListIntent.ToggleOnlyAvailable)
        store.accept(SlotListIntent.ApplyFilters)
        yield()

        val state = store.state.value
        val empty = assertIs<Loadable.Empty>(state.slots)
        assertEquals(EmptyReason.NoSlotsByFilters, empty.reason)
    }

    @Test
    fun closeFiltersWithoutApplyDoesNotChangeAppliedFilters() = runTest {
        val slotRepository = FakeSlotRepository(
            results = listOf(Result.success(Page(items = emptyList(), limit = 20, offset = 0, total = 0))),
        )
        val store = SlotListStore(
            slotRepository = slotRepository,
            instructorRepository = FakeInstructorRepository(),
            scope = CoroutineScope(coroutineContext),
        )

        store.accept(SlotListIntent.OpenFilters)
        yield()
        store.accept(SlotListIntent.ToggleOnlyAvailable)
        store.accept(SlotListIntent.CloseFilters)

        assertEquals(false, store.state.value.filters.onlyAvailable)
        assertEquals(0, slotRepository.calls)
    }

    @Test
    fun applyFiltersSanitizesUnknownInstructorIdsWhenDictionaryUnavailable() = runTest {
        val slotRepository = FakeSlotRepository(
            results = listOf(Result.success(Page(items = emptyList(), limit = 20, offset = 0, total = 0))),
        )
        val store = SlotListStore(
            slotRepository = slotRepository,
            instructorRepository = FakeInstructorRepository(
                results = listOf(Result.success(Page(items = emptyList(), limit = 20, offset = 0, total = 0))),
            ),
            scope = CoroutineScope(coroutineContext),
        )

        store.accept(SlotListIntent.OpenFilters)
        yield()
        store.accept(SlotListIntent.ToggleInstructor(InstructorId("ghost")))
        store.accept(SlotListIntent.ToggleOnlyAvailable)
        store.accept(SlotListIntent.ApplyFilters)
        yield()

        val sentFilters = slotRepository.capturedFilters.last()
        assertEquals(emptySet(), sentFilters.instructorIds)
        assertEquals(true, sentFilters.onlyAvailable)
    }

    @Test
    fun manualDateShiftSetsCustomRangeAndKeepsItValid() = runTest {
        val now = Instant.parse("2026-07-01T10:00:00Z")
        val slotRepository = FakeSlotRepository(
            results = listOf(Result.success(Page(items = emptyList(), limit = 20, offset = 0, total = 0))),
        )
        val store = SlotListStore(
            slotRepository = slotRepository,
            instructorRepository = FakeInstructorRepository(),
            nowProvider = { now },
            timeZoneProvider = { TimeZone.UTC },
            scope = CoroutineScope(coroutineContext),
        )

        store.accept(SlotListIntent.OpenFilters)
        store.accept(SlotListIntent.ShiftDateFrom(2))
        store.accept(SlotListIntent.ShiftDateTo(-5))
        store.accept(SlotListIntent.ApplyFilters)
        yield()

        val sentFilters = slotRepository.capturedFilters.last()
        assertEquals(true, sentFilters.dateFrom != null)
        assertEquals(true, sentFilters.dateTo != null)
        assertEquals(false, store.state.value.filtersVisible)
    }

    @Test
    fun openFiltersAlwaysReloadsInstructorDictionary() = runTest {
        val instructorRepository = FakeInstructorRepository()
        val store = SlotListStore(
            slotRepository = FakeSlotRepository(
                results = listOf(Result.success(Page(items = emptyList(), limit = 20, offset = 0, total = 0))),
            ),
            instructorRepository = instructorRepository,
            scope = CoroutineScope(coroutineContext),
        )

        store.accept(SlotListIntent.OpenFilters)
        yield()
        store.accept(SlotListIntent.CloseFilters)
        store.accept(SlotListIntent.OpenFilters)
        yield()

        assertEquals(2, instructorRepository.calls)
    }

    private class FakeSlotRepository(
        private val results: List<Result<Page<Slot>>>,
    ) : SlotRepository {
        var calls: Int = 0
            private set
        val capturedFilters: MutableList<SlotFilters> = mutableListOf()

        override suspend fun listSlots(filters: SlotFilters, page: PageRequest): Result<Page<Slot>> {
            capturedFilters += filters
            val result = results.getOrElse(calls) { results.last() }
            calls += 1
            return result
        }

        override suspend fun getSlot(slotId: SlotId): Result<Slot> =
            Result.failure(UnsupportedOperationException())
    }

    private class FakeInstructorRepository(
        private val results: List<Result<Page<Instructor>>> = listOf(
            Result.success(Page(items = emptyList(), limit = 20, offset = 0, total = 0)),
        ),
    ) : InstructorRepository {
        var calls: Int = 0
            private set

        override suspend fun listInstructors(page: PageRequest): Result<Page<Instructor>> {
            val result = results.getOrElse(calls) { results.last() }
            calls += 1
            return result
        }
    }

    private fun slot(
        id: String,
        startAt: String,
    ): Slot = Slot(
        id = SlotId(id),
        startAt = Instant.parse(startAt),
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
        price = MoneyRub(2_500),
        rentalPrice = MoneyRub(800),
        meetingPoint = MeetingPoint("Лодочная станция", GeoPoint(59.978, 30.262)),
        status = SlotStatus.Scheduled,
    )
}
