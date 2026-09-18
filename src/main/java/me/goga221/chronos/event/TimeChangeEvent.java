package me.goga221.chronos.event;

import me.goga221.chronos.api.GameDateTime;
import org.bukkit.event.Event;

/**
 * Base type for events fired when the Chronos calendar crosses a unit
 * boundary (minute, hour, day, week, month, or year).
 * <p>
 * Fired synchronously — either from the global-region tick thread (routine
 * clock advancement) or from whichever thread an admin's {@code /chronos}
 * command executed on. Either way it is a tick-owning thread, never a
 * specific player's or world's region, so a listener that needs to touch a
 * particular {@code Player}, {@code Entity}, or {@code World} must still
 * re-dispatch that work through the appropriate Folia scheduler itself.
 */
public abstract class TimeChangeEvent extends Event {

    private final GameDateTime previous;
    private final GameDateTime current;

    protected TimeChangeEvent(final GameDateTime previous, final GameDateTime current) {
        this.previous = previous;
        this.current = current;
    }

    /**
     * Returns the calendar position immediately before this change.
     */
    public final GameDateTime getPrevious() {
        return previous;
    }

    /**
     * Returns the calendar position immediately after this change.
     */
    public final GameDateTime getCurrent() {
        return current;
    }
}
