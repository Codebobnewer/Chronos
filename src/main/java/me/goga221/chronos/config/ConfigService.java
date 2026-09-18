package me.goga221.chronos.config;

import me.goga221.chronos.calendar.CalendarEngine;
import me.goga221.chronos.calendar.CalendarStructure;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads and validates {@code config.yml} into an immutable {@link ChronosConfig}.
 */
public final class ConfigService {

    private final ChronosConfig config;

    public ConfigService(final JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        this.config = parse(plugin.getConfig());
    }

    /**
     * Returns the parsed configuration.
     */
    public ChronosConfig config() {
        return config;
    }

    private static ChronosConfig parse(final FileConfiguration raw) {
        try {
            final CalendarStructure structure = new CalendarStructure(
                    raw.getInt("calendar.months-per-year"),
                    raw.getInt("calendar.weeks-per-month"),
                    raw.getInt("calendar.days-per-week"),
                    raw.getInt("calendar.hours-per-day"),
                    raw.getInt("calendar.minutes-per-hour"),
                    raw.getInt("calendar.seconds-per-minute"),
                    raw.getStringList("calendar.month-names"),
                    raw.getStringList("calendar.day-names")
            );

            final int startYear = raw.getInt("starting-date-time.year");
            final int startMonth = raw.getInt("starting-date-time.month");
            final int startDay = raw.getInt("starting-date-time.day");
            final int startHour = raw.getInt("starting-date-time.hour");
            final int startMinute = raw.getInt("starting-date-time.minute");
            final int startSecond = raw.getInt("starting-date-time.second");

            CalendarEngine.toTotalSeconds(startYear, startMonth, startDay, startHour, startMinute, startSecond, structure);

            return new ChronosConfig(
                    structure,
                    raw.getDouble("time-conversion.game-seconds-per-real-second"),
                    raw.getLong("time-conversion.tick-interval-ticks"),
                    startYear,
                    startMonth,
                    startDay,
                    startHour,
                    startMinute,
                    startSecond,
                    raw.getString("persistence.file-name", "chronos-time.db"),
                    raw.getLong("persistence.persist-interval-seconds"),
                    raw.getBoolean("debug")
            );
        } catch (final IllegalArgumentException exception) {
            throw new ConfigurationException("Invalid config.yml: " + exception.getMessage(), exception);
        }
    }
}
