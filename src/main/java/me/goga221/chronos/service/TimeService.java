package me.goga221.chronos.service;

import me.goga221.chronos.api.CalendarEvent;
import me.goga221.chronos.api.ChronosAPI;
import me.goga221.chronos.api.GameDate;
import me.goga221.chronos.api.GameDateTime;
import me.goga221.chronos.api.GameTime;
import me.goga221.chronos.api.UpcomingCalendarEvent;
import me.goga221.chronos.calendar.CalendarEngine;
import me.goga221.chronos.calendar.CalendarStructure;
import me.goga221.chronos.calendar.GameTimeUnit;
import me.goga221.chronos.config.ChronosConfig;
import me.goga221.chronos.event.TimeDayChangeEvent;
import me.goga221.chronos.event.TimeHourChangeEvent;
import me.goga221.chronos.event.TimeMinuteChangeEvent;
import me.goga221.chronos.event.TimeMonthChangeEvent;
import me.goga221.chronos.event.TimeWeekChangeEvent;
import me.goga221.chronos.event.TimeYearChangeEvent;
import me.goga221.chronos.message.MessageKey;
import me.goga221.chronos.storage.PersistedGameTime;
import me.goga221.chronos.storage.TimeRepository;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.PluginManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The Chronos calendar engine: owns the current position on the calendar,
 * advances it, detects and announces rollovers, and persists it.
 * <p>
 * Read access ({@link ChronosAPI}) is lock-free; state changes are
 * serialized so a scheduled tick and an administrative command can never
 * interleave into a lost update.
 */
@RequiredArgsConstructor
public final class TimeService implements ChronosAPI {

    private final ChronosConfig config;
    private final TimeRepository repository;
    private final PluginManager pluginManager;
    private final CalendarEventService calendarEventService;
    private final TaskScheduler scheduler;
    private final Logger logger;

    private volatile long totalGameSeconds;
    private volatile boolean paused;
    private double fractionalGameSeconds;
    private double secondsSinceLastPersist;

    /**
     * Loads the persisted calendar position, or falls back to the
     * configured starting date/time if none has ever been saved.
     */
    public void initialize() {
        final Optional<PersistedGameTime> saved = repository.load();
        if (saved.isPresent()) {
            this.totalGameSeconds = saved.get().totalGameSeconds();
            this.paused = saved.get().paused();
            return;
        }
        this.totalGameSeconds = CalendarEngine.toTotalSeconds(
                config.startYear(), config.startMonth(), config.startDay(),
                config.startHour(), config.startMinute(), config.startSecond(),
                config.calendarStructure());
    }

    /**
     * Advances the calendar by the game-time equivalent of
     * {@code realSecondsElapsed}, firing rollover events and persisting on
     * the configured interval. Called from the global server tick listener.
     * Does nothing while the calendar is paused.
     */
    public synchronized void tick(final double realSecondsElapsed) {
        if (paused || realSecondsElapsed <= 0.0) {
            return;
        }

        final double exactGameSeconds = realSecondsElapsed * config.gameSecondsPerRealSecond() + fractionalGameSeconds;
        final long wholeSecondsToAdd = (long) Math.floor(exactGameSeconds);
        fractionalGameSeconds = exactGameSeconds - wholeSecondsToAdd;

        if (wholeSecondsToAdd > 0) {
            applyDelta(wholeSecondsToAdd);
        }

        secondsSinceLastPersist += realSecondsElapsed;
        if (secondsSinceLastPersist >= config.persistIntervalSeconds()) {
            persistNow();
            secondsSinceLastPersist = 0.0;
        }
    }

    /**
     * Sets the calendar to an absolute date and time, validating the
     * request before applying it. Persists immediately.
     */
    public synchronized void setDateTime(final int year, final int month, final int day,
                                          final int hour, final int minute, final int second) {
        final long newTotal;
        try {
            newTotal = CalendarEngine.toTotalSeconds(year, month, day, hour, minute, second, config.calendarStructure());
        } catch (final IllegalArgumentException exception) {
            throw new InvalidTimeOperationException(MessageKey.ERROR_INVALID_OPERATION, exception.getMessage());
        }

        final GameDateTime before = getCurrentDateTime();
        this.totalGameSeconds = newTotal;
        fireRolloverEvents(before.totalGameSeconds(), newTotal);
        persistNow();
    }

    /**
     * Advances or rewinds the calendar by a whole number of units,
     * validating that the result is feasible before applying it. Persists
     * immediately.
     */
    public synchronized void addAmount(final long amount, final GameTimeUnit unit) {
        if (amount == 0) {
            throw new InvalidTimeOperationException(MessageKey.ERROR_INVALID_OPERATION, "Amount must not be zero");
        }

        final long newTotal;
        try {
            newTotal = CalendarEngine.addAmount(totalGameSeconds, amount, unit, config.calendarStructure());
        } catch (final ArithmeticException exception) {
            throw new InvalidTimeOperationException(MessageKey.ERROR_INVALID_OPERATION, "Resulting time is out of range");
        }
        if (newTotal < 0) {
            throw new InvalidTimeOperationException(MessageKey.ERROR_INVALID_OPERATION, "Resulting time would be negative");
        }

        final long before = totalGameSeconds;
        this.totalGameSeconds = newTotal;
        fireRolloverEvents(before, newTotal);
        persistNow();
    }

    /**
     * Pauses the calendar, halting further advancement until {@link #resume()}
     * is called. Persists immediately.
     */
    public synchronized void pause() {
        if (paused) {
            throw new InvalidTimeOperationException(MessageKey.ERROR_INVALID_OPERATION, "The calendar is already paused");
        }
        this.paused = true;
        persistNow();
    }

    /**
     * Resumes a paused calendar. Persists immediately.
     */
    public synchronized void resume() {
        if (!paused) {
            throw new InvalidTimeOperationException(MessageKey.ERROR_INVALID_OPERATION, "The calendar is not paused");
        }
        this.paused = false;
        persistNow();
    }

    /**
     * Flushes the current calendar position to storage asynchronously, so
     * the tick listener or command that triggered it is never blocked on
     * disk I/O. Used for routine persistence during normal operation.
     */
    public void persistNow() {
        final PersistedGameTime state = currentPersistedState();
        scheduler.runTaskAsynchronously(() -> save(state));
    }

    /**
     * Flushes the current calendar position to storage synchronously,
     * blocking until it completes. Only for plugin shutdown, where a
     * dispatched async write isn't guaranteed to finish before the JVM exits.
     */
    public void persistNowBlocking() {
        save(currentPersistedState());
    }

    private PersistedGameTime currentPersistedState() {
        return new PersistedGameTime(totalGameSeconds, paused);
    }

    private void save(final PersistedGameTime state) {
        repository.save(state);
        if (config.debug()) {
            logger.info("Persisted Chronos time: " + state.totalGameSeconds() + " game seconds (paused=" + state.paused() + ")");
        }
    }

    private void applyDelta(final long secondsToAdd) {
        final long before = totalGameSeconds;
        final long after;
        try {
            after = Math.addExact(before, secondsToAdd);
        } catch (final ArithmeticException exception) {
            logger.severe("Chronos time overflowed a long; halting further advancement");
            return;
        }
        this.totalGameSeconds = after;
        fireRolloverEvents(before, after);
    }

    private void fireRolloverEvents(final long before, final long after) {
        final CalendarStructure structure = config.calendarStructure();
        if (CalendarEngine.unitIndex(before, GameTimeUnit.MINUTE, structure) == CalendarEngine.unitIndex(after, GameTimeUnit.MINUTE, structure)) {
            return;
        }

        final GameDateTime beforeDateTime = CalendarEngine.toDateTime(before, structure);
        final GameDateTime afterDateTime = CalendarEngine.toDateTime(after, structure);

        // Called inline: both triggers of a time change (the global tick
        // listener and an admin's /chronos command) already run on a
        // tick-owning thread, so these plain synchronous events can be
        // fired directly with no scheduler hop.
        pluginManager.callEvent(new TimeMinuteChangeEvent(beforeDateTime, afterDateTime));
        if (CalendarEngine.unitIndex(before, GameTimeUnit.HOUR, structure) != CalendarEngine.unitIndex(after, GameTimeUnit.HOUR, structure)) {
            pluginManager.callEvent(new TimeHourChangeEvent(beforeDateTime, afterDateTime));
        }
        if (CalendarEngine.unitIndex(before, GameTimeUnit.DAY, structure) != CalendarEngine.unitIndex(after, GameTimeUnit.DAY, structure)) {
            pluginManager.callEvent(new TimeDayChangeEvent(beforeDateTime, afterDateTime));
        }
        if (CalendarEngine.unitIndex(before, GameTimeUnit.WEEK, structure) != CalendarEngine.unitIndex(after, GameTimeUnit.WEEK, structure)) {
            pluginManager.callEvent(new TimeWeekChangeEvent(beforeDateTime, afterDateTime));
        }
        if (CalendarEngine.unitIndex(before, GameTimeUnit.MONTH, structure) != CalendarEngine.unitIndex(after, GameTimeUnit.MONTH, structure)) {
            pluginManager.callEvent(new TimeMonthChangeEvent(beforeDateTime, afterDateTime));
        }
        if (CalendarEngine.unitIndex(before, GameTimeUnit.YEAR, structure) != CalendarEngine.unitIndex(after, GameTimeUnit.YEAR, structure)) {
            pluginManager.callEvent(new TimeYearChangeEvent(beforeDateTime, afterDateTime));
        }
    }

    @Override
    public GameDateTime getCurrentDateTime() {
        return CalendarEngine.toDateTime(totalGameSeconds, config.calendarStructure());
    }

    @Override
    public GameDate getCurrentDate() {
        return getCurrentDateTime().date();
    }

    @Override
    public GameTime getCurrentTime() {
        return getCurrentDateTime().time();
    }

    @Override
    public int getYear() {
        return getCurrentDate().year();
    }

    @Override
    public int getMonth() {
        return getCurrentDate().month();
    }

    @Override
    public int getWeek() {
        return getCurrentDate().week();
    }

    @Override
    public int getDay() {
        return getCurrentDate().day();
    }

    @Override
    public int getDayOfWeek() {
        return getCurrentDate().dayOfWeek();
    }

    @Override
    public int getHour() {
        return getCurrentTime().hour();
    }

    @Override
    public int getMinute() {
        return getCurrentTime().minute();
    }

    @Override
    public int getSecond() {
        return getCurrentTime().second();
    }

    @Override
    public String getMonthName(final int month) {
        return config.calendarStructure().monthName(month);
    }

    @Override
    public String getDayOfWeekName(final int dayOfWeek) {
        return config.calendarStructure().dayOfWeekName(dayOfWeek);
    }

    @Override
    public int getMonthsPerYear() {
        return config.calendarStructure().monthsPerYear();
    }

    @Override
    public int getWeeksPerMonth() {
        return config.calendarStructure().weeksPerMonth();
    }

    @Override
    public int getDaysPerWeek() {
        return config.calendarStructure().daysPerWeek();
    }

    @Override
    public int getHoursPerDay() {
        return config.calendarStructure().hoursPerDay();
    }

    @Override
    public int getMinutesPerHour() {
        return config.calendarStructure().minutesPerHour();
    }

    @Override
    public int getSecondsPerMinute() {
        return config.calendarStructure().secondsPerMinute();
    }

    @Override
    public boolean isBefore(final GameDateTime other) {
        return getCurrentDateTime().isBefore(other);
    }

    @Override
    public boolean isAfter(final GameDateTime other) {
        return getCurrentDateTime().isAfter(other);
    }

    @Override
    public boolean isSameDay(final GameDateTime other) {
        return getCurrentDateTime().isSameDay(other);
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public UUID registerCalendarEvent(final CalendarEvent event) {
        return calendarEventService.registerEvent(event);
    }

    @Override
    public boolean unregisterCalendarEvent(final UUID id) {
        return calendarEventService.unregisterEvent(id);
    }

    @Override
    public List<UpcomingCalendarEvent> getUpcomingCalendarEvents(final int limit) {
        return calendarEventService.getUpcomingEvents(getCurrentDateTime(), limit);
    }

    @Override
    public List<CalendarEvent> getCalendarEventsOn(final int year, final int month, final int day) {
        return calendarEventService.getEventsOn(year, month, day);
    }

    @Override
    public GameDateTime getDateTimeAt(final long totalGameSeconds) {
        return CalendarEngine.toDateTime(totalGameSeconds, config.calendarStructure());
    }
}
