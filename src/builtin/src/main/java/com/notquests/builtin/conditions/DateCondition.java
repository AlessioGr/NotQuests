package com.notquests.builtin.conditions;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class DateCondition {
    private static final String OPERATION = "Date operation";
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";
    private static final String HOURS = "hours";
    private static final String MINUTES = "minutes";
    private static final String SECONDS = "seconds";
    private static final String TIME_ZONE = "timeZone";

    private DateCondition() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.conditions().condition("Date")
                .displayName("Date")
                .description("Checks whether the current real-world date or time is before or after configured values.")
                .field(
                        OPERATION,
                        adapter.fields().text(() -> List.of("after", "before")).config("specifics.operation"),
                        "Whether the current date/time must be before or after the configured values.")
                .flag(
                        YEAR,
                        adapter.fields().integer(-1).config("specifics.year"),
                        "Year that must match for this date condition.")
                .flag(
                        MONTH,
                        adapter.fields().integer(-1).config("specifics.month"),
                        "Month that must match for this date condition.")
                .flag(
                        DAY,
                        adapter.fields().integer(-1).config("specifics.day"),
                        "Day of month that must match for this date condition.")
                .flag(
                        HOURS,
                        adapter.fields().integer(-1).config("specifics.hours"),
                        "Hour that must match for this date condition.")
                .flag(
                        MINUTES,
                        adapter.fields().integer(-1).config("specifics.minutes"),
                        "Minute that must match for this date condition.")
                .flag(
                        SECONDS,
                        adapter.fields().integer(-1).config("specifics.seconds"),
                        "Second that must match for this date condition.")
                .flag(
                        TIME_ZONE,
                        adapter.fields()
                                .text(() -> Arrays.stream(TimeZone.getAvailableIDs()).toList())
                                .config("specifics.timeZone"),
                        "Time zone used when evaluating the date condition.")
                .singleLine((condition, arguments) -> {
                    condition.setValue(OPERATION, arguments.get(0).toLowerCase(Locale.ROOT));
                    condition.setValue(YEAR, Integer.parseInt(arguments.get(1)));
                    condition.setValue(MONTH, Integer.parseInt(arguments.get(2)));
                    condition.setValue(DAY, Integer.parseInt(arguments.get(3)));
                    condition.setValue(HOURS, Integer.parseInt(arguments.get(4)));
                    condition.setValue(MINUTES, Integer.parseInt(arguments.get(5)));
                    condition.setValue(SECONDS, Integer.parseInt(arguments.get(6)));
                    condition.setValue(TIME_ZONE, arguments.get(7));
                })
                .check(DateCondition::check)
                .conditionDescription(DateCondition::description)
                .register();
    }

    private static String check(final Conditions.Data condition, final PlatformPlayer questPlayer) {
        final LocalDateTime currentTime = currentTime(condition);
        final LocalDateTime timeToCompare = configuredTime(condition, currentTime);
        final String operation = condition.text(OPERATION);
        if (operation.equals("before")) {
            if (!currentTime.isBefore(timeToCompare)) {
                return "<YELLOW>The current date needs to be before the " + timeToCompare;
            }
        } else if (operation.equals("after")) {
            if (!currentTime.isAfter(timeToCompare)) {
                return "<YELLOW>The current date needs to be after the " + timeToCompare;
            }
        } else {
            return "<error>Invalid date operator: <highlight>" + operation + "</highlight>.";
        }
        return "";
    }

    private static String description(
            final Conditions.Data condition,
            final PlatformPlayer questPlayer,
            final Object... objects) {
        final LocalDateTime currentTime = currentTime(condition);
        final LocalDateTime timeToCompare = configuredTime(condition, currentTime);
        final String operation = condition.text(OPERATION);
        if (operation.equals("before")) {
            return "<GRAY>- Current date before " + timeToCompare;
        }
        if (operation.equals("after")) {
            return "<GRAY>- Current date after " + timeToCompare;
        }
        return "<error>Invalid date operator: <highlight>" + operation + "</highlight>.";
    }

    private static LocalDateTime currentTime(final Conditions.Data condition) {
        final String zone = condition.text(TIME_ZONE);
        return zone.isBlank() ? LocalDateTime.now() : LocalDateTime.now(TimeZone.getTimeZone(zone).toZoneId());
    }

    private static LocalDateTime configuredTime(
            final Conditions.Data condition, final LocalDateTime currentTime) {
        return LocalDateTime.of(
                condition.integer(YEAR, -1) > -1 ? condition.integer(YEAR, -1) : currentTime.getYear(),
                condition.integer(MONTH, -1) > -1 ? condition.integer(MONTH, -1) : currentTime.getMonthValue(),
                condition.integer(DAY, -1) > -1 ? condition.integer(DAY, -1) : currentTime.getDayOfMonth(),
                condition.integer(HOURS, -1) > -1 ? condition.integer(HOURS, -1) : currentTime.getHour(),
                condition.integer(MINUTES, -1) > -1 ? condition.integer(MINUTES, -1) : currentTime.getMinute(),
                condition.integer(SECONDS, -1) > -1 ? condition.integer(SECONDS, -1) : currentTime.getSecond());
    }
}
