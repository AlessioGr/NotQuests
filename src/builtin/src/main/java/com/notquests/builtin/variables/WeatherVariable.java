package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.List;

public final class WeatherVariable {
    private WeatherVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .stringVariable("Weather")
                .displayName("Weather")
                .description("Reads or changes the weather in the target player's current world.")
                .singular("Weather")
                .plural("Weather States")
                .get((questPlayer, objects) -> questPlayer == null ? "" : questPlayer.weather())
                .set((newValue, questPlayer, objects) -> questPlayer != null && questPlayer.setWeather(newValue))
                .possibleValues((questPlayer, objects) -> List.of("clear", "rain", "thunder"))
                .register();
    }
}
