package com.notquests.builtin.actions;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.List;
import java.util.Locale;

public final class BeamAction {
    private static final String BEAM_NAME = "beamName";
    private static final String REMOVE = "remove";
    private static final String LOCATION = "location";

    private BeamAction() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("Beam")
                .displayName("Beam")
                .description("Shows or removes a guiding beam marker for the target player.")
                .field(
                        BEAM_NAME,
                        adapter.fields().text(() -> List.of("<beam name>")).config("specifics.beamName"),
                        "Beam identifier. Reusing the same identifier replaces or removes that player's existing beam.")
                .flag(
                        REMOVE,
                        adapter.fields().presenceFlag().config("specifics.remove"),
                        "Whether this action removes the named beam instead of showing it.")
                .flag(
                        LOCATION,
                        adapter.fields().storedLocation().config("specifics.location"),
                        "World location where the beam should point.")
                .singleLine((action, arguments) -> {
                    action.setValue(BEAM_NAME, arguments.get(0));
                    final String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT);
                    final boolean remove = joined.contains("--remove") || joined.contains(" remove");
                    action.setValue(REMOVE, remove);
                    if (remove) {
                        return;
                    }
                    final int offset = arguments.size() > 1 && arguments.get(1).equalsIgnoreCase("spawn") ? 1 : 0;
                    if (arguments.size() >= offset + 5) {
                        action.setValue(
                                LOCATION,
                                adapter.location(
                                        arguments.get(offset + 1),
                                        Double.parseDouble(arguments.get(offset + 2)),
                                        Double.parseDouble(arguments.get(offset + 3)),
                                        Double.parseDouble(arguments.get(offset + 4))));
                    }
                })
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer == null) {
                        return;
                    }
                    final String beamName = action.text(BEAM_NAME);
                    if (action.flag(REMOVE)) {
                        plugin.removeObjectiveMarker(questPlayer, beamName);
                        return;
                    }
                    final NQLocation location = action.location(LOCATION);
                    if (location != null) {
                        plugin.showObjectiveMarker(questPlayer, beamName, location);
                    }
                })
                .actionDescription((action, questPlayer, objects) ->
                        (action.flag(REMOVE) ? "Removes beam: " : "Shows beam: ") + action.text(BEAM_NAME))
                .register();
    }
}
