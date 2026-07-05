# Data Model

Документ описывает ресурсную модель клиентского API для мобильного приложения. Это не схема БД, а контрактные сущности, которые читает и отправляет клиент.

## Основные сущности

### Client

Источник: [api/profile/models.yaml](../api/profile/models.yaml), [api/auth/models.yaml](../api/auth/models.yaml)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор клиента |
| `name` | string, nullable | Имя клиента |
| `phone` | string | Номер телефона в формате E.164 |
| `created_at` | datetime | Дата регистрации |

### Slot

Источник: [api/slots/models.yaml](../api/slots/models.yaml)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор слота |
| `start_at` | datetime | Дата и время начала |
| `route` | Route | Маршрут занятия |
| `instructor` | Instructor | Мастер |
| `total_seats` | int | Общее количество мест |
| `free_seats` | int | Свободные места |
| `free_rental_kits` | int | Свободные комплекты проката |
| `price` | int | Цена занятия в рублях |
| `rental_price` | int | Цена проката в рублях |
| `meeting_point` | string | Точка встречи |
| `meeting_point_lat` | float | Широта точки встречи |
| `meeting_point_lng` | float | Долгота точки встречи |
| `status` | `scheduled` / `cancelled` | Статус слота |

### Route

Источник: [api/instructors/models.yaml](../api/instructors/models.yaml)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор маршрута |
| `name` | string | Название |
| `description` | string, nullable | Описание маршрута |
| `type` | `novice` / `experienced` | Тип маршрута |
| `capacity_cap` | int | Верхняя граница вместимости |
| `duration_min` | int | Длительность занятия |
| `geometry` | array / encoded polyline | Геометрия для карты |

### Instructor

Источник: [api/instructors/models.yaml](../api/instructors/models.yaml)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор мастера |
| `name` | string | Имя мастера |

### Booking

Источник: [api/bookings/models.yaml](../api/bookings/models.yaml)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор брони |
| `slot_id` | UUID | Идентификатор слота |
| `client_id` | UUID | Идентификатор клиента |
| `equipment_mode` | `own` / `rental` | Способ участия |
| `status` | `active` / `cancelled` / `late_cancel` / `club_cancelled` | Статус брони |
| `price_total` | int, readOnly | Итоговая цена |
| `created_at` | datetime | Время создания |
| `cancelled_at` | datetime, nullable | Время отмены |
| `cancellation_reason` | string, nullable, readOnly | Причина отмены от бэкенда |
| `slot` | Slot | Вложенные данные слота |

## Производные значения

| Значение | Правило |
|----------|---------|
| `Booking` как «прошедшая» запись | Вычисляется по `slot.start_at` в прошлом, отдельным статусом не хранится |
| Доступность отмены | Доступна только для `active` и только если до `slot.start_at` остается более 1 часа |
| Экранная стоимость на записи | Предварительно вычисляется на клиенте, финальная стоимость подтверждается API |

## Важные инварианты

- Клиент не создает и не редактирует `Slot`, `Route` или `Instructor`.
- `price_total` приходит от сервера и используется как источник истины для экрана успеха и деталей брони.
- Слот отменён мастерской означает, что повторная запись на него недоступна.
- Наличие свободных мест проверяется сервером при бронировании.
