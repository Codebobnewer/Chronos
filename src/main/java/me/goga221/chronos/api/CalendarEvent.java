package me.goga221.chronos.api;

/**
 * A named point on the calendar that other plugins can register through
 * {@link ChronosAPI#registerCalendarEvent(CalendarEvent)} for display in
 * Chronos's calendar GUI and for other plugins to query.
 * <p>
 * An event with a {@code null} {@link #year()} recurs every year on the
 * same month/day/time; an event with a year set occurs exactly once.
 *
 * @param name        display name
 * @param description short flavor text; never {@code null}, defaults to empty
 * @param year        1-based year, or {@code null} to recur annually
 * @param month       1-based month
 * @param day         1-based day within the month
 * @param hour        0-based hour
 * @param minute      0-based minute
 * @param second      0-based second
 */
public record CalendarEvent(
        String name,
        String description,
        Integer year,
        int month,
        int day,
        int hour,
        int minute,
        int second
) {

    public CalendarEvent {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Calendar event name must not be blank");
        }
        if (description == null) {
            description = "";
        }
        if (year != null && year < 1) {
            throw new IllegalArgumentException("Year must be at least 1: " + year);
        }
        if (month < 1) {
            throw new IllegalArgumentException("Month must be at least 1: " + month);
        }
        if (day < 1) {
            throw new IllegalArgumentException("Day must be at least 1: " + day);
        }
        if (hour < 0 || minute < 0 || second < 0) {
            throw new IllegalArgumentException("Hour, minute, and second must not be negative");
        }
    }

    /**
     * Creates an event that recurs every year at midnight on the given month/day.
     */
    public static CalendarEvent recurringAnnual(final String name, final String description, final int month, final int day) {
        return new CalendarEvent(name, description, null, month, day, 0, 0, 0);
    }

    /**
     * Creates an event that occurs exactly once, at midnight on the given date.
     */
    public static CalendarEvent oneTime(final String name, final String description, final int year, final int month, final int day) {
        return new CalendarEvent(name, description, year, month, day, 0, 0, 0);
    }

    /**
     * Returns whether this event recurs every year, as opposed to occurring
     * once on a specific {@link #year()}.
     */
    public boolean isRecurring() {
        return year == null;
    }
}
