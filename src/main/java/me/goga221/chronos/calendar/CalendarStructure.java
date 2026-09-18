package me.goga221.chronos.calendar;

import java.util.List;

/**
 * The configurable shape of the in-game calendar: how many of each smaller
 * unit make up the next larger one, plus display names for months and days
 * of the week. All months have the same fixed length, so every unit
 * ultimately converts to a whole number of seconds.
 */
public record CalendarStructure(
        int monthsPerYear,
        int weeksPerMonth,
        int daysPerWeek,
        int hoursPerDay,
        int minutesPerHour,
        int secondsPerMinute,
        List<String> monthNames,
        List<String> dayNames
) {

    public CalendarStructure {
        if (monthsPerYear < 1 || weeksPerMonth < 1 || daysPerWeek < 1
                || hoursPerDay < 1 || minutesPerHour < 1 || secondsPerMinute < 1) {
            throw new IllegalArgumentException("Calendar structure units must all be at least 1");
        }
        monthNames = List.copyOf(monthNames);
        dayNames = List.copyOf(dayNames);
        if (monthNames.size() != monthsPerYear) {
            throw new IllegalArgumentException("Expected " + monthsPerYear + " month names, got " + monthNames.size());
        }
        if (dayNames.size() != daysPerWeek) {
            throw new IllegalArgumentException("Expected " + daysPerWeek + " day names, got " + dayNames.size());
        }
    }

    /**
     * Returns the fixed number of days in every month.
     */
    public int daysPerMonth() {
        return weeksPerMonth * daysPerWeek;
    }

    /**
     * Returns the number of seconds in an hour.
     */
    public long secondsPerHour() {
        return (long) minutesPerHour * secondsPerMinute;
    }

    /**
     * Returns the number of seconds in a day.
     */
    public long secondsPerDay() {
        return (long) hoursPerDay * secondsPerHour();
    }

    /**
     * Returns the number of seconds in a week.
     */
    public long secondsPerWeek() {
        return (long) daysPerWeek * secondsPerDay();
    }

    /**
     * Returns the number of seconds in a month.
     */
    public long secondsPerMonth() {
        return (long) weeksPerMonth * secondsPerWeek();
    }

    /**
     * Returns the number of seconds in a year.
     */
    public long secondsPerYear() {
        return (long) monthsPerYear * secondsPerMonth();
    }

    /**
     * Returns the display name of the given 1-based month.
     */
    public String monthName(final int month) {
        if (month < 1 || month > monthsPerYear) {
            throw new IllegalArgumentException("Month out of range: " + month);
        }
        return monthNames.get(month - 1);
    }

    /**
     * Returns the display name of the given 1-based day of week.
     */
    public String dayOfWeekName(final int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > daysPerWeek) {
            throw new IllegalArgumentException("Day of week out of range: " + dayOfWeek);
        }
        return dayNames.get(dayOfWeek - 1);
    }
}
