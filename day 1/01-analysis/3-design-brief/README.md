# Реестр экранов и шторок

**Этап:** 3. Design brief · **Статус:** Черновик · **Версия:** 0.1 · **Дата:** 2026-07-04

> Реестр и набор брифов по экранам клиентского мобильного приложения. Каждый объект описан в отдельном файле.

**Источники:**
[Business requirements](../2-requirements/business-requirements.md) ·
[Functional requirements](../2-requirements/functional-requirements.md) ·
[Non-functional requirements](../2-requirements/non-functional-requirements.md) ·
[Use cases](../2-requirements/use-cases.md) ·
[User stories](../2-requirements/user-stories.md) ·
[Feature list](../5-mobile-app-spec/feature-list.md)

## Реестр

| ID | Название | Тип | Зона | Приоритет | Файл |
|----|----------|-----|------|-----------|------|
| SCR-001 | Регистрация / Вход | Экран | НЗ | Critical | [SCR-001-registration.md](SCR-001-registration.md) |
| SCR-002 | Список слотов | Экран | АЗ | Critical | [SCR-002-slot-list.md](SCR-002-slot-list.md) |
| BS-001 | Фильтры | Bottom Sheet | АЗ | High | [BS-001-filters.md](BS-001-filters.md) |
| SCR-003 | Карточка слота | Экран | АЗ | Critical | [SCR-003-slot-card.md](SCR-003-slot-card.md) |
| SCR-004 | Оформление записи | Экран | АЗ | Critical | [SCR-004-booking.md](SCR-004-booking.md) |
| BS-002 | Подтверждение записи | Экран | АЗ | High | [BS-002-booking-success.md](BS-002-booking-success.md) |
| SCR-005 | Мои бронирования | Экран | АЗ | Critical | [SCR-005-my-bookings.md](SCR-005-my-bookings.md) |
| SCR-006 | Детали брони + отмена | Экран | АЗ | Critical | [SCR-006-booking-details.md](SCR-006-booking-details.md) |
| BS-003 | Подтверждение отмены | Bottom Sheet | АЗ | High | [BS-003-cancel-confirm.md](BS-003-cancel-confirm.md) |
| BS-004 | Карта маршрута | Bottom Sheet | АЗ | Medium | [BS-004-route-map.md](BS-004-route-map.md) |
| SCR-007 | Профиль клиента | Экран | АЗ | Medium | [SCR-007-profile.md](SCR-007-profile.md) |

## Состав брифов

Все файлы построены по одному каркасу:
- назначение и контекст;
- навигация;
- структура и иерархия;
- состав контента;
- состояния экрана;
- поведение, фильтры и валидации;
- критерии приемки.

Для экранов со списками и формами отдельно зафиксированы состояния загрузки, пустых результатов и ошибки, а также правила фильтрации и недоступные действия.
