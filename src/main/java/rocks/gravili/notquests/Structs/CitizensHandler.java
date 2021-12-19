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

package rocks.gravili.notquests.Structs;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.trait.FollowTrait;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Objectives.EscortNPCObjective;

import java.util.Locale;

public class CitizensHandler {
    private final NotQuests main;

    public CitizensHandler(final NotQuests main) {
        this.main = main;
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
                npcToEscort.addTrait(followerTrait);
            }

            if (followerTrait != null) {
                final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                if (player != null) {
                    Audience audience = main.adventure().player(player);
                    if (!npcToEscort.isSpawned()) {
                        npcToEscort.spawn(player.getLocation());
                    }

                    if (followerTrait.getFollowingPlayer() == null || !followerTrait.getFollowingPlayer().equals(player)) {
                        if (!Bukkit.isPrimaryThread()) {
                            final FollowTrait finalFollowerTrait = followerTrait;
                            Bukkit.getScheduler().runTask(main, () -> {
                                finalFollowerTrait.toggle(player, false);
                            });
                        } else {
                            followerTrait.toggle(player, false);
                        }
                    }

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            "<GREEN>Escort quest started! Please escort <AQUA>" + npcToEscort.getName() + "</AQUA> to <AQUA>" + destinationNPC.getName() + "</AQUA>."
                    ));
                } else {
                    main.getLogManager().warn("Error: The escort objective could not be started, because the player with the UUID <AQUA>" + activeQuest.getQuestPlayer().getUUID() + "</AQUA> was not found!");


                }
            } else {
                final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                if (player != null) {
                    Audience audience = main.adventure().player(player);
                    audience.sendMessage(Component.text("The NPC you have to escort is not configured properly. Please consult an admin."));
                }
                main.getLogManager().warn("Error: The escort NPC with the ID <AQUA>" + npcToEscortID + "</AQUA> is not configured properly (Follow trait not found)!");

            }
        } else {
            if (destinationNPC == null) {
                final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                if (player != null) {
                    Audience audience = main.adventure().player(player);
                    audience.sendMessage(Component.text("The Destination NPC does not exist. Please consult an admin."));
                }
                main.getLogManager().warn("Error: The destination NPC with the ID <AQUA>" + npcToEscortID + "</AQUA> was not found!");

            }
            if (npcToEscort == null) {
                final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                if (player != null) {
                    Audience audience = main.adventure().player(player);
                    audience.sendMessage(Component.text("The NPC you have to escort does not exist. Please consult an admin."));
                }
                main.getLogManager().warn("Error: The escort NPC with the ID <AQUA>" + npcToEscortID + "</AQUA> was not found!");

            }

        }
    }
}
