package me.goga221.chronos.event;

import me.goga221.chronos.api.GameDateTime;
import org.bukkit.event.HandlerList;

/**
 * Fired when the in-game week changes.
 */
public final class TimeWeekChangeEvent extends TimeChangeEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public TimeWeekChangeEvent(final GameDateTime previous, final GameDateTime current) {
        super(previous, current);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
