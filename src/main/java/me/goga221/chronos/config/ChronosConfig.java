package me.goga221.chronos.config;

import me.goga221.chronos.calendar.CalendarStructure;

/**
 * Fully parsed and validated {@code config.yml} contents.
 *
 * @param calendarStructure       the configured shape of the calendar
 * @param gameSecondsPerRealSecond how many game seconds pass per real second
 * @param tickIntervalTicks       how often, in server ticks, the clock advances
 * @param startYear               starting year used only when no persisted state exists
 * @param startMonth              starting month used only when no persisted state exists
 * @param startDay                starting day used only when no persisted state exists
 * @param startHour               starting hour used only when no persisted state exists
 * @param startMinute             starting minute used only when no persisted state exists
 * @param startSecond             starting second used only when no persisted state exists
 * @param persistenceFileName     the SQLite database file name, relative to the plugin data folder
 * @param persistIntervalSeconds  how often, in real seconds, in-memory state is flushed to storage
 * @param debug                   whether to log verbose diagnostic information
 */
public record ChronosConfig(
        CalendarStructure calendarStructure,
        double gameSecondsPerRealSecond,
        long tickIntervalTicks,
        int startYear,
        int startMonth,
        int startDay,
        int startHour,
        int startMinute,
        int startSecond,
        String persistenceFileName,
        long persistIntervalSeconds,
        boolean debug
) {

    public ChronosConfig {
        if (gameSecondsPerRealSecond <= 0.0) {
            throw new IllegalArgumentException("time-conversion.game-seconds-per-real-second must be positive");
        }
        if (tickIntervalTicks < 1) {
            throw new IllegalArgumentException("time-conversion.tick-interval-ticks must be at least 1");
        }
        if (persistIntervalSeconds < 1) {
            throw new IllegalArgumentException("persistence.persist-interval-seconds must be at least 1");
        }
        if (persistenceFileName == null || persistenceFileName.isBlank()) {
            throw new IllegalArgumentException("persistence.file-name must not be blank");
        }
    }
}
