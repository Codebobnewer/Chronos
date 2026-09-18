package me.goga221.chronos.api;

import java.util.List;
import java.util.UUID;

/**
 * Public, read-only view of the Chronos in-game calendar.
 * <p>
 * Other plugins obtain an instance through {@link ChronosProvider#get()} and
 * should depend only on this interface and the value types in this package
 * ({@link GameDate}, {@link GameTime}, {@link GameDateTime}) — never on
 * Chronos's internal services, repositories or configuration.
 */
public interface ChronosAPI {

    /**
     * Returns the current in-game date and time.
     */
    GameDateTime getCurrentDateTime();

    /**
     * Returns the current in-game date.
     */
    GameDate getCurrentDate();

    /**
     * Returns the current in-game time of day.
     */
    GameTime getCurrentTime();

    /**
     * Returns the current in-game year.
     */
    int getYear();

    /**
     * Returns the current in-game month (1-based).
     */
    int getMonth();

    /**
     * Returns the current in-game week within the month (1-based).
     */
    int getWeek();

    /**
     * Returns the current in-game day within the month (1-based).
     */
    int getDay();

    /**
     * Returns the current in-game day within the week (1-based).
     */
    int getDayOfWeek();

    /**
     * Returns the current in-game hour (0-based).
     */
    int getHour();

    /**
     * Returns the current in-game minute (0-based).
     */
    int getMinute();

    /**
     * Returns the current in-game second (0-based).
     */
    int getSecond();

    /**
     * Returns the configured display name of the given 1-based month.
     *
     * @throws IllegalArgumentException if the month is out of range
     */
    String getMonthName(int month);

    /**
     * Returns the configured display name of the given 1-based day of week.
     *
     * @throws IllegalArgumentException if the day of week is out of range
     */
    String getDayOfWeekName(int dayOfWeek);

    /**
     * Returns how many months make up a year under the active calendar structure.
     */
    int getMonthsPerYear();

    /**
     * Returns how many weeks make up a month under the active calendar structure.
     */
    int getWeeksPerMonth();

    /**
     * Returns how many days make up a week under the active calendar structure.
     */
    int getDaysPerWeek();

    /**
     * Returns how many hours make up a day under the active calendar structure.
     */
    int getHoursPerDay();

    /**
     * Returns how many minutes make up an hour under the active calendar structure.
     */
    int getMinutesPerHour();

    /**
     * Returns how many seconds make up a minute under the active calendar structure.
     */
    int getSecondsPerMinute();

    /**
     * Returns whether the current in-game time is strictly before {@code other}.
     */
    boolean isBefore(GameDateTime other);

    /**
     * Returns whether the current in-game time is strictly after {@code other}.
     */
    boolean isAfter(GameDateTime other);

    /**
     * Returns whether the current in-game time falls on the same calendar day as {@code other}.
     */
    boolean isSameDay(GameDateTime other);

    /**
     * Returns whether the calendar is currently paused.
     */
    boolean isPaused();

    /**
     * Registers a calendar event — one-time or annually recurring — for
     * display in Chronos's calendar GUI and for other plugins to query.
     * Registrations are in-memory only; re-register on every enable, the
     * same way you would with any other Bukkit service registration.
     *
     * @return a handle that can be passed to {@link #unregisterCalendarEvent(UUID)}
     * @throws IllegalArgumentException if the event's schedule is not valid under the active calendar structure
     */
    UUID registerCalendarEvent(CalendarEvent event);

    /**
     * Unregisters a previously registered calendar event.
     *
     * @return true if an event with that id was registered and has now been removed
     */
    boolean unregisterCalendarEvent(UUID id);

    /**
     * Returns up to {@code limit} registered calendar events, soonest
     * occurrence first, excluding one-time events that have already passed.
     */
    List<UpcomingCalendarEvent> getUpcomingCalendarEvents(int limit);

    /**
     * Returns every registered calendar event that falls on the given date —
     * a recurring event whose month/day matches, or a one-time event whose
     * year/month/day all match.
     */
    List<CalendarEvent> getCalendarEventsOn(int year, int month, int day);

    /**
     * Converts an absolute total-game-seconds value back into a full
     * calendar breakdown. Useful for a plugin that has computed some future
     * or past instant of its own (e.g. {@code getCurrentDateTime()
     * .totalGameSeconds() + someInterval}) and needs it as a date/time —
     * to pass to {@link #registerCalendarEvent(CalendarEvent)}, for
     * instance — without duplicating Chronos's own calendar math.
     *
     * @throws IllegalArgumentException if totalGameSeconds is negative
     */
    GameDateTime getDateTimeAt(long totalGameSeconds);
}
