package me.goga221.chronos.listener;

import me.goga221.chronos.Chronos;
import me.goga221.chronos.event.TimeMinuteChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Keeps the boss bar in sync with the calendar by refreshing it on every
 * in-game minute change — reusing Chronos's own rollover event rather than
 * a separate scheduled task, and naturally matching the bar's own
 * minute-level display granularity.
 */
public final class BossBarRefreshListener implements Listener {

    @EventHandler
    public void onMinuteChange(final TimeMinuteChangeEvent event) {
        Chronos.getBossBarService().refresh();
    }
}
