# StaffLens

StaffLens - плагин для аудита действий стаффа на Paper 1.21.

Плагин записывает действия модерации и администрирования, сохраняет их в SQLite или MySQL и даёт быстрый просмотр через команды. Поддерживаются как встроенные команды, так и внешние плагины-интеграции.

## Возможности

- Логирование действий стаффа в базу данных.
- Просмотр истории по исполнителю.
- Просмотр действий по цели.
- Поиск по журналу.
- Просмотр действий за текущий день.
- Экспорт истории в текстовый файл.
- Уведомления о критических действиях в игре.
- Перезагрузка конфигурации без перезапуска сервера.

## Поддерживаемые действия

Плагин умеет фиксировать:

- бан, разбан, мут, размут, кик, предупреждение
- выдачу и снятие OP
- выдачу и снятие permission/group
- телепорты и служебные перемещения
- fly, god, vanish, socialspy
- просмотр инвентаря и эндер-сундука
- выдачу предметов, очистку инвентаря, heal, feed, repair
- jail, unjail, nick, sudo, broadcast
- lookup-команды вроде `seen` и `history`
- смену gamemode

Точный список типов действий задаётся в коде через `ActionType`.

## Поддерживаемые интеграции

- Vanilla
- LiteBans
- BanManager
- AdvancedBan
- Essentials
- CMI
- LuckPerms

Каждую интеграцию можно включать и выключать в конфиге.

## Требования

- Java 21
- Paper 1.21.x

## Установка

1. Соберите JAR плагина.
2. Поместите файл в папку `plugins`.
3. Запустите сервер.
4. Настройте `plugins/StaffLens/config.yml`.
5. При необходимости выполните `/sl reload`.

## База данных

Поддерживаются:

- `sqlite`
- `mysql`

По умолчанию используется SQLite.

### Настройка SQLite

Ничего дополнительно настраивать не нужно. База будет создана автоматически в папке плагина.

### Настройка MySQL

Укажите в `config.yml`:

- `database.type: mysql`
- `database.mysql.host`
- `database.mysql.port`
- `database.mysql.database`
- `database.mysql.username`
- `database.mysql.password`

Таблица и индексы создаются автоматически при запуске.

## Команды

Основная команда:

- `/stafflens`
- `/sl`

Подкоманды:

- `/sl log <player> [page]` - история действий конкретного сотрудника
- `/sl who <player> [page]` - действия против указанной цели
- `/sl search <text> [page]` - поиск по журналу
- `/sl today [page]` - действия за текущий день
- `/sl export <player>` - экспорт истории в файл
- `/sl reload` - перезагрузка конфигурации и интеграций

## Права

- `stafflens.use` - доступ к просмотру журналов
- `stafflens.export` - доступ к экспорту
- `stafflens.admin` - доступ к `/sl reload`
- `stafflens.notify` - получение уведомлений о критических действиях

По умолчанию все права выданы `op`.

## Конфигурация

Файл: `plugins/StaffLens/config.yml`

```yml
locale: en

database:
  type: sqlite
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: ""

log:
  retention-days: 90
  page-size: 15

notify:
  critical-actions:
    - OP_GIVE
    - PERMISSION_ADD
    - GROUP_ADD

integrations:
  LiteBans: true
  BanManager: true
  AdvancedBan: true
  Essentials: true
  CMI: true
  LuckPerms: true
  Vanilla: true
```

### Параметры

- `locale` - язык сообщений
- `database.type` - тип базы: `sqlite` или `mysql`
- `log.retention-days` - срок хранения логов в днях
- `log.page-size` - размер страницы в командах просмотра
- `notify.critical-actions` - список действий, при которых отправляются уведомления
- `integrations.*` - включение отдельных интеграций

## Локализация

Поддерживаются файлы локализации в папке `locale`:

- `en.yml`
- `ru.yml`
- `ua.yml`

Язык выбирается через параметр `locale` в конфиге.

## Экспорт

Команда `/sl export <player>` создаёт текстовый файл в папке:

`plugins/StaffLens/exports`

Файл содержит:

- время генерации
- список действий
- цель
- причину
- дополнительные детали

## Уведомления

Если действие входит в `notify.critical-actions`, игроки с правом `stafflens.notify` получают уведомление в игре.

## Хранение логов

- При запуске плагин удаляет записи старше `retention-days`.
- Для SQLite и MySQL создаются индексы для ускорения основных запросов.
- При перезагрузке плагин дожидается завершения очереди записи логов перед закрытием базы.
