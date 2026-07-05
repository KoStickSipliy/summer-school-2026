package com.volna.app.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.volna.app.catalog.PageRequest
import com.volna.app.catalog.InstructorRepository
import com.volna.app.catalog.SlotFilters
import com.volna.app.catalog.SlotRepository
import com.volna.app.core.error.AppFailure
import com.volna.app.core.error.asAppFailure
import com.volna.app.core.logging.AppLogger
import com.volna.app.core.mvi.MviStore
import com.volna.app.core.ui.EmptyReason
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Instructor
import com.volna.app.domain.model.InstructorId
import com.volna.app.domain.model.RouteType
import com.volna.app.domain.model.Slot
import kotlinx.datetime.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

data class SlotListState(
    val slots: Loadable<List<Slot>> = Loadable.Initial,
    val filters: SlotFilters = SlotFilters(),
    val datePreset: SlotDatePreset = SlotDatePreset.Any,
    val draftFilters: SlotFilters = SlotFilters(),
    val draftDatePreset: SlotDatePreset = SlotDatePreset.Any,
    val instructors: Loadable<List<Instructor>> = Loadable.Initial,
    val filtersVisible: Boolean = false,
    val message: String? = null,
)

enum class SlotDatePreset {
    Any,
    Today,
    NextSevenDays,
    Weekend,
}

sealed interface SlotListIntent {
    data object Load : SlotListIntent
    data object Refresh : SlotListIntent
    data object Retry : SlotListIntent
    data object MessageShown : SlotListIntent
    data object OpenFilters : SlotListIntent
    data object CloseFilters : SlotListIntent
    data object ApplyFilters : SlotListIntent
    data object ResetFilters : SlotListIntent
    data object RetryInstructors : SlotListIntent
    data class SelectDatePreset(val preset: SlotDatePreset) : SlotListIntent
    data class ShiftDateFrom(val days: Int) : SlotListIntent
    data class ShiftDateTo(val days: Int) : SlotListIntent
    data class ToggleRouteType(val routeType: RouteType) : SlotListIntent
    data class ToggleInstructor(val instructorId: InstructorId) : SlotListIntent
    data object ToggleOnlyAvailable : SlotListIntent
    data object Reset : SlotListIntent
}

sealed interface SlotListEffect {
    data object SignedOut : SlotListEffect
}

// CMP-13 / BS-001: slot filters, instructor dictionary loading, and SCR-002 filtered reload.
class SlotListStore(
    private val slotRepository: SlotRepository,
    private val instructorRepository: InstructorRepository,
    private val nowProvider: () -> Instant = { Clock.System.now() },
    private val timeZoneProvider: () -> TimeZone = { TimeZone.currentSystemDefault() },
    scope: CoroutineScope? = null,
) : ViewModel(), MviStore<SlotListState, SlotListIntent, SlotListEffect> {
    private val mutableState = MutableStateFlow(SlotListState())
    private val effects = Channel<SlotListEffect>(Channel.BUFFERED)
    private val storeScope = scope ?: viewModelScope

    override val state: StateFlow<SlotListState> = mutableState

    override fun accept(intent: SlotListIntent) {
        when (intent) {
            SlotListIntent.Load -> load(force = false)
            SlotListIntent.Refresh -> load(force = true)
            SlotListIntent.Retry -> load(force = true)
            SlotListIntent.MessageShown -> mutableState.update { it.copy(message = null) }
            SlotListIntent.OpenFilters -> openFilters()
            SlotListIntent.CloseFilters -> mutableState.update { it.copy(filtersVisible = false, message = null) }
            SlotListIntent.ApplyFilters -> applyFilters()
            SlotListIntent.ResetFilters -> mutableState.update {
                it.copy(draftFilters = SlotFilters(), draftDatePreset = SlotDatePreset.Any)
            }
            SlotListIntent.RetryInstructors -> loadInstructors(force = true)
            is SlotListIntent.SelectDatePreset -> selectDatePreset(intent.preset)
            is SlotListIntent.ShiftDateFrom -> shiftDateFrom(intent.days)
            is SlotListIntent.ShiftDateTo -> shiftDateTo(intent.days)
            is SlotListIntent.ToggleRouteType -> toggleRouteType(intent.routeType)
            is SlotListIntent.ToggleInstructor -> toggleInstructor(intent.instructorId)
            SlotListIntent.ToggleOnlyAvailable -> mutableState.update {
                it.copy(draftFilters = it.draftFilters.copy(onlyAvailable = !it.draftFilters.onlyAvailable))
            }
            SlotListIntent.Reset -> mutableState.value = SlotListState()
        }
    }

    override suspend fun effects(): SlotListEffect = effects.receive()

    private fun load(force: Boolean) {
        val current = mutableState.value.slots
        if (!force && (current == Loadable.Loading || current is Loadable.Content)) return

        storeScope.launch {
            val filters = mutableState.value.filters
            mutableState.update {
                it.copy(
                    slots = if (force && current is Loadable.Content) {
                        current.copy(refreshing = true)
                    } else {
                        Loadable.Loading
                    },
                    message = null,
                )
            }
            slotRepository.listSlots(effectiveFilters(filters), PageRequest()).fold(
                onSuccess = { page ->
                    val sortedSlots = page.items.sortedBy { it.startAt }
                    mutableState.update {
                        it.copy(
                            slots = if (sortedSlots.isEmpty()) {
                                Loadable.Empty(
                                    if (filters == SlotFilters()) {
                                        EmptyReason.NoSlots
                                    } else {
                                        EmptyReason.NoSlotsByFilters
                                    },
                                )
                            } else {
                                Loadable.Content(sortedSlots)
                            },
                        )
                    }
                },
                onFailure = { failure ->
                    AppLogger.e(failure, "Failed to load slots")
                    val appFailure = failure.asAppFailure()
                    if (appFailure == AppFailure.Unauthorized) {
                        effects.send(SlotListEffect.SignedOut)
                    } else if (force && current is Loadable.Content) {
                        mutableState.update {
                            it.copy(
                                slots = current.copy(refreshing = false),
                                message = "Не удалось обновить. Проверьте соединение и попробуйте снова.",
                            )
                        }
                    } else {
                        mutableState.update { it.copy(slots = Loadable.Error(appFailure)) }
                    }
                },
            )
        }
    }

    private fun openFilters() {
        mutableState.update {
            it.copy(
                filtersVisible = true,
                draftFilters = it.filters,
                draftDatePreset = it.datePreset,
            )
        }
        loadInstructors(force = true)
    }

    private fun applyFilters() {
        mutableState.update {
            val sanitizedInstructors = sanitizeInstructorIds(
                selected = it.draftFilters.instructorIds,
                instructors = it.instructors,
            )
            val appliedFilters = it.draftFilters.copy(instructorIds = sanitizedInstructors)
            it.copy(
                filters = appliedFilters,
                datePreset = it.draftDatePreset,
                filtersVisible = false,
                message = null,
            )
        }
        load(force = true)
    }

    private fun selectDatePreset(preset: SlotDatePreset) {
        val (dateFrom, dateTo) = resolvePresetRange(preset)
        mutableState.update {
            it.copy(
                draftDatePreset = preset,
                draftFilters = it.draftFilters.copy(
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                ),
                message = null,
            )
        }
    }

    private fun shiftDateFrom(days: Int) {
        if (days == 0) return
        val nowDate = nowLocalDate()
        val zone = timeZoneProvider()
        mutableState.update { state ->
            val currentFrom = state.draftFilters.dateFrom?.toLocalDateTime(zone)?.date ?: nowDate
            val shiftedFrom = currentFrom.plus(DatePeriod(days = days))
            val boundedFrom = if (shiftedFrom < nowDate) nowDate else shiftedFrom
            val currentTo = state.draftFilters.dateTo?.toLocalDateTime(zone)?.date ?: boundedFrom
            val normalizedTo = if (currentTo < boundedFrom) boundedFrom else currentTo
            state.copy(
                draftDatePreset = SlotDatePreset.Any,
                draftFilters = state.draftFilters.copy(
                    dateFrom = boundedFrom.atStartOfDayIn(zone),
                    dateTo = endOfDay(normalizedTo, zone),
                ),
                message = null,
            )
        }
    }

    private fun shiftDateTo(days: Int) {
        if (days == 0) return
        val nowDate = nowLocalDate()
        val zone = timeZoneProvider()
        mutableState.update { state ->
            val currentFrom = state.draftFilters.dateFrom?.toLocalDateTime(zone)?.date ?: nowDate
            val currentTo = state.draftFilters.dateTo?.toLocalDateTime(zone)?.date ?: currentFrom
            val shiftedTo = currentTo.plus(DatePeriod(days = days))
            val normalizedTo = when {
                shiftedTo < nowDate -> nowDate
                shiftedTo < currentFrom -> currentFrom
                else -> shiftedTo
            }
            state.copy(
                draftDatePreset = SlotDatePreset.Any,
                draftFilters = state.draftFilters.copy(
                    dateFrom = currentFrom.atStartOfDayIn(zone),
                    dateTo = endOfDay(normalizedTo, zone),
                ),
                message = null,
            )
        }
    }

    private fun toggleRouteType(routeType: RouteType) {
        mutableState.update { state ->
            state.copy(
                draftFilters = state.draftFilters.copy(
                    routeTypes = state.draftFilters.routeTypes.toggle(routeType),
                ),
            )
        }
    }

    private fun toggleInstructor(instructorId: InstructorId) {
        mutableState.update { state ->
            state.copy(
                draftFilters = state.draftFilters.copy(
                    instructorIds = state.draftFilters.instructorIds.toggle(instructorId),
                ),
            )
        }
    }

    private fun loadInstructors(force: Boolean) {
        val current = mutableState.value.instructors
        if (!force && (current == Loadable.Loading || current is Loadable.Content || current is Loadable.Empty)) return

        storeScope.launch {
            mutableState.update { it.copy(instructors = Loadable.Loading) }
            instructorRepository.listInstructors().fold(
                onSuccess = { page ->
                    mutableState.update {
                        it.copy(
                            instructors = if (page.items.isEmpty()) {
                                Loadable.Empty(EmptyReason.NoSlots)
                            } else {
                                Loadable.Content(page.items)
                            },
                        )
                    }
                },
                onFailure = { failure ->
                    AppLogger.e(failure, "Failed to load instructors")
                    mutableState.update { it.copy(instructors = Loadable.Error(failure.asAppFailure())) }
                },
            )
        }
    }

    private fun resolvePresetRange(preset: SlotDatePreset): Pair<Instant?, Instant?> {
        val today = nowLocalDate()
        val zone = timeZoneProvider()
        return when (preset) {
            SlotDatePreset.Any -> null to null
            SlotDatePreset.Today -> {
                today.atStartOfDayIn(zone) to endOfDay(today, zone)
            }
            SlotDatePreset.NextSevenDays -> {
                val dayNumber = today.dayOfWeek.isoDayNumber()
                val weekStart = today.plus(DatePeriod(days = -(dayNumber - 1)))
                val weekEnd = weekStart.plus(DatePeriod(days = 6))
                weekStart.atStartOfDayIn(zone) to endOfDay(weekEnd, zone)
            }
            SlotDatePreset.Weekend -> {
                val dayNumber = today.dayOfWeek.isoDayNumber()
                val daysUntilSaturday = when (today.dayOfWeek) {
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY -> 0
                    else -> 6 - dayNumber
                }
                val start = today.plus(DatePeriod(days = daysUntilSaturday))
                val end = if (start.dayOfWeek == DayOfWeek.SATURDAY) {
                    start.plus(DatePeriod(days = 1))
                } else {
                    start
                }
                start.atStartOfDayIn(zone) to endOfDay(end, zone)
            }
        }
    }

    private fun sanitizeInstructorIds(
        selected: Set<InstructorId>,
        instructors: Loadable<List<Instructor>>,
    ): Set<InstructorId> {
        val known = (instructors as? Loadable.Content)?.value
            ?.map { it.id }
            ?.toSet()
            ?: emptySet()
        if (known.isEmpty()) return emptySet()
        return selected.filterTo(mutableSetOf()) { it in known }
    }

    private fun nowLocalDate(): LocalDate = nowProvider().toLocalDateTime(timeZoneProvider()).date

    private fun endOfDay(date: LocalDate, timeZone: TimeZone): Instant =
        date.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone)
            .minus(1.milliseconds)

    private fun effectiveFilters(filters: SlotFilters): SlotFilters {
        if (filters.dateFrom != null || filters.dateTo != null) {
            val resolvedDateFrom = filters.dateFrom ?: nowProvider()
            val resolvedDateTo = filters.dateTo ?: (resolvedDateFrom + 7.days)
            return filters.copy(dateFrom = resolvedDateFrom, dateTo = resolvedDateTo)
        }

        val now = nowProvider()
        return filters.copy(
            dateFrom = now,
            dateTo = now + 7.days,
        )
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> =
    if (item in this) this - item else this + item

private fun DayOfWeek.isoDayNumber(): Int = when (this) {
    DayOfWeek.MONDAY -> 1
    DayOfWeek.TUESDAY -> 2
    DayOfWeek.WEDNESDAY -> 3
    DayOfWeek.THURSDAY -> 4
    DayOfWeek.FRIDAY -> 5
    DayOfWeek.SATURDAY -> 6
    DayOfWeek.SUNDAY -> 7
}
