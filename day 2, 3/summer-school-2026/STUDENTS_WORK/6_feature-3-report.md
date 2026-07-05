# Отчёт по реализации фичи 3 (BS-002 «Экран успешной записи»)

Дата: 2026-07-05

## Что реализовано

Реализована фича 3 из `features.md` в клиентском приложении (KMP shared + platform adapters):

- Доработан сценарий успеха после `createBooking` на экране оформления записи (`SCR-004 -> BS-002`).
- На экране успеха (`BS-002`) сводка теперь использует серверный итог `booking.priceTotal` как основной источник суммы.
- Добавлена корректная разбивка досок в сводке:
  - «Своих досок = seats_count - rental_count»
  - строка про прокатные доски показывается только когда `rental_count > 0`.
- Добавлена поддержка `reminder_hours` в клиентской модели брони (DTO -> domain mapping).
- Реализована one-time логика запроса push-разрешения после первой успешной брони:
  - учитывается `is_first_booking`;
  - добавлено локально-персистентное состояние «запрос уже показывался»;
  - повторный показ блокируется между сессиями;
  - добавлен платформенный boundary (`requester` + `state storage`) для Android/iOS/Web.
- На BS-002 добавлена подводка «Напомним за ... до старта» (если `reminder_hours` заполнен).
- Исправлено поведение системной кнопки «назад» при открытом BS-002:
  - теперь эквивалентно действию «Готово» (закрытие success-состояния + возврат к списку прогулок).

## Изменённые файлы

- `client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingFormScreen.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingFormStore.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/domain/model/BookingModels.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/booking/data/BookingDto.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/booking/data/BookingMappers.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/VolnaApp.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/di/AppModule.kt`
- `client/androidApp/src/main/kotlin/com/volna/app/android/MainActivity.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/notifications/PushPermissionContracts.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/notifications/DefaultPushPermissionPromptCoordinator.kt`
- `client/shared/src/androidMain/kotlin/com/volna/app/notifications/PlatformPushPermissionRequester.android.kt`
- `client/shared/src/androidMain/kotlin/com/volna/app/notifications/PlatformPushPermissionPromptStateStorage.android.kt`
- `client/shared/src/iosMain/kotlin/com/volna/app/notifications/PlatformPushPermissionRequester.ios.kt`
- `client/shared/src/iosMain/kotlin/com/volna/app/notifications/PlatformPushPermissionPromptStateStorage.ios.kt`
- `client/shared/src/wasmJsMain/kotlin/com/volna/app/notifications/PlatformPushPermissionRequester.wasm.kt`
- `client/shared/src/wasmJsMain/kotlin/com/volna/app/notifications/PlatformPushPermissionPromptStateStorage.wasm.kt`
- `client/shared/src/commonTest/kotlin/com/volna/app/booking/presentation/BookingFormStoreTest.kt`
- `client/shared/src/commonTest/kotlin/com/volna/app/notifications/DefaultPushPermissionPromptCoordinatorTest.kt`

## Тестирование

Команда:

```bash
cd client
./gradlew :shared:wasmJsTest --no-daemon
```

Результат: **BUILD SUCCESSFUL**.

Проверено:

- новый `BookingFormStore` сценарий успеха (сохранение/очистка success-состояния и push-подсказки);
- новая координация one-time push-запроса (`DefaultPushPermissionPromptCoordinator`);
- существующие тесты shared-модуля не сломаны.


## Примечания

- Бизнес-ограничение «запрашивать push только один раз после первой успешной записи» соблюдено на уровне персистентного флага.
