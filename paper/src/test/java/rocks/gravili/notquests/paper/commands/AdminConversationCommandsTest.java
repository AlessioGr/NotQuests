/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.managers.npc.NQNPCID;
import rocks.gravili.notquests.paper.structs.Quest;

class AdminConversationCommandsTest {
    @Test
    void attachedNpcListUsesReadableLabels() {
        final String formatted = AdminConversationCommands.formatAttachedNPCs(List.of(
                new FakeNpc("citizens", NQNPCID.fromInteger(5), "Guard Captain"),
                new FakeNpc("fancynpcs", NQNPCID.fromString("merchant"), null)));

        assertEquals("citizens:5 (Guard Captain), fancynpcs:merchant", formatted);
    }

    @Test
    void emptyAttachedNpcListShowsNone() {
        assertEquals("none", AdminConversationCommands.formatAttachedNPCs(List.of()));
    }

    private static final class FakeNpc extends NQNPC {
        private final NQNPCID id;
        private final String name;

        private FakeNpc(final String type, final NQNPCID id, final String name) {
            super((NotQuests) null, type);
            this.id = id;
            this.name = name;
        }

        @Nullable
        @Override
        public String getName() {
            return name;
        }

        @Override
        public NQNPCID getID() {
            return id;
        }

        @Override
        public void bindToConversation(final Conversation conversation) {
        }

        @Override
        public String removeQuestGiverNPCTrait(final @Nullable Boolean showQuestInNPC, final Quest quest) {
            return "";
        }

        @Override
        public String addQuestGiverNPCTrait(final @Nullable Boolean showQuestInNPC, final Quest quest) {
            return "";
        }

        @Nullable
        @Override
        public Entity getEntity() {
            return null;
        }
    }
}
