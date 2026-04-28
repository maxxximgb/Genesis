# Экраны (snapshot перед rewrite)

> Документ описывает экраны и навигационные переходы текущей версии плеера. Не список фич — а пошаговый разбор того, что видит пользователь и какие действия доступны.

## Карта навигации

```
[PermissionGate]
      │ (после грантa READ_MEDIA_AUDIO)
      ▼
[MorningPlayerApp] ─── BottomBar ──┬── Library
                                   └── Playlists ──► PlaylistDetail
                                                          │
                                                          └─► Library?addToPlaylistId=N (режим добавления)

[NowPlayingBar] — закреплён над BottomBar на всех экранах, кроме PlaylistDetail (там скрыт BottomBar но есть свой NowPlayingBar? — нет, он отображается во всех роутах поверх контента, см. ниже)

[Intent] EXTRA_PLAYLIST_ID ──► PlaylistDetail (deep-link из виджета)

[WidgetConfigActivity] — отдельная активити, открывается при добавлении виджета
```

### Routes (NavHost)

| Route | Аргументы | Composable |
|---|---|---|
| `library?addToPlaylistId={addToPlaylistId}` (start) | `addToPlaylistId: Long?` | `LibraryScreen` |
| `playlists` | — | `PlaylistsScreen` |
| `playlist/{playlistId}` | `playlistId: Long` | `PlaylistDetailScreen` |

Bottom bar:
- Видим в `library*` и `playlists`
- Скрыт в `playlist/{id}`

`NowPlayingBar`:
- Отображается на всех экранах поверх контента, если `playbackState.title != null`
- Скрывается, когда `title == null` (нет очереди / ничего не играет)

---

## Экран 1 — PermissionGate

**Цель.** Гейт перед основным приложением. Без `READ_MEDIA_AUDIO` → весь UI заменён на экран запроса.

### Состояния
| Состояние | Что показывает |
|---|---|
| Permission granted | Прозрачен — рендерит дочерний контент (`MorningPlayerApp`) |
| Not requested yet | Контент с описанием + кнопка "Grant permission" |
| Denied (rationale) | То же что выше — повторный запрос |
| Permanently denied | Кнопка ведёт в системные настройки приложения |

### Поведение
- Проверка при входе (DisposableEffect) и на `Lifecycle.Event.ON_RESUME` (поймать грант через системные настройки)
- POST_NOTIFICATIONS — упомянут в комментариях, не запрашивается. Notification всё равно создастся MediaSession-ом.

---

## Экран 2 — Library

**Route:** `library?addToPlaylistId={addToPlaylistId}` (start destination)

Имеет **два режима** в зависимости от наличия аргумента `addToPlaylistId`.

### 2.A. Режим Browse (стандартный, `addToPlaylistId == null`)

#### Содержимое сверху вниз
1. **TopAppBar** — заголовок "Library", action-кнопка "Settings" (открывает диалог).
2. **Поле поиска** (TextField) с иконкой поиска и крестиком-очисткой. Дебаунс 300мс.
3. **Список треков** (LazyColumn). Каждая строка:
   - Title (1 строка, ellipsis)
   - Artist (1 строка, ellipsis, меньшим шрифтом)
   - Длительность (mm:ss)
   - Кнопка **+** справа — открывает bottom sheet "Add to playlist"
4. **Empty state** — если треков нет: иконка + сообщение "No tracks found" / "Allow access".
5. **Loading state** — НЕТ. Пустота во время загрузки.

#### Действия
| Действие | Результат |
|---|---|
| Тап по треку | Запустить плейбэк ОДНОГО трека? Нет — в текущей версии тап не запускает воспроизведение в Library, только long-press включает мультиселект. Воспроизведение из Library в текущей версии **не реализовано**. |
| Long-press по треку | Включает selection mode (см. ниже) |
| Тап на + | Bottom sheet со списком плейлистов; тап по плейлисту — добавить трек |
| Тап на иконку Settings | Диалог настроек (см. 2.C) |
| Ввод в поле поиска | Дебаунс 300мс → перезапрос |

#### Selection mode (по long-press)
- TopAppBar меняется на contextual: "N selected" + actions: "Add to playlist", "Cancel"
- Чекбоксы появляются у строк
- Bulk add to playlist через bottom sheet

### 2.B. Режим Add Tracks (`addToPlaylistId != null`)

Открывается из PlaylistDetailScreen → "Add tracks" FAB.

#### Содержимое
1. **TopAppBar** — "Add tracks to <playlistName>", крестик "Cancel"
2. Поле поиска — то же самое
3. Список треков — те же строки, но с чекбоксами (без кнопки "+")
4. Внизу — фиксированный footer **"Add N tracks"** (CTA, дизейблится при N=0)

#### Поведение
- Множественный выбор по тапу
- На "Add N tracks" — `addTracksToPlaylist(playlistId, ids)` → pop back

### 2.C. Settings Dialog
Открывается из Library только.

| Опция | UI |
|---|---|
| Font scale | 4 кнопки (85% / 100% / 115% / 130%) — выделена активная |

Закрывается крестиком / тапом вне диалога.

---

## Экран 3 — Playlists

**Route:** `playlists`

### Содержимое сверху вниз
1. **TopAppBar** — "Playlists"
2. **Список плейлистов** (LazyColumn) — сортировка `createdAt DESC`. Каждая карточка:
   - Имя плейлиста
   - Количество треков ("N tracks")
3. **Empty state** — "No playlists yet" + сообщение "Create one with the + button"
4. **FAB** — большая "+" в правом нижнем углу

### Действия
| Действие | Результат |
|---|---|
| Тап по плейлисту | Переход на `playlist/{id}` |
| Long-press | Меню (DropdownMenu или ModalBottomSheet) с пунктами: Rename, Delete |
| Rename | Диалог с TextField, Save / Cancel |
| Delete | Диалог подтверждения "Delete playlist?", Confirm / Cancel — удаляет плейлист (+ join-rows) |
| FAB "+" | Диалог "Create playlist" с TextField, Create / Cancel |

---

## Экран 4 — PlaylistDetail

**Route:** `playlist/{playlistId}`

BottomBar здесь скрыт (см. контракт навигации).

### Содержимое сверху вниз
1. **TopAppBar** — title = имя плейлиста, subtitle = `"N tracks - "` (с висящим тире — баг, не переносим)
   - Navigation icon: back-стрелка
   - Action: иконка Play (запускает плейбэк с трека 0)
2. **Список треков** в порядке `position ASC`. Строка:
   - Title / Artist / Duration
3. **FAB** — "Add tracks" с иконкой плюса (внизу справа). Открывает Library в режиме Add Tracks.
4. **Empty state** — "No tracks in this playlist"

### Действия
| Действие | Результат |
|---|---|
| Тап по треку | `playPlaylist(playlistId, startIndex = тапнутый индекс)` |
| Long-press по треку | Включает selection mode |
| Action "Play" в TopAppBar | `playPlaylist(playlistId, 0)` |
| FAB "Add tracks" | Переход на Library с `addToPlaylistId = playlistId` |

### Selection mode
- TopAppBar contextual: "N selected" + actions Cancel
- Чекбоксы у строк
- **Bottom action bar** (закреплён внизу) с действиями:
  - Move up (стрелка вверх) — двигает выбранные на одну позицию вверх
  - Move down — соответственно
  - Delete — удаляет выбранные из плейлиста (без подтверждения, без Undo)

---

## Глобальные элементы (поверх всех экранов)

### NowPlayingBar
**Размещение.** Закреплён над BottomBar на всех роутах, где `playbackState.title != null`.

**Содержимое.**
- Иконка-плейсхолдер слева (note icon, без album art)
- Title + artist (две строки, ellipsis)
- Кнопки prev / play-pause / next в один ряд справа
- prev и next дизейблятся при `!canGoPrevious` / `!canGoNext`

**Тап по бару** в текущей версии **ничего не делает** (нет полноэкранного Now Playing).

### BottomBar
**Размещение.** Закреплён внизу. Скрыт на `playlist/{id}`.

**Tabs.**
- Library (иконка LibraryMusic)
- Playlists (иконка PlaylistPlay)

Активная вкладка подсвечена. При тапе — `popUpTo(start)`/`launchSingleTop`.

---

## Экран 5 — WidgetConfigActivity

Отдельная активити (`android.appwidget.action.APPWIDGET_CONFIGURE`), запускается системой при добавлении виджета на рабочий стол.

### Содержимое
1. Заголовок "Choose playlist" (или эквивалент)
2. Список плейлистов (как на экране Playlists)
3. Если плейлистов нет — сообщение "No playlists yet" с предложением создать в приложении

### Действия
- Тап по плейлисту: сохранить в `WidgetPreferences` `(appWidgetId → playlistId, name, loopMode = OFF)` → `setResult(RESULT_OK, intent_with_appWidgetId)` → `finish()`
- Тап по back: `setResult(RESULT_CANCELED)` → виджет не добавляется системой
- "No playlists" → нет CTA, кроме закрытия

---

## Empty / Error states по экранам

| Экран | Empty | Error | Loading |
|---|---|---|---|
| Library (browse) | "No tracks found" | — | — |
| Library (search не нашёл) | "No tracks match" | — | — |
| Library (add mode) | то же | — | — |
| Playlists | "No playlists yet" | — | — |
| PlaylistDetail | "No tracks in this playlist" | — | — |
| WidgetConfig | "No playlists" | — | — |

**Везде отсутствует error state и loading state** — это пробел текущей версии, в новой исправляем.

---

## Гесты

| Жест | Где | Результат |
|---|---|---|
| Long-press на треке | Library, PlaylistDetail | Selection mode |
| Long-press на плейлисте | Playlists | Меню (rename/delete) |
| Тап на NowPlayingBar | везде | **Ничего** (планируется fullscreen в новой версии) |
| Свайп вниз/вверх | — | Не используется |
| Pull-to-refresh | — | Не используется |
| Drag-to-reorder | PlaylistDetail (запланировано) | Не подключено в текущей версии — есть только move up/down кнопки |

---

## Deep links

| Источник | Куда ведёт | Механизм |
|---|---|---|
| Тап по заголовку виджета | `playlist/{id}` | `Intent` с `EXTRA_PLAYLIST_ID` → `MainActivity.handleIntent` → `LaunchedEffect` в `AppNavHost` дёргает `navController.navigate` |

---

## Чего на экранах НЕТ (фиксируем как пробелы, в новой версии добавим)

- Полноэкранный Now Playing
- Очередь как видимый список
- Tabs Albums / Artists / Folders в Library
- Страница артиста / альбома
- Sort & filter в Library
- Search history (последние запросы)
- Snackbar после действий (add/delete)
- Skeleton при загрузке
- Pull-to-refresh
- Tap на NowPlayingBar (раскрытие)
- Свайп для удаления / Add to queue
- Sleep timer UI
- Equalizer UI
- Playback speed UI
- Settings: dark mode, dynamic color, equalizer presets, backup/restore
