package com.notquests.core.gui;

import com.notquests.core.npc.NQNPCID;

public record GuiContext(
        String questIdentifier,
        String categoryIdentifier,
        String npcType,
        NQNPCID npcId,
        int page,
        int tab) {
    public static final GuiContext EMPTY = new GuiContext("", "", "", null, 0, -1);

    public GuiContext(
            final String questIdentifier,
            final String categoryIdentifier,
            final String npcType,
            final NQNPCID npcId) {
        this(questIdentifier, categoryIdentifier, npcType, npcId, 0, -1);
    }

    public GuiContext {
        questIdentifier = clean(questIdentifier);
        categoryIdentifier = clean(categoryIdentifier);
        npcType = clean(npcType);
        page = Math.max(0, page);
        tab = Math.max(-1, tab);
    }

    public static GuiContext of(
            final String questIdentifier,
            final String categoryIdentifier) {
        return new GuiContext(questIdentifier, categoryIdentifier, "", null);
    }

    public GuiContext withQuest(final String questIdentifier) {
        return new GuiContext(questIdentifier, categoryIdentifier, npcType, npcId, page, tab);
    }

    public GuiContext withCategory(final String categoryIdentifier) {
        return new GuiContext(questIdentifier, categoryIdentifier, npcType, npcId, page, tab);
    }

    public GuiContext withNpc(final String npcType, final NQNPCID npcId) {
        return new GuiContext(questIdentifier, categoryIdentifier, npcType, npcId, page, tab);
    }

    public GuiContext withPage(final int page) {
        return new GuiContext(questIdentifier, categoryIdentifier, npcType, npcId, page, tab);
    }

    public GuiContext withTab(final int tab) {
        return new GuiContext(questIdentifier, categoryIdentifier, npcType, npcId, 0, tab);
    }

    private static String clean(final String value) {
        return value == null ? "" : value;
    }
}
