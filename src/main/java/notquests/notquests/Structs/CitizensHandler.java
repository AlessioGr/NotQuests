package notquests.notquests.Structs;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.trait.FollowTrait;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Objectives.EscortNPCObjective;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

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
                if (trait.getName().toLowerCase().contains("follow")) {
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
                if (trait.getName().toLowerCase().contains("follow")) {
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


                    player.sendMessage("§aEscort quest started! Please escort §b" + npcToEscort.getName() + " §ato §b" + destinationNPC.getName() + "§a.");
                } else {
                    main.getLogger().log(Level.WARNING, "§cNotQuests > Error: The escort objective could not be started, because the player with the UUID §b" + activeQuest.getQuestPlayer().getActiveQuests() + " §cwas not found!");


                }
            } else {
                final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                if (player != null) {
                    player.sendMessage("§cNotQuests > The NPC you have to escort is not configured properly. Please consult an admin.");
                }
                main.getLogger().log(Level.WARNING, "§cNotQuests > Error: The escort NPC with the ID §b" + npcToEscortID + " §cis not configured properly (Follow trait not found)!");

            }
        } else {
            if (destinationNPC == null) {
                final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                if (player != null) {
                    player.sendMessage("§cNotQuests > The Destination NPC does not exist. Please consult an admin.");
                }
                main.getLogger().log(Level.WARNING, "§cNotQuests > Error: The destination NPC with the ID §b" + npcToEscortID + " §cwas not found!");

            }
            if (npcToEscort == null) {
                final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                if (player != null) {
                    player.sendMessage("§cNotQuests > The NPC you have to escort does not exist. Please consult an admin.");
                }
                main.getLogger().log(Level.WARNING, "§cNotQuests > Error: The escort NPC with the ID §b" + npcToEscortID + " §cwas not found!");

            }

        }
    }
}
