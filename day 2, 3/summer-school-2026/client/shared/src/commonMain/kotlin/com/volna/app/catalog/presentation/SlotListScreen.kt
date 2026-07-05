package com.volna.app.catalog.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.volna.app.catalog.SlotFilters
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.ui.EmptyReason
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Instructor
import com.volna.app.domain.model.RouteType
import com.volna.app.domain.model.Slot
import com.volna.app.domain.model.SlotStatus
import com.volna.app.uikit.icons.Icons
import com.volna.app.uikit.icons.Tune
import com.volna.app.uikit.icons.VolnaIcon
import kotlinx.coroutines.delay
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotListScreen(
    state: SlotListState,
    onIntent: (SlotListIntent) -> Unit,
    onSlotClick: (Slot) -> Unit,
) {
    LaunchedEffect(Unit) {
        onIntent(SlotListIntent.Load)
    }
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(2_500)
            onIntent(SlotListIntent.MessageShown)
        }
    }

    val refreshing = (state.slots as? Loadable.Content)?.refreshing == true

    Column(modifier = Modifier.fillMaxSize()) {
        SlotListHeader(
            activeFiltersCount = state.filters.activeGroupsCount(),
            onFiltersClick = { onIntent(SlotListIntent.OpenFilters) },
        )
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { onIntent(SlotListIntent.Refresh) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (val slots = state.slots) {
                Loadable.Initial -> SlotInitialLoader()
                Loadable.Loading -> SlotLoadingSkeleton()
                is Loadable.Content -> SlotCards(
                    slots = slots.value,
                    message = state.message,
                    onSlotClick = onSlotClick,
                )
                is Loadable.Empty -> if (slots.reason == EmptyReason.NoSlotsByFilters) {
                    StateMessage(
                        title = "Ничего не найдено по фильтрам",
                        description = "Попробуйте изменить параметры фильтрации",
                        buttonText = "Изменить фильтры",
                        artwork = StateArtwork.Empty,
                        onClick = { onIntent(SlotListIntent.OpenFilters) },
                    )
                } else {
                    StateMessage(
                        title = "Пока нет доступных прогулок",
                        description = "Загляните позже",
                    )
                }

                is Loadable.Error -> StateMessage(
                    title = "Не удалось загрузить",
                    description = "Проверьте соединение и попробуйте снова",
                    buttonText = "Обновить",
                    artwork = StateArtwork.Error,
                    onClick = { onIntent(SlotListIntent.Retry) },
                )
            }
        }
    }
    if (state.filtersVisible) {
        SlotFiltersSheet(
            state = state,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun SlotListHeader(
    activeFiltersCount: Int,
    onFiltersClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        ScreenTitle("Прогулки")

        val filterButtonModifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = VolnaTheme.tokens.sizing.screenMaxWidth - VolnaTheme.tokens.sizing.filterIconX - VolnaTheme.tokens.spacing.xl)
            .size(VolnaTheme.tokens.spacing.xl)
            .clickable { onFiltersClick() }

        Box(modifier = filterButtonModifier) {
            VolnaIcon(
                imageVector = Icons.Tune,
                contentDescription = "Фильтры",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = VolnaTheme.tokens.spacing.xl,
            )
            if (activeFiltersCount > 0) {
                FilterBadge(
                    count = activeFiltersCount,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-6).dp),
                )
            }
        }
    }
}

@Composable
private fun FilterBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotFiltersSheet(
    state: SlotListState,
    onIntent: (SlotListIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    val hasDraftChanges = state.draftFilters != SlotFilters()

    ModalBottomSheet(
        onDismissRequest = { onIntent(SlotListIntent.CloseFilters) },
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = VolnaTheme.tokens.spacing.xl,
            topEnd = VolnaTheme.tokens.spacing.xl,
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = Color(0xFFCCCCCC).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
                        ),
                )
            }
        },
    ) {
        // CMP-13 / BS-001: filter form only collects conditions; SCR-002 reloads after apply.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = VolnaTheme.tokens.spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Фильтры", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Сбросить",
                            modifier = Modifier.clickable(enabled = hasDraftChanges) { onIntent(SlotListIntent.ResetFilters) },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (hasDraftChanges) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            text = "✕",
                            modifier = Modifier.clickable { onIntent(SlotListIntent.CloseFilters) },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.width(VolnaTheme.tokens.sizing.contentWidth),
                verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.lg),
            ) {
                FilterGroup(title = "Дата старта") {
                    FilterChipRow {
                        FilterChipButton(
                            label = "Любая",
                            selected = state.draftDatePreset == SlotDatePreset.Any
                                && state.draftFilters.dateFrom == null
                                && state.draftFilters.dateTo == null,
                        ) {
                            onIntent(SlotListIntent.SelectDatePreset(SlotDatePreset.Any))
                        }
                        FilterChipButton("Сегодня", state.draftDatePreset == SlotDatePreset.Today) {
                            onIntent(SlotListIntent.SelectDatePreset(SlotDatePreset.Today))
                        }
                        FilterChipButton("Эта неделя", state.draftDatePreset == SlotDatePreset.NextSevenDays) {
                            onIntent(SlotListIntent.SelectDatePreset(SlotDatePreset.NextSevenDays))
                        }
                        FilterChipButton("Выходные", state.draftDatePreset == SlotDatePreset.Weekend) {
                            onIntent(SlotListIntent.SelectDatePreset(SlotDatePreset.Weekend))
                        }
                    }
                    DateRangePreviewRow(state)
                    DateRangeAdjustRow(
                        onFromMinus = { onIntent(SlotListIntent.ShiftDateFrom(-1)) },
                        onFromPlus = { onIntent(SlotListIntent.ShiftDateFrom(1)) },
                        onToMinus = { onIntent(SlotListIntent.ShiftDateTo(-1)) },
                        onToPlus = { onIntent(SlotListIntent.ShiftDateTo(1)) },
                    )
                }

                FilterGroup(title = "Тип маршрута") {
                    FilterChipRow {
                        FilterChipButton("Новичковый", RouteType.Novice in state.draftFilters.routeTypes) {
                            onIntent(SlotListIntent.ToggleRouteType(RouteType.Novice))
                        }
                        FilterChipButton("Опытный", RouteType.Experienced in state.draftFilters.routeTypes) {
                            onIntent(SlotListIntent.ToggleRouteType(RouteType.Experienced))
                        }
                    }
                }

                InstructorFilterSection(
                    instructors = state.instructors,
                    selected = state.draftFilters.instructorIds,
                    onToggle = { onIntent(SlotListIntent.ToggleInstructor(it.id)) },
                    onRetry = { onIntent(SlotListIntent.RetryInstructors) },
                )

                AvailabilitySwitchRow(
                    checked = state.draftFilters.onlyAvailable,
                    onToggle = { onIntent(SlotListIntent.ToggleOnlyAvailable) },
                )
            }

            Button(
                onClick = { onIntent(SlotListIntent.ApplyFilters) },
                modifier = Modifier
                    .width(VolnaTheme.tokens.sizing.contentWidth)
                    .height(VolnaTheme.tokens.sizing.buttonHeight),
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Применить", fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .width(138.dp)
                    .height(4.dp)
                    .background(Color(0xFFCCCCCC), RoundedCornerShape(VolnaTheme.tokens.radius.pill)),
            )
            Spacer(Modifier.height(VolnaTheme.tokens.spacing.xs))
        }
    }
}

@Composable
private fun FilterGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun FilterChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
        content = { content() },
    )
}

@Composable
private fun FilterChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .height(40.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
            )
            .clickable { onClick() }
            .padding(horizontal = VolnaTheme.tokens.spacing.sm, vertical = 10.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun DateRangePreviewRow(state: SlotListState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
    ) {
        DateRangeField(
            text = state.draftFilters.dateFrom.toFilterDateText("с", "не выбрано"),
            modifier = Modifier.weight(1f),
        )
        DateRangeField(
            text = state.draftFilters.dateTo.toFilterDateText("по", "не выбрано"),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DateRangeField(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = VolnaTheme.tokens.spacing.sm, vertical = 10.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun DateRangeAdjustRow(
    onFromMinus: () -> Unit,
    onFromPlus: () -> Unit,
    onToMinus: () -> Unit,
    onToPlus: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
        ) {
            OutlinedButton(
                onClick = onFromMinus,
                modifier = Modifier.weight(1f),
            ) {
                Text("С -1д")
            }
            OutlinedButton(
                onClick = onFromPlus,
                modifier = Modifier.weight(1f),
            ) {
                Text("С +1д")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
        ) {
            OutlinedButton(
                onClick = onToMinus,
                modifier = Modifier.weight(1f),
            ) {
                Text("По -1д")
            }
            OutlinedButton(
                onClick = onToPlus,
                modifier = Modifier.weight(1f),
            ) {
                Text("По +1д")
            }
        }
    }
}

@Composable
private fun AvailabilitySwitchRow(
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Только со свободными местами",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
private fun InstructorFilterSection(
    instructors: Loadable<List<Instructor>>,
    selected: Set<com.volna.app.domain.model.InstructorId>,
    onToggle: (Instructor) -> Unit,
    onRetry: () -> Unit,
) {
    when (instructors) {
        Loadable.Initial,
        Loadable.Loading -> FilterGroup("Инструктор") {
            Text("Загружаем инструкторов", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is Loadable.Empty -> Unit
        is Loadable.Error -> FilterGroup("Инструктор") {
            Text("Не удалось загрузить инструкторов", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onRetry) {
                Text("Обновить")
            }
        }
        is Loadable.Content -> FilterGroup("Инструктор") {
            Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
                instructors.value.chunked(2).forEach { row ->
                    FilterChipRow {
                        row.forEach { instructor ->
                            FilterChipButton(
                                label = instructor.name,
                                selected = instructor.id in selected,
                                onClick = { onToggle(instructor) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotInitialLoader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = 309.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
            strokeWidth = 6.dp,
        )
    }
}

@Composable
private fun SlotLoadingSkeleton() {
    SkeletonCard(y = 136.dp, height = 160.dp)
    SkeletonCard(y = 308.dp, height = 160.dp)
}

@Composable
private fun SlotCards(
    slots: List<Slot>,
    message: String?,
    onSlotClick: (Slot) -> Unit,
) {
    val groupedSlots = remember(slots) { slots.toDaySections() }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = VolnaTheme.tokens.spacing.md,
            end = VolnaTheme.tokens.spacing.md,
            bottom = VolnaTheme.tokens.sizing.navContentBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        if (message != null) {
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        groupedSlots.forEach { section ->
            item(key = "header-${section.title}") {
                SlotDayHeader(section.title)
            }
            items(section.slots, key = { it.id.value }) { slot ->
                SlotCard(slot, onSlotClick)
            }
        }
    }
}

@Composable
private fun SlotDayHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SlotCard(
    slot: Slot,
    onSlotClick: (Slot) -> Unit,
) {
    val canOpen = slot.isBookableFromList()
    val availability = slot.availabilityLabel()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.listCardHeight)
            .clickable(enabled = canOpen) { onSlotClick(slot) }
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.spacing.xl),
            )
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
            SlotPreviewPhoto()
            Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xxs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xxs)) {
                    SlotTag(
                        text = slot.route.type.toTagText(),
                        color = Color(0xFF92FF9A),
                    )
                    SlotTag(
                        text = slot.route.name,
                        color = Color(0xFFFFF897),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (slot.status == SlotStatus.Cancelled) {
                        SlotTag(
                            text = "Отменён",
                            color = Color(0xFFFFD5D5),
                        )
                    }
                }
                Text(
                    text = slot.startAt.toSlotCardStartText(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Инструктор: ${slot.instructor.name}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${slot.price.value} ₽",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
                )
                .padding(horizontal = VolnaTheme.tokens.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = availability.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = availability.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SlotPreviewPhoto() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFD8EEF0),
                        Color(0xFFF7F0D8),
                        Color(0xFFCFE4E8),
                    ),
                ),
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.36f)),
                    ),
                    shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
                ),
        )
    }
}

private data class SlotDaySection(
    val title: String,
    val slots: List<Slot>,
)

private data class SlotAvailability(
    val title: String,
    val value: String,
)

private fun SlotFilters.activeGroupsCount(): Int {
    var count = 0
    if (dateFrom != null || dateTo != null) count += 1
    if (routeTypes.isNotEmpty()) count += 1
    if (instructorIds.isNotEmpty()) count += 1
    if (onlyAvailable) count += 1
    return count
}

private fun Slot.isBookableFromList(): Boolean = status == SlotStatus.Scheduled && freeSeats > 0

private fun Slot.availabilityLabel(): SlotAvailability = when {
    status == SlotStatus.Cancelled -> SlotAvailability(
        title = "Слот отменён",
        value = "недоступен",
    )
    freeSeats > 0 -> SlotAvailability(
        title = "Свободно мест",
        value = "$freeSeats из $totalSeats",
    )
    else -> SlotAvailability(
        title = "Мест нет",
        value = "$freeSeats из $totalSeats",
    )
}

private fun List<Slot>.toDaySections(): List<SlotDaySection> {
    val zone = TimeZone.currentSystemDefault()
    return groupBy { slot -> slot.startAt.toLocalDateTime(zone).date }
        .entries
        .sortedBy { it.key.toEpochDays() }
        .map { entry ->
            SlotDaySection(
                title = entry.key.toSlotDayHeader(),
                slots = entry.value.sortedBy { it.startAt },
            )
        }
}

private fun LocalDate.toSlotDayHeader(): String = "${dayOfWeek.shortName()}, $day ${month.monthName()}"

private fun DayOfWeek.shortName(): String = when (this) {
    DayOfWeek.MONDAY -> "Пн"
    DayOfWeek.TUESDAY -> "Вт"
    DayOfWeek.WEDNESDAY -> "Ср"
    DayOfWeek.THURSDAY -> "Чт"
    DayOfWeek.FRIDAY -> "Пт"
    DayOfWeek.SATURDAY -> "Сб"
    DayOfWeek.SUNDAY -> "Вс"
}

private fun Month.monthName(): String = when (this) {
    Month.JANUARY -> "января"
    Month.FEBRUARY -> "февраля"
    Month.MARCH -> "марта"
    Month.APRIL -> "апреля"
    Month.MAY -> "мая"
    Month.JUNE -> "июня"
    Month.JULY -> "июля"
    Month.AUGUST -> "августа"
    Month.SEPTEMBER -> "сентября"
    Month.OCTOBER -> "октября"
    Month.NOVEMBER -> "ноября"
    Month.DECEMBER -> "декабря"
}
