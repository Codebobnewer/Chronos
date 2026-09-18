package me.goga221.chronos.util;

/**
 * Small formatting helpers shared by any Command or GUI that displays
 * calendar values, so the same padding rule isn't duplicated per caller.
 */
public final class GameDateTimeFormat {

    private GameDateTimeFormat() {
    }

    /**
     * Zero-pads a clock field (hour/minute/second) to two digits.
     */
    public static String pad(final int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
