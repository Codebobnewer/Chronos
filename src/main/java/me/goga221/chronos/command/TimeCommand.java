package me.goga221.chronos.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.goga221.chronos.Chronos;
import me.goga221.chronos.api.ChronosAPI;
import me.goga221.chronos.api.GameDate;
import me.goga221.chronos.api.GameTime;
import me.goga221.chronos.calendar.GameTimeUnit;
import me.goga221.chronos.gui.CalendarGui;
import me.goga221.chronos.message.MessageKey;
import me.goga221.chronos.message.MessageService;
import me.goga221.chronos.service.InvalidTimeOperationException;
import me.goga221.chronos.service.TimeService;
import me.goga221.chronos.util.GameDateTimeFormat;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/**
 * Registers and handles {@code /chronos} and its subcommands. Parses input
 * and delegates entirely to {@link TimeService} (reached via the
 * {@link Chronos} static locator); holds no calendar logic itself.
 */
public final class TimeCommand {

    private static final String USE_PERMISSION = "chronos.time.use";
    private static final String ADMIN_PERMISSION = "chronos.time.admin";

    /**
     * Builds and registers the {@code /chronos} command tree.
     */
    public void register() {
        final String[] unitNames = Arrays.stream(GameTimeUnit.values()).map(Enum::name).toArray(String[]::new);

        new CommandAPICommand("chronos")
                .withPermission(USE_PERMISSION)
                .executes((CommandExecutor) (sender, args) -> sendFull(sender))
                .withSubcommand(new CommandAPICommand("now")
                        .withPermission(USE_PERMISSION)
                        .executes((CommandExecutor) (sender, args) -> sendTime(sender)))
                .withSubcommand(new CommandAPICommand("date")
                        .withPermission(USE_PERMISSION)
                        .executes((CommandExecutor) (sender, args) -> sendDate(sender)))
                .withSubcommand(new CommandAPICommand("set")
                        .withPermission(ADMIN_PERMISSION)
                        .withArguments(
                                new IntegerArgument("year", 1),
                                new IntegerArgument("month", 1),
                                new IntegerArgument("day", 1),
                                new IntegerArgument("hour", 0),
                                new IntegerArgument("minute", 0),
                                new IntegerArgument("second", 0))
                        .executes((CommandExecutor) this::handleSet))
                .withSubcommand(new CommandAPICommand("add")
                        .withPermission(ADMIN_PERMISSION)
                        .withArguments(
                                new LongArgument("amount"),
                                new MultiLiteralArgument("unit", unitNames))
                        .executes((CommandExecutor) this::handleAdd))
                .withSubcommand(new CommandAPICommand("pause")
                        .withPermission(ADMIN_PERMISSION)
                        .executes((CommandExecutor) (sender, args) -> handlePause(sender)))
                .withSubcommand(new CommandAPICommand("resume")
                        .withPermission(ADMIN_PERMISSION)
                        .executes((CommandExecutor) (sender, args) -> handleResume(sender)))
                .withSubcommand(new CommandAPICommand("calendar")
                        .withPermission(USE_PERMISSION)
                        .executesPlayer((PlayerCommandExecutor) (player, args) ->
                                new CalendarGui(Chronos.getTimeService(), Chronos.getMessageService()).open(player)))
                .register();
    }

    private void sendTime(final CommandSender sender) {
        final ChronosAPI api = Chronos.getTimeService();
        final MessageService messages = Chronos.getMessageService();
        final GameTime time = api.getCurrentTime();
        messages.send(sender, MessageKey.TIME_NOW,
                Placeholder.unparsed("hour", GameDateTimeFormat.pad(time.hour())),
                Placeholder.unparsed("minute", GameDateTimeFormat.pad(time.minute())),
                Placeholder.unparsed("second", GameDateTimeFormat.pad(time.second())),
                pauseStateResolver(api));
    }

    private void sendDate(final CommandSender sender) {
        Chronos.getMessageService().send(sender, MessageKey.TIME_DATE, dateResolvers(Chronos.getTimeService()));
    }

    private void sendFull(final CommandSender sender) {
        final ChronosAPI api = Chronos.getTimeService();
        final MessageService messages = Chronos.getMessageService();
        final GameTime time = api.getCurrentTime();
        final TagResolver[] resolvers = concat(dateResolvers(api),
                Placeholder.unparsed("hour", GameDateTimeFormat.pad(time.hour())),
                Placeholder.unparsed("minute", GameDateTimeFormat.pad(time.minute())),
                Placeholder.unparsed("second", GameDateTimeFormat.pad(time.second())),
                pauseStateResolver(api));
        messages.send(sender, MessageKey.TIME_FULL, resolvers);
    }

    private void handlePause(final CommandSender sender) {
        final MessageService messages = Chronos.getMessageService();
        try {
            Chronos.getTimeService().pause();
        } catch (final InvalidTimeOperationException exception) {
            messages.send(sender, exception.messageKey(), Placeholder.unparsed("reason", exception.getMessage()));
            return;
        }

        Chronos.getBossBarService().refresh();
        messages.send(sender, MessageKey.TIME_PAUSED);
    }

    private void handleResume(final CommandSender sender) {
        final MessageService messages = Chronos.getMessageService();
        try {
            Chronos.getTimeService().resume();
        } catch (final InvalidTimeOperationException exception) {
            messages.send(sender, exception.messageKey(), Placeholder.unparsed("reason", exception.getMessage()));
            return;
        }

        Chronos.getBossBarService().refresh();
        messages.send(sender, MessageKey.TIME_RESUMED);
    }

    private TagResolver pauseStateResolver(final ChronosAPI api) {
        return Placeholder.unparsed("pause_state", api.isPaused() ? " (Paused)" : "");
    }

    private TagResolver[] dateResolvers(final ChronosAPI api) {
        final GameDate date = api.getCurrentDate();
        return new TagResolver[]{
                Placeholder.unparsed("year", String.valueOf(date.year())),
                Placeholder.unparsed("month", String.valueOf(date.month())),
                Placeholder.unparsed("month_name", api.getMonthName(date.month())),
                Placeholder.unparsed("week", String.valueOf(date.week())),
                Placeholder.unparsed("day", String.valueOf(date.day())),
                Placeholder.unparsed("day_of_week", api.getDayOfWeekName(date.dayOfWeek()))
        };
    }

    private void handleSet(final CommandSender sender, final CommandArguments args) {
        final int year = (int) args.get("year");
        final int month = (int) args.get("month");
        final int day = (int) args.get("day");
        final int hour = (int) args.get("hour");
        final int minute = (int) args.get("minute");
        final int second = (int) args.get("second");

        final TimeService timeService = Chronos.getTimeService();
        final MessageService messages = Chronos.getMessageService();
        try {
            timeService.setDateTime(year, month, day, hour, minute, second);
        } catch (final InvalidTimeOperationException exception) {
            messages.send(sender, exception.messageKey(), Placeholder.unparsed("reason", exception.getMessage()));
            return;
        }

        messages.send(sender, MessageKey.TIME_SET_SUCCESS, dateResolvers(timeService));
    }

    private void handleAdd(final CommandSender sender, final CommandArguments args) {
        final long amount = (long) args.get("amount");
        final String unitName = (String) args.get("unit");
        final GameTimeUnit unit = GameTimeUnit.valueOf(unitName);

        final MessageService messages = Chronos.getMessageService();
        try {
            Chronos.getTimeService().addAmount(amount, unit);
        } catch (final InvalidTimeOperationException exception) {
            messages.send(sender, exception.messageKey(), Placeholder.unparsed("reason", exception.getMessage()));
            return;
        }

        messages.send(sender, MessageKey.TIME_ADD_SUCCESS,
                Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.unparsed("unit", unitName));
    }

    private static TagResolver[] concat(final TagResolver[] first, final TagResolver... second) {
        final TagResolver[] combined = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        return combined;
    }
}
