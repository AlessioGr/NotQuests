package com.notquests.builtin.actions;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.DoubleDashFlagParser;
import com.notquests.core.gui.GuiContext;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.List;

public final class OpenGuiAction {
    private static final String GUI_NAME = "guiName";
    private static final String PLAYER = "player";
    private static final String QUEST = "quest";
    private static final String NPC = "npc";
    private static final String CATEGORY = "category";

    private OpenGuiAction() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("OpenGui")
                .displayName("Open GUI")
                .description("Opens a NotQuests GUI for the target player.")
                .field(
                        GUI_NAME,
                        adapter.fields().text().config("specifics.guiName"),
                        "Name of the NotQuests GUI file to open.")
                .flag(PLAYER, adapter.fields().text().config("specifics.player"), "Optional player name who should see the GUI.")
                .flag(QUEST, adapter.fields().text().config("specifics.quest"), "Optional quest identifier used as GUI context.")
                .flag(NPC, adapter.fields().integer(-1).config("specifics.npc"), "Optional Citizens NPC id used as GUI context.")
                .flag(CATEGORY, adapter.fields().text().config("specifics.category"), "Optional quest category identifier used as GUI context.")
                .singleLine((action, arguments) -> {
                    if (arguments.isEmpty()) {
                        throw new IllegalArgumentException("OpenGui action requires a GUI name.");
                    }
                    action.setValue(GUI_NAME, arguments.get(0));
                    final var flags = DoubleDashFlagParser.parse(
                            String.join(" ", arguments),
                            List.of(
                                    new ActionFlag(PLAYER),
                                    new ActionFlag(QUEST),
                                    new ActionFlag(NPC),
                                    new ActionFlag(CATEGORY)),
                            ActionFlag::name,
                            ignored -> false,
                            (flag, raw) -> raw);
                    action.setValue(PLAYER, String.valueOf(flags.values().getOrDefault(PLAYER, "")));
                    action.setValue(QUEST, String.valueOf(flags.values().getOrDefault(QUEST, "")));
                    action.setValue(NPC, parseNpcId(String.valueOf(flags.values().getOrDefault(NPC, "-1"))));
                    action.setValue(CATEGORY, String.valueOf(flags.values().getOrDefault(CATEGORY, "")));
                })
                .execute((action, questPlayer, objects) -> {
                    final int npcId = action.integer(NPC, -1);
                    if (questPlayer == null
                            || !plugin.openGui(
                                    questPlayer,
                                    action.text(GUI_NAME),
                                    action.text(PLAYER),
                                    new GuiContext(
                                            action.text(QUEST),
                                            action.text(CATEGORY),
                                            npcId < 0 ? "" : "citizens",
                                            npcId < 0 ? null : NQNPCID.fromInteger(npcId)))) {
                        adapter.warn("Tried to execute OpenGui action without a valid target player.");
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Opens GUI: " + action.text(GUI_NAME))
                .register();
    }

    private record ActionFlag(String name) {}

    private static int parseNpcId(final String value) {
        if (value == null || value.contains("%")) {
            return -1;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException exception) {
            return -1;
        }
    }
}
