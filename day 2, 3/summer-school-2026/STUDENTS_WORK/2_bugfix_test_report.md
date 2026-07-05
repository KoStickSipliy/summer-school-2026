# Отчет по исправлению багов и тестированию

Дата: 2026-07-05

## Исправленные баги

1. Баг: при бронировании несуществующего слота возвращалась ошибка `slot_started`.
- Исправление: в `backend/internal/storage/postgres/bookings.go` при отсутствии слота (`pgx.ErrNoRows`) теперь возвращается `booking.ErrNotFound`.
- Добавлен тест: `TestCreateBookingReturnsNotFoundWhenSlotDoesNotExist` в `backend/internal/http/handlers/bookings_integration_test.go`.

2. Баг: номер в формате `8XXXXXXXXXX` обрабатывался с потерей последней цифры.
- Исправление: в `client/shared/src/commonMain/kotlin/com/volna/app/core/phone/PhoneInputCore.kt` добавлена корректная обработка префикса `8`:
  - `sanitizePhoneInput("89991234567") -> "9991234567"`
  - `normalizePhoneE164("89991234567") -> "+79991234567"`
- Добавлены тесты в `client/shared/src/commonTest/kotlin/com/volna/app/core/phone/PhoneInputCoreTest.kt`.

## Результаты повторного тестирования

### Backend
Команда:
```bash
go test ./internal/storage/postgres ./internal/http/handlers
```
Результат: **успешно**
- `ok summer-school-2026/backend/internal/storage/postgres`
- `ok summer-school-2026/backend/internal/http/handlers`

### Client
Команда:
```bash
./gradlew :shared:wasmJsTest --no-daemon
```
Результат: **успешно**
- `BUILD SUCCESSFUL`

## Итог
- Оба заявленных бага исправлены.
- Тесты backend и платформенно-независимые client-тесты (через wasmJs target) прошли успешно.