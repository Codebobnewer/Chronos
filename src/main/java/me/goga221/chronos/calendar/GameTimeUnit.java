package me.goga221.chronos.calendar;

/**
 * A unit of in-game time, used for administrative time advancement and
 * rollover detection. Each constant knows its own fixed length in seconds
 * under a given {@link CalendarStructure}.
 */
public enum GameTimeUnit {
    SECOND {
        @Override
        public long lengthInSeconds(final CalendarStructure structure) {
            return 1L;
        }
    },
    MINUTE {
        @Override
        public long lengthInSeconds(final CalendarStructure structure) {
            return structure.secondsPerMinute();
        }
    },
    HOUR {
        @Override
        public long lengthInSeconds(final CalendarStructure structure) {
            return structure.secondsPerHour();
        }
    },
    DAY {
        @Override
        public long lengthInSeconds(final CalendarStructure structure) {
            return structure.secondsPerDay();
        }
    },
    WEEK {
        @Override
        public long lengthInSeconds(final CalendarStructure structure) {
            return structure.secondsPerWeek();
        }
    },
    MONTH {
        @Override
        public long lengthInSeconds(final CalendarStructure structure) {
            return structure.secondsPerMonth();
        }
    },
    YEAR {
        @Override
        public long lengthInSeconds(final CalendarStructure structure) {
            return structure.secondsPerYear();
        }
    };

    /**
     * Returns this unit's fixed length, in seconds, under the given calendar structure.
     */
    public abstract long lengthInSeconds(CalendarStructure structure);
}
