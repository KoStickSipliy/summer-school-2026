# API Sequence

Документ показывает ключевые цепочки взаимодействия клиентского приложения с backend API. Сервер считается источником истины для доступности слотов, статусов броней и итоговой стоимости.

## Общие правила

- Все защищённые запросы идут с `Authorization: Bearer <token>`.
- Для `POST /bookings` обязателен `Idempotency-Key`.
- Клиент не пересчитывает статус слота или брони самостоятельно.
- Ошибки `400`, `401`, `409`, `410`, `422` и `5xx` должны приводить к понятному UI-сценарию повторной попытки или отказа.

## Сценарий 1: Авторизация по телефону

Связанные экраны: [SCR-001-registration.md](SCR-001-registration.md), [SCR-002-slot-list.md](SCR-002-slot-list.md)

```mermaid
sequenceDiagram
    actor User as Клиент
    participant App as App
    participant API as API (auth)

    User->>App: Вводит номер телефона
    App->>API: POST /auth/request-code
    API-->>App: 200 {ttl_seconds, resend_after_seconds}
    User->>App: Вводит OTP-код и имя при первичной регистрации
    App->>API: POST /auth/verify-code
    API-->>App: 200 {tokens, client, is_new}
    App-->>User: Переход к списку слотов
```

Ключевые операции: `requestAuthCode`, `verifyAuthCode`.

## Сценарий 2: Просмотр слотов и фильтров

Связанные экраны: [SCR-002-slot-list.md](SCR-002-slot-list.md), [BS-001-filters.md](BS-001-filters.md), [SCR-003-slot-card.md](SCR-003-slot-card.md)

```mermaid
sequenceDiagram
    actor User as Клиент
    participant App as App
    participant API as API (slots)
    participant Ref as API (instructors)

    App->>API: GET /slots?date_from=now&date_to=now+7d
    API-->>App: 200 SlotListResponse
    App->>Ref: GET /instructors
    Ref-->>App: 200 InstructorListResponse
    User->>App: Меняет фильтры
    App->>API: GET /slots with filters
    API-->>App: 200 SlotListResponse or empty items
```

Ключевые операции: `listSlots`, `listInstructors`, `getSlot`.

## Сценарий 3: Оформление записи

Связанные экраны: [SCR-003-slot-card.md](SCR-003-slot-card.md), [SCR-004-booking.md](SCR-004-booking.md), [BS-002-booking-success.md](BS-002-booking-success.md)

```mermaid
sequenceDiagram
    actor User as Клиент
    participant App as App
    participant API as API (bookings)

    User->>App: Выбирает equipment_mode
    App->>App: Показывает предварительную стоимость
    User->>App: Подтверждает запись
    App->>API: POST /bookings + Idempotency-Key
    API-->>App: 201 Booking + is_first_booking
    App-->>User: Экран успеха записи
```

Ключевые операции: `createBooking`.

## Сценарий 4: Отмена записи

Связанные экраны: [SCR-006-booking-details.md](SCR-006-booking-details.md), [BS-003-cancel-confirm.md](BS-003-cancel-confirm.md)

```mermaid
sequenceDiagram
    actor User as Клиент
    participant App as App
    participant API as API (bookings)

    User->>App: Нажимает отмену
    App->>App: Проверяет доступность отмены по времени и статусу
    App->>API: POST /bookings/{bookingId}/cancel
    API-->>App: 200 Booking with updated status
    App-->>User: Обновлённые детали и список бронирований
```

Ключевые операции: `getBooking`, `cancelBooking`, `listBookings`.

## Сценарий 5: Профиль и push-токены

Связанные экраны: [SCR-007-profile.md](SCR-007-profile.md), [LOGIC-004-push-token-registration.md](LOGIC-004-push-token-registration.md)

```mermaid
sequenceDiagram
    actor User as Клиент
    participant App as App
    participant API as API (profile/auth)

    App->>API: GET /profile
    API-->>App: 200 Client
    App->>API: PATCH /profile
    API-->>App: 200 Client
    App->>API: POST /auth/push-tokens
    API-->>App: 204 No Content
```

Ключевые операции: `getProfile`, `updateProfile`, `registerPushToken`, `logout`.
