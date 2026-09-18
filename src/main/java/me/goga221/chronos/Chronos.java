package me.goga221.chronos;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import me.goga221.chronos.api.ChronosAPI;
import me.goga221.chronos.command.TimeCommand;
import me.goga221.chronos.config.ChronosConfig;
import me.goga221.chronos.config.ConfigService;
import me.goga221.chronos.config.ConfigurationException;
import me.goga221.chronos.listener.BossBarJoinListener;
import me.goga221.chronos.listener.BossBarRefreshListener;
import me.goga221.chronos.listener.TimeClockListener;
import me.goga221.chronos.message.MessageService;
import me.goga221.chronos.service.CalendarEventService;
import me.goga221.chronos.service.TimeBossBarService;
import me.goga221.chronos.service.TimeService;
import me.goga221.chronos.storage.SQLiteTimeRepository;
import me.goga221.chronos.storage.TimeRepository;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Composition root for the Chronos plugin, and the static locator other
 * classes in this plugin use to reach its shared services (the pattern
 * this network's plugins use for internal wiring — see the "Service
 * access" section of CLAUDE.md). {@link #onEnable()} does the actual
 * construction; the static fields only ever hold what it built there.
 * <p>
 * This is a wiring/lifecycle convenience only — every shared service
 * remains its own properly encapsulated, constructor-injected instance;
 * nothing here holds business logic.
 */
public final class Chronos extends JavaPlugin {

    private static ChronosConfig config;
    private static MessageService messageService;
    private static TimeService timeService;
    private static TimeBossBarService bossBarService;
    private static TaskScheduler scheduler;

    private TimeRepository repository;

    @Override
    public void onEnable() {
        final ChronosConfig loadedConfig;
        try {
            loadedConfig = new ConfigService(this).config();
        } catch (final ConfigurationException exception) {
            getLogger().severe(exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        final MessageService loadedMessageService = new MessageService(this);

        final TimeRepository loadedRepository;
        try {
            loadedRepository = new SQLiteTimeRepository(new File(getDataFolder(), loadedConfig.persistenceFileName()));
        } catch (final IllegalStateException exception) {
            getLogger().severe(exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.repository = loadedRepository;

        final TaskScheduler loadedScheduler = UniversalScheduler.getScheduler(this);
        final CalendarEventService calendarEventService = new CalendarEventService(loadedConfig);
        final TimeService loadedTimeService = new TimeService(loadedConfig, loadedRepository,
                getServer().getPluginManager(), calendarEventService, loadedScheduler, getLogger());
        loadedTimeService.initialize();

        getServer().getServicesManager().register(ChronosAPI.class, loadedTimeService, this, ServicePriority.Normal);

        final TimeBossBarService loadedBossBarService = new TimeBossBarService(loadedTimeService, loadedMessageService);
        loadedBossBarService.initialize();

        init(loadedConfig, loadedMessageService, loadedTimeService, loadedBossBarService, loadedScheduler);

        final TimeClockListener clockListener = new TimeClockListener();
        clockListener.start();
        getServer().getPluginManager().registerEvents(clockListener, this);
        getServer().getPluginManager().registerEvents(new BossBarRefreshListener(), this);
        getServer().getPluginManager().registerEvents(new BossBarJoinListener(), this);

        new TimeCommand().register();
    }

    /**
     * Populates the static locator. Called exactly once, from
     * {@link #onEnable()}, after every shared service has been constructed
     * and (where needed) initialized.
     */
    private static void init(final ChronosConfig config, final MessageService messageService,
                              final TimeService timeService, final TimeBossBarService bossBarService,
                              final TaskScheduler scheduler) {
        Chronos.config = config;
        Chronos.messageService = messageService;
        Chronos.timeService = timeService;
        Chronos.bossBarService = bossBarService;
        Chronos.scheduler = scheduler;
    }

    /**
     * Returns the parsed, immutable configuration.
     */
    public static ChronosConfig getChronosConfig() {
        return config;
    }

    /**
     * Returns the message-rendering service.
     */
    public static MessageService getMessageService() {
        return messageService;
    }

    /**
     * Returns the calendar engine. Also implements {@link ChronosAPI}, but
     * other plugins should reach it through {@code ChronosProvider.get()},
     * not this accessor.
     */
    public static TimeService getTimeService() {
        return timeService;
    }

    /**
     * Returns the persistent time-and-date boss bar service.
     */
    public static TimeBossBarService getBossBarService() {
        return bossBarService;
    }

    /**
     * Returns the Folia-safe task scheduler.
     */
    public static TaskScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void onDisable() {
        if (bossBarService != null) {
            bossBarService.shutdown();
        }
        if (timeService != null) {
            timeService.persistNowBlocking();
        }
        if (repository != null) {
            repository.close();
        }
    }
}
