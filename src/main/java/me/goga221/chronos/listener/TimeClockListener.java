package me.goga221.chronos.listener;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import me.goga221.chronos.Chronos;
import me.goga221.chronos.service.TimeService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Drives {@link TimeService} forward using Paper's global server tick
 * event, rather than a self-scheduled repeating task.
 * <p>
 * {@link ServerTickStartEvent} fires once per tick of Folia's single global
 * region (the one region-independent tick loop Folia always runs at a
 * steady rate), so listening for it is both an appropriate event-based
 * trigger and reliably synchronous — there is no need to hand this off to
 * UniversalScheduler.
 */
public final class TimeClockListener implements Listener {

    private final long tickIntervalTicks = Chronos.getChronosConfig().tickIntervalTicks();

    private long ticksSinceLastAdvance;
    private long lastAdvanceNanos;

    /**
     * Marks the starting point for real-time measurement. Call once, right
     * before registering this listener.
     */
    public void start() {
        this.lastAdvanceNanos = System.nanoTime();
    }

    @EventHandler
    public void onServerTickStart(final ServerTickStartEvent event) {
        ticksSinceLastAdvance++;
        if (ticksSinceLastAdvance < tickIntervalTicks) {
            return;
        }
        ticksSinceLastAdvance = 0;

        final long now = System.nanoTime();
        final double realSecondsElapsed = (now - lastAdvanceNanos) / 1_000_000_000.0;
        lastAdvanceNanos = now;
        Chronos.getTimeService().tick(realSecondsElapsed);
    }
}
