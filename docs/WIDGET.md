# Виджет (snapshot перед rewrite)

> Документирует поведение homescreen-виджета текущей версии. Реализация на AndroidX Glance + Material3 Glance. В новой версии — те же контракты, но с темой системы и масштабируемыми размерами (small / medium / large).

## 1. Регистрация в системе

### Манифест
```xml
<receiver
    android:name=".widget.MorningPlayerWidgetReceiver"
    android:exported="true"
    android:permission="android.permission.BIND_APPWIDGET">
  <intent-filter>
    <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
  </intent-filter>
  <meta-data
    android:name="android.appwidget.provider"
    android:resource="@xml/morning_player_widget" />
</receiver>

<activity
    android:name=".widget.WidgetConfigActivity"
    android:exported="true">
  <intent-filter>
    <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
  </intent-filter>
</activity>
```

### XML provider (`@xml/morning_player_widget`)
- Min size: фиксированный (4×2 cells типичный размер)
- Configure activity: `WidgetConfigActivity`
- Update period: 0 (обновления только pushем через `widget.updateAll`)
- Resize: ограниченный (HORIZONTAL | VERTICAL — уточняется по `appwidget-provider`)

В новой версии расширим до трёх размеров: small (1×1 функциональный — play/pause + title) / medium (текущие 4×2) / large (4×3 с обложкой).

## 2. Конфигурирование (при добавлении на рабочий стол)

1. Пользователь добавляет виджет → система запускает `WidgetConfigActivity` с extra `EXTRA_APPWIDGET_ID`
2. Активити показывает **список существующих плейлистов**
3. Если плейлистов нет — сообщение и кнопка "OK" → `setResult(RESULT_CANCELED)` → виджет НЕ добавляется
4. Тап по плейлисту:
   - `WidgetPreferences.setPlaylist(appWidgetId, playlistId, name, loopMode = OFF)`
   - `WidgetUpdater.updateAll(context)` (форсит первый рендер)
   - `setResult(RESULT_OK, intent_with_appWidgetId)`
   - `finish()`
5. Back / закрытие → `RESULT_CANCELED` → виджет не добавляется

## 3. Контент виджета

Все элементы рендерятся через Glance Composable (`MorningPlayerWidget.provideGlance`).

### Структура (сверху вниз, текущая версия)

| Элемент | Описание | Действие при тапе |
|---|---|---|
| **Заголовок плейлиста** | Имя плейлиста, выбранного при конфигурации | Открыть приложение на `playlist/{id}` через deep-link Intent |
| **Метаданные текущего трека** | Если `playbackState.playlistId == widget.playlistId` — title + artist; иначе текст "Не играет" / "Not playing" | Нет действия |
| **Кнопка Prev** | Иконка | Действие `WidgetPreviousAction` → `controller.previous()` + `WidgetUpdater.updateAll` |
| **Кнопка Play/Pause** | Иконка-toggle (зависит от `isPlaying`) | `WidgetPlayPauseAction` → `controller.togglePlayPause()` + если очередь пустая, `playPlaylist(widget.playlistId)` |
| **Кнопка Next** | Иконка | `WidgetNextAction` → `controller.next()` |
| **Кнопка Loop** | Иконка-cycle (см. ниже) | `WidgetLoopAction` → циклирует loop mode |

### Loop mode виджета (важная особенность)

В виджете **один контрол** циклирует **четыре состояния**, тогда как в приложении shuffle и repeat — отдельные кнопки. Цикл:

```
OFF → REPEAT_ALL → REPEAT_ONE → SHUFFLE → OFF
```

Иконки:
- `OFF` — стандартная иконка repeat (приглушённая)
- `REPEAT_ALL` — repeat (активная)
- `REPEAT_ONE` — repeat-1 (активная)
- `SHUFFLE` — shuffle (активная)

Соответствие при применении к плееру:
| Mode | shuffleEnabled | repeatMode |
|---|---|---|
| OFF | false | OFF |
| REPEAT_ALL | false | ALL |
| REPEAT_ONE | false | ONE |
| SHUFFLE | true | OFF (или ALL — уточнить в коде) |

Loop mode виджета сохраняется в `widgetDataStore` per `appWidgetId`. **Не синхронизируется** обратно с состоянием плеера — это локальная настройка виджета, применяющаяся при тапе.

### Дизейбл кнопок

- Prev disabled при `!playbackState.canGoPrevious`
- Next disabled при `!playbackState.canGoNext`
- Play/Pause никогда не disabled
- Loop никогда не disabled

## 4. Цвета и тема (текущая версия — пробел)

**Захардкожены тёмные цвета** независимо от темы устройства:
- Фон: `#0F172A` (slate-900)
- Текст: белый
- Иконки активные: оранжевый (акцент)
- Иконки приглушённые: серый

**Это пробел.** В новой версии:
- Виджет следует системной теме (light / dark) через Glance Material3 colors
- Учитывает Material You (динамический акцент) на Android 12+
- Для Android < 12 — fallback на статичные палитры в синхроне с приложением

## 5. Обновление виджета

Источники обновления:
- `PlayerService` listener'ы (на каждое значимое событие плеера) → `WidgetUpdater.updateAll()`
- `WidgetXxxAction` после выполнения команды → `WidgetUpdater.updateAll()`
- `WidgetConfigActivity` после выбора плейлиста → `WidgetUpdater.updateAll()`
- `MorningPlayerWidgetReceiver` на `APPWIDGET_UPDATE` от системы → стандартный путь Glance

`WidgetUpdater.updateAll(context)` — синхронизированный (Mutex) вызов `widget.updateAll(context)` от Glance. Перерисовывает **все** размещённые виджеты текущего provider'а (не точечно по `appWidgetId`).

Узкое место: при быстром потоке событий (например, шквал `onPositionDiscontinuity` при перемотке) — лишние редроу. В новой версии добавим throttle / coalesce.

## 6. Поведение при множественных виджетах

- Каждый виджет — независимая запись в `widgetDataStore` по `appWidgetId`
- Каждый виджет может ссылаться на разные плейлисты
- Действия пользователя адресуются конкретному виджету (его `appWidgetId` пробрасывается через `ActionParameters`)
- Управление playback'ом, естественно, общее — нельзя одновременно играть два плейлиста на одном устройстве. Виджет, чей `playlistId` совпадает с играющим, показывает текущий трек; остальные показывают "Не играет"

## 7. Удаление виджета

- При удалении виджета пользователем — система шлёт `onDeleted(int[] appWidgetIds)`
- Receiver **должен** очистить соответствующие записи из `widgetDataStore`
- В текущей версии этой очистки **может не быть** — проверить по реализации `MorningPlayerWidgetReceiver.onDeleted`. Если отсутствует — фиксируется как пробел (мёртвые ключи в DataStore)

## 8. Deep-links из виджета в приложение

| Действие | Intent |
|---|---|
| Тап по заголовку плейлиста | `MainActivity` с `EXTRA_PLAYLIST_ID = widget.playlistId` → `AppNavHost` навигирует на `playlist/{id}` |

Реализуется через Glance `actionStartActivity` с заранее сконструированным Intent.

## 9. Контракты для новой версии

В новой версии (rewrite) сохраняются следующие инварианты:

1. **Per-widget playlist** — каждый виджет привязан к своему плейлисту
2. **Loop cycle 4 состояния** в одной кнопке — оставляем как есть (UX-решение, простое и компактное)
3. **Реал-тайм отражение playback** — задержка не больше 500мс от события плеера
4. **Deep-link на playlist** — открытие приложения сразу на нужном экране
5. **Independence** — действия в одном виджете не влияют на другой (кроме общего состояния плеера, что физически неизбежно)

В новой версии добавляется:
- Тема следует системе
- Material You на Android 12+
- Размеры (small / medium / large) с разным набором контролов
- Album art в medium / large
- Прогресс-бар трека (тонкая полоска) в medium / large
- Корректная очистка `widgetDataStore` в `onDeleted`
- Throttle обновлений при потоке событий
