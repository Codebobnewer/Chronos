package me.goga221.chronos.api;

import java.util.UUID;

/**
 * A registered {@link CalendarEvent} paired with the computed date/time of
 * its next occurrence, as returned by
 * {@link ChronosAPI#getUpcomingCalendarEvents(int)}.
 *
 * @param id             the handle returned by {@link ChronosAPI#registerCalendarEvent(CalendarEvent)}
 * @param event          the registered event definition
 * @param nextOccurrence when this event next occurs
 */
public record UpcomingCalendarEvent(UUID id, CalendarEvent event, GameDateTime nextOccurrence) implements Comparable<UpcomingCalendarEvent> {

    @Override
    public int compareTo(final UpcomingCalendarEvent other) {
        return nextOccurrence.compareTo(other.nextOccurrence);
    }
}
