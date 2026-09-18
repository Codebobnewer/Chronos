package me.goga221.chronos.api;

/**
 * An immutable snapshot of a point on the in-game calendar.
 * <p>
 * Instances are only ever produced by the calendar engine from a single
 * source of truth (total elapsed game seconds), so {@code week} and
 * {@code dayOfWeek} are always internally consistent with {@code day}.
 *
 * @param year      1-based calendar year
 * @param month     1-based month within the year
 * @param week      1-based week within the month
 * @param day       1-based day within the month
 * @param dayOfWeek 1-based day within the week
 */
public record GameDate(int year, int month, int week, int day, int dayOfWeek) implements Comparable<GameDate> {

    /**
     * Returns whether this date is strictly before {@code other}.
     */
    public boolean isBefore(final GameDate other) {
        return compareTo(other) < 0;
    }

    /**
     * Returns whether this date is strictly after {@code other}.
     */
    public boolean isAfter(final GameDate other) {
        return compareTo(other) > 0;
    }

    /**
     * Returns whether this date falls on the same day as {@code other}.
     */
    public boolean isSameDay(final GameDate other) {
        return year == other.year && month == other.month && day == other.day;
    }

    /**
     * Returns whether this date falls in the same month as {@code other}.
     */
    public boolean isSameMonth(final GameDate other) {
        return year == other.year && month == other.month;
    }

    /**
     * Returns whether this date falls in the same year as {@code other}.
     */
    public boolean isSameYear(final GameDate other) {
        return year == other.year;
    }

    @Override
    public int compareTo(final GameDate other) {
        if (year != other.year) {
            return Integer.compare(year, other.year);
        }
        if (month != other.month) {
            return Integer.compare(month, other.month);
        }
        return Integer.compare(day, other.day);
    }
}
