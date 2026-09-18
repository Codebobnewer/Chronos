package me.goga221.chronos.listener;

import me.goga221.chronos.Chronos;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Shows the persistent time boss bar to players as they join, since a
 * newly connecting player is never among the online players iterated when
 * the bar was first created.
 */
public final class BossBarJoinListener implements Listener {

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        Chronos.getBossBarService().show(event.getPlayer());
    }
}
