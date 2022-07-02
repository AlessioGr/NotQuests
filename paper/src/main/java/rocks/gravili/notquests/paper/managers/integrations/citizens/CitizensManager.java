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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.ConversationSelector;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
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
      final EscortNPCObjective escortNPCObjective, final ActiveQuest activeQuest) {
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
                        npcToEscort, destinationNPC, followTrait, activeQuest, escortNPCObjective);
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
                        activeQuest,
                        escortNPCObjective);
                  });
        }
      } else {
        if (followerTrait == null) {
          followerTrait = new FollowTrait();
          npcToEscort.addTrait(followerTrait);
        }
        handleEscortNPCObjectiveForActiveObjectiveSynchronous(
            npcToEscort, destinationNPC, followerTrait, activeQuest, escortNPCObjective);
      }

    } else {
      if (destinationNPC == null) {
        final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUniqueId());
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
        final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUniqueId());
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
      final ActiveQuest activeQuest,
      final EscortNPCObjective escortNPCObjective) {
    final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUniqueId());
    if (player != null) {
      final Location spawnLocation =
          escortNPCObjective.getSpawnLocation() != null
              ? escortNPCObjective.getSpawnLocation()
              : player.getLocation();
      if (!npcToEscort.isSpawned()) {
        npcToEscort.spawn(spawnLocation);
      }

      if (followerTrait.getFollowingPlayer() == null
          || !followerTrait.getFollowingPlayer().equals(player)) {
        if (!Bukkit.isPrimaryThread()) {
          Bukkit.getScheduler().runTask(main.getMain(), () -> followerTrait.toggle(player, false));
        } else {
          followerTrait.toggle(player, false);
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
                  + activeQuest.getQuestPlayer().getUniqueId()
                  + "</highlight> was not found!");
    }
  }

  public void registerAnyCitizensCommands() {

    main.getLogManager().info("Registering Citizens commands...");

    final PaperCommandManager<CommandSender> manager =
        main.getCommandManager().getPaperCommandManager();

    // Conversations

    final Command.Builder<CommandSender> conversationBuilder =
        main.getCommandManager().getAdminConversationCommandBuilder();

    final Command.Builder<CommandSender> conversationEditBuilder =
        conversationBuilder
            .literal("edit")
            .argument(
                ConversationSelector.of("conversation", main),
                ArgumentDescription.of("Name of the Conversation."));
    manager.command(
        conversationEditBuilder
            .literal("npcs")
            .literal("add")
            .argument(
                IntegerArgument.<CommandSender>newBuilder("NPC")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          ArrayList<String> completions = new ArrayList<>();
                          completions.add("-1");
                          for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                            completions.add("" + npc.getId());
                          }
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[NPC ID]",
                                  "");

                          return completions;
                        })
                    .build(),
                ArgumentDescription.of(
                    "ID of the Citizens NPC which should start the conversation (set to -1 to disable)"))
            .meta(CommandMeta.DESCRIPTION, "Set conversation NPC (-1 = disabled)")
            .handler(
                (context) -> {
                  final Conversation foundConversation = context.get("conversation");
                  final int npcID = context.get("NPC");

                  foundConversation.addNPC(npcID);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<main>NPCs of conversation <highlight>"
                                  + foundConversation.getIdentifier()
                                  + "</highlight> has been added by <highlight2>"
                                  + npcID
                                  + "</highlight2>!"));
                }));

    // Edit commands

    final Command.Builder<CommandSender> editBuilder =
        main.getCommandManager().getAdminEditCommandBuilder();

    final Command.Builder<CommandSender> citizensNPCsBuilder = editBuilder.literal("npcs");
    manager.command(
        citizensNPCsBuilder
            .literal("add")
            .argument(
                IntegerArgument.<CommandSender>newBuilder("npc ID")
                    .withMin(0)
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          ArrayList<String> completions = new ArrayList<>();
                          for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                            completions.add("" + npc.getId());
                          }
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[ID of the NPC you wish to add]",
                                  "(optional: --hideInNPC)");

                          return completions;
                        })
                    .build(),
                ArgumentDescription.of(
                    "ID of the Citizens NPC to whom the Quest should be attached."))
            .flag(
                manager
                    .flagBuilder("hideInNPC")
                    .withDescription(
                        ArgumentDescription.of("Makes the Quest hidden from in the NPC.")))
            .meta(CommandMeta.DESCRIPTION, "Attaches the Quest to a Citizens NPC.")
            .handler(
                (context) -> {
                  final Quest quest = context.get("quest");
                  boolean showInNPC = !context.flags().isPresent("hideInNPC");

                  int npcID = context.get("npc ID");

                  final NPC npc = CitizensAPI.getNPCRegistry().getById(npcID);
                  if (npc != null) {
                    if (!quest.getAttachedNPCsWithQuestShowing().contains(npc)
                        && !quest.getAttachedNPCsWithoutQuestShowing().contains(npc)) {
                      quest.bindToNPC(npc, showInNPC);
                      context
                          .getSender()
                          .sendMessage(
                              main.parse(
                                  "<success>Quest <highlight>"
                                      + quest.getQuestName()
                                      + "</highlight> has been bound to the NPC with the ID <highlight2>"
                                      + npcID
                                      + "</highlight2>! Showing Quest: <highlight>"
                                      + showInNPC
                                      + "</highlight>."));
                    } else {
                      context
                          .getSender()
                          .sendMessage(
                              main.parse(
                                  "<warn>Quest <highlight>"
                                      + quest.getQuestName()
                                      + "</highlight> has already been bound to the NPC with the ID <highlight2>"
                                      + npcID
                                      + "</highlight2>!"));
                    }

                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>NPC with the ID <highlight>"
                                    + npcID
                                    + "</highlight> was not found!"));
                  }
                }));

    manager.command(
        citizensNPCsBuilder
            .literal("clear")
            .meta(CommandMeta.DESCRIPTION, "De-attaches this Quest from all Citizens NPCs.")
            .handler(
                (context) -> {
                  final Quest quest = context.get("quest");
                  quest.clearNPCs();
                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>All NPCs of Quest <highlight>"
                                  + quest.getQuestName()
                                  + "</highlight> have been removed!"));
                }));

    manager.command(
        citizensNPCsBuilder
            .literal("list")
            .meta(
                CommandMeta.DESCRIPTION, "Lists all Citizens NPCs which have this Quest attached.")
            .handler(
                (context) -> {
                  final Quest quest = context.get("quest");
                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<highlight>NPCs bound to quest <highlight2>"
                                  + quest.getQuestName()
                                  + "</highlight2> with Quest showing:"));
                  int counter = 1;
                  for (final NPC npc : quest.getAttachedNPCsWithQuestShowing()) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<highlight>"
                                    + counter
                                    + ".</highlight> <main>ID:</main> <highlight2>"
                                    + npc.getId()));
                    counter++;
                  }
                  counter = 1;
                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<highlight>NPCs bound to quest <highlight2>"
                                  + quest.getQuestName()
                                  + "</highlight2> without Quest showing:"));
                  for (NPC npc : quest.getAttachedNPCsWithoutQuestShowing()) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<highlight>"
                                    + counter
                                    + ".</highlight> <main>ID:</main> <highlight2>"
                                    + npc.getId()));
                    counter++;
                  }
                }));
  }
}
