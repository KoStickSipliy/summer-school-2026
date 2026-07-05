# Mobile App Spec

**Этап:** 5. Mobile app spec · **Дата:** 2026-07-04

Этот раздел содержит прикладное ТЗ для клиентского мобильного приложения гончарной мастерской. Документы собраны по смысловым блокам: отдельные экраны, bottom sheet, переиспользуемые логики, модель данных и последовательности API.

## Источники

- [Бизнес-требования](../2-requirements/business-requirements.md)
- [Функциональные требования](../2-requirements/functional-requirements.md)
- [Нефункциональные требования](../2-requirements/non-functional-requirements.md)
- [Use cases](../2-requirements/use-cases.md)
- [User stories](../2-requirements/user-stories.md)
- [Design brief](../3-design-brief/README.md)
- [OpenAPI registry](../api/redocly.yaml)

## Границы решения

- Клиент записывается только на себя.
- По умолчанию показываются слоты на ближайшие 7 дней.
- Доступны только свободные слоты.
- Запись и отмена определяются сервером, клиент не является источником истины.
- Онлайн-оплата и лояльность не входят в первую версию.

## Состав документации

### Сквозные документы

- [feature-list.md](feature-list.md)
- [data-model.md](data-model.md)
- [api-sequence.md](api-sequence.md)

### Экраны и шторки

- [SCR-001-registration.md](SCR-001-registration.md)
- [SCR-002-slot-list.md](SCR-002-slot-list.md)
- [BS-001-filters.md](BS-001-filters.md)
- [SCR-003-slot-card.md](SCR-003-slot-card.md)
- [SCR-004-booking.md](SCR-004-booking.md)
- [BS-002-booking-success.md](BS-002-booking-success.md)
- [SCR-005-my-bookings.md](SCR-005-my-bookings.md)
- [SCR-006-booking-details.md](SCR-006-booking-details.md)
- [BS-003-cancel-confirm.md](BS-003-cancel-confirm.md)
- [BS-004-route-map.md](BS-004-route-map.md)
- [SCR-007-profile.md](SCR-007-profile.md)

### Переиспользуемые логики

- [LOGIC-001-slot-list-query.md](LOGIC-001-slot-list-query.md)
- [LOGIC-002-booking-total.md](LOGIC-002-booking-total.md)
- [LOGIC-003-cancel-availability.md](LOGIC-003-cancel-availability.md)
- [LOGIC-004-push-token-registration.md](LOGIC-004-push-token-registration.md)

## Как читать документы

- Для каждого экрана зафиксированы назначение, навигация, входные данные, API, состояния и критерии приёмки.
- Для логик описаны точки применения, входные данные, поток обработки и связка с API.
- `data-model.md` и `api-sequence.md` используются как сквозная опора для экранных сценариев.
