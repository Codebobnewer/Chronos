package me.goga221.chronos.storage;

/**
 * The durable representation of the calendar's current position, stored
 * independently of any calendar structure so that structure changes between
 * restarts reinterpret the same instant consistently.
 *
 * @param totalGameSeconds elapsed game seconds since the calendar epoch
 * @param paused           whether the calendar was paused when this was saved
 */
public record PersistedGameTime(long totalGameSeconds, boolean paused) {
}
