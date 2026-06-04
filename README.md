![Platform](https://img.shields.io/badge/platform-Paper%20%7C%20Spigot%20%7C%20Folia-green.svg)
![Java](https://img.shields.io/badge/java-21%2B-orange.svg)
![Version minecraft](https://img.shields.io/badge/Minecraft-1.21--26.1-red.svg)
[![Support me](https://img.shields.io/badge/Support%20me-Ko--fi-ff5f5f?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/erotoro)

# StaffLens

StaffLens is a staff-action **audit and oversight** plugin for Paper / Spigot / Folia (Minecraft 1.21 – 26.1).

It records moderation and administration actions, stores them in SQLite or MySQL, and gives fast in-game access through commands. Both built-in commands and external plugins are supported via integrations. Beyond passive logging, StaffLens **actively watches** for suspicious behaviour and **protects the log from tampering**.

> 🇷🇺 Русская версия — [ниже](#stafflens-ru).

## Features

- Logs staff actions to a database (SQLite / MySQL).
- Context for every action: world, coordinates, executor IP and server name (for networks/proxies).
- Views by executor, by target, free-text search, and actions for the current day.
- **Query filters**: `action:`, `time:`, `target:`, `staff:`, `flagged`.
- **Per-staff summary**: `/sl stats`.
- **Anomaly detection**: self-granting OP/permissions/items, mass punishments, repeated inventory checks, creative-then-give. Suspicious entries are flagged and alerted.
- **Tamper-evident log**: every entry is linked with a SHA-256 hash chain; `/sl verify` detects rows edited or deleted directly in the database.
- **Discord webhook**: notifications for critical actions and anomalies.
- **Configurable command mapping**: map custom commands → actions without code.
- Export to a text file (with coordinates, server and anomaly flag).
- In-game notifications for critical actions.
- Tab completion for all subcommands, players and filters.
- Periodic cleanup of old logs.
- Reload configuration without a restart.

## Version support

The plugin compiles against the Paper 1.21 API and only uses stable APIs (Bukkit / Paper / Adventure / Folia schedulers), so the same JAR runs on servers **from 1.21 to 26.1**. `api-version` stays `1.21`, which every version up to 26.x recognises.

## Tracked actions

ban/unban, mute/unmute, kick, warn, grant/remove OP, add/remove permission and group, teleports, fly, god, vanish, disguise/undisguise, socialspy, inventory and ender-chest views, item giving, inventory clearing, heal, feed, repair, jail/unjail, nick, sudo, broadcast, lookup commands (`seen`, `history`), gamemode change, and more. See `ActionType` for the full list.

## Integrations

- Vanilla (command parser + config mapping)
- LiteBans
- BanManager
- AdvancedBan
- Essentials
- CMI
- LuckPerms (correct executor attribution via the action log)
- LibsDisguises (disguise / undisguise)
- SuperVanish / PremiumVanish (vanish — shared API)

Each integration can be toggled in the config.

## Requirements

- Java 21
- Paper / Spigot / Folia 1.21.x – 26.1

## Installation

1. Download the latest StaffLens JAR from the [Releases](https://github.com/Erotoro/StaffLens/releases) page.
2. Drop the file into the `plugins` folder.
3. Start the server.
4. Configure `plugins/StaffLens/config.yml`.
5. Run `/sl reload` if needed.

## Commands

Main command: `/stafflens` (`/sl`).

- `/sl log <player> [page] [action:..] [time:..] [flagged]` — actions performed by a staff member
- `/sl who <target> [page] [action:..] [time:..]` — actions against a target
- `/sl search <text> [filters]` — search the journal
- `/sl today [filters]` — actions for the current day
- `/sl stats <player> [time:..]` — summary of a staff member's actions
- `/sl export <player>` — export history to a file
- `/sl verify` — check log integrity (hash chain)
- `/sl reload` — reload config and integrations

### Filters

Filters can be combined in `log` / `who` / `search` / `today`:

- `action:BAN` — a specific action
- `time:7d` — within the last 7 days (`s`, `m`, `h`, `d`, `w`)
- `target:Steve` — by target
- `staff:Alex` — by executor
- `flagged` — only entries flagged as anomalous

Example: `/sl log Alex action:op_give time:30d`

## Permissions

- `stafflens.use` — view logs and statistics
- `stafflens.export` — export
- `stafflens.admin` — `/sl reload` and `/sl verify`
- `stafflens.notify` — notifications for critical actions
- `stafflens.alerts` — alerts for detected anomalies

All permissions default to `op`.

## Database

SQLite (default) and MySQL are supported. The table, indexes and new columns are created and migrated automatically on startup — upgrading from an older version is safe.

For MySQL, set `database.type: mysql` and the `database.mysql.*` values in `config.yml`.

## Configuration

Main `config.yml` sections:

```yml
locale: en
server-name: ""          # server name written into entries (for networks)

database:
  type: sqlite

log:
  retention-days: 90
  page-size: 15
  cleanup-interval-hours: 6   # periodic cleanup (0 = on startup only)

tracking:
  mode: permission            # permission | all
  permission: stafflens.admin
  command-tracking:           # custom commands -> actions
    # freeze: { action: JAIL, target-arg: 1 }

notify:
  critical-actions: [OP_GIVE, PERMISSION_ADD, GROUP_ADD]

anomaly:
  enabled: true
  self-target-actions: [OP_GIVE, PERMISSION_ADD, GROUP_ADD, GIVE_ITEM]
  mass-punish: { enabled: true, threshold: 5, window-seconds: 60 }
  repeated-invsee: { enabled: true, threshold: 4, window-seconds: 120 }
  creative-then-give: true

discord:
  enabled: false
  webhook-url: ""
  username: StaffLens
  send-critical: true
  send-anomalies: true

integrations:
  Vanilla: true
  LiteBans: true
  LibsDisguises: true
  SuperVanish: true   # also covers PremiumVanish
  # ...
```

## Log integrity

Every entry stores `prev_hash` and `entry_hash` (SHA-256 of the content plus the previous entry's hash). `/sl verify`:

- confirms the whole chain is intact, or
- reports the id of the first broken entry if rows were edited or deleted directly in the database.

Entries created before the upgrade are treated as legacy (no hash) and do not break verification.

## Metrics

StaffLens uses [bStats](https://bstats.org) to collect anonymous usage statistics. You can opt out per-plugin with `metrics: false` in `config.yml`, or globally in `plugins/bStats/config.yml`.

## Localization

Locale files live in the `locale` folder: `en.yml`, `ru.yml`, `ua.yml`. The language is chosen via the `locale` option.

## Log storage

- On startup, and then on a timer, the plugin deletes entries older than `retention-days`.
- Indexes are created for SQLite and MySQL to speed up queries.
- Writes go through a single consumer thread (deterministic order and a correct hash chain); on shutdown the plugin waits for the queue to drain.

---

<a name="stafflens-ru"></a>

# StaffLens (RU)

StaffLens — плагин для аудита и **надзора** за действиями стаффа на Paper / Spigot / Folia (Minecraft 1.21 – 26.1).

Плагин записывает действия модерации и администрирования, сохраняет их в SQLite или MySQL и даёт быстрый просмотр через команды. Поддерживаются как встроенные команды, так и внешние плагины-интеграции. Помимо пассивного журналирования StaffLens **активно следит** за подозрительным поведением и **защищает журнал от подделки**.

## Возможности

- Логирование действий стаффа в базу данных (SQLite / MySQL).
- Контекст каждого действия: мир, координаты, IP исполнителя и имя сервера (для сетей/прокси).
- Просмотр по исполнителю, по цели, поиск, действия за день.
- **Фильтры в запросах**: `action:`, `time:`, `target:`, `staff:`, `flagged`.
- **Сводная статистика** по сотруднику: `/sl stats`.
- **Детектор аномалий**: само-выдача OP/прав/предметов, массовые наказания, повторные просмотры инвентаря, creative→give. Подозрительные записи помечаются и рассылаются как алерты.
- **Tamper-evident журнал**: записи связаны SHA-256 хеш-цепочкой; `/sl verify` обнаруживает изменение или удаление записей напрямую в БД.
- **Discord-webhook**: уведомления о критичных действиях и аномалиях.
- **Конфигурируемый маппинг команд**: свои команды → действия без правки кода.
- Экспорт истории в текстовый файл (с координатами, сервером и пометкой аномалии).
- Уведомления о критических действиях в игре.
- Автодополнение (TabComplete) для всех подкоманд, игроков и фильтров.
- Периодическая очистка устаревших логов.
- Перезагрузка конфигурации без перезапуска сервера.

## Поддержка версий

Плагин компилируется против Paper API 1.21 и использует только стабильные API (Bukkit / Paper / Adventure / Folia-планировщики), поэтому один и тот же JAR работает на серверах **от 1.21 до 26.1**. `api-version` остаётся `1.21` — это значение распознают все версии вплоть до 26.x.

## Поддерживаемые действия

бан/разбан, мут/размут, кик, предупреждение, выдача/снятие OP, выдача/снятие permission и group, телепорты, fly, god, vanish, маскировка (disguise/undisguise), socialspy, просмотр инвентаря и эндер-сундука, выдача предметов, очистка инвентаря, heal, feed, repair, jail/unjail, nick, sudo, broadcast, lookup-команды (`seen`, `history`), смена gamemode и др. Полный список — в `ActionType`.

## Интеграции

- Vanilla (парсер команд + конфиг-маппинг)
- LiteBans
- BanManager
- AdvancedBan
- Essentials
- CMI
- LuckPerms (корректное определение исполнителя через action log)
- LibsDisguises (маскировка / снятие маскировки)
- SuperVanish / PremiumVanish (vanish — общий API)

Каждую интеграцию можно включать и выключать в конфиге.

## Требования

- Java 21
- Paper / Spigot / Folia 1.21.x – 26.1

## Установка

1. Скачайте последнюю версию StaffLens JAR со страницы [Releases](https://github.com/Erotoro/StaffLens/releases).
2. Поместите файл в папку `plugins`.
3. Запустите сервер.
4. Настройте `plugins/StaffLens/config.yml`.
5. При необходимости выполните `/sl reload`.

## Команды

Основная команда: `/stafflens` (`/sl`).

- `/sl log <игрок> [страница] [action:..] [time:..] [flagged]` — история действий сотрудника
- `/sl who <цель> [страница] [action:..] [time:..]` — действия против цели
- `/sl search <текст> [фильтры]` — поиск по журналу
- `/sl today [фильтры]` — действия за текущий день
- `/sl stats <игрок> [time:..]` — сводка действий сотрудника
- `/sl export <игрок>` — экспорт истории в файл
- `/sl verify` — проверка целостности журнала (хеш-цепочка)
- `/sl reload` — перезагрузка конфигурации и интеграций

### Фильтры

Фильтры можно комбинировать в `log` / `who` / `search` / `today`:

- `action:BAN` — конкретное действие
- `time:7d` — за последние 7 дней (`s`, `m`, `h`, `d`, `w`)
- `target:Steve` — по цели
- `staff:Alex` — по исполнителю
- `flagged` — только помеченные аномалиями

Пример: `/sl log Alex action:op_give time:30d`

## Права

- `stafflens.use` — просмотр журналов и статистики
- `stafflens.export` — экспорт
- `stafflens.admin` — `/sl reload` и `/sl verify`
- `stafflens.notify` — уведомления о критических действиях
- `stafflens.alerts` — алерты о выявленных аномалиях

По умолчанию все права выданы `op`.

## База данных

Поддерживаются `sqlite` (по умолчанию) и `mysql`. Таблица, индексы и новые колонки создаются и мигрируются автоматически при запуске — обновление со старой версии безопасно.

Для MySQL укажите `database.type: mysql` и параметры `database.mysql.*` в `config.yml`.

## Конфигурация

Основные секции `config.yml`:

```yml
locale: en
server-name: ""          # имя сервера в записях (для сетей)

database:
  type: sqlite

log:
  retention-days: 90
  page-size: 15
  cleanup-interval-hours: 6   # периодическая очистка (0 = только при старте)

tracking:
  mode: permission            # permission | all
  permission: stafflens.admin
  command-tracking:           # свои команды -> действия
    # freeze: { action: JAIL, target-arg: 1 }

notify:
  critical-actions: [OP_GIVE, PERMISSION_ADD, GROUP_ADD]

anomaly:
  enabled: true
  self-target-actions: [OP_GIVE, PERMISSION_ADD, GROUP_ADD, GIVE_ITEM]
  mass-punish: { enabled: true, threshold: 5, window-seconds: 60 }
  repeated-invsee: { enabled: true, threshold: 4, window-seconds: 120 }
  creative-then-give: true

discord:
  enabled: false
  webhook-url: ""
  username: StaffLens
  send-critical: true
  send-anomalies: true

integrations:
  Vanilla: true
  LiteBans: true
  LibsDisguises: true
  SuperVanish: true   # покрывает и PremiumVanish
  # ...
```

## Целостность журнала

Каждая запись хранит `prev_hash` и `entry_hash` (SHA-256 от содержимого и хеша предыдущей записи). Команда `/sl verify`:

- подтверждает целостность всей цепочки, либо
- сообщает id первой повреждённой записи, если строки изменили или удалили напрямую в базе.

Записи, созданные до обновления, считаются «legacy» (без хеша) и не ломают проверку.

## Метрики

StaffLens использует [bStats](https://bstats.org) для сбора анонимной статистики. Отключить можно для плагина — `metrics: false` в `config.yml`, или глобально в `plugins/bStats/config.yml`.

## Локализация

Файлы в папке `locale`: `en.yml`, `ru.yml`, `ua.yml`. Язык выбирается параметром `locale`.

## Хранение логов

- При запуске и далее по таймеру плагин удаляет записи старше `retention-days`.
- Для SQLite и MySQL создаются индексы для ускорения запросов.
- Запись ведётся одним потоком-консьюмером (детерминированный порядок и корректная хеш-цепочка); при выключении плагин дожидается опустошения очереди.
