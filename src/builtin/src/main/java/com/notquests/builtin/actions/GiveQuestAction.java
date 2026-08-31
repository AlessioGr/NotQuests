package com.notquests.builtin.actions;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.structs.Quest;

import java.util.Locale;

public final class GiveQuestAction {
    private static final String QUEST = "quest";
    private static final String FORCE_GIVE = "forceGive";

    private GiveQuestAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("GiveQuest")
                .displayName("Give Quest")
                .description("Gives another quest to the target player.")
                .field(
                        QUEST,
                        adapter.fields().text(plugin::questNames).config("specifics.quest"),
                        "Quest that should be given to the target player.")
                .flag(
                        FORCE_GIVE,
                        adapter.fields().presenceFlag().config("specifics.forceGive"),
                        "Force-gives the quest and skips the normal take requirements and cooldown checks.")
                .singleLine((action, arguments) -> {
                    action.setValue(QUEST, arguments.get(0));
                    action.setValue(FORCE_GIVE, String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--forcegive"));
                })
                .execute((action, questPlayer, objects) ->
                        plugin.giveQuest(
                                questPlayer,
                                action.text(QUEST),
                                action.flag(FORCE_GIVE)
                                        ? Quest.GiveOptions.forcedSilent()
                                        : Quest.GiveOptions.normal(),
                                adapter::warn))
                .actionDescription((action, questPlayer, objects) -> "Gives quest: " + action.text(QUEST))
                .register();
    }
}
