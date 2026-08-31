package com.notquests.builtin.actions;

import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;

import java.util.Locale;

public final class PlaySoundAction {
    private static final String SOUND = "sound";
    private static final String STOP_OTHER_SOUNDS = "stopOtherSounds";
    private static final String PLAY_FOR_EVERYONE_AT_SET_LOCATION = "playForEveryoneAtSetLocation";
    private static final String PLAY_FOR_EVERYONE_AT_THEIR_LOCATION = "playForEveryoneAtTheirLocation";
    private static final String WORLD_NAME = "world";
    private static final String LOCATION_X = "locationX";
    private static final String LOCATION_Y = "locationY";
    private static final String LOCATION_Z = "locationZ";
    private static final String VOLUME = "volume";
    private static final String PITCH = "pitch";
    private static final String SOUND_CATEGORY = "SoundCategory";

    private PlaySoundAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("PlaySound")
                .displayName("Play Sound")
                .description("Plays a Minecraft sound for the target player or all online players.")
                .field(
                        SOUND,
                        adapter.fields().text(adapter::soundTypeIds).config("specifics.soundName"),
                        "Minecraft sound key to play.")
                .flag(
                        STOP_OTHER_SOUNDS,
                        adapter.fields().presenceFlag().config("specifics.stopOtherSounds"),
                        "Stops all currently playing sounds before playing this one.")
                .flag(
                        PLAY_FOR_EVERYONE_AT_SET_LOCATION,
                        adapter.fields().presenceFlag().config("specifics.playForEveryoneAtSetLocation"),
                        "All online players near the configured location hear the sound.")
                .flag(
                        PLAY_FOR_EVERYONE_AT_THEIR_LOCATION,
                        adapter.fields().presenceFlag().config("specifics.playForEveryoneAtTheirLocation"),
                        "All online players hear the sound at their own current location.")
                .flag(
                        WORLD_NAME,
                        adapter.fields().text(adapter::worldNames).config("specifics.worldName"),
                        "World used when playing the sound at a fixed location.")
                .flag(
                        LOCATION_X,
                        adapter.fields().optionalDouble().config("specifics.locationX"),
                        "X coordinate used when playing the sound at a fixed location.")
                .flag(
                        LOCATION_Y,
                        adapter.fields().optionalDouble().config("specifics.locationY"),
                        "Y coordinate used when playing the sound at a fixed location.")
                .flag(
                        LOCATION_Z,
                        adapter.fields().optionalDouble().config("specifics.locationZ"),
                        "Z coordinate used when playing the sound at a fixed location.")
                .flag(
                        VOLUME,
                        adapter.fields().optionalDouble().config("specifics.volume"),
                        "Sound volume, usually between 0 and 1.")
                .flag(
                        PITCH,
                        adapter.fields().optionalDouble().config("specifics.pitch"),
                        "Sound pitch multiplier.")
                .flag(
                        SOUND_CATEGORY,
                        adapter.fields().text(adapter::soundCategoryIds).config("specifics.soundCategory"),
                        "Minecraft sound category. Defaults to master.")
                .singleLine((action, arguments) -> {
                    action.setValue(SOUND, arguments.get(0));
                    final String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT);
                    action.setValue(STOP_OTHER_SOUNDS, joined.contains("--stopothersounds"));
                    action.setValue(PLAY_FOR_EVERYONE_AT_SET_LOCATION, joined.contains("--playforeveryoneatsetlocation"));
                    action.setValue(
                            PLAY_FOR_EVERYONE_AT_THEIR_LOCATION,
                            joined.contains("--playforeveryoneattheirlocation"));
                    action.setValue(SOUND_CATEGORY, "master");
                })
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer == null || !questPlayer.hasPlayer()) {
                        return;
                    }
                    final String worldName = action.text(WORLD_NAME);
                    final Double x = optionalNumber(action, LOCATION_X);
                    final Double y = optionalNumber(action, LOCATION_Y);
                    final Double z = optionalNumber(action, LOCATION_Z);
                    final Double volume = optionalNumber(action, VOLUME);
                    final Double pitch = optionalNumber(action, PITCH);
                    final NQLocation location = worldName.isBlank()
                            ? NQLocation.at(
                                    questPlayer.worldName(),
                                    questPlayer.positionX(),
                                    questPlayer.positionY(),
                                    questPlayer.positionZ())
                            : x == null || y == null || z == null
                                    ? null
                                    : NQLocation.at(worldName, x, y, z);
                    if (location == null) {
                        return;
                    }
                    final String soundCategory = soundCategory(adapter, action.text(SOUND_CATEGORY));
                    if (soundCategory == null) {
                        return;
                    }
                    final PlatformPlayer.SoundAudience audience = action.flag(PLAY_FOR_EVERYONE_AT_THEIR_LOCATION)
                            ? PlatformPlayer.SoundAudience.EVERYONE_AT_OWN_LOCATION
                            : action.flag(PLAY_FOR_EVERYONE_AT_SET_LOCATION)
                                    ? PlatformPlayer.SoundAudience.WORLD
                                    : PlatformPlayer.SoundAudience.PLAYER;
                    if (action.flag(STOP_OTHER_SOUNDS)) {
                        questPlayer.stopSounds();
                    }
                    questPlayer.playSound(
                            action.text(SOUND),
                            audience,
                            location,
                            volume == null ? 1.0d : volume,
                            pitch == null ? 1.0d : pitch,
                            soundCategory);
                })
                .actionDescription((action, questPlayer, objects) -> "Plays sound: " + action.text(SOUND))
                .register();
    }

    private static Double optionalNumber(
            final Actions.Data action, final String name) {
        final double value = action.number(name, Double.NaN);
        return Double.isNaN(value) ? null : value;
    }

    private static String soundCategory(final NotQuestsAdapter adapter, final String configuredCategory) {
        final String requested = configuredCategory == null || configuredCategory.isBlank()
                ? "master"
                : configuredCategory.trim();
        return adapter.soundCategoryIds().stream()
                .filter(category -> category.equalsIgnoreCase(requested))
                .findFirst()
                .orElse(null);
    }
}
