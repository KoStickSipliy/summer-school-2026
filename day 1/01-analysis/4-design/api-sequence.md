# Sequence-диаграмма API-взаимодействия

**Источники:**
[Use cases](../2-requirements/use-cases.md) ·
[Functional requirements](../2-requirements/functional-requirements.md) ·
[Business requirements](../2-requirements/business-requirements.md) ·
[SCR-004](../3-design-brief/SCR-004-booking.md) ·
[SCR-006](../3-design-brief/SCR-006-booking-details.md) ·
[data-model](data-model.md)

## Сквозные правила взаимодействия

- Все вызовы выполняются с `Authorization: Bearer <token>`.
- Сервер является источником истины по времени, доступности мест и состоянию брони.
- Запись и отмена выполняются атомарно.
- При ошибке сети или сервера пользователь должен получить понятное сообщение и возможность повторить действие.

## Сценарий 1: Создание брони

Поток: [SCR-004 «Оформление записи»](../3-design-brief/SCR-004-booking.md) → `POST /bookings` → [BS-002 «Подтверждение записи»](../3-design-brief/BS-002-booking-success.md).

Клиент отправляет `slot_id`, `seats_count` и `rental_count`. Итоговую стоимость `price_total` рассчитывает сервер, а клиент её только отображает.

```mermaid
sequenceDiagram
    actor User as Клиент
    participant App as Приложение
    participant API as API (bookings)

    Note over App: SCR-004: выбран слот, места и вариант участия
    User->>App: Тап «Записаться»
    App->>App: Формирует Idempotency-Key

    App->>API: POST /bookings<br/>{slot_id, seats_count, rental_count}<br/>Authorization: Bearer, Idempotency-Key
    Note over API: Проверка свободных мест, досок и актуальности слота

    alt Успех
        API-->>App: 201 Booking {id, status: active, price_total, created_at}
        App-->>User: Подтверждение записи и переход к успеху
    else Нет свободных мест
        API-->>App: 409 {code: slot_full}
        App-->>User: Сообщение о нехватке мест
    else Слот отменён мастерской
        API-->>App: 410 {code: slot_cancelled}
        App-->>User: Сообщение, что запись недоступна
    else Невалидные данные
        API-->>App: 400 / 422
        App-->>User: Подсказка по ошибке ввода
    else Токен истёк
        API-->>App: 401 Unauthorized
        App-->>User: Переход на экран входа
    else Сеть или серверная ошибка
        API-->>App: Ошибка / нет ответа
        App-->>User: Error state + «Повторить»
    end
```

| Шаг | Что происходит | Источник |
| :-- | :-- | :-- |
| Запрос | `POST /bookings` с `Idempotency-Key` | bookings API |
| Проверка | Сервер атомарно проверяет доступность и фиксирует цену | НФТ + домен |
| `201` | Возвращается `Booking` со статусом `active` | модель данных |
| `409` | Нет мест или конфликт состояния слота | use cases |
| `410` | Слот отменён мастерской | бизнес-правила |

## Сценарий 2: Отмена брони

Поток: [SCR-006 «Детали брони + отмена»](../3-design-brief/SCR-006-booking-details.md) → [BS-003 «Подтверждение отмены»](../3-design-brief/BS-003-cancel-confirm.md) → `POST /bookings/{bookingId}/cancel`.

Отмена доступна только до старта занятия. Сервер определяет итоговый тип отмены по времени до начала: ранняя или поздняя.

```mermaid
sequenceDiagram
    actor User as Клиент
    participant App as Приложение
    participant API as API (bookings)

    Note over App: SCR-006: бронь активна и занятие ещё не началось
    User->>App: Тап «Отменить запись»
    App-->>User: BS-003 «Подтверждение отмены»
    User->>App: Подтверждает отмену

    App->>API: POST /bookings/{bookingId}/cancel<br/>Authorization: Bearer
    Note over API: Сервер сверяет текущее время со start_at и выбирает тип отмены

    alt Отмена в допустимый срок
        API-->>App: 200 Booking {status: cancelled, cancelled_at}
        App-->>User: Бронь отменена, список и детали обновлены
    else Отмена позже допустимого срока
        API-->>App: 200 Booking {status: late_cancel, cancelled_at}
        App-->>User: Сообщение, что отмена прошла без освобождения места
    else Уже началось занятие
        API-->>App: 422 {code: slot_started}
        App-->>User: Сообщение, что отмена недоступна
    else Бронь уже отменена
        API-->>App: 409 {code: already_cancelled}
        App-->>User: Статус актуализируется без повторного действия
    else Токен истёк
        API-->>App: 401 Unauthorized
        App-->>User: Переход на экран входа
    else Сеть или серверная ошибка
        API-->>App: Ошибка / нет ответа
        App-->>User: Ошибка на шторке подтверждения с возможностью повтора
    end
```

| Шаг | Что происходит | Источник |
| :-- | :-- | :-- |
| Запрос | `POST /bookings/{bookingId}/cancel` | bookings API |
| Решение | Сервер определяет `cancelled` или `late_cancel` | домен + use cases |
| `200` | Бронь получает новый статус и `cancelled_at` | модель данных |
| `422` | Отмена недоступна после старта | UC-2 |
| `409` | Повторная отмена | UC-2 |

> Полная модель состояний брони и слота — в [data-model.md](data-model.md).