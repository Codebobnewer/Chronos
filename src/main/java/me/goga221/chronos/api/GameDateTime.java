package me.goga221.chronos.api;

/**
 * An immutable combined date and time snapshot on the in-game calendar.
 * <p>
 * {@code totalGameSeconds} is the authoritative value for ordering and
 * equality of instants; {@link #date()} and {@link #time()} are its
 * human-readable decomposition under the calendar structure active when
 * this snapshot was produced.
 *
 * @param date             the calendar date component
 * @param time             the time-of-day component
 * @param totalGameSeconds elapsed game seconds since the calendar epoch
 */
public record GameDateTime(GameDate date, GameTime time, long totalGameSeconds) implements Comparable<GameDateTime> {

    /**
     * Returns whether this instant is strictly before {@code other}.
     */
    public boolean isBefore(final GameDateTime other) {
        return totalGameSeconds < other.totalGameSeconds;
    }

    /**
     * Returns whether this instant is strictly after {@code other}.
     */
    public boolean isAfter(final GameDateTime other) {
        return totalGameSeconds > other.totalGameSeconds;
    }

    /**
     * Returns whether this instant falls on the same calendar day as {@code other}.
     */
    public boolean isSameDay(final GameDateTime other) {
        return date.isSameDay(other.date);
    }

    @Override
    public int compareTo(final GameDateTime other) {
        return Long.compare(totalGameSeconds, other.totalGameSeconds);
    }
}
