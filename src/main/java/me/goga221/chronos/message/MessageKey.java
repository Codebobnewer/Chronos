package me.goga221.chronos.message;

/**
 * Identifiers for player-facing messages, each backed by a key in
 * {@code messages.yml}.
 */
public enum MessageKey {
    TIME_NOW("time-now"),
    TIME_DATE("time-date"),
    TIME_FULL("time-full"),
    TIME_SET_SUCCESS("time-set-success"),
    TIME_ADD_SUCCESS("time-add-success"),
    TIME_PAUSED("time-paused"),
    TIME_RESUMED("time-resumed"),
    ERROR_NO_PERMISSION("error-no-permission"),
    ERROR_INVALID_UNIT("error-invalid-unit"),
    ERROR_INVALID_OPERATION("error-invalid-operation"),
    CALENDAR_TITLE("calendar.title"),
    CALENDAR_DAY_ITEM_NAME("calendar.day-item-name"),
    CALENDAR_TODAY_SUFFIX("calendar.today-suffix"),
    CALENDAR_DAY_DATE_LINE("calendar.day-date-line"),
    CALENDAR_DAY_EVENT_LINE("calendar.day-event-line"),
    CALENDAR_DAY_NO_EVENTS("calendar.day-no-events"),
    CALENDAR_PREVIOUS_PAGE("calendar.previous-page"),
    CALENDAR_NEXT_PAGE("calendar.next-page"),
    BOSSBAR_TEXT("bossbar.text");

    private final String configKey;

    MessageKey(final String configKey) {
        this.configKey = configKey;
    }

    /**
     * Returns the key this message is stored under in {@code messages.yml}.
     */
    public String configKey() {
        return configKey;
    }
}
