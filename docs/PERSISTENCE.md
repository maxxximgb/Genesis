# Персистентность (snapshot перед rewrite)

> Что переживает рестарт приложения и где это лежит. Документ описывает текущую версию (Room schema v1, три отдельных DataStore-файла). В новой версии решено сносить старую БД и стартовать с чистого листа — миграция со схемы ниже не пишется.

## 1. Карта хранилищ

| Хранилище | Тип | Файл / имя | Назначение |
|---|---|---|---|
| Room | SQLite | `morning_player.db` | Плейлисты + треки + порядок |
| `playbackDataStore` | DataStore Preferences | `playback_state.preferences_pb` | Зеркало текущего состояния плеера |
| `widgetDataStore` | DataStore Preferences | `widget_prefs.preferences_pb` | Per-widget конфиги (плейлист, loop) |
| `uiDataStore` | DataStore Preferences | `ui_prefs.preferences_pb` | UI-настройки приложения |

Три отдельных DataStore-файла создавались поэтапно. В новой версии будет один (`prefs.preferences_pb`) с префиксами ключей.

## 2. Room схема (текущая, v1)

### Таблица `playlists`
| Поле | Тип | Constraint |
|---|---|---|
| `id` | INTEGER | PK, autoGenerate |
| `name` | TEXT | NOT NULL |
| `createdAt` | INTEGER | NOT NULL (epoch millis) |

### Таблица `tracks`
| Поле | Тип | Constraint |
|---|---|---|
| `mediaStoreId` | INTEGER | **PK** (естественный ключ из MediaStore `_ID`) |
| `title` | TEXT | NOT NULL |
| `artist` | TEXT | NULLABLE |
| `album` | TEXT | NULLABLE |
| `durationMs` | INTEGER | NOT NULL |
| `contentUri` | TEXT | NOT NULL (строковое представление `content://...`) |

Используется как локальный кеш метаданных для треков, попавших в плейлисты. Если трек удалён из MediaStore — запись остаётся в `tracks`, но `contentUri` указывает в никуда.

### Таблица `playlist_tracks` (join)
| Поле | Тип | Constraint |
|---|---|---|
| `playlistId` | INTEGER | NOT NULL, часть composite PK |
| `mediaStoreId` | INTEGER | NOT NULL, часть composite PK |
| `position` | INTEGER | NOT NULL |

**Composite PK:** `(playlistId, mediaStoreId)` — обеспечивает дедупликацию.

**Foreign keys:** НЕТ. Каскад при удалении плейлиста сделан в коде (`PlaylistRepository.deletePlaylist`). Это пробел — в новой версии добавляем `ON DELETE CASCADE`.

### Запросы DAO (что наблюдается извне)
- `observePlaylists(): Flow<List<PlaylistEntity>>` — `ORDER BY createdAt DESC`
- `observePlaylist(id): Flow<PlaylistEntity?>`
- `observePlaylistTrackItems(playlistId): Flow<List<PlaylistTrackItem>>` — JOIN tracks/playlist_tracks с `ORDER BY position ASC`
- `getPlaylistName(id): String?` — suspend, single-shot
- Insert / delete / update — `@Insert(REPLACE)` для `tracks`, `@Insert(IGNORE)` для `playlist_tracks`
- `reorderTracks(playlistId, orderedMediaIds: List<Long>)` — `@Transaction`, цикл `UPDATE playlist_tracks SET position = ? WHERE ...` (N запросов на N треков — переделываем в новой версии)

## 3. DataStore: `playbackDataStore` (`playback_state`)

Зеркало текущего состояния плеера. Пишется и сервисом (через listener'ы ExoPlayer), и контроллером (после команд) — в новой версии оставим **одного писателя**.

| Ключ | Тип | Что значит |
|---|---|---|
| `is_playing` | Boolean | Сейчас идёт воспроизведение |
| `title` | String? | Заголовок текущего трека |
| `artist` | String? | Исполнитель |
| `shuffle_enabled` | Boolean | shuffle on/off |
| `repeat_mode` | Int | `Player.REPEAT_MODE_OFF / ALL / ONE` |
| `can_go_previous` | Boolean | Доступна ли prev-кнопка |
| `can_go_next` | Boolean | Доступна ли next-кнопка |
| `playlist_id` | Long? | id плейлиста, чьи треки сейчас в очереди (для матчинга в виджете) |

Остальные параметры (позиция в треке, индекс в очереди, длительность очереди, current `mediaStoreId`) **не сохраняются**. После рестарта приложения очередь пустая.

## 4. DataStore: `widgetDataStore` (`widget_prefs`)

Per-widget записи, ключи строятся через шаблон `{appWidgetId}_<suffix>`.

| Ключ-суффикс | Тип | Что значит |
|---|---|---|
| `playlist_id` | Long | id плейлиста, выбранный при конфигурировании |
| `playlist_name` | String | Имя плейлиста (cached, чтобы виджет рендерился без обращения к Room) |
| `loop_mode` | Int | Текущий режим loop у этого виджета (`OFF / REPEAT_ALL / REPEAT_ONE / SHUFFLE`) |

Записи живут до удаления виджета пользователем. Чистка при удалении виджета — обязанность receiver'а (`onDeleted`).

## 5. DataStore: `uiDataStore` (`ui_prefs`)

| Ключ | Тип | Что значит |
|---|---|---|
| `font_scale` | Float | Множитель шрифта: 0.85 / 1.0 / 1.15 / 1.30 |

Один ключ. В новой версии сюда же приедут: тема (`Auto/Light/Dark`), флаг `dynamic_color`, выбранный EQ preset, sleep timer default и т.п.

## 6. Что переживает рестарт — итоговая матрица

| Сущность | Хранится | Восстанавливается |
|---|---|---|
| Плейлисты (список + имена) | Room | ✅ |
| Треки в плейлистах | Room | ✅ |
| Порядок треков | Room (`position`) | ✅ |
| Локальный кеш метаданных трека | Room (`tracks`) | ✅ |
| Title / artist текущего трека | DataStore | ✅ (для отображения в мини-баре до старта плеера) |
| isPlaying / shuffle / repeatMode | DataStore | ✅ |
| Текущий `playlistId` очереди | DataStore | ✅ (для матчинга в виджете) |
| **Сама очередь (media items)** | — | ❌ |
| **Индекс текущего трека в очереди** | — | ❌ |
| **Позиция в треке (timestamp)** | — | ❌ |
| Конфиги виджетов | DataStore | ✅ |
| Font scale | DataStore | ✅ |

Следствие: после рестарта мини-бар показывает последний трек по метаданным, но play-кнопка не сработает (queue empty → `NEEDS_QUEUE`). В новой версии планируется восстанавливать очередь (через сохранение `List<mediaStoreId>` + `currentIndex` + `positionMs` в DataStore).

## 7. Что НЕ переживает рестарт (намеренно или из-за пробелов)

| Сущность | Почему не хранится |
|---|---|
| Очередь | Не сохраняется — пробел текущей версии, фиксим |
| Позиция в треке | Не сохраняется — пробел, фиксим |
| Search query / search history | Не реализовано |
| Selection state на экранах | Эфемерное состояние UI |
| Listening history (recently played, most played) | Не реализовано |
| Favorites (избранные треки) | Не реализовано |
| Last opened tab | Не сохраняется (всегда стартует на Library) |

## 8. Решение по миграции в новой версии

**Сносим в ноль.** При первом запуске новой версии:
- Старая БД `morning_player.db` удаляется (`fallbackToDestructiveMigration` или явное удаление файла)
- Все три DataStore-файла удаляются
- Виджеты, оставшиеся на рабочем столе, перерисовываются как "Не настроен" (потеряют конфиг) — пользователь должен пересоздать

Это — осознанный компромисс. Цена: пользовательские плейлисты и конфиги виджетов теряются. Выгода: чистая схема без legacy, FK с CASCADE, единый prefs-файл, корректная новая версия с нулевыми тех-долгами.

## 9. Новая схема (предварительный набросок для Части 1)

Будет уточнена в expectations итерации 1.1, фиксирую общий контур здесь:

### Room v1 (новая)
- `playlists(id, name, createdAt, updatedAt)`
- `tracks(mediaStoreId, title, artist, album, albumId, durationMs, contentUri, dateAdded)` — добавляется `albumId` (для album art) и `dateAdded` (для сортировки)
- `playlist_tracks(playlistId FK→playlists.id ON DELETE CASCADE, mediaStoreId FK→tracks.mediaStoreId, position)` с composite PK
- `favorites(mediaStoreId FK→tracks.mediaStoreId ON DELETE CASCADE, addedAt)` — для функции "Liked"
- `play_history(id PK auto, mediaStoreId, playedAt)` — для Recently Played

### Единый DataStore (новая) — `prefs.preferences_pb`
- `playback.*` — текущее состояние плеера
- `playback.queue_ids` — `List<Long>` сериализованный (для восстановления очереди)
- `playback.current_index` — Int
- `playback.position_ms` — Long
- `widget.{appWidgetId}.*` — per-widget конфиги
- `ui.theme_mode` — `Auto / Light / Dark`
- `ui.dynamic_color` — Boolean
- `ui.font_scale` — Float
- `ui.sort_order` — для Library
- `ui.last_search_history` — `List<String>` (≤ 10)
- `audio.eq_preset` — Int
- `audio.eq_band_levels` — `List<Int>` (custom preset)
- `audio.playback_speed` — Float
- `audio.sleep_timer_default_min` — Int
