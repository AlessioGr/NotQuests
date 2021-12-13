/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.Hooks.Citizens;

import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Bukkit;
import rocks.gravili.notquests.Conversation.Conversation;
import rocks.gravili.notquests.NotQuests;

import java.util.ArrayList;

public class CitizensManager {
    private final NotQuests main;

    public CitizensManager(final NotQuests main) {
        this.main = main;
    }

    public void registerQuestGiverTrait() {
        main.getLogManager().info("Registering Citizens nquestgiver trait...");

        final ArrayList<TraitInfo> toDeregister = new ArrayList<>();
        for (final TraitInfo traitInfo : net.citizensnpcs.api.CitizensAPI.getTraitFactory().getRegisteredTraits()) {
            if (traitInfo.getTraitName().equals("nquestgiver")) {
                toDeregister.add(traitInfo);

            }
        }
        for (final TraitInfo traitInfo : toDeregister) {
            net.citizensnpcs.api.CitizensAPI.getTraitFactory().deregisterTrait(traitInfo);
        }

        net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(QuestGiverNPCTrait.class).withName("nquestgiver"));
        main.getLogManager().info("Citizens nquestgiver trait has been registered!");
        if (!main.getDataManager().isAlreadyLoadedNPCs()) {
            main.getDataManager().loadNPCData();
        }

        postRegister();
    }

    private void postRegister() {
        if (main.getConversationManager() != null) {
            main.getLogManager().info("Trying to bind Conversations to NPCs...");
            for (Conversation conversation : main.getConversationManager().getAllConversations()) {
                if (!Bukkit.isPrimaryThread()) {
                    Bukkit.getScheduler().runTask(main, conversation::bindToCitizensNPC);
                } else {
                    conversation.bindToCitizensNPC();
                }

            }
        }

    }
}
