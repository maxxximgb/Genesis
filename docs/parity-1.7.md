# Parity report — 1.7

Сверка с `SPEC.md §14`. Все 11 инвариантов имеют автоматическое или явное ручное покрытие.

| # | Инвариант | Источник истины (код) | Тесты |
|---|---|---|---|
| 1 | Reorder preserves order across restart | `PlaylistDao.move()` + `PlaylistTrackEntity.position` | `PlaylistTrackReorderTest`, `ComputeMoveTest`, `PlaylistDetailViewModelTest` |
| 2 | FK CASCADE при удалении плейлиста | Room v1 schema (`onDelete = CASCADE`) | `PlaylistDaoCascadeTest`, `TrackDaoCascadeTest` |
| 3 | Дедупликация `(playlistId, mediaStoreId)` | UNIQUE index в схеме + `OnConflictStrategy.IGNORE` | `PlaylistTrackDedupTest` |
| 4 | Widget reflects playback в <1s | `WidgetUpdater` + `RefreshDebouncer` (250ms) | `RefreshDebouncerTest` (5 кейсов, плюс guard-тест на DEBOUNCE_MS < 1000) |
| 5 | `READ_MEDIA_AUDIO` gate | `PermissionGate` composable | Manual; ручная проверка после установки на S24 |
| 6 | Multi-widget independence | `WidgetPreferencesStore` per-`appWidgetId` ключи | `WidgetPreferencesStoreTest.setAndGetPersistsPerWidget` |
| 7 | Notification metadata = current track | `MediaItemMapping.toMediaItem()` | `MediaItemMappingTest` (5 кейсов: title, artist, album, artwork, mediaStoreId/playlistId roundtrip, sparse fields) |
| 8 | shuffle/repeat persist across restart | `PlaybackStateStore` DataStore | `PlaybackStateStoreTest`, `PlaybackControllerImplTest`, `RepeatModeMappingTest` |
| 9 | Font scale applies globally | `UserPreferencesStore.fontScale` + `LocalDensity` override | `UserPreferencesStoreSettingsTest`; ручная проверка |
| 10 | BottomBar hidden на `playlist/{id}` | `MainActivity:57-65` (`currentRoute.startsWith("playlist/")` + hierarchy-check) | Manual; routes тестов нет — это композиционная логика |
| 11 | Per-playlist mode persists в `PlaylistModeStore` | `PlaylistModeStore` DataStore + `PlayPlaylistUseCase` применяет на старт | `PlaylistModeStoreTest`, `PlayPlaylistUseCaseTest`, `SetPlaylistLoopModeUseCaseTest`, `LoopStateTest` |

## Тесты — итог

- **Baseline после 1.6:** 155 тестов (включая widget redesign + bg picker, `RefreshDebouncerTest` уже в 1.6)
- **После 1.7:** 160 тестов (+5): `MediaItemMappingTest` (×5)
- Все 160 — green

## Bug sweep

Просмотрены потенциальные риски, по итогам:

| Риск | Статус | Действие |
|---|---|---|
| `WidgetArtLoader` cache unbounded | OK | LinkedHashMap LRU с cap 32 (≈8MB при ARGB 256×256) |
| `pendingPlaylistId` race при тапе нескольких виджетов подряд | OK | mutableStateOf принимает последнее значение; LaunchedEffect отрабатывает один раз и сбрасывает — приемлемо |
| `actionStartActivity` PendingIntent collision при разных playlistId | OK | Glance гарантирует уникальность через `GlanceId`; extra пробрасываются |
| Coil bitmap не recycle'ится | OK | `produceState` сбрасывается на смену `albumId`; bitmap остаётся в общем cache |
| Reorder + параллельное remove в `PlaylistDetailViewModel` | OK | Все мутации идут через `viewModelScope` + StateFlow; индексы пересчитываются от текущего snapshot |
| WidgetUpdater.requestUpdate потерянный коалесс | OK | RefreshDebouncer extracted + покрыт тестами |
| Виджет при отозванном `READ_MEDIA_AUDIO` | Acceptable | MediaStoreSource отдаёт пусто → виджет показывает `EMPTY_PLAYLIST`. Спец-стейт «нет доступа» можно добавить в 2.x, но не блокирует парность |

## Не делается в 1.7 (намеренно)

- iTunes-style layout — **2.1**
- Album/Artist views — **2.2**
- Now Playing fullscreen, sleep timer — **2.3**
- Favorites, Equalizer, playback speed — **2.4**
- Backup/restore JSON, widget sizes (XL tier), mosaic playlist art — **2.5**

## Решение

Парность с легаси-плеером по `SPEC.md §14` — **подтверждена**. Готовы переходить к UI-полировке (часть 2.x).
