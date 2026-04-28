# Спецификация плеера (snapshot перед rewrite)

> **Назначение документа.** Зафиксировать функционал текущей версии (commit на момент написания). Источник истины для проверки парности новой версии со старой по итогам Части 1. После завершения rewrite этот документ становится архивным — актуальная спека живёт в `SCREENS.md` + `PERSISTENCE.md` + `WIDGET.md`.

## 1. Платформа и стек

| Параметр | Значение |
|---|---|
| `applicationId` | `com.maximg.player` |
| `versionCode` / `versionName` | `1` / `1.0` |
| `minSdk` | `35` (Android 15) |
| `targetSdk` / `compileSdk` | `35` |
| Язык | Kotlin (JVM 17) |
| UI | Jetpack Compose + Material3 |
| Воспроизведение | AndroidX Media3 (ExoPlayer + MediaSessionService) |
| База данных | Room 2.6.1 |
| Настройки | Jetpack DataStore Preferences |
| Виджет | AndroidX Glance + Material3 |
| Структура процесса | single-activity + foreground service `PlayerService` |

## 2. Разрешения

| Permission | Когда запрашивается | Блокирует UI |
|---|---|---|
| `READ_MEDIA_AUDIO` | при старте через PermissionGate, повторно при `ON_RESUME` | **Да** — приложение не запускается без него |
| `POST_NOTIFICATIONS` | в манифесте, явный rationale-flow отсутствует | Нет — нотификация всё равно создастся, просто без алерта |
| `FOREGROUND_SERVICE` | автоматически | — |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | автоматически | — |

## 3. Источник треков

### Запрос к MediaStore
- Таблица: `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`
- Поля: `_ID`, `TITLE`, `ARTIST`, `ALBUM`, `DURATION`
- Фильтр: `IS_MUSIC = 1` (только аудио, помеченное как музыка)
- Доп. фильтр (если задан поиск): `TITLE LIKE ?` OR `ARTIST LIKE ?` без учёта регистра, паттерн `%query%`
- Сортировка: `DATE_ADDED DESC` (только эта, без UI-выбора)

### Поиск
- Поле ввода в LibraryScreen
- Дебаунс **300мс** на стороне UI
- Локального кеша нет — каждый ввод триггерит новый запрос к ContentResolver на `Dispatchers.IO`

### Album art
- НЕ загружается. Поле есть в MediaStore (`ALBUM_ID` + `albumArtUri`), но в текущей версии не используется.

## 4. Воспроизведение

### Очередь
- Очередь = все треки одного плейлиста, в порядке `position` или перемешанные при включённом shuffle
- Заводится только через `playPlaylist(playlistId, startIndex = 0)` на `PlaybackController`
- Каждый вызов **полностью заменяет** текущую очередь
- Понятий "play next" / "add to queue" / "play single track without playlist" — нет

### Контролы (что умеет PlaybackController)
| Действие | Метод | Поведение |
|---|---|---|
| Play / Pause toggle | `togglePlayPause()` | Если очередь пуста — возвращает `NEEDS_QUEUE`, UI ничего не делает |
| Next | `next()` | Делегирует `controller.seekToNext()` |
| Previous | `previous()` | Делегирует `controller.seekToPrevious()` |
| Shuffle on/off | `setShuffle(Boolean)` | Применяется к контроллеру + DataStore |
| Repeat mode | `setRepeat(Int)` | `OFF` / `ALL` / `ONE` (`Player.REPEAT_MODE_*`) |
| Cycle repeat | `cycleRepeatMode(current)` | `OFF → ALL → ONE → OFF` |

### Repeat в виджете (отдельный цикл)
В виджете один loop-button циклирует 4 состояния: **OFF → REPEAT_ALL → REPEAT_ONE → SHUFFLE → OFF**. Это шире, чем в приложении: shuffle в виджете ставится через тот же контрол, что и repeat. В приложении shuffle и repeat — разные кнопки.

### Notification (системная)
- Создаётся `DefaultMediaNotificationProvider` от Media3 — не кастомная
- Контент: title + artist + transport controls (prev / play-pause / next)
- Без album art, без custom actions, без seek-bar в нотификации

### Lifecycle сервиса
- `PlayerService` — `MediaSessionService`, foreground type `mediaPlayback`
- Стартует при первом обращении из приложения (через `MediaController.Builder(...).buildAsync()`)
- Слушатель ExoPlayer пишет в DataStore при: `onIsPlayingChanged`, `onMediaItemTransition`, `onShuffleModeEnabledChanged`, `onRepeatModeChanged`, `onTimelineChanged`, `onPositionDiscontinuity`
- На каждое изменение — `WidgetUpdater.updateAll()` → `widget.updateAll(context)` (синхронизирует все размещённые виджеты)

## 5. Плейлисты

| Операция | Поведение |
|---|---|
| Создать | Имя задаёт пользователь в диалоге, `createdAt = System.currentTimeMillis()` |
| Переименовать | Через long-press → меню |
| Удалить | Через long-press → меню; **каскад** на join-таблицу делается в коде (`PlaylistRepository.deletePlaylist`), не FK |
| Добавить треки | Один трек: кнопка-плюс справа от строки в Library. Массово: режим "добавить треки" — отдельный флоу с чекбоксами и подтверждением "Add N tracks" |
| Удалить треки из плейлиста | В режиме мультиселекта в PlaylistDetail |
| Переместить трек | В режиме мультиселекта — кнопки "Move up" / "Move down" в bottom action bar. Drag-to-reorder из `ReorderableLazyColumn` **в коде есть, но не подключён** к этому экрану |
| Дубликаты | Один и тот же `mediaStoreId` нельзя добавить в один плейлист дважды (`OnConflictStrategy.IGNORE` на join-вставке) |

Сортировка списка плейлистов: `createdAt DESC` (новые сверху).

## 6. Виджет (homescreen)

### Конфигурирование
- При добавлении на рабочий стол открывается `WidgetConfigActivity`
- Пользователь выбирает плейлист из списка существующих
- Если плейлистов нет — UI показывает соответствующее сообщение
- Конфиг сохраняется: `(appWidgetId → playlistId, playlistName, loopMode)`

### Содержимое
| Элемент | Описание |
|---|---|
| Заголовок | Имя плейлиста — кликабелен, открывает плейлист в приложении |
| Текущий трек | Title + artist, если `playbackState.playlistId == widget.playlistId`, иначе "Не играет" |
| Prev | Кнопка; дизейблится по `canGoPrevious` |
| Play/Pause | Иконка-toggle |
| Next | Кнопка; дизейблится по `canGoNext` |
| Loop | Цикл OFF / REPEAT_ALL / REPEAT_ONE / SHUFFLE с визуальным выделением активного |

### Цвета виджета (текущая версия)
**Захардкожены тёмными** независимо от темы устройства/приложения: фон `#0F172A`, текст белый, акцент оранжевый. Не уважает system dark/light. Это пробел, исправляется в Части 2.

### Множественные виджеты
Поддержаны. Каждый — со своим `appWidgetId` и своим выбором плейлиста. Действия пользователя по виджету относятся только к этому виджету (его `appWidgetId` пробрасывается в `ActionParameters`).

## 7. Настройки приложения

Единственная настройка — **масштаб шрифта**. 4 уровня: 85% / 100% / 115% / 130%. Хранится в `uiDataStore` (`font_scale`). Применяется ко всему UI через `LocalDensity` (примерный механизм, см. App.kt).

## 8. Локализация

- `res/values/strings.xml` — английский, ~60 строк
- `res/values-ru/strings.xml` — русский перевод
- Никаких pluralizations (используется `%1$d tracks` через `String.format`, не `<plurals>`)

## 9. Темы и стили

| Параметр | Значение |
|---|---|
| Color scheme | Только `lightColorScheme` |
| Dark theme | Нет |
| Material You / dynamic color | Нет |
| Primary | `#B45309` (amber-700) |
| Secondary | `#0F766E` (teal-700) |
| Tertiary | `#1D4ED8` (blue-700) |
| Background | `#F7F4EE` (cream) |
| Surface | `#FFFCF7` |
| Typography | Кастомизирована: headlineLarge 30sp/bold, titleLarge 22sp/semibold, bodyLarge 16sp |
| Шрифт | Системный по умолчанию |

## 10. Состояния UI

| Состояние | Реализовано |
|---|---|
| Loading | ❌ Нет индикаторов. Во время загрузки — пустая область |
| Empty | ✅ На каждом экране — иконка + текст. Унифицированный шаблон |
| Error | ❌ Ошибки только в logcat. Пользователь не уведомляется |
| Skeleton | ❌ |
| Snackbar / Toast | ❌ Нет фидбека при действиях ("добавлено N треков", "плейлист удалён" и т.п.) |
| Undo | ❌ Удаление неотменяемое |

## 11. Что переживает рестарт приложения

| Что | Где | Чем гарантируется |
|---|---|---|
| Плейлисты + треки + порядок | Room (`playlists`, `tracks`, `playlist_tracks`) | БД на диске |
| Title / artist / isPlaying / shuffle / repeatMode / canGoPrev / canGoNext / playlistId | DataStore `playback_state` | DataStore |
| Конфиги виджетов | DataStore `widget_prefs` (per `appWidgetId`) | DataStore |
| Font scale | DataStore `ui_prefs` | DataStore |

## 12. Что НЕ переживает рестарт

- Позиция воспроизведения внутри трека (timestamp в миллисекундах)
- Индекс текущего трека в очереди
- Сама очередь (после рестарта без явного `playPlaylist` — пустая, ExoPlayer не запоминает media items между процессами)

Следствие: после рестарта мини-бар пуст, метаданные в DataStore есть, но кнопка play не сработает (queue empty → `NEEDS_QUEUE`). Это легитимное текущее поведение, в новой версии исправим (восстановление очереди + позиции).

## 13. Что в текущей версии отсутствует

Фиксируется как пробелы — НЕ нужно переносить в новую версию (потому что нечего переносить):

- Album / Artist / Folder browsing
- Album art (Library / NowPlaying / виджет / notification)
- Полноэкранный Now Playing (есть только мини-бар)
- Очередь как UI-объект (drag, add to queue, play next)
- Sort/filter в Library кроме `DATE_ADDED DESC`
- Search history
- Sleep timer
- Equalizer
- Playback speed
- Favorites
- Recently played
- Backup / restore плейлистов
- Snackbar / Undo
- Loading и Error states в UI
- Skeleton placeholders
- Dark / dynamic theme

## 14. Технические инварианты, которые ОБЯЗАНЫ сохраниться

Это контракты функциональной парности — проверяются вручную по итогам итерации 1.5 и автоматически тестами в 1.7:

1. **Reorder сохраняет порядок.** Перетаскивание / move up/down N раз → закрытие приложения → открытие → порядок треков совпадает.
2. **Каскад при удалении плейлиста.** Удаление плейлиста с N треками → join-таблица не содержит ни одной записи с этим `playlistId`. (В новой версии — через FK ON DELETE CASCADE, не в коде.)
3. **Дедупликация в плейлисте.** Добавление одного и того же `mediaStoreId` дважды → в плейлисте остаётся одна запись.
4. **Виджет всегда отражает актуальное состояние.** Любое изменение через приложение/нотификацию → виджет обновляется в течение секунды.
5. **READ_MEDIA_AUDIO — gate.** Без разрешения приложение не работает (gate-экран). С разрешением — работает.
6. **Конфиги виджетов независимы.** 2 виджета на 2 разных плейлиста → действия в одном не затрагивают другой.
7. **Notification отражает актуальный трек.** Title + artist в notification совпадают с текущим воспроизведением.
8. **shuffle / repeat переживают рестарт.** Включил shuffle → закрыл → открыл → флаг включён.
9. **Font scale применяется глобально.** Меняется в одном месте — применяется на всех экранах.
10. **Bottom bar скрыт на playlist detail.** Контракт навигации, не визуальное предпочтение — оставляем.

## 15. Известные баги текущей версии (не воспроизводим)

- Subtitle TopAppBar в PlaylistDetail имеет формат `"%1$d tracks - %2$s"`, но второй placeholder получает `""` — всегда отображается `"N tracks - "` с висящим тире. Не переносим.
- `ReorderableLazyColumn` написан и не используется. Не переносим как есть — drag-to-reorder реализуем заново уже в подключённом виде.
- POST_NOTIFICATIONS rationale-флоу описан в комментариях, но кода нет. Не переносим.
- Виджет хардкодит тёмную тему. Не переносим — в новой версии следуем системе.
