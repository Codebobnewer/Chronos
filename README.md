# Chronos

A self-contained in-game calendar and clock for **Paper/Folia 1.21.11**, with a public API and custom events so other plugins can build on it without touching any internals.

Chronos maintains its own calendar — year, month, week, day, day-of-week, hour, minute, second — running on a configurable structure and a configurable real-time/game-time conversion rate. It survives restarts (SQLite), can be paused, and shows the current date and time to every player at all times via a boss bar. Other plugins can also register their own calendar events (one-time or annually recurring) that show up in Chronos's own `/chronos calendar` GUI and can be queried programmatically.

The bundled defaults model Hypixel SkyBlock's calendar: 12 seasonal months of 31 days each, and a day that lasts exactly 20 real minutes (matching vanilla Minecraft's day/night cycle) — but every one of those numbers is configurable.

## Requirements

- Java 21
- A Paper or Folia server on 1.21.11
- [CommandAPI](https://commandapi.jorel.dev/) installed as its own plugin on the server (Chronos declares a hard dependency on it — see `depend: [CommandAPI]` in `plugin.yml`)

## Commands

| Command | Permission | Description |
|---|---|---|
| `/chronos` | `chronos.time.use` (default: everyone) | Shows the full current date and time |
| `/chronos now` | `chronos.time.use` | Shows just the time |
| `/chronos date` | `chronos.time.use` | Shows just the date |
| `/chronos calendar` | `chronos.time.use` | Opens an InvUI GUI showing today plus the next 7 days, and any events registered on each |
| `/chronos set <year> <month> <day> <hour> <minute> <second>` | `chronos.time.admin` (default: op) | Sets the calendar to an absolute date/time |
| `/chronos add <amount> <unit>` | `chronos.time.admin` | Advances or rewinds the calendar by whole units (`SECOND`, `MINUTE`, `HOUR`, `DAY`, `WEEK`, `MONTH`, `YEAR`) |
| `/chronos pause` | `chronos.time.admin` | Freezes the calendar |
| `/chronos resume` | `chronos.time.admin` | Unfreezes it |

`/chronos` and `/chronos now` show `(Paused)` when the calendar is frozen.

## Configuration

### `config.yml`

- **`calendar`** — the shape of the calendar: how many months per year, weeks per month, days per week, hours per day, minutes per hour, seconds per minute, plus display names for each month and each day of the week.
- **`time-conversion`** — `game-seconds-per-real-second` (the core real-time-to-game-time ratio) and `tick-interval-ticks` (how often, in server ticks, the clock advances).
- **`starting-date-time`** — used only the very first time the plugin ever starts, before anything has been persisted.
- **`persistence`** — the SQLite file name and how often (in real seconds) the running clock checkpoints itself to disk.
- **`debug`** — verbose logging for persistence operations.

All values are read once at startup; restart the server after editing.

### `messages.yml`

Every piece of player-facing text — command replies, error messages, the calendar GUI's titles/labels/lore, and the boss bar's text — is a MiniMessage template in this file, substituted with placeholders at render time. Nothing is hardcoded in the plugin's code, so wording and colors can be changed freely without recompiling.

## Public API

Other plugins depend on Chronos as a **soft dependency** (`softdepend: [Chronos]` in their own `plugin.yml`) and fetch the API through Bukkit's `ServicesManager`:

```java
import me.goga221.chronos.api.ChronosAPI;
import me.goga221.chronos.api.ChronosProvider;

ChronosAPI chronos = ChronosProvider.get(); // throws IllegalStateException if Chronos isn't installed/enabled
```

`ChronosAPI` exposes:

- Current date/time: `getCurrentDateTime()`, `getCurrentDate()`, `getCurrentTime()`, and individual `getYear()`/`getMonth()`/`getWeek()`/`getDay()`/`getDayOfWeek()`/`getHour()`/`getMinute()`/`getSecond()` getters.
- Calendar shape: `getMonthsPerYear()`, `getWeeksPerMonth()`, `getDaysPerWeek()`, `getHoursPerDay()`, `getMinutesPerHour()`, `getSecondsPerMinute()`, `getMonthName(int)`, `getDayOfWeekName(int)`.
- Comparisons: `isBefore(GameDateTime)`, `isAfter(GameDateTime)`, `isSameDay(GameDateTime)`.
- State: `isPaused()`.
- Calendar events: `registerCalendarEvent(CalendarEvent)` (returns a `UUID` handle), `unregisterCalendarEvent(UUID)`, `getUpcomingCalendarEvents(int limit)` (soonest-first), `getCalendarEventsOn(year, month, day)`.
- `getDateTimeAt(long totalGameSeconds)` — converts an absolute game-time instant (e.g. one your plugin computed as `getCurrentDateTime().totalGameSeconds() + someInterval`) back into a full date/time breakdown, useful for registering a future occurrence in advance rather than only advertising it once it starts.

### Registering a calendar event

```java
import me.goga221.chronos.api.CalendarEvent;

// Recurs every year on the same month/day, at midnight:
UUID id = chronos.registerCalendarEvent(
        CalendarEvent.recurringAnnual("Harvest Festival", "Bring your crops to the town square!", 10, 15));

// Occurs exactly once, at midnight on the given date:
UUID id2 = chronos.registerCalendarEvent(
        CalendarEvent.oneTime("Grand Opening", "Come check out the new build!", 2, 3, 1));

// ...later:
chronos.unregisterCalendarEvent(id);
```

Registrations are **in-memory only** — re-register on every `onEnable()`, the same way you would with any other Bukkit service.

### Events

Chronos fires a Bukkit event for every calendar rollover: `TimeMinuteChangeEvent`, `TimeHourChangeEvent`, `TimeDayChangeEvent`, `TimeWeekChangeEvent`, `TimeMonthChangeEvent`, `TimeYearChangeEvent` (package `me.goga221.chronos.event`). Each carries `getPrevious()`/`getCurrent()` (`GameDateTime`). Only the events for boundaries actually crossed are fired — a large `/chronos add` jump fires each crossed event once, not once per unit passed.

These fire synchronously, from whatever thread advanced the calendar (the global tick, or an admin's `/chronos` command) — always a tick-owning thread, never a specific player's or world's region. A listener that needs to touch a particular player/entity/world must still dispatch that work through the appropriate Folia scheduler itself.

## Persistence

The calendar's position (`totalGameSeconds` and whether it's paused) is stored in a single-row SQLite database, checkpointed periodically while running and on every admin `/chronos set`/`add`/`pause`/`resume`, plus a final guaranteed flush on shutdown. A crash (not a clean shutdown) can lose up to one persist interval's worth of progress — see `persist-interval-seconds` in `config.yml`.

## Folia compatibility

Chronos runs its clock off Paper's `ServerTickStartEvent` rather than any scheduler, and every admin mutation happens on whatever tick-owning thread issued it — so it works correctly under Folia's regionized threading model without assuming a single global thread anywhere.
