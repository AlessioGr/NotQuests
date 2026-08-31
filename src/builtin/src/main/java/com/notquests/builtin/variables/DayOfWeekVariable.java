package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;

public final class DayOfWeekVariable {
    private DayOfWeekVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables().stringVariable("DayOfWeek")
                .displayName("DayOfWeek")
                .description("Returns the current day of week as text.")
                .singular("Day of Week")
                .plural("Day of Week")
                .get((questPlayer, objects) -> LocalDate.now().getDayOfWeek().name().toLowerCase(Locale.ROOT))
                .possibleValues((questPlayer, objects) -> Arrays.stream(DayOfWeek.values())
                        .map(day -> day.name().toLowerCase(Locale.ROOT))
                        .toList())
                .register();
    }
}
