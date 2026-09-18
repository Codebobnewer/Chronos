package me.goga221.chronos.service;

import lombok.RequiredArgsConstructor;
import me.goga221.chronos.api.CalendarEvent;
import me.goga221.chronos.api.GameDateTime;
import me.goga221.chronos.api.UpcomingCalendarEvent;
import me.goga221.chronos.calendar.CalendarEngine;
import me.goga221.chronos.calendar.CalendarStructure;
import me.goga221.chronos.config.ChronosConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds calendar events registered through {@link me.goga221.chronos.api.ChronosAPI},
 * and computes their upcoming occurrences on request.
 * <p>
 * Registrations are in-memory only, on the same basis as most Bukkit
 * service registrations (e.g. PlaceholderAPI expansions): a registering
 * plugin re-registers its events on every enable, so nothing needs to
 * survive a Chronos restart independently of its registrant.
 */
@RequiredArgsConstructor
public final class CalendarEventService {

    private final ChronosConfig config;

    private final Map<UUID, CalendarEvent> events = new ConcurrentHashMap<>();

    /**
     * Registers a calendar event, validating its schedule against the
     * active calendar structure before accepting it.
     *
     * @throws IllegalArgumentException if the event's schedule is out of range
     */
    public UUID registerEvent(final CalendarEvent event) {
        validate(event);
        final UUID id = UUID.randomUUID();
        events.put(id, event);
        return id;
    }

    /**
     * Unregisters a previously registered event.
     *
     * @return true if an event with that id was registered and has now been removed
     */
    public boolean unregisterEvent(final UUID id) {
        return events.remove(id) != null;
    }

    /**
     * Returns up to {@code limit} registered events, soonest occurrence
     * first, excluding one-time events that have already passed.
     */
    public List<UpcomingCalendarEvent> getUpcomingEvents(final GameDateTime now, final int limit) {
        if (limit <= 0) {
            return List.of();
        }

        final CalendarStructure structure = config.calendarStructure();
        return events.entrySet().stream()
                .map(entry -> toUpcoming(entry.getKey(), entry.getValue(), now.totalGameSeconds(), structure))
                .flatMap(Optional::stream)
                .sorted()
                .limit(limit)
                .toList();
    }

    /**
     * Returns every registered event that falls on the given calendar date —
     * a recurring event whose month/day matches, or a one-time event whose
     * year/month/day all match.
     */
    public List<CalendarEvent> getEventsOn(final int year, final int month, final int day) {
        return events.values().stream()
                .filter(event -> event.month() == month && event.day() == day)
                .filter(event -> event.isRecurring() || event.year() == year)
                .toList();
    }

    private Optional<UpcomingCalendarEvent> toUpcoming(final UUID id, final CalendarEvent event,
                                                         final long currentTotalSeconds, final CalendarStructure structure) {
        final OptionalLong occurrenceSeconds = CalendarEngine.nextOccurrenceSeconds(event, currentTotalSeconds, structure);
        if (occurrenceSeconds.isEmpty()) {
            return Optional.empty();
        }
        final GameDateTime nextOccurrence = CalendarEngine.toDateTime(occurrenceSeconds.getAsLong(), structure);
        return Optional.of(new UpcomingCalendarEvent(id, event, nextOccurrence));
    }

    private void validate(final CalendarEvent event) {
        final int yearForValidation = event.isRecurring() ? 1 : event.year();
        try {
            CalendarEngine.toTotalSeconds(yearForValidation, event.month(), event.day(),
                    event.hour(), event.minute(), event.second(), config.calendarStructure());
        } catch (final IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid schedule for calendar event \"" + event.name() + "\": " + exception.getMessage(), exception);
        }
    }
}
