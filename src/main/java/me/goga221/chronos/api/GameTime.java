package me.goga221.chronos.api;

/**
 * An immutable time-of-day snapshot on the in-game calendar.
 *
 * @param hour   0-based hour within the day
 * @param minute 0-based minute within the hour
 * @param second 0-based second within the minute
 */
public record GameTime(int hour, int minute, int second) implements Comparable<GameTime> {

    /**
     * Returns whether this time is strictly before {@code other}.
     */
    public boolean isBefore(final GameTime other) {
        return compareTo(other) < 0;
    }

    /**
     * Returns whether this time is strictly after {@code other}.
     */
    public boolean isAfter(final GameTime other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(final GameTime other) {
        if (hour != other.hour) {
            return Integer.compare(hour, other.hour);
        }
        if (minute != other.minute) {
            return Integer.compare(minute, other.minute);
        }
        return Integer.compare(second, other.second);
    }
}
