package me.goga221.chronos.gui;

import lombok.RequiredArgsConstructor;
import me.goga221.chronos.api.CalendarEvent;
import me.goga221.chronos.api.ChronosAPI;
import me.goga221.chronos.api.GameDate;
import me.goga221.chronos.api.GameDateTime;
import me.goga221.chronos.message.MessageKey;
import me.goga221.chronos.message.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * A calendar view listing today and the next {@value #LOOKAHEAD_DAYS} days,
 * and whatever {@link CalendarEvent}s fall on each one — a rolling window,
 * not bound to the active calendar structure's week/month boundaries, so a
 * tournament scheduled for next month still shows up if it's soon enough.
 * Events are supplied entirely through
 * {@link ChronosAPI#registerCalendarEvent(CalendarEvent)}; this class only
 * displays them.
 */
@RequiredArgsConstructor
public final class CalendarGui {

    private static final int LOOKAHEAD_DAYS = 7;

    private final ChronosAPI api;
    private final MessageService messages;

    /**
     * Opens the calendar GUI for {@code player}, showing today plus the
     * next {@value #LOOKAHEAD_DAYS} days.
     */
    public void open(final Player player) {
        final long secondsPerDay = (long) api.getHoursPerDay() * api.getMinutesPerHour() * api.getSecondsPerMinute();
        final long startTotalSeconds = api.getCurrentDateTime().totalGameSeconds();

        final List<Item> items = new ArrayList<>(LOOKAHEAD_DAYS);
        for (int offset = 0; offset < LOOKAHEAD_DAYS; offset++) {
            final GameDateTime at = api.getDateTimeAt(startTotalSeconds + offset * secondsPerDay);
            final GameDate date = at.date();
            items.add(dayItem(date.year(), date.month(), date.day(), date.dayOfWeek(), offset == 0));
        }

        final Gui gui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# # # < # > # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")))
                .addIngredient('<', new PreviousPageItem())
                .addIngredient('>', new NextPageItem())
                .setContent(items)
                .build();

        Window.single(builder -> builder
                .setViewer(player)
                .setTitle(new AdventureComponentWrapper(messages.renderPlain(MessageKey.CALENDAR_TITLE)))
                .setGui(gui)
        ).open();
    }

    private Item dayItem(final int year, final int month, final int day, final int dayOfWeek, final boolean isToday) {
        final List<CalendarEvent> events = api.getCalendarEventsOn(year, month, day);

        final Component todaySuffix = isToday ? messages.renderPlain(MessageKey.CALENDAR_TODAY_SUFFIX) : Component.empty();
        final Component name = messages.renderPlain(MessageKey.CALENDAR_DAY_ITEM_NAME,
                Placeholder.unparsed("day_of_week_name", api.getDayOfWeekName(dayOfWeek)),
                Placeholder.component("today_suffix", todaySuffix));

        final List<ComponentWrapper> lore = new ArrayList<>();
        lore.add(new AdventureComponentWrapper(messages.renderPlain(MessageKey.CALENDAR_DAY_DATE_LINE,
                Placeholder.unparsed("day", String.valueOf(day)),
                Placeholder.unparsed("month_name", api.getMonthName(month)),
                Placeholder.unparsed("year", String.valueOf(year)))));
        lore.add(new AdventureComponentWrapper(Component.empty()));

        if (events.isEmpty()) {
            lore.add(new AdventureComponentWrapper(messages.renderPlain(MessageKey.CALENDAR_DAY_NO_EVENTS)));
        } else {
            for (final CalendarEvent event : events) {
                lore.add(new AdventureComponentWrapper(messages.renderPlain(MessageKey.CALENDAR_DAY_EVENT_LINE,
                        Placeholder.unparsed("event_name", event.name()))));
            }
        }

        final Material material = isToday ? Material.CLOCK : events.isEmpty() ? Material.PAPER : Material.WRITABLE_BOOK;
        final ItemProvider provider = new ItemBuilder(material)
                .setDisplayName(new AdventureComponentWrapper(name))
                .setLore(lore);

        return new SimpleItem(provider);
    }

    private final class PreviousPageItem extends PageItem {

        private PreviousPageItem() {
            super(false);
        }

        @Override
        public ItemProvider getItemProvider(final PagedGui<?> gui) {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName(new AdventureComponentWrapper(messages.renderPlain(MessageKey.CALENDAR_PREVIOUS_PAGE)));
        }
    }

    private final class NextPageItem extends PageItem {

        private NextPageItem() {
            super(true);
        }

        @Override
        public ItemProvider getItemProvider(final PagedGui<?> gui) {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName(new AdventureComponentWrapper(messages.renderPlain(MessageKey.CALENDAR_NEXT_PAGE)));
        }
    }
}
