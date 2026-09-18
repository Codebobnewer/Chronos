package me.goga221.chronos.service;

import lombok.RequiredArgsConstructor;
import me.goga221.chronos.api.ChronosAPI;
import me.goga221.chronos.message.MessageKey;
import me.goga221.chronos.message.MessageService;
import me.goga221.chronos.util.GameDateTimeFormat;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Maintains a boss bar shown to every online player with the current date
 * and time, its progress representing how much of the current day has
 * elapsed.
 * <p>
 * Bukkit's traditional Scoreboard API is not supported on Folia (a shared,
 * globally-mutable scoreboard object doesn't fit its per-region threading
 * model); Adventure's {@link BossBar} is inherently per-viewer and
 * packet-based, so it works natively on both Paper and Folia with no extra
 * dependency.
 */
@RequiredArgsConstructor
public final class TimeBossBarService {

    private final ChronosAPI api;
    private final MessageService messages;

    private BossBar bossBar;

    /**
     * Creates the boss bar and shows it to every currently online player.
     */
    public void initialize() {
        this.bossBar = BossBar.bossBar(renderText(), computeDayProgress(), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        for (final Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bossBar);
        }
    }

    /**
     * Shows the boss bar to a single player, e.g. one who just joined.
     */
    public void show(final Player player) {
        if (bossBar != null) {
            player.showBossBar(bossBar);
        }
    }

    /**
     * Recomputes the boss bar's text and progress from the current calendar state.
     */
    public void refresh() {
        if (bossBar == null) {
            return;
        }
        bossBar.name(renderText());
        bossBar.progress(computeDayProgress());
    }

    /**
     * Hides the boss bar from every currently online player. Called on plugin disable.
     */
    public void shutdown() {
        if (bossBar == null) {
            return;
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    private Component renderText() {
        return messages.renderPlain(MessageKey.BOSSBAR_TEXT,
                Placeholder.unparsed("day", String.valueOf(api.getDay())),
                Placeholder.unparsed("month_name", api.getMonthName(api.getMonth())),
                Placeholder.unparsed("year", String.valueOf(api.getYear())),
                Placeholder.unparsed("hour", GameDateTimeFormat.pad(api.getHour())),
                Placeholder.unparsed("minute", GameDateTimeFormat.pad(api.getMinute())),
                Placeholder.unparsed("pause_state", api.isPaused() ? " (Paused)" : ""));
    }

    private float computeDayProgress() {
        final long secondsPerHour = (long) api.getMinutesPerHour() * api.getSecondsPerMinute();
        final long secondsPerDay = (long) api.getHoursPerDay() * secondsPerHour;
        final long elapsedSeconds = (long) api.getHour() * secondsPerHour
                + (long) api.getMinute() * api.getSecondsPerMinute()
                + api.getSecond();

        final float progress = (float) elapsedSeconds / secondsPerDay;
        return Math.max(BossBar.MIN_PROGRESS, Math.min(BossBar.MAX_PROGRESS, progress));
    }
}
