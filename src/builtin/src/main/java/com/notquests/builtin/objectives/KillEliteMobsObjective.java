package com.notquests.builtin.objectives;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;

public final class KillEliteMobsObjective {
    public static final String TYPE = "KillEliteMobs";
    private static final String AMOUNT = "amount";
    private static final String MOB_NAME = "mobname";
    private static final String MINIMUM_LEVEL = "minimumLevel";
    private static final String MAXIMUM_LEVEL = "maximumLevel";
    private static final String SPAWN_REASON = "spawnReason";
    private static final String MINIMUM_DAMAGE_PERCENTAGE = "minimumDamagePercentage";

    private KillEliteMobsObjective() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        if (!plugin.integrationEnabled("EliteMobs")) {
            return;
        }
        adapter.objectives().objective(TYPE)
                .displayName("Kill EliteMobs")
                .description("Counts EliteMobs kills matching optional mob name, level, spawn reason, and damage-share filters.")
                .field(
                        AMOUNT,
                        adapter.fields().numberExpression(false).progressNeeded(),
                        "Number of matching EliteMobs the player must kill.")
                .flag(
                        MOB_NAME,
                        adapter.fields().text().config("specifics.eliteMobToKill"),
                        "EliteMobs mob name that counts for this objective, or blank for any EliteMob.")
                .flag(
                        MINIMUM_LEVEL,
                        adapter.fields().storedInteger(-1).config("specifics.minimumLevel"),
                        "Minimum EliteMobs level that counts, or -1 for no minimum.")
                .flag(
                        MAXIMUM_LEVEL,
                        adapter.fields().storedInteger(-1).config("specifics.maximumLevel"),
                        "Maximum EliteMobs level that counts, or -1 for no maximum.")
                .flag(
                        SPAWN_REASON,
                        adapter.fields().text().config("specifics.spawnReason"),
                        "EliteMobs spawn reason that counts, or blank for any spawn reason.")
                .flag(
                        MINIMUM_DAMAGE_PERCENTAGE,
                        adapter.fields().storedInteger(-1).config("specifics.minimumDamagePercentage"),
                        "Minimum percent of total damage the player must deal, or -1 for no minimum.")
                .taskDescription((objective, questPlayer, activeObjective) ->
                        plugin.killEliteMobsTaskDescription(questPlayer, objective))
                .register();
    }
}
