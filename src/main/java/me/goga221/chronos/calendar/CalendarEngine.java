package me.goga221.chronos.calendar;

import me.goga221.chronos.api.CalendarEvent;
import me.goga221.chronos.api.GameDate;
import me.goga221.chronos.api.GameDateTime;
import me.goga221.chronos.api.GameTime;

import java.util.OptionalLong;

/**
 * Pure, stateless conversions between total elapsed game seconds and
 * calendar-shaped values, under a given {@link CalendarStructure}.
 * <p>
 * Total game seconds is the single source of truth for time in this plugin;
 * every other representation is derived from it by this engine.
 */
public final class CalendarEngine {

    private CalendarEngine() {
    }

    /**
     * Decomposes total elapsed game seconds into a full {@link GameDateTime}
     * under the given calendar structure.
     */
    public static GameDateTime toDateTime(final long totalGameSeconds, final CalendarStructure structure) {
        if (totalGameSeconds < 0) {
            throw new IllegalArgumentException("Total game seconds must not be negative: " + totalGameSeconds);
        }

        long remaining = totalGameSeconds;
        final int second = (int) (remaining % structure.secondsPerMinute());
        remaining /= structure.secondsPerMinute();
        final int minute = (int) (remaining % structure.minutesPerHour());
        remaining /= structure.minutesPerHour();
        final int hour = (int) (remaining % structure.hoursPerDay());
        remaining /= structure.hoursPerDay();

        final int daysPerMonth = structure.daysPerMonth();
        final int dayIndex = (int) (remaining % daysPerMonth);
        remaining /= daysPerMonth;
        final int monthIndex = (int) (remaining % structure.monthsPerYear());
        remaining /= structure.monthsPerYear();
        final int year = (int) remaining + 1;

        final int day = dayIndex + 1;
        final int week = (dayIndex / structure.daysPerWeek()) + 1;
        final int dayOfWeek = (dayIndex % structure.daysPerWeek()) + 1;
        final int month = monthIndex + 1;

        final GameDate date = new GameDate(year, month, week, day, dayOfWeek);
        final GameTime time = new GameTime(hour, minute, second);
        return new GameDateTime(date, time, totalGameSeconds);
    }

    /**
     * Converts calendar field values into total elapsed game seconds under
     * the given calendar structure.
     *
     * @throws IllegalArgumentException if any field is out of range for the structure
     */
    public static long toTotalSeconds(final int year, final int month, final int day,
                                       final int hour, final int minute, final int second,
                                       final CalendarStructure structure) {
        if (year < 1) {
            throw new IllegalArgumentException("Year must be at least 1: " + year);
        }
        if (month < 1 || month > structure.monthsPerYear()) {
            throw new IllegalArgumentException("Month out of range: " + month);
        }
        if (day < 1 || day > structure.daysPerMonth()) {
            throw new IllegalArgumentException("Day out of range: " + day);
        }
        if (hour < 0 || hour >= structure.hoursPerDay()) {
            throw new IllegalArgumentException("Hour out of range: " + hour);
        }
        if (minute < 0 || minute >= structure.minutesPerHour()) {
            throw new IllegalArgumentException("Minute out of range: " + minute);
        }
        if (second < 0 || second >= structure.secondsPerMinute()) {
            throw new IllegalArgumentException("Second out of range: " + second);
        }

        long total = year - 1;
        total = Math.addExact(Math.multiplyExact(total, structure.monthsPerYear()), month - 1);
        total = Math.addExact(Math.multiplyExact(total, structure.daysPerMonth()), day - 1);
        total = Math.addExact(Math.multiplyExact(total, structure.hoursPerDay()), hour);
        total = Math.addExact(Math.multiplyExact(total, structure.minutesPerHour()), minute);
        total = Math.addExact(Math.multiplyExact(total, structure.secondsPerMinute()), second);
        return total;
    }

    /**
     * Returns the fixed length, in seconds, of the given unit under the
     * given calendar structure.
     */
    public static long unitLengthInSeconds(final GameTimeUnit unit, final CalendarStructure structure) {
        return unit.lengthInSeconds(structure);
    }

    /**
     * Returns {@code totalGameSeconds} plus {@code amount} whole units of
     * {@code unit}, under the given calendar structure.
     *
     * @throws ArithmeticException if the result overflows a {@code long}
     */
    public static long addAmount(final long totalGameSeconds, final long amount,
                                  final GameTimeUnit unit, final CalendarStructure structure) {
        final long unitSeconds = unitLengthInSeconds(unit, structure);
        final long secondsToAdd = Math.multiplyExact(amount, unitSeconds);
        return Math.addExact(totalGameSeconds, secondsToAdd);
    }

    /**
     * Returns how many whole units of {@code unit} have elapsed by
     * {@code totalGameSeconds}, used to detect rollovers by comparing this
     * value before and after a time change.
     */
    public static long unitIndex(final long totalGameSeconds, final GameTimeUnit unit, final CalendarStructure structure) {
        return totalGameSeconds / unitLengthInSeconds(unit, structure);
    }

    /**
     * Returns the total game seconds of {@code event}'s next occurrence at
     * or after {@code currentTotalSeconds}, under the given calendar
     * structure.
     * <p>
     * For a recurring event, this is this year's occurrence if it hasn't
     * passed yet, otherwise next year's. For a one-time event, this is its
     * fixed date if it hasn't passed yet, otherwise empty — a one-time
     * event that has already occurred never occurs again.
     */
    public static OptionalLong nextOccurrenceSeconds(final CalendarEvent event, final long currentTotalSeconds, final CalendarStructure structure) {
        if (!event.isRecurring()) {
            final long occurrence = toTotalSeconds(event.year(), event.month(), event.day(),
                    event.hour(), event.minute(), event.second(), structure);
            return occurrence >= currentTotalSeconds ? OptionalLong.of(occurrence) : OptionalLong.empty();
        }

        final int currentYear = toDateTime(currentTotalSeconds, structure).date().year();
        final long thisYearOccurrence = toTotalSeconds(currentYear, event.month(), event.day(),
                event.hour(), event.minute(), event.second(), structure);
        if (thisYearOccurrence >= currentTotalSeconds) {
            return OptionalLong.of(thisYearOccurrence);
        }

        final long nextYearOccurrence = toTotalSeconds(currentYear + 1, event.month(), event.day(),
                event.hour(), event.minute(), event.second(), structure);
        return OptionalLong.of(nextYearOccurrence);
    }
}
