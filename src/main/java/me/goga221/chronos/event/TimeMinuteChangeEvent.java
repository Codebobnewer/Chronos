package me.goga221.chronos.event;

import me.goga221.chronos.api.GameDateTime;
import org.bukkit.event.HandlerList;

/**
 * Fired when the in-game minute changes.
 */
public final class TimeMinuteChangeEvent extends TimeChangeEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public TimeMinuteChangeEvent(final GameDateTime previous, final GameDateTime current) {
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
