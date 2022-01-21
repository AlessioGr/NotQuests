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

package rocks.gravili.notquests.paper.managers.integrations.citizens;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.trait.FollowTrait;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.objectives.hooks.citizens.EscortNPCObjective;

import java.util.ArrayList;
import java.util.Locale;

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
                    Bukkit.getScheduler().runTask(main.getMain(), conversation::bindToCitizensNPC);
                } else {
                    conversation.bindToCitizensNPC();
                }

            }
        }

    }

    public void onDisable() {
        /*
         * All Citizen NPCs which have quests attached to them have the Citizens NPC trait "nquestgiver".
         * When the plugin is disabled right here, this piece of code will try removing this trait from all+
         * NPCs which currently have this trait.
         */
        final ArrayList<Trait> traitsToRemove = new ArrayList<>();
        for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
            for (final Trait trait : npc.getTraits()) {
                if (trait.getName().equalsIgnoreCase("nquestgiver")) {
                    traitsToRemove.add(trait);

                }
            }
            for (final Trait traitToRemove : traitsToRemove) {
                //npc.removeTrait(traitToRemove.getClass()); //TODO: Fucks up loading for some reason
                main.getLogManager().info("Removed nquestgiver trait from NPC with the ID <highlight>" + npc.getId());
            }
            traitsToRemove.clear();

        }


        /*
         * Next, the nquestgiver trait itself which is registered via the Citizens API on startup is being
         * de-registered.
         */
        main.getLogManager().info("Deregistering nquestgiver trait...");
        final ArrayList<TraitInfo> toDeregister = new ArrayList<>();
        for (final TraitInfo traitInfo : net.citizensnpcs.api.CitizensAPI.getTraitFactory().getRegisteredTraits()) {
            if (traitInfo.getTraitName().equals("nquestgiver")) {
                toDeregister.add(traitInfo);

            }
        }
        //Actual nquestgiver trait de-registering happens here, to prevent a ConcurrentModificationException
        for (final TraitInfo traitInfo : toDeregister) {
            net.citizensnpcs.api.CitizensAPI.getTraitFactory().deregisterTrait(traitInfo);
        }
    }


    public void handleEscortObjective(final ActiveObjective activeObjective) {
        final NPC npcToEscort = CitizensAPI.getNPCRegistry().getById(((EscortNPCObjective) activeObjective.getObjective()).getNpcToEscortID());
        if (npcToEscort != null) {
            FollowTrait followerTrait = null;
            for (final Trait trait : npcToEscort.getTraits()) {
                if (trait.getName().toLowerCase(Locale.ROOT).contains("follow")) {
                    followerTrait = (FollowTrait) trait;
                }
            }
            if (followerTrait != null) {
                npcToEscort.removeTrait(followerTrait.getClass());
            }

            npcToEscort.despawn();
        }
    }

    public void handleEscortNPCObjectiveForActiveObjective(final EscortNPCObjective escortNPCObjective, final ActiveQuest activeQuest) {
        final int npcToEscortID = escortNPCObjective.getNpcToEscortID();
        final int destinationNPCID = escortNPCObjective.getNpcToEscortToID();
        final NPC npcToEscort = CitizensAPI.getNPCRegistry().getById(npcToEscortID);
        final NPC destinationNPC = CitizensAPI.getNPCRegistry().getById(destinationNPCID);
        if (npcToEscort != null && destinationNPC != null) {
            FollowTrait followerTrait = null;
            for (final Trait trait : npcToEscort.getTraits()) {
                if (trait.getName().toLowerCase(Locale.ROOT).contains("follow")) {
                    followerTrait = (FollowTrait) trait;
                }
            }
            if (followerTrait == null) {
                followerTrait = new FollowTrait();
                if (!Bukkit.isPrimaryThread()) {
                    FollowTrait finalFollowerTrait1 = followerTrait;
                    Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                        npcToEscort.addTrait(finalFollowerTrait1);
                    });
                }
            }


            if (followerTrait != null) {
                final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                if (player != null) {
                    if (!npcToEscort.isSpawned()) {
                        npcToEscort.spawn(player.getLocation());
                    }

                    if (followerTrait.getFollowingPlayer() == null || !followerTrait.getFollowingPlayer().equals(player)) {
                        if (!Bukkit.isPrimaryThread()) {
                            final FollowTrait finalFollowerTrait = followerTrait;
                            Bukkit.getScheduler().runTask(main.getMain(), () -> {
                                finalFollowerTrait.toggle(player, false);
                            });
                        } else {

                            followerTrait.toggle(player, false);
                        }
                    }

                    player.sendMessage(main.parse(
                            "<GREEN>Escort quest started! Please escort <highlight>" + npcToEscort.getName() + "</highlight> to <highlight>" + destinationNPC.getName() + "</highlight>."
                    ));
                } else {
                    main.getLogManager().warn("Error: The escort objective could not be started, because the player with the UUID <highlight>" + activeQuest.getQuestPlayer().getUUID() + "</highlight> was not found!");


                }
            } else {
                final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                if (player != null) {
                    player.sendMessage(Component.text("The NPC you have to escort is not configured properly. Please consult an admin."));
                }
                main.getLogManager().warn("Error: The escort NPC with the ID <highlight>" + npcToEscortID + "</highlight> is not configured properly (Follow trait not found)!");

            }
        } else {
            if (destinationNPC == null) {
                final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                if (player != null) {
                    player.sendMessage(Component.text("The Destination NPC does not exist. Please consult an admin."));
                }
                main.getLogManager().warn("Error: The destination NPC with the ID <highlight>" + npcToEscortID + "</highlight> was not found!");

            }
            if (npcToEscort == null) {
                final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                if (player != null) {
                    player.sendMessage(Component.text("The NPC you have to escort does not exist. Please consult an admin."));
                }
                main.getLogManager().warn("Error: The escort NPC with the ID <highlight>" + npcToEscortID + "</highlight> was not found!");

            }

        }
    }
}
