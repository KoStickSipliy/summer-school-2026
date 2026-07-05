# Локальный запуск BE, DB и клиента

Краткий guide для разработки Volna: база и backend запускаются из `backend/`, клиент - из `client/`.

## 1. Требования

- Docker Compose.
- Go 1.23+.
- JDK 17+.
- Android Studio или IntelliJ IDEA для Android/Web.
- Xcode для iOS.
- Node.js/npm нужны только для OpenAPI-команд из `01-analysis/api`.

## 2. DB: PostgreSQL

1. Перейдите в backend:

```bash
cd backend
```

2. Поднимите PostgreSQL:

```bash
docker compose --profile db up -d db
```

3. Примените миграции и dev-seed:

```bash
    make migrate

    или

    docker compose --profile migrations run --rm migrate
```

4. Если нужны готовые состояния для проверки клиентских экранов, примените mock-seed:

```bash
docker compose -f compose.yaml exec -T db psql -U volna -d volna < seed/mock_client_app_states.sql
```

Подключение по умолчанию: `postgres://volna:volna@localhost:5432/volna?sslmode=disable`.

## 3. BE: запуск API

1. Из `backend/` запустите API локально:

```bash
make run
```

**Если ошибки скачивания Go-модулей (proxy.golang.org не доступен), используйте Docker:**

```bash
docker compose --profile db up -d db
```

2. Проверьте, что сервис жив:

```bash
curl http://127.0.0.1:8080/healthz
curl http://127.0.0.1:8080/readyz
```

**Для Docker (API будет доступен на http://127.0.0.1:8080 после сборки):**

```bash
# Сервис готов, когда "api server started" появится в логах
```

Основные переменные можно положить в `backend/.env`, взяв шаблон из `backend/.env.example`. Важные значения: `HTTP_ADDR`, `DATABASE_URL`, `TEST_DATABASE_URL`, `BASE_URL`.

## 4. BE: полезные команды

```bash
make test          # Go-тесты
make lint          # go vet
make generate      # генерация OpenAPI transport-кода
make lint-api      # lint OpenAPI-контрактов
```

Для k6:

```bash
make k6-seed
BASE_URL=http://127.0.0.1:8080 make k6-smoke
```

## 5. Клиент: запуск и проверка

1. Перейдите в клиент:

```bash
cd client
```

2. Быстрая проверка shared-кода:

```bash
./gradlew :shared:compileDebugKotlinAndroid
```

3. Android debug build:

```bash
./gradlew :androidApp:assembleDebug
```

4. Web dev server:

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

5. iOS: откройте `client/iosApp/iosApp.xcodeproj` в Xcode и запустите схему `iosApp`. Shared framework собирается Gradle-задачей Xcode build phase.

## 6. Клиент и локальный BE

- Shared-клиент по умолчанию ходит на `http://localhost:8080`.
- Для Web и iOS simulator локальный backend доступен как `localhost:8080`.
- Для Android emulator используйте `10.0.2.2:8080`, если запросы из эмулятора не доходят до host-машины.
- В Android уже есть debug cleartext-настройка для `localhost`, `127.0.0.1` и `10.0.2.2`.

## 7. Рекомендуемый порядок ежедневного запуска

**Вариант 1: локальный запуск (если у вас установлены Go 1.25.7+, make и доступен proxy.golang.org):**

```bash
cd backend
docker compose --profile db up -d db
make migrate
make run
```

**Вариант 2: через Docker (рекомендуется при сетевых ошибках скачивания модулей):**

```bash
cd backend
docker compose --profile db up -d db
docker compose --profile migrations run --rm migrate
docker compose --profile app up --build
```

В соседнем терминале (оба варианта, для клиента):

```bash
cd client
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Для Android/iOS удобнее запускать host-приложения из IDE после поднятия DB и BE.

## 8. Устранение проблем (Troubleshooting)

### Ошибки скачивания Go-модулей (`wsarecv`, `proxy.golang.org`)

**Причина:** локальная машина не может достучаться до proxy.golang.org (сетевые проблемы, Firewall, VPN).

**Решение 1 (быстрое) — используйте Docker:**

Все команды `make` запустите через Docker Compose с соответствующим профилем:

```bash
# Вместо: make migrate
docker compose --profile migrations run --rm migrate

# Вместо: make run
docker compose --profile app up --build

# Вместо: make test
docker run --rm -v $(pwd):/src -w /src golang:1.25-alpine go test ./...
```

В Windows PowerShell используйте:

```powershell
docker compose --profile migrations run --rm migrate
docker compose --profile app up --build
```

**Решение 2 — если локальный Go необходим, переключите GOPROXY:**

```powershell
go env -w GOPROXY=https://goproxy.io,direct
```

Повторно запустите команду:

```bash
make migrate
make run
```

**Решение 3 — очистить кэш модулей и повторить:**

```bash
go clean -modcache
make migrate
```
