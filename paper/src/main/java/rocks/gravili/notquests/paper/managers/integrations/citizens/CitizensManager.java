/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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


import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.trait.FollowTrait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.managers.npc.NQNPCID;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveObjectiveHolder;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.objectives.hooks.citizens.EscortNPCObjective;

public class CitizensManager {
  private final NotQuests main;

  private Consumer<Trait> traitRun; // Runs in the run() method for NPCs with the QuestGiverNPC trait

  public CitizensManager(final NotQuests main) {
    this.main = main;
  }

  public void setTraitRun(final Consumer<Trait> traitRun) {
    this.traitRun = traitRun;
  }

  public final Consumer<Trait> getTraitRun() {
    return traitRun;
  }

  public final ArrayList<Integer> getAllNPCIDs(){
    final ArrayList<Integer> npcIDs = new ArrayList<>();

    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
      npcIDs.add(npc.getId());
    }
    return npcIDs;
  }

  public void registerQuestGiverTrait() {
    main.getLogManager().info("Registering Citizens nquestgiver trait...");

    final ArrayList<TraitInfo> toDeregister = new ArrayList<>();

    for (final TraitInfo traitInfo : CitizensAPI.getTraitFactory().getRegisteredTraits()) {
      if (traitInfo.getTraitName() != null && traitInfo.getTraitName().equals("nquestgiver")) {
        toDeregister.add(traitInfo);
      }
    }

    for (final TraitInfo traitInfo : toDeregister) {
      net.citizensnpcs.api.CitizensAPI.getTraitFactory().deregisterTrait(traitInfo);
    }

    net.citizensnpcs.api.CitizensAPI.getTraitFactory()
        .registerTrait(
            net.citizensnpcs.api.trait.TraitInfo.create(QuestGiverNPCTrait.class)
                .withName("nquestgiver"));
    main.getMain().getServer().getPluginManager().registerEvents(new QuestGiverNPCTrait.NPCTPListener(),main.getMain());
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
          Bukkit.getScheduler().runTask(main.getMain(), conversation::bindToAllCitizensNPCs);
        } else {
          conversation.bindToAllCitizensNPCs();
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
        if (trait.getName() != null && trait.getName().equalsIgnoreCase("nquestgiver")) {
          traitsToRemove.add(trait);
        }
      }
      for (final Trait traitToRemove : traitsToRemove) {
        // npc.removeTrait(traitToRemove.getClass()); //TODO: Fucks up loading for some reason
        main.getLogManager()
            .info("Removed nquestgiver trait from NPC with the ID <highlight>" + npc.getId());
      }
      traitsToRemove.clear();
    }

    /*
     * Next, the nquestgiver trait itself which is registered via the Citizens API on startup is being
     * de-registered.
     */
    main.getLogManager().info("Deregistering nquestgiver trait...");
    final ArrayList<TraitInfo> toDeregister = new ArrayList<>();
    for (final TraitInfo traitInfo :
        net.citizensnpcs.api.CitizensAPI.getTraitFactory().getRegisteredTraits()) {
      if (traitInfo.getTraitName() != null && traitInfo.getTraitName().equals("nquestgiver")) {
        toDeregister.add(traitInfo);
      }
    }
    // Actual nquestgiver trait de-registering happens here, to prevent a
    // ConcurrentModificationException
    for (final TraitInfo traitInfo : toDeregister) {
      net.citizensnpcs.api.CitizensAPI.getTraitFactory().deregisterTrait(traitInfo);
    }
  }

  public void handleEscortObjective(final ActiveObjective activeObjective) {
    final NPC npcToEscort =
        CitizensAPI.getNPCRegistry()
            .getById(((EscortNPCObjective) activeObjective.getObjective()).getNpcToEscortID());
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

  public void handleEscortNPCObjectiveForActiveObjective(
      final EscortNPCObjective escortNPCObjective, final ActiveObjectiveHolder activeObjectiveHolder) {
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
      if (!Bukkit.isPrimaryThread()) {
        if (followerTrait == null) {
          Bukkit.getScheduler()
              .runTask(
                  main.getMain(),
                  () -> {
                    FollowTrait followTrait = new FollowTrait();
                    npcToEscort.addTrait(followTrait);
                    handleEscortNPCObjectiveForActiveObjectiveSynchronous(
                        npcToEscort, destinationNPC, followTrait, activeObjectiveHolder, escortNPCObjective);
                  });
        } else {
          final FollowTrait finalFollowerTrait = followerTrait;
          Bukkit.getScheduler()
              .runTask(
                  main.getMain(),
                  () -> {
                    handleEscortNPCObjectiveForActiveObjectiveSynchronous(
                        npcToEscort,
                        destinationNPC,
                        finalFollowerTrait,
                        activeObjectiveHolder,
                        escortNPCObjective);
                  });
        }
      } else {
        if (followerTrait == null) {
          followerTrait = new FollowTrait();
          npcToEscort.addTrait(followerTrait);
        }
        handleEscortNPCObjectiveForActiveObjectiveSynchronous(
            npcToEscort, destinationNPC, followerTrait, activeObjectiveHolder, escortNPCObjective);
      }

    } else {
      if (destinationNPC == null) {
        final Player player = Bukkit.getPlayer(activeObjectiveHolder.getQuestPlayer().getUniqueId());
        if (player != null) {
          player.sendMessage(
              Component.text("The Destination NPC does not exist. Please consult an admin."));
        }
        main.getLogManager()
            .warn(
                "Error: The destination NPC with the ID <highlight>"
                    + npcToEscortID
                    + "</highlight> was not found!");
      }
      if (npcToEscort == null) {
        final Player player = Bukkit.getPlayer(activeObjectiveHolder.getQuestPlayer().getUniqueId());
        if (player != null) {
          player.sendMessage(
              Component.text(
                  "The NPC you have to escort does not exist. Please consult an admin."));
        }
        main.getLogManager()
            .warn(
                "Error: The escort NPC with the ID <highlight>"
                    + npcToEscortID
                    + "</highlight> was not found!");
      }
    }
  }

  private void handleEscortNPCObjectiveForActiveObjectiveSynchronous(
      final NPC npcToEscort,
      final NPC destinationNPC,
      final FollowTrait followerTrait,
      final ActiveObjectiveHolder activeObjectiveHolder,
      final EscortNPCObjective escortNPCObjective) {
    final Player player = Bukkit.getPlayer(activeObjectiveHolder.getQuestPlayer().getUniqueId());
    if (player != null) {
      final Location spawnLocation =
          escortNPCObjective.getSpawnLocation() != null
              ? escortNPCObjective.getSpawnLocation()
              : player.getLocation();
      if (!npcToEscort.isSpawned()) {
        npcToEscort.spawn(spawnLocation);
      }

      if (followerTrait.getFollowing() == null
          || !followerTrait.getFollowing().equals(player)) {
        if (!Bukkit.isPrimaryThread()) {
          Bukkit.getScheduler().runTask(main.getMain(), () -> followerTrait.follow(player));
        } else {
          followerTrait.follow(player);
        }
      }
      final String escortNpcName =
          main.getMiniMessage()
              .serialize(
                  LegacyComponentSerializer.legacyAmpersand().deserialize(npcToEscort.getName()));
      final String destinationNpcName =
          main.getMiniMessage()
              .serialize(
                  LegacyComponentSerializer.legacyAmpersand()
                      .deserialize(destinationNPC.getName()));

      player.sendMessage(
          main.parse(
              "<GREEN>Escort quest started! Please escort <highlight>"
                  + escortNpcName
                  + "</highlight> to <highlight>"
                  + destinationNpcName
                  + "</highlight>."));
    } else {
      main.getLogManager()
          .warn(
              "Error: The escort objective could not be started, because the player with the UUID <highlight>"
                  + activeObjectiveHolder.getQuestPlayer().getUniqueId()
                  + "</highlight> was not found!");
    }
  }



  public void cleanupBuggedNPCs() { //TODO: Currently only works with Citizens :(

    main.getLogManager().info("Checking for bugged NPCs...");

    int buggedNPCsFound = 0;
    int allNPCsFound = 0;
    //Clean up bugged NPCs with quests attached wrongly

    final ArrayList<Trait> traitsToRemove = new ArrayList<>();
    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
      allNPCsFound += 1;

      final NQNPC nqnpc = main.getNPCManager().getOrCreateNQNpc("Citizens", NQNPCID.fromInteger(npc.getId()));
      //No quests attached to NPC => check if it has the trait
      if (main.getQuestManager().getAllQuestsAttachedToNPC(nqnpc).isEmpty() && (main.getConversationManager() != null && main.getConversationManager().getConversationForNPC(nqnpc) == null)) {
        for (final Trait trait : npc.getTraits()) {
          if (trait.getName().contains("questgiver")) {
            traitsToRemove.add(trait);
          }
        }

        if (!Bukkit.isPrimaryThread()) {
          Bukkit.getScheduler().runTask(main.getMain(), () -> {
            for (Trait trait : traitsToRemove) {
              npc.removeTrait(trait.getClass());
            }
          });
        } else {
          for (Trait trait : traitsToRemove) {
            npc.removeTrait(trait.getClass());
          }
        }

        if (!traitsToRemove.isEmpty()) {
          buggedNPCsFound += 1;
          final String mmNpcName = main.getMiniMessage().serialize(
              LegacyComponentSerializer.legacyAmpersand().deserialize(npc.getName().replace("§","&")));

          main.getLogManager().info("  Bugged trait removed from npc with ID <highlight>" + npc.getId() + "</highlight> and name <highlight>" + mmNpcName + "</highlight>!");
        }


      } else {
        //TODO: Remove debug shit or improve performance
        final ArrayList<String> attachedQuestNames = new ArrayList<>();
        for (final Quest attachedQuest : main.getQuestManager().getAllQuestsAttachedToNPC(nqnpc)) {
          attachedQuestNames.add(attachedQuest.getIdentifier());
        }
        main.getLogManager().info("  NPC with the ID: <highlight>" + npc.getId() + "</highlight> is not bugged, because it has the following quests attached: <highlight>" + attachedQuestNames + "</highlight>");

      }
      traitsToRemove.clear();

    }
    if (buggedNPCsFound == 0) {
      main.getLogManager().info("No bugged NPCs found! Amount of checked NPCs: <highlight>" + allNPCsFound + "</highlight>");

    } else {
      main.getLogManager().info("<YELLOW><highlight>" + buggedNPCsFound + "</highlight> bugged NPCs have been found and removed! Amount of checked NPCs: <highlight>" + allNPCsFound + "</highlight>");

    }
  }


}
