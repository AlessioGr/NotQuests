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

package rocks.gravili.notquests.paper.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.DurationArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.CategorySelector;
import rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument;
import rocks.gravili.notquests.paper.commands.arguments.MiniMessageSelector;
import rocks.gravili.notquests.paper.commands.arguments.NQNPCSelector;
import rocks.gravili.notquests.paper.commands.arguments.ObjectiveSelector;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.NQNPCResult;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.PredefinedProgressOrder;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;


public class AdminEditCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;

    public AdminEditCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> editBuilder) {
        this.main = main;
        this.manager = manager;


        manager.command(editBuilder.literal("acceptCooldown").literal("complete")
                .literal("set")
                .argument(DurationArgument.of("duration"), ArgumentDescription.of("New accept cooldown measured by quest completion time."))
                .meta(CommandMeta.DESCRIPTION, "Sets the time players have to wait between accepting quests.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final Duration durationCooldown = context.get("duration");
                    final long cooldownInMinutes = durationCooldown.toMinutes();

                    quest.setAcceptCooldownComplete(cooldownInMinutes, true);
                    context.getSender().sendMessage(main.parse(
                            "<success>Complete acceptCooldown for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                    + durationCooldown.toDaysPart() + " days, " + durationCooldown.toHoursPart() + " hours, " + durationCooldown.toMinutesPart() + " minutes" + "</highlight2>!"
                    ));
                }));

        manager.command(editBuilder.literal("acceptCooldown").literal("complete")
                .literal("disable")
                .meta(CommandMeta.DESCRIPTION, "Disables the wait time for players between accepting quests.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    quest.setAcceptCooldownComplete(-1, true);
                    context.getSender().sendMessage(main.parse(
                            "<success>Complete acceptCooldown for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>disabled</highlight2>!"
                    ));
                }));


        final Command.Builder<CommandSender> armorstandBuilder = editBuilder.literal("armorstands");
        handleArmorStands(armorstandBuilder);



        manager.command(editBuilder.literal("description")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current Quest description.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.getSender().sendMessage(main.parse(
                            "<main>Current description of Quest <highlight>" + quest.getIdentifier() + "</highlight>: <highlight2>"
                                    + quest.getObjectiveHolderDescription()
                    ));
                }));
        manager.command(editBuilder.literal("description")
                .literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes current Quest description.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    quest.removeQuestDescription(true);
                    context.getSender().sendMessage(main.parse("<success>Description successfully removed from quest <highlight>"
                            + quest.getIdentifier() + "</highlight>!"
                    ));
                }));


        manager.command(editBuilder.literal("description")
                .literal("set")
                .argument(StringArrayArgument.of("Description",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter new Quest description>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            String rawInput = context.getRawInputJoined();
                            if (lastString.startsWith("{")) {
                                completions.addAll(main.getCommandManager().getAdminCommands().placeholders);
                            } else {
                                if(lastString.startsWith("<")){
                                    for(String color : main.getUtilManager().getMiniMessageTokens()){
                                        completions.add("<"+ color +">");
                                        //Now the closings. First we search IF it contains an opening and IF it doesnt contain more closings than the opening
                                        if(rawInput.contains("<"+color+">")){
                                            if(StringUtils.countMatches(rawInput, "<"+color+">") > StringUtils.countMatches(rawInput, "</"+color+">")){
                                                completions.add("</"+ color +">");
                                            }
                                        }
                                    }
                                }else{
                                    completions.add("<Enter new Quest description>");
                                }
                            }
                            return completions;
                        }
                ), ArgumentDescription.of("Quest description"))
                .meta(CommandMeta.DESCRIPTION, "Sets the new description of the Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    final String description = String.join(" ", (String[]) context.get("Description"));

                    quest.setQuestDescription(description, true);
                    context.getSender().sendMessage(main.parse("<success>Description successfully added to quest <highlight>"
                            + quest.getIdentifier() + "</highlight>! New description: <highlight2>"
                            + quest.getObjectiveHolderDescription()
                    ));
                }));

        manager.command(editBuilder.literal("displayName")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current Quest display name.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.getSender().sendMessage(main.parse(
                            "<main>Current display name of Quest <highlight>" + quest.getIdentifier() + "</highlight>: <highlight2>"
                                    + quest.getQuestDisplayName()
                    ));
                }));
        manager.command(editBuilder.literal("displayName")
                .literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes current Quest display name.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");


                    quest.removeQuestDisplayName(true);
                    context.getSender().sendMessage(main.parse("<success>Display name successfully removed from quest <highlight>"
                            + quest.getIdentifier() + "</highlight>!"
                    ));
                }));

        manager.command(editBuilder.literal("displayName")
        .literal("set")
            .argument(MiniMessageSelector.<CommandSender>newBuilder("DisplayName", main).withPlaceholders().build(), ArgumentDescription.of("Quest display name"))
                .meta(CommandMeta.DESCRIPTION, "Sets the new display name of the Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    final String displayName = String.join(" ", (String[]) context.get("DisplayName"));

                    quest.setQuestDisplayName(displayName, true);
                    context.getSender().sendMessage(main.parse("<success>Display name successfully added to quest <highlight>"
                            + quest.getIdentifier() + "</highlight>! New display name: <highlight2>"
                            + quest.getQuestDisplayName()
                    ));
                }));


        manager.command(editBuilder.literal("limits").literal("completions")
                .argument(IntegerArgument.<CommandSender>newBuilder("max. completions").withMin(-1).withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    completions.add("<amount of maximum completions>");
                    return completions;
                }).build(), ArgumentDescription.of("Maximum amount of completions. Set to -1 for unlimited (default)."))
                .meta(CommandMeta.DESCRIPTION, "Sets the maximum amount of times you can complete this Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int maxCompletions = context.get("max. completions");
                    if (maxCompletions > 0) {
                        quest.setMaxCompletions(maxCompletions, true);
                        context.getSender().sendMessage(main.parse(
                                "<success>Maximum amount of completions for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + maxCompletions + "</highlight2>!"
                        ));
                    } else {
                        quest.setMaxCompletions(-1, true);
                        context.getSender().sendMessage(main.parse(
                                "<success>Maximum amount of completions for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + "unlimited (default)</highlight2>!"
                        ));
                    }
        }));

        manager.command(editBuilder.literal("limits").literal("accepts")
                .argument(IntegerArgument.<CommandSender>newBuilder("max. accepts").withMin(-1).withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    completions.add("<amount of maximum accepts>");
                    return completions;
                }).build(), ArgumentDescription.of("Maximum amount of accepts. Set to -1 for unlimited (default)."))
                .meta(CommandMeta.DESCRIPTION, "Sets the maximum amount of times you can accept this Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int maxAccepts = context.get("max. accepts");
                    if (maxAccepts > 0) {
                        quest.setMaxAccepts(maxAccepts, true);
                        context.getSender().sendMessage(main.parse(
                                "<success>Maximum amount of accepts for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + maxAccepts + "</highlight2>!"
                        ));
                    } else {
                        quest.setMaxAccepts(-1, true);
                        context.getSender().sendMessage(main.parse(
                                "<success>Maximum amount of accepts for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + "unlimited (default)</highlight2>!"
                        ));
                    }
                }));

        manager.command(editBuilder.literal("limits").literal("fails")
                .argument(IntegerArgument.<CommandSender>newBuilder("max. fails").withMin(-1).withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    completions.add("<amount of maximum fails>");
                    return completions;
                }).build(), ArgumentDescription.of("Maximum amount of fails. Set to -1 for unlimited (default)."))
                .meta(CommandMeta.DESCRIPTION, "Sets the maximum amount of times you can fail this Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int maxFails = context.get("max. fails");
                    if (maxFails > 0) {
                        quest.setMaxFails(maxFails, true);
                        context.getSender().sendMessage(main.parse(
                                "<success>Maximum amount of fails for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + maxFails + "</highlight2>!"
                        ));
                    } else {
                        quest.setMaxFails(-1, true);
                        context.getSender().sendMessage(main.parse(
                                "<success>Maximum amount of fails for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + "unlimited (default)</highlight2>!"
                        ));
                    }
                }));


        manager.command(editBuilder.literal("takeEnabled")
                .argument(BooleanArgument.<CommandSender>newBuilder("Take Enabled").withLiberal(true).build(),
                        ArgumentDescription.of("Enabled by default. Yes / no"))
                .meta(CommandMeta.DESCRIPTION, "Sets if players can accept the Quest using /notquests take.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    boolean takeEnabled = context.get("Take Enabled");
                    quest.setTakeEnabled(takeEnabled, true);
                    if (takeEnabled) {
                        context.getSender().sendMessage(main.parse(
                                 "<success>Quest taking (/notquests take) for the Quest <highlight>"
                                        + quest.getIdentifier() + "</highlight> has been set to <highlight2>enabled</highlight2>!"
                        ));
                    } else {
                        context.getSender().sendMessage(main.parse(
                                "<success>Quest taking (/notquests take) for the Quest <highlight>"
                                        + quest.getIdentifier() + "</highlight> has been set to <highlight2>disabled</highlight2>!"
                        ));
                    }
                }));

        manager.command(editBuilder.literal("abortEnabled")
                .argument(BooleanArgument.<CommandSender>newBuilder("Abort Enabled").withLiberal(true).build(),
                        ArgumentDescription.of("Enabled by default. Yes / no"))
                .meta(CommandMeta.DESCRIPTION, "Sets if players can abort the Quest using /notquests abort.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    boolean abortEnabled = context.get("Abort Enabled");
                    quest.setAbortEnabled(abortEnabled, true);
                    if (abortEnabled) {
                        context.getSender().sendMessage(main.parse(
                                "<success>Quest aborting (/notquests abort) for the Quest <highlight>"
                                        + quest.getIdentifier() + "</highlight> has been set to <highlight2>enabled</highlight2>!"
                        ));
                    } else {
                        context.getSender().sendMessage(main.parse(
                                "<success>Quest aborting (/notquests Abort) for the Quest <highlight>"
                                        + quest.getIdentifier() + "</highlight> has been set to <highlight2>disabled</highlight2>!"
                        ));
                    }
                }));

        manager.command(editBuilder.literal("guiItem")

                .argument(ItemStackSelectionArgument.of("material", main), ArgumentDescription.of("Material of item displayed in the Quest take GUI."))
                .flag(
                        manager.flagBuilder("glow")
                                .withDescription(ArgumentDescription.of("Makes the item have the enchanted glow."))
                )
                .meta(CommandMeta.DESCRIPTION, "Sets the item displayed in the Quest take GUI (default: book).")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final boolean glow = context.flags().isPresent("glow");

                    final ItemStackSelection itemStackSelection= context.get("material");
                    ItemStack guiItem = itemStackSelection.toFirstItemStack();
                    if (guiItem == null) {
                        guiItem = new ItemStack(Material.BOOK, 1);
                    }

                    if (glow) {
                        guiItem.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
                        ItemMeta meta = guiItem.getItemMeta();
                        if (meta == null) {
                            meta = Bukkit.getItemFactory().getItemMeta(guiItem.getType());
                        }
                        if (meta != null) {
                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            guiItem.setItemMeta(meta);
                        }

                    }


                    quest.setTakeItem(guiItem, true);
                    context.getSender().sendMessage(main.parse(
                            "<success>Take Item Material for Quest <highlight>" + quest.getIdentifier()
                                    + "</highlight> has been set to <highlight2>" + guiItem.getType().name() + "</highlight2>!"
                    ));


                }));

        final Command.Builder<CommandSender> objectivesBuilder = editBuilder.literal("objectives", "o");
        //qa edit questname objectives

      final String objectiveIDIdentifier = "Objective ID";
      //qa edit questname objectives edit <objectiveID> objectives
      final Command.Builder<CommandSender> objectivesBuilderLevel1 =
          objectivesBuilder
              .literal("edit")
              .argument(
                  ObjectiveSelector.<CommandSender>newBuilder(objectiveIDIdentifier, main, 0).build(),
                  ArgumentDescription.of(objectiveIDIdentifier))
              .literal("objectives", "o");


      final String objectiveIDIdentifier2 = "Objective ID 2";
      final Command.Builder<CommandSender> objectivesBuilderLevel2 =
          objectivesBuilderLevel1
              .literal("edit")
              .argument(
                  ObjectiveSelector.<CommandSender>newBuilder(objectiveIDIdentifier2, main, 1).build(),
                  ArgumentDescription.of(objectiveIDIdentifier2))
              .literal("objectives", "o");

      handleObjectives(objectivesBuilder, 0);
      handleObjectives(objectivesBuilderLevel1, 1);
      handleObjectives(objectivesBuilderLevel2, 2);

      final Command.Builder<CommandSender> requirementsBuilder = editBuilder.literal("requirements");
        handleRequirements(requirementsBuilder);
        final Command.Builder<CommandSender> rewardsBuilder = editBuilder.literal("rewards");
        handleRewards(rewardsBuilder);
        final Command.Builder<CommandSender> triggersBuilder = editBuilder.literal("triggers");
        handleTriggers(triggersBuilder);

        final Command.Builder<CommandSender> categoryBuilder = editBuilder.literal("category");
        handleCategories(categoryBuilder);


      final Command.Builder<CommandSender> npcsBuilder = editBuilder.literal("npcs");
      handleNPCs(npcsBuilder);

    }

  public void handleNPCs(final Command.Builder<CommandSender> builder){
    manager.command(
        builder
            .literal("add")
            .argument(NQNPCSelector.of("NPC", main, false, true), ArgumentDescription.of("ID of the Citizens NPC to whom the Quest should be attached."))
            .flag(
                manager
                    .flagBuilder("hideInNPC")
                    .withDescription(
                        ArgumentDescription.of("Makes the Quest hidden from in the NPC.")))
            .meta(CommandMeta.DESCRIPTION, "Attaches the Quest to a Citizens NPC.")
            .handler(
                (context) -> {
                  final Quest quest = context.get("quest");
                  final boolean showInNPC = !context.flags().isPresent("hideInNPC");

                  final NQNPCResult nqnpcResult = context.get("NPC");

                  if (nqnpcResult.isRightClickSelect()) {//Armor Stands
                    if (context.getSender() instanceof final Player player) {
                      main.getNPCManager().handleRightClickNQNPCSelectionWithAction(
                          (nqnpc) -> {
                            if (!quest.getAttachedNPCsWithQuestShowing().contains(nqnpc)
                                && !quest.getAttachedNPCsWithoutQuestShowing().contains(nqnpc)) {
                              final String result = quest.bindToNPC(nqnpc, showInNPC);
                              if(!result.isBlank()){
                                player.sendMessage(main.parse(result));
                                return;
                              }
                              context
                                  .getSender()
                                  .sendMessage(
                                      main.parse(
                                          "<success>Quest <highlight>"
                                              + quest.getIdentifier()
                                              + "</highlight> has been bound to the NPC with the ID <highlight2>"
                                              + nqnpc.getID().toString()
                                              + "</highlight2>! Showing Quest: <highlight>"
                                              + showInNPC
                                              + "</highlight>."));
                            } else {
                              context
                                  .getSender()
                                  .sendMessage(
                                      main.parse(
                                          "<warn>Quest <highlight>"
                                              + quest.getIdentifier()
                                              + "</highlight> has already been bound to the NPC with the ID <highlight2>"
                                              + nqnpc.getID().toString()
                                              + "</highlight2>!"));
                            }
                          },
                          player,
                          "<success>You have been given an item with which you can attach the NPC to a Quest by rightclicking the NPC. Check your inventory!",
                          "<LIGHT_PURPLE>Attach Quest <highlight>" + quest.getIdentifier() + "</highlight> to this NPC",
                          "<WHITE>Right-click an NPC to attach it to the Quest <highlight>" + quest.getIdentifier() + "</highlight> and ObjectiveID."
                      );

                    } else {
                      context.getSender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                    }

                  }else {
                    final NQNPC nqnpc = nqnpcResult.getNQNPC();
                    if(nqnpc == null){
                      context.getSender().sendMessage(main.parse(
                          "<error>Error: NPC does not exist"
                      ));
                      return;
                    }
                    if (!quest.getAttachedNPCsWithQuestShowing().contains(nqnpc)
                        && !quest.getAttachedNPCsWithoutQuestShowing().contains(nqnpc)) {
                      quest.bindToNPC(nqnpc, showInNPC);
                      context
                          .getSender()
                          .sendMessage(
                              main.parse(
                                  "<success>Quest <highlight>"
                                      + quest.getIdentifier()
                                      + "</highlight> has been bound to the NPC with the ID <highlight2>"
                                      + nqnpc.getID().toString()
                                      + "</highlight2>! Showing Quest: <highlight>"
                                      + showInNPC
                                      + "</highlight>."));
                    } else {
                      context
                          .getSender()
                          .sendMessage(
                              main.parse(
                                  "<warn>Quest <highlight>"
                                      + quest.getIdentifier()
                                      + "</highlight> has already been bound to the NPC with the ID <highlight2>"
                                      + nqnpc.getID().toString()
                                      + "</highlight2>!"));
                    }
                  }

                }));

    manager.command(
        builder
            .literal("clear")
            .meta(CommandMeta.DESCRIPTION, "De-attaches this Quest from all NPCs.")
            .handler(
                (context) -> {
                  final Quest quest = context.get("quest");
                  quest.clearNPCs();
                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>All NPCs of Quest <highlight>"
                                  + quest.getIdentifier()
                                  + "</highlight> have been removed!"));
                }));

    manager.command(
        builder
            .literal("list")
            .meta(
                CommandMeta.DESCRIPTION, "Lists all NPCs which have this Quest attached.")
            .handler(
                (context) -> {
                  final Quest quest = context.get("quest");
                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<highlight>NPCs bound to quest <highlight2>"
                                  + quest.getIdentifier()
                                  + "</highlight2> with Quest showing:"));
                  int counter = 1;
                  for (final NQNPC nqNPC : quest.getAttachedNPCsWithQuestShowing()) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<highlight>"
                                    + counter
                                    + ".</highlight> <main>ID:</main> <highlight2>"
                                    + nqNPC.getID().toString()));
                    counter++;
                  }
                  counter = 1;
                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<highlight>NPCs bound to quest <highlight2>"
                                  + quest.getIdentifier()
                                  + "</highlight2> without Quest showing:"));
                  for (final NQNPC nqNPC : quest.getAttachedNPCsWithoutQuestShowing()) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<highlight>"
                                    + counter
                                    + ".</highlight> <main>ID:</main> <highlight2>"
                                    + nqNPC.getID().toString()));
                    counter++;
                  }
                }));
  }


  public void handleCategories(final Command.Builder<CommandSender> builder){
        manager.command(builder.literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows the current category of this Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.getSender().sendMessage(main.parse(
                            "<main>Category for Quest <highlight>" + quest.getIdentifier() + "</highlight>: <highlight2>"
                                    + quest.getCategory().getCategoryFullName() + "</highlight2>."
                    ));
                }));

        manager.command(builder.literal("set")
                .argument(CategorySelector.of("category", main), ArgumentDescription.of("New category for this Quest."))
                .meta(CommandMeta.DESCRIPTION, "Changes the current category of this Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final Category category = context.get("category");
                    if(quest.getCategory().getCategoryFullName().equalsIgnoreCase(category.getCategoryFullName())){
                        context.getSender().sendMessage(main.parse(
                                "<error> Error: The quest <highlight>" + quest.getIdentifier() + "</highlight> already has the category <highlight2>" + quest.getCategory().getCategoryFullName() + "</highlight2>."
                        ));
                        return;
                    }


                    context.getSender().sendMessage(main.parse(
                            "<success>Category for Quest <highlight>" + quest.getIdentifier() + "</highlight> has successfully been changed from <highlight2>"
                                    + quest.getCategory().getCategoryFullName() + "</highlight2> to <highlight2>" + category.getCategoryFullName() + "</highlight2>!"
                    ));

                    quest.switchCategory(category);

                }));
    }



    public void handleArmorStands(final Command.Builder<CommandSender> builder) {

        manager.command(builder.literal("check")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Gives you an item with which you check what Quests are attached to an armor stand.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();

                    ItemStack itemStack = new ItemStack(Material.LEATHER, 1);
                    //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                    NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-item");

                    ItemMeta itemMeta = itemStack.getItemMeta();
                    List<Component> lore = new ArrayList<>();

                    assert itemMeta != null;


                    itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 4);

                    itemMeta.displayName(main.parse(
                            "<LIGHT_PURPLE>Check Armor Stand"
                    ));
                    //Only paper lore.add(Component.text("§fRight-click an Armor Stand to see which Quests are attached to it."));
                    lore.add(main.parse(
                            "<WHITE>"
                    ));

                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                    itemMeta.lore(lore);

                    itemStack.setItemMeta(itemMeta);

                    player.getInventory().addItem(itemStack);

                    context.getSender().sendMessage(main.parse("<success>You have been given an item with which you can check armor stands!"));
                }));


        manager.command(builder.literal("add")
                .senderType(Player.class)
                .flag(
                        manager.flagBuilder("hideInArmorStand")
                                .withDescription(ArgumentDescription.of("Makes the Quest hidden from armor stands"))
                )
                .meta(CommandMeta.DESCRIPTION, "Gives you an item with which you can add the quest to an armor stand.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    boolean showInArmorStand = !context.flags().isPresent("hideInArmorStand");
                    final Player player = (Player) context.getSender();

                    ItemStack itemStack = new ItemStack(Material.GHAST_TEAR, 1);
                    //give a specialitem. clicking an armorstand with that special item will give it the pdb.

                    NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-item");
                    NamespacedKey QuestNameKey = new NamespacedKey(main.getMain(), "notquests-questname");
                    ItemMeta itemMeta = itemStack.getItemMeta();

                    if (itemMeta == null) {
                        context.getSender().sendMessage(main.parse("<error>Error: ItemMeta is null."));
                        return;
                    }
                    List<Component> lore = new ArrayList<>();

                    if (showInArmorStand) {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 0);
                        itemMeta.displayName(main.parse(
                                "<GOLD>Add showing Quest <highlight>" + quest.getIdentifier() + "</highlight> to Armor Stand"
                        ));
                        lore.add(main.parse(
                                "<WHITE>Hit an armor stand to add the showing Quest <highlight>" + quest.getIdentifier() + "</highlight> to it."
                        ));

                    } else {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
                        itemMeta.displayName(main.parse(
                                "<GOLD>Add non-showing Quest </highlight>" + quest.getIdentifier() + "</highlight> to Armor Stand"
                        ));
                        lore.add(main.parse(
                                "<WHITE>Hit an armor stand to add the non-showing Quest <highlight>" + quest.getIdentifier() + "</highlight> to it."
                        ));

                    }
                    itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getIdentifier());
                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


                    itemMeta.lore(lore);
                    itemStack.setItemMeta(itemMeta);
                    player.getInventory().addItem(itemStack);
                    context.getSender().sendMessage(main.parse("<success>You have been given an item with which you can add this quest to armor stands!"));
                }));

        manager.command(builder.literal("clear")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "This command is not done yet.")
                .handler((context) -> context.getSender().sendMessage(main.parse("<error>Sorry, this command is not done yet! I'll add it in future versions."))));

        manager.command(builder.literal("list")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "This command is not done yet.")
                .handler((context) -> context.getSender().sendMessage(main.parse("<error>Sorry, this command is not done yet! I'll add it in future versions."))));


        manager.command(builder.literal("remove")
                .senderType(Player.class)
                .flag(
                        manager.flagBuilder("hideInArmorStand")
                                .withDescription(ArgumentDescription.of("Sets if you want to remove the Quest which is hidden in an armor stand."))
                )
                .meta(CommandMeta.DESCRIPTION, "Gives you an item with which you can remove the quest from an armor stand.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    boolean showInArmorStand = !context.flags().isPresent("hideInArmorStand");
                    final Player player = (Player) context.getSender();

                    ItemStack itemStack = new ItemStack(Material.NETHER_STAR, 1);
                    //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                    NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-item");
                    NamespacedKey QuestNameKey = new NamespacedKey(main.getMain(), "notquests-questname");

                    ItemMeta itemMeta = itemStack.getItemMeta();

                    if (itemMeta == null) {
                        context.getSender().sendMessage(main.parse("<errorError: ItemMeta is null."));
                        return;
                    }

                    List<Component> lore = new ArrayList<>();

                    if (showInArmorStand) {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 2);
                        itemMeta.displayName(main.parse(
                                "<RED>Remove showing Quest <highlight>" + quest.getIdentifier() + "</highlight> from Armor Stand"
                        ));

                        lore.add(main.parse(
                                "<WHITE>Hit an armor stand to remove the showing Quest <highlight>" + quest.getIdentifier() + "</highlight> from it."
                        ));

                    } else {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 3);

                        itemMeta.displayName(main.parse(
                                "<RED>Remove non-showing Quest <highlight>" + quest.getIdentifier() + "</highlight> from Armor Stand"
                        ));

                        lore.add(main.parse(
                                "<WHITE>Hit an armor stand to remove the non-showing Quest <highlight>" + quest.getIdentifier() + "</highlight> from it."
                        ));
                    }
                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                    itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getIdentifier());


                    itemMeta.lore(lore);

                    itemStack.setItemMeta(itemMeta);

                    player.getInventory().addItem(itemStack);

                    context.getSender().sendMessage(main.parse("<success>You have been given an item with which you can remove this quest from armor stands!"));
                }));
    }

    public void handleObjectives(final Command.Builder<CommandSender> builder, final int level) {
        //Add is handled individually by each objective

      //Builder: qa edit questname objectives edit <objectiveID> objectives

        main.getLogManager().debug("Handling objectives for level <highlight>" + level + "</highlight>...");

        final Command.Builder<CommandSender> predefinedProgressOrderBuilder = builder.literal("predefinedProgressOrder");

      manager.command(predefinedProgressOrderBuilder.literal("show")
          .meta(CommandMeta.DESCRIPTION, "Shows the current predefined order in which the objectives need to be progressed for your quest.")
          .handler((context) -> {
            final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);


            context.getSender().sendMessage(Component.empty());

            final String predefinedProgressOrderString = objectiveHolder.getPredefinedProgressOrder() != null ? (objectiveHolder.getPredefinedProgressOrder().getReadableString())
                : "None"
                ;

            context.getSender().sendMessage(main.parse(
                "<success>Current predefined progress order of Quest <highlight>" + objectiveHolder.getIdentifier()
                    + "</highlight>: <highlight2>" + predefinedProgressOrderString
            ));
          }));

      manager.command(predefinedProgressOrderBuilder.literal("set")
              .literal("none")
          .meta(CommandMeta.DESCRIPTION, "Sets a predefined order in which the objectives need to be progressed for your quest.")
          .handler((context) -> {
            final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);


            objectiveHolder.setPredefinedProgressOrder(null, true);
            context.getSender().sendMessage(Component.empty());
            context.getSender().sendMessage(main.parse(
                "<success>Predefined progress order of Quest <highlight>" + objectiveHolder.getIdentifier()
                    + "</highlight> have been removed!"
            ));
          }));

      manager.command(predefinedProgressOrderBuilder.literal("set")
          .literal("firstToLast")
          .meta(CommandMeta.DESCRIPTION, "Sets a predefined order in which the objectives need to be progressed for your quest.")
          .handler((context) -> {
            final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);


            objectiveHolder.setPredefinedProgressOrder(PredefinedProgressOrder.firstToLast(), true);
            context.getSender().sendMessage(Component.empty());
            context.getSender().sendMessage(main.parse(
                "<success>Predefined progress order of Quest <highlight>" + objectiveHolder.getIdentifier()
                    + "</highlight> have been set to first to last!"
            ));
          }));

      manager.command(predefinedProgressOrderBuilder.literal("set")
          .literal("lastToFirst")
          .meta(CommandMeta.DESCRIPTION, "Sets a predefined order in which the objectives need to be progressed for your quest.")
          .handler((context) -> {
            final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);

            objectiveHolder.setPredefinedProgressOrder(PredefinedProgressOrder.lastToFirst(), true);
            context.getSender().sendMessage(Component.empty());
            context.getSender().sendMessage(main.parse(
                "<success>Predefined progress order of Quest <highlight>" + objectiveHolder.getIdentifier()
                    + "</highlight> have been set to last to first!"
            ));
          }));

      manager.command(predefinedProgressOrderBuilder.literal("set")
          .literal("custom")
          .argument(StringArrayArgument.of("order",
              (context, lastString) -> {
                final List<String> allArgs = context.getRawInput();
                main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter custom order (numbers of objective IDs separated by space)>", "");
                ArrayList<String> completions = new ArrayList<>();
                final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);

                for(final Objective objective : objectiveHolder.getObjectives()){
                  completions.add(objective.getObjectiveID()+"");
                }

                return completions;
              }
          ), ArgumentDescription.of("Custom order. Example: 2 1 3 4 5 6 7 9 8"))
          .meta(CommandMeta.DESCRIPTION, "Sets a predefined order in which the objectives need to be progressed for your quest.")
          .handler((context) -> {
            final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);

            final String[] order = context.get("order");
            final String orderString = String.join(" ", order);
            final ArrayList<String> orderParsed = new ArrayList<>();
            Collections.addAll(orderParsed, order);

            objectiveHolder.setPredefinedProgressOrder(PredefinedProgressOrder.custom(orderParsed), true);
            context.getSender().sendMessage(Component.empty());
            context.getSender().sendMessage(main.parse(
                "<success>Predefined progress order of Quest <highlight>" + objectiveHolder.getIdentifier()
                    + "</highlight> have been set to custom with this order: " + orderString
            ));
          }));

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Removes all objectives from a Quest.")
                .handler((context) -> {
                  final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);

                  objectiveHolder.clearObjectives();
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse(
                            "<success>All objectives of Quest <highlight>" + objectiveHolder.getIdentifier()
                                    + "</highlight> have been removed!"
                    ));
                }));
        manager.command(builder.literal("list")
                .meta(CommandMeta.DESCRIPTION, "Lists all objectives of a Quest.")
                .handler((context) -> {
                  final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);

                  context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<highlight>Objectives for Quest <highlight2>" + objectiveHolder.getIdentifier() + "</highlight2>:"));
                    main.getQuestManager().sendObjectivesAdmin(context.getSender(), objectiveHolder);
                }));


        final String objectiveIDIdentifier = (level == 0 ? "Objective ID" : "Objective ID " + (level+1));

      //Builder: qa edit questname objectives edit <Objective ID> objectives

      //adminEditObjectivesBuilderWithLevels: qa edit questname objectives edit <Objective ID> objectives edit <Objective ID 2>

      final Command.Builder<CommandSender> adminEditObjectivesBuilderWithLevels =
          builder
              .literal("edit")
              .argument(
                  ObjectiveSelector.<CommandSender>newBuilder(objectiveIDIdentifier, main, level).build(),
                  ArgumentDescription.of(objectiveIDIdentifier));


      handleEditObjectives(adminEditObjectivesBuilderWithLevels, level);


    }

    public void handleEditObjectives(final Command.Builder<CommandSender> builder, final int level) {

        final String objectiveIDIdentifier;
        if(level == 0){
          objectiveIDIdentifier = "Objective ID";
        }else {
          objectiveIDIdentifier = "Objective ID " + (level+1);
        }

      main.getLogManager().debug("Handling EDIT objectives for level <highlight>" + level + "</highlight>... objectiveIDIdentifier: " + objectiveIDIdentifier);


      manager.command(builder.literal("location")
                .literal("enable")
                .meta(CommandMeta.DESCRIPTION, "Shows the location to the player.")
                .handler((context) -> {
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);


                    objective.setShowLocation(true, true);

                    context.getSender().sendMessage(main.parse(
                            "<main>The objective with ID <highlight>" + objective.getObjectiveID() + "</highlight> is now showing the location to the player!"
                    ));
                }));

        manager.command(builder.literal("location")
                .literal("disables")
                .meta(CommandMeta.DESCRIPTION, "Disables showing the location to the player.")
                .handler((context) -> {
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                  objective.setShowLocation(false, true);

                    context.getSender().sendMessage(main.parse(
                            "<main>The objective with ID <highlight>" + objective.getObjectiveID() + "</highlight> is now no longer showing the location to the player!"
                    ));
                }));

        manager.command(builder.literal("location")
                .literal("set")
                .argument(WorldArgument.of("world"), ArgumentDescription.of("World name"))
                /* .argumentTriplet(
                         "coords",
                         TypeToken.get(Vector.class),
                         Triplet.of("x", "y", "z"),
                         Triplet.of(Integer.class, Integer.class, Integer.class),
                         (sender, triplet) -> new Vector(triplet.getFirst(), triplet.getSecond(),
                                 triplet.getThird()
                         ),
                         ArgumentDescription.of("Coordinates")
                 )*/ //Commented out, because this somehow breaks flags
                .argument(IntegerArgument.newBuilder("x"), ArgumentDescription.of("X coordinate"))
                .argument(IntegerArgument.newBuilder("y"), ArgumentDescription.of("Y coordinate"))
                .argument(IntegerArgument.newBuilder("z"), ArgumentDescription.of("Z coordinate"))
                .meta(CommandMeta.DESCRIPTION, "Disables showing the location to the player.")
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                    final World world = context.get("world");
                    final Vector coordinates = new Vector(context.get("x"), context.get("y"), context.get("z"));
                    final Location location = coordinates.toLocation(world);

                    objective.setLocation(location, true);
                    objective.setShowLocation(true, true);

                    context.getSender().sendMessage(main.parse(
                            "<main>The objective with ID <highlight>" + objective.getObjectiveID() + "</highlight> is now has a location!"
                    ));
                }));

        manager.command(builder.literal("completionNPC")
                .literal("show", "view")
                .meta(CommandMeta.DESCRIPTION, "Shows the completionNPC of an objective.")
                .handler((context) -> {
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                  context.getSender().sendMessage(main.parse(
                            "<main>The completionNPCID of the objective with the ID <highlight>" + objective.getObjectiveID() + "</highlight> is <highlight2>"
                                    + (objective.getCompletionNPC() != null ? objective.getCompletionNPC().getID() : "null" ) + "</highlight2>!"
                    ));
                }));
        manager.command(builder.literal("completionNPC")
                .literal("set")
                .argument(NQNPCSelector.of("Completion NPC", main, true, true), ArgumentDescription.of("Completion NPC"))
                .meta(CommandMeta.DESCRIPTION, "Sets the completionNPC of an objective.")
                .handler((context) -> {

                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                  final NQNPCResult completionNPCResult = context.get("Completion NPC");

                    if (completionNPCResult.isNone()) {

                        objective.setCompletionNPC(null, true);
                        context.getSender().sendMessage(main.parse(
                                "<success>The completionNPC of the objective with the ID <highlight>" + objective.getObjectiveID() + "</highlight> has been removed!"
                        ));

                    } else if (completionNPCResult.isRightClickSelect()) {//Armor Stands
                      if (context.getSender() instanceof final Player player) {
                        main.getNPCManager().handleRightClickNQNPCSelectionWithAction(
                            (nqnpc) -> {
                              objective.setCompletionNPC(nqnpc, true);
                              player.sendMessage(main.parse(
                                  "<success>The completionArmorStandUUID of the objective with the ID <highlight>" + objective.getObjectiveID() + "</highlight> has been set to the NPC with the ID <highlight2>" + nqnpc.getID().toString()+ "</highlight2> and name <highlight2>" + "todo" + "</highlight2>!"
                              ));
                            },
                            player,
                            "<success>You have been given an item with which you can add the completionNPC of this Objective to an NPC. Check your inventory!",
                            "<LIGHT_PURPLE>Set completionNPC of Quest <highlight>" + objective.getObjectiveHolder().getIdentifier() + "</highlight> to this NPC",
                            "<WHITE>Right-click an NPC to set it as the completionNPC of Quest <highlight>" +  objective.getObjectiveHolder().getIdentifier() + "</highlight> and ObjectiveID <highlight>" + objective.getObjectiveID() + "</highlight>."
                        );

                      } else {
                        context.getSender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                      }

                    } else {
                      objective.setCompletionNPC(completionNPCResult.getNQNPC(), true);

                      context.getSender().sendMessage(main.parse(
                          "<success>The completionNPC of the objective with the ID <highlight>" + objective.getObjectiveID()
                              + "</highlight> has been set to the NPC with the ID <highlight2>" + (completionNPCResult.getNQNPC() != null ? completionNPCResult.getNQNPC().getID() : "null") + "</highlight2>!"
                      ));
                    }
                }));


      final Command.Builder<CommandSender> editObjectiveConditionsUnlockBuilder = builder
          .literal("conditions")
          .literal("unlock");

      handleEditObjectivesUnlockConditions(editObjectiveConditionsUnlockBuilder, level);

      final Command.Builder<CommandSender> editObjectiveConditionsProgressBuilder = builder
          .literal("conditions")
          .literal("progress");

      handleEditObjectivesProgressConditions(editObjectiveConditionsProgressBuilder, level);

      final Command.Builder<CommandSender> editObjectiveConditionsCompleteBuilder = builder
          .literal("conditions")
          .literal("complete");

      handleEditObjectivesCompleteConditions(editObjectiveConditionsCompleteBuilder, level);



        manager.command(builder.literal("description")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current objective description.")
                .handler((context) -> {
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                  context.getSender().sendMessage(main.parse(
                            "<main>Current description of objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>: <highlight2>"
                                    + objective.getObjectiveHolderDescription()
                    ));
                }));
        manager.command(builder.literal("description")
                .literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes current objective description.")
                .handler((context) -> {
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                  objective.removeDescription(true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Description successfully removed from objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New description: <highlight2>"
                                    + objective.getObjectiveHolderDescription()
                    ));
                }));

      manager.command(builder.literal("taskDescription")
          .literal("show")
          .meta(CommandMeta.DESCRIPTION, "Shows current objective task description.")
          .handler((context) -> {
            final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

            context.getSender().sendMessage(main.parse(
                "<main>Current task description of objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>: <highlight2>"
                    + objective.getTaskDescriptionProvided()
            ));
          }));
      manager.command(builder.literal("taskDescription")
          .literal("remove")
          .meta(CommandMeta.DESCRIPTION, "Removes current objective task description.")
          .handler((context) -> {
            final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

            objective.removeTaskDescription(true);
            context.getSender().sendMessage(main.parse(
                "<main>Task description successfully removed from objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New description: <highlight2>"
                    + objective.getTaskDescriptionProvided()
            ));
          }));

        manager.command(builder.literal("description")
                .literal("set")
                .argument(MiniMessageSelector.<CommandSender>newBuilder("Objective Description", main)
                    .withPlaceholders()
                    .build(), ArgumentDescription.of("Objective description"))
                .meta(CommandMeta.DESCRIPTION, "Sets current objective description.")
                .handler((context) -> {
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                  final String description = String.join(" ", (String[]) context.get("Objective Description"));
                    objective.setDescription(description, true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Description successfully added to objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New description: <highlight2>"
                                    + objective.getObjectiveHolderDescription()
                    ));
                }));

      manager.command(builder.literal("taskDescription")
          .literal("set")
          .argument(MiniMessageSelector.<CommandSender>newBuilder("Task Description", main)
              .withPlaceholders()
              .build(), ArgumentDescription.of("Objective task description"))
          .meta(CommandMeta.DESCRIPTION, "Sets current objective task description.")
          .handler((context) -> {
            final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

            final String taskDescription = String.join(" ", (String[]) context.get("Task Description"));
            objective.setTaskDescription(taskDescription, true);
            context.getSender().sendMessage(main.parse(
                "<main>Task Description successfully added to objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New description: <highlight2>"
                    + objective.getTaskDescriptionProvided()
            ));
          }));


        manager.command(builder.literal("displayName")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current objective displayname.")
                .handler((context) -> {
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                  context.getSender().sendMessage(main.parse(
                            "<main>Current displayname of objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>: <highlight2>"
                                    + objective.getDisplayName()
                    ));
                }));
        manager.command(builder.literal("displayName")
                .literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes current objective displayname.")
                .handler((context) -> {
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                  objective.removeDisplayName(true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Displayname successfully removed from objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New displayname: <highlight2>"
                                    + objective.getObjectiveHolderDescription()
                    ));
                }));

        manager.command(builder.literal("displayName")
                .literal("set")
            .argument(MiniMessageSelector.<CommandSender>newBuilder("DisplayName", main).withPlaceholders().build(), ArgumentDescription.of("Quest display name"))

            .meta(CommandMeta.DESCRIPTION, "Sets current objective displayname.")
                .handler((context) -> {
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                  final String displayName = String.join(" ", (String[]) context.get("DisplayName"));
                    objective.setDisplayName(displayName, true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Displayname successfully added to objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New displayname: <highlight2>"
                                    + objective.getDisplayName()
                    ));
                }));


        manager.command(builder.literal("info")
                .meta(CommandMeta.DESCRIPTION, "Shows everything there is to know about this objective.")
                .handler((context) -> {
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                  context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse(
                            "<highlight>Information of objective with the ID <highlight2>" + objective.getObjectiveID()
                                    + "</highlight2> from Quest <highlight2>" + objective.getObjectiveHolder().getIdentifier() + "</highlight2>:"
                    ));
                    context.getSender().sendMessage(main.parse(
                            "<highlight>Objective Type: <main>" + main.getObjectiveManager().getObjectiveType(objective.getClass())
                    ));
                    context.getSender().sendMessage(main.parse(
                            "<highlight>Objective Content:</highlight>"
                    ));

                    context.getSender().sendMessage(main.parse(main.getQuestManager().getObjectiveTaskDescription(objective, false, null)));

                    context.getSender().sendMessage(main.parse(
                            "<highlight>Objective DisplayName: <main>" + objective.getDisplayName()
                    ));
                    context.getSender().sendMessage(main.parse(
                            "<highlight>Objective Description: <main>" + objective.getObjectiveHolderDescription()
                    ));

                    {
                      context.getSender().sendMessage(main.parse(
                          "<highlight>Objective unlock conditions:"
                      ));
                      int counter = 1;
                      for (final Condition condition : objective.getUnlockConditions()) {
                        if(context.getSender() instanceof Player player){
                          context.getSender().sendMessage(main.parse(
                              "    <highlight>" + counter + ". Description: " + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))
                          ));
                        }else {
                          context.getSender().sendMessage(main.parse(
                              "    <highlight>" + counter + ". Description: " + condition.getConditionDescription(null)
                          ));
                        }

                        counter++;
                      }
                    }

                  {
                    context.getSender().sendMessage(main.parse(
                        "<highlight>Objective progress conditions:"
                    ));
                    int counter = 1;
                    for (final Condition condition : objective.getProgressConditions()) {
                      if(context.getSender() instanceof Player player){
                        context.getSender().sendMessage(main.parse(
                            "    <highlight>" + counter + ". Description: " + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))
                        ));
                      }else {
                        context.getSender().sendMessage(main.parse(
                            "    <highlight>" + counter + ". Description: " + condition.getConditionDescription(null)
                        ));
                      }

                      counter++;
                    }
                  }

                  {
                    context.getSender().sendMessage(main.parse(
                        "<highlight>Objective complete conditions:"
                    ));
                    int counter = 1;
                    for (final Condition condition : objective.getCompleteConditions()) {
                      if(context.getSender() instanceof Player player){
                        context.getSender().sendMessage(main.parse(
                            "    <highlight>" + counter + ". Description: " + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))
                        ));
                      }else {
                        context.getSender().sendMessage(main.parse(
                            "    <highlight>" + counter + ". Description: " + condition.getConditionDescription(null)
                        ));
                      }

                      counter++;
                    }
                  }

                }));

        manager.command(builder.literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes the objective from the Quest.")
                .handler((context) -> {
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                   objective.getObjectiveHolder().removeObjective(objective);
                    context.getSender().sendMessage(main.parse(
                            "<success>Objective with the ID <highlight>" + objective.getObjectiveID() + "</highlight> has been successfully removed from Quest <highlight2>"
                                    + objective.getObjectiveHolder().getIdentifier() + "</highlight2>!"
                        ));
                }));


        final Command.Builder<CommandSender> rewardsBuilder = builder.literal("rewards");
        handleObjectiveRewards(rewardsBuilder, level);
    }


    public void handleRequirements(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each requirement


        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the requirements this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.getSender().sendMessage(main.parse("<highlight>Requirements for Quest <highlight2>" + quest.getIdentifier() + "</highlight2>:"));
                    for (final Condition condition : quest.getRequirements()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + condition.getConditionID() + ".</highlight> <main>" + condition.getConditionType()));
                        if(context.getSender() instanceof Player player){
                            context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        }else {
                            context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(null)));
                        }
                    }
                }));

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Clears all the requirements this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    quest.clearRequirements();
                    context.getSender().sendMessage(main.parse("<main>All requirements of Quest <highlight>" + quest.getIdentifier() + "</highlight> have been removed!"));
                }));

        final Command.Builder<CommandSender> editQuestRequirementsBuilder = builder.literal("edit")
                .argument(IntegerArgument.<CommandSender>newBuilder("Requirement ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Requirement ID]", "[...]");

                                    ArrayList<String> completions = new ArrayList<>();

                                    final Quest quest = context.get("quest");
                                    for (final Condition condition : quest.getRequirements()) {
                                        completions.add("" + condition.getConditionID());
                                    }

                                    return completions;
                                }
                        ));

        manager.command(editQuestRequirementsBuilder.literal("delete", "remove")
                .meta(CommandMeta.DESCRIPTION, "Removes a requirement from this Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int conditionID = context.get("Requirement ID");
                    Condition condition = quest.getRequirementFromID(conditionID);

                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Requirement with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }
                    quest.removeRequirement(condition);


                    context.getSender().sendMessage(main.parse("<main>The requirement with the ID <highlight>" + conditionID + "</highlight> of Quest <highlight2>" + quest.getIdentifier() + "</highlight2> has been removed!"));
                }));


        manager.command(editQuestRequirementsBuilder.literal("description")
                .literal("set")
                        .argument(MiniMessageSelector.<CommandSender>newBuilder("description", main).withPlaceholders().build(), ArgumentDescription.of("Quest requirementdescription"))
                .meta(CommandMeta.DESCRIPTION, "Sets the new description of the Quest requirement.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int conditionID = context.get("Requirement ID");
                    Condition condition = quest.getRequirementFromID(conditionID);
                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Requirement with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String description = String.join(" ", (String[]) context.get("description"));

                    condition.setDescription(description);

                    quest.getCategory().getQuestsConfig().set("quests." + quest.getIdentifier() + ".requirements." + conditionID + ".description", description);
                    quest.getCategory().saveQuestsConfig();

                    context.getSender().sendMessage(main.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of quest <highlight2>"
                            + quest.getIdentifier() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));


      manager.command(editQuestRequirementsBuilder.literal("hidden")
          .literal("set")
          .argument(
              BooleanVariableValueArgument.newBuilder("hiddenStatusExpression", main, null),
              ArgumentDescription.of("Expression"))
          .meta(CommandMeta.DESCRIPTION, "Sets the new hidden status of the Quest requirement.")
          .handler((context) -> {
            final Quest quest = context.get("quest");
            int conditionID = context.get("Requirement ID");
            Condition condition = quest.getRequirementFromID(conditionID);
            if(condition == null){
              context.getSender().sendMessage(main.parse(
                  "<error>Requirement with the ID <highlight>" + conditionID + "</highlight> was not found!"
              ));
              return;
            }

            final String hiddenStatusExpression = context.get("hiddenStatusExpression");
            final NumberExpression hiddenExpression = new NumberExpression(main, hiddenStatusExpression);

            condition.setHidden(hiddenExpression);

            quest.getCategory().getQuestsConfig().set("quests." + quest.getIdentifier() + ".requirements." + conditionID + ".hiddenStatusExpression", hiddenStatusExpression);
            quest.getCategory().saveQuestsConfig();

            context.getSender().sendMessage(main.parse("<success>Hidden status successfully added to condition with ID <highlight>" + conditionID + "</highlight> of quest <highlight2>"
                + quest.getIdentifier() + "</highlight2>! New hidden status: <highlight2>"
                + condition.getHiddenExpression()
            ));
          }));

        manager.command(editQuestRequirementsBuilder.literal("description")
                .literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes the description of the Quest requirement.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int conditionID = context.get("Requirement ID");
                    Condition condition = quest.getRequirementFromID(conditionID);
                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Requirement with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    condition.removeDescription();

                    quest.getCategory().getQuestsConfig().set("quests." + quest.getIdentifier() + ".requirements." + conditionID + ".description", "");
                    quest.getCategory().saveQuestsConfig();


                    context.getSender().sendMessage(main.parse("<success>Description successfully removed from condition with ID <highlight>" + conditionID + "</highlight> of quest <highlight2>"
                            + quest.getIdentifier() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editQuestRequirementsBuilder.literal("description")
                .literal("show", "check")
                .meta(CommandMeta.DESCRIPTION, "Shows the description of the Quest requirement.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int conditionID = context.get("Requirement ID");
                    Condition condition = quest.getRequirementFromID(conditionID);
                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Requirement with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    context.getSender().sendMessage(main.parse("<main>Description of condition with ID <highlight>" + conditionID + "</highlight> of quest <highlight2>"
                            + quest.getIdentifier() + "</highlight2>:\n"
                            + condition.getDescription()
                    ));
                }));

    }

  public void handleEditObjectivesUnlockConditions(final Command.Builder<CommandSender> builder, final int level) {

    manager.command(builder
        .literal("clear")
        .meta(CommandMeta.DESCRIPTION, "Removes all unlock conditions from this objective.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          objective.clearUnlockConditions();
          context.getSender().sendMessage(main.parse(
              "<success>All unlock conditions of objective with ID <highlight>" + objective.getObjectiveID()
                  + "</highlight> have been removed!"
          ));

        }));

    manager.command(builder
        .literal("list", "show")
        .meta(CommandMeta.DESCRIPTION, "Lists all unlock conditions of this objective.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          context.getSender().sendMessage(main.parse(
              "<highlight>Unlock conditions of objective with ID <highlight2>" + objective.getObjectiveID()
                  + "</highlight2>:"
          ));
          for (Condition condition : objective.getUnlockConditions()) {
            context.getSender().sendMessage(main.parse("<highlight>" + condition.getConditionID() + ".</highlight> <main>" + condition.getConditionType() + "</main>"));
            if(context.getSender() instanceof Player player){
              context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
            }else{
              context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(null)));
            }
          }

          if (objective.getUnlockConditions().size() == 0) {
            context.getSender().sendMessage(main.parse("<warn>This objective has no unlock conditions!"));
          }
        }));


    final Command.Builder<CommandSender> editObjectiveConditionsBuilder = builder
        .literal("edit")
        .argument(IntegerArgument.<CommandSender>newBuilder("Condition ID").withMin(1).withSuggestionsProvider(
            (context, lastString) -> {
              final List<String> allArgs = context.getRawInput();
              main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Condition ID]", "[...]");

              ArrayList<String> completions = new ArrayList<>();

              final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

              for (final Condition condition : objective.getUnlockConditions()) {
                completions.add("" + condition.getConditionID());
              }

              return completions;
            }
        ));

    manager.command(editObjectiveConditionsBuilder
        .literal("delete", "remove")
        .meta(CommandMeta.DESCRIPTION, "Removes an unlock condition from this Objective.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getUnlockConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Unlock condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }

          objective.removeUnlockCondition(condition, true);
          context.getSender().sendMessage(main.parse("<main>The unlock condition with the ID <highlight>" + conditionID + "</highlight> of Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> has been removed!"));
        }));


    manager.command(editObjectiveConditionsBuilder.literal("description")
        .literal("set")
        .argument(MiniMessageSelector.<CommandSender>newBuilder("description", main).withPlaceholders().build(), ArgumentDescription.of("Objective condition description"))
        .meta(CommandMeta.DESCRIPTION, "Sets the new description of the Objective unlock condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getUnlockConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }

          final String description = String.join(" ", (String[]) context.get("description"));

          condition.setDescription(description);


          objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", description);
          objective.getObjectiveHolder().saveConfig();

          context.getSender().sendMessage(main.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
              + condition.getDescription()
          ));
        }));

    manager.command(editObjectiveConditionsBuilder.literal("hidden")
        .literal("set")
        .argument(
            BooleanVariableValueArgument.newBuilder("hiddenStatusExpression", main, null),
            ArgumentDescription.of("Expression"))
        .meta(CommandMeta.DESCRIPTION, "Sets the new hidden status of the Objective unlock condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getUnlockConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }

          final String hiddenStatusExpression = context.get("hiddenStatusExpression");
          final NumberExpression hiddenExpression = new NumberExpression(main, hiddenStatusExpression);

          condition.setHidden(hiddenExpression);

          objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".hiddenStatusExpression", hiddenStatusExpression);
          objective.getObjectiveHolder().saveConfig();

          context.getSender().sendMessage(main.parse("<success>Hidden status successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>! New hidden status: <highlight2>"
              + condition.getHiddenExpression()
          ));
        }));

    manager.command(editObjectiveConditionsBuilder.literal("description")
        .literal("remove", "delete")
        .meta(CommandMeta.DESCRIPTION, "Removes the description of the objective unlock condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getUnlockConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Unlock condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }


          condition.removeDescription();

          objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", "");
          objective.getObjectiveHolder().saveConfig();

          context.getSender().sendMessage(main.parse("<success>Description successfully removed from unlock condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
              + condition.getDescription()
          ));
        }));

    manager.command(editObjectiveConditionsBuilder.literal("description")
        .literal("show", "check")
        .meta(CommandMeta.DESCRIPTION, "Shows the description of the objective unlock condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getUnlockConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Unlock condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }


          context.getSender().sendMessage(main.parse("<main>Description of unlock condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>:\n"
              + condition.getDescription()
          ));
        }));
  }

  public void handleEditObjectivesProgressConditions(final Command.Builder<CommandSender> builder, final int level) {
    manager.command(builder
        .literal("clear")
        .meta(CommandMeta.DESCRIPTION, "Removes all progress conditions from this objective.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          objective.clearProgressConditions();
          context.getSender().sendMessage(main.parse(
              "<success>All progress conditions of objective with ID <highlight>" + objective.getObjectiveID()
                  + "</highlight> have been removed!"
          ));

        }));

    manager.command(builder
        .literal("list", "show")
        .meta(CommandMeta.DESCRIPTION, "Lists all progress conditions of this objective.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          context.getSender().sendMessage(main.parse(
              "<highlight>Progress conditions of objective with ID <highlight2>" + objective.getObjectiveID()
                  + "</highlight2>:"
          ));
          for (Condition condition : objective.getProgressConditions()) {
            context.getSender().sendMessage(main.parse("<highlight>" + condition.getConditionID() + ".</highlight> <main>" + condition.getConditionType() + "</main>"));
            if(context.getSender() instanceof Player player){
              context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
            }else{
              context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(null)));
            }
          }

          if (objective.getProgressConditions().size() == 0) {
            context.getSender().sendMessage(main.parse("<warn>This objective has no progress conditions!"));
          }
        }));


    final Command.Builder<CommandSender> editObjectiveConditionsBuilder = builder
        .literal("edit")
        .argument(IntegerArgument.<CommandSender>newBuilder("Condition ID").withMin(1).withSuggestionsProvider(
            (context, lastString) -> {
              final List<String> allArgs = context.getRawInput();
              main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Condition ID]", "[...]");

              ArrayList<String> completions = new ArrayList<>();

              final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

              for (final Condition condition : objective.getProgressConditions()) {
                completions.add("" + condition.getConditionID());
              }

              return completions;
            }
        ));

    manager.command(editObjectiveConditionsBuilder
        .literal("delete", "remove")
        .meta(CommandMeta.DESCRIPTION, "Removes an progress condition from this Objective.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getProgressConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Progress condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }

          objective.removeProgressCondition(condition, true);
          context.getSender().sendMessage(main.parse("<main>The progress condition with the ID <highlight>" + conditionID + "</highlight> of Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> has been removed!"));
        }));


    manager.command(editObjectiveConditionsBuilder.literal("description")
        .literal("set")
        .argument(MiniMessageSelector.<CommandSender>newBuilder("description", main).withPlaceholders().build(), ArgumentDescription.of("Objective condition description"))
        .meta(CommandMeta.DESCRIPTION, "Sets the new description of the Objective progress condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getProgressConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }

          final String description = String.join(" ", (String[]) context.get("description"));

          condition.setDescription(description);

          objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", description);
          objective.getObjectiveHolder().saveConfig();

          context.getSender().sendMessage(main.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
              + condition.getDescription()
          ));
        }));

    manager.command(editObjectiveConditionsBuilder.literal("hidden")
        .literal("set")
        .argument(
            BooleanVariableValueArgument.newBuilder("hiddenStatusExpression", main, null),
            ArgumentDescription.of("Expression"))
        .meta(CommandMeta.DESCRIPTION, "Sets the new hidden status of the Objective progress condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getProgressConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }

          final String hiddenStatusExpression = context.get("hiddenStatusExpression");
          final NumberExpression hiddenExpression = new NumberExpression(main, hiddenStatusExpression);

          condition.setHidden(hiddenExpression);

          objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".hiddenStatusExpression", hiddenStatusExpression);
          objective.getObjectiveHolder().saveConfig();

          context.getSender().sendMessage(main.parse("<success>Hidden status successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>! New hidden status: <highlight2>"
              + condition.getHiddenExpression()
          ));
        }));

    manager.command(editObjectiveConditionsBuilder.literal("description")
        .literal("remove", "delete")
        .meta(CommandMeta.DESCRIPTION, "Removes the description of the objective progress condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getProgressConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Progress condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }


          condition.removeDescription();

          objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", "");
          objective.getObjectiveHolder().saveConfig();

          context.getSender().sendMessage(main.parse("<success>Description successfully removed from progress condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
              + condition.getDescription()
          ));
        }));

    manager.command(editObjectiveConditionsBuilder.literal("description")
        .literal("show", "check")
        .meta(CommandMeta.DESCRIPTION, "Shows the description of the objective progress condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getProgressConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Progress condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }


          context.getSender().sendMessage(main.parse("<main>Description of progress condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>:\n"
              + condition.getDescription()
          ));
        }));
  }

  public void handleEditObjectivesCompleteConditions(final Command.Builder<CommandSender> builder, final int level) {
    manager.command(builder
        .literal("clear")
        .meta(CommandMeta.DESCRIPTION, "Removes all complete conditions from this objective.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          objective.clearCompleteConditions();
          context.getSender().sendMessage(main.parse(
              "<success>All complete conditions of objective with ID <highlight>" + objective.getObjectiveID()
                  + "</highlight> have been removed!"
          ));

        }));

    manager.command(builder
        .literal("list", "show")
        .meta(CommandMeta.DESCRIPTION, "Lists all complete conditions of this objective.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          context.getSender().sendMessage(main.parse(
              "<highlight>Complete conditions of objective with ID <highlight2>" + objective.getObjectiveID()
                  + "</highlight2>:"
          ));
          for (Condition condition : objective.getCompleteConditions()) {
            context.getSender().sendMessage(main.parse("<highlight>" + condition.getConditionID() + ".</highlight> <main>" + condition.getConditionType() + "</main>"));
            if(context.getSender() instanceof Player player){
              context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
            }else{
              context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(null)));
            }
          }

          if (objective.getCompleteConditions().size() == 0) {
            context.getSender().sendMessage(main.parse("<warn>This objective has no complete conditions!"));
          }
        }));


    final Command.Builder<CommandSender> editObjectiveConditionsBuilder = builder
        .literal("edit")
        .argument(IntegerArgument.<CommandSender>newBuilder("Condition ID").withMin(1).withSuggestionsProvider(
            (context, lastString) -> {
              final List<String> allArgs = context.getRawInput();
              main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Condition ID]", "[...]");

              ArrayList<String> completions = new ArrayList<>();

              final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

              for (final Condition condition : objective.getCompleteConditions()) {
                completions.add("" + condition.getConditionID());
              }

              return completions;
            }
        ));

    manager.command(editObjectiveConditionsBuilder
        .literal("delete", "remove")
        .meta(CommandMeta.DESCRIPTION, "Removes an complete condition from this Objective.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getCompleteConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Complete condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }

          objective.removeCompleteCondition(condition, true);
          context.getSender().sendMessage(main.parse("<main>The complete condition with the ID <highlight>" + conditionID + "</highlight> of Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> has been removed!"));
        }));


    manager.command(editObjectiveConditionsBuilder.literal("description")
        .literal("set")
        .argument(MiniMessageSelector.<CommandSender>newBuilder("description", main).withPlaceholders().build(), ArgumentDescription.of("Objective condition description"))
        .meta(CommandMeta.DESCRIPTION, "Sets the new description of the Objective complete condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getCompleteConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }

          final String description = String.join(" ", (String[]) context.get("description"));

          condition.setDescription(description);

          objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", description);
          objective.getObjectiveHolder().saveConfig();

          context.getSender().sendMessage(main.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
              + condition.getDescription()
          ));
        }));

    manager.command(editObjectiveConditionsBuilder.literal("hidden")
        .literal("set")
        .argument(
            BooleanVariableValueArgument.newBuilder("hiddenStatusExpression", main, null),
            ArgumentDescription.of("Expression"))
        .meta(CommandMeta.DESCRIPTION, "Sets the new hidden status of the Objective complete condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getCompleteConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }

          final String hiddenStatusExpression = context.get("hiddenStatusExpression");
          final NumberExpression hiddenExpression = new NumberExpression(main, hiddenStatusExpression);

          condition.setHidden(hiddenExpression);

          objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".hiddenStatusExpression", hiddenStatusExpression);
          objective.getObjectiveHolder().saveConfig();

          context.getSender().sendMessage(main.parse("<success>Hidden status successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>! New hidden status: <highlight2>"
              + condition.getHiddenExpression()
          ));
        }));

    manager.command(editObjectiveConditionsBuilder.literal("description")
        .literal("remove", "delete")
        .meta(CommandMeta.DESCRIPTION, "Removes the description of the objective complete condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getCompleteConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Complete condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }


          condition.removeDescription();

          objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", "");
          objective.getObjectiveHolder().saveConfig();

          context.getSender().sendMessage(main.parse("<success>Description successfully removed from complete condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
              + condition.getDescription()
          ));
        }));

    manager.command(editObjectiveConditionsBuilder.literal("description")
        .literal("show", "check")
        .meta(CommandMeta.DESCRIPTION, "Shows the description of the objective complete condition.")
        .handler((context) -> {
          final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

          int conditionID = context.get("Condition ID");
          Condition condition = objective.getCompleteConditionFromID(conditionID);

          if(condition == null){
            context.getSender().sendMessage(main.parse(
                "<error>Complete condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
            ));
            return;
          }


          context.getSender().sendMessage(main.parse("<main>Description of complete condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
              + objective.getObjectiveID() + "</highlight2>:\n"
              + condition.getDescription()
          ));
        }));
  }

    public void handleRewards(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each reward

        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the rewards this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.getSender().sendMessage(main.parse("<highlight>Rewards for Quest <highlight2>" + quest.getIdentifier() + "</highlight2>:"));
                    for (final Action action : quest.getRewards()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + action.getActionID() + ".</highlight> <main>" + action.getActionType()));
                        if(context.getSender() instanceof Player player){
                            context.getSender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + action.getActionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        }else {
                            context.getSender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + action.getActionDescription(null)));
                        }
                    }

                }));

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Clears all the rewards this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    quest.clearRewards();
                    context.getSender().sendMessage(main.parse("<success>All rewards of Quest <highlight>" + quest.getIdentifier() + "</highlight> have been removed!"));
                }));


        final Command.Builder<CommandSender> editRewardsBuilder = builder.literal("edit")
                .argument(IntegerArgument.<CommandSender>newBuilder("Reward ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Reward ID]", "[...]");

                                    ArrayList<String> completions = new ArrayList<>();

                                    final Quest quest = context.get("quest");
                                    for (final Action action : quest.getRewards()) {
                                        completions.add("" + action.getActionID());
                                    }

                                    return completions;
                                }
                        )/*.withParser((context, lastString) -> { //TODO: Fix this parser. It isn't run at all.
                            final int ID = context.get("Reward ID");
                            final Quest quest = context.get("quest");
                            final Action foundReward = quest.getRewards().get(ID - 1);
                            if (foundReward == null) {
                                return ArgumentParseResult.failure(new IllegalArgumentException("Reward with the ID '" + ID + "' does not belong to Quest '" + quest.getIdentifier() + "'!"));
                            } else {
                                return ArgumentParseResult.success(ID);
                            }
                        })
                        , ArgumentDescription.of("Reward ID")*/);
        handleEditRewards(editRewardsBuilder);
    }

    public void handleObjectiveRewards(final Command.Builder<CommandSender> builder, final int level) {
        //Add is handled individually by each reward

        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the rewards this Objective has.")
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    context.getSender().sendMessage(main.parse("<highlight>Rewards for Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> of Quest <highlight2>" + objective.getObjectiveHolder().getIdentifier() + "</highlight2>:"));
                    for (final Action action : objective.getRewards()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + action.getActionID() + ".</highlight> <main>" + action.getActionType()));
                        if(context.getSender() instanceof Player player){
                            context.getSender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + action.getActionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        }else{
                            context.getSender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + action.getActionDescription(null)));
                        }
                    }
                }));

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Clears all the rewards this Objective has.")
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    objective.clearRewards();
                    context.getSender().sendMessage(main.parse("<success>All rewards of Objective with ID <highlight>" + objective.getObjectiveID() + "</highlight> of Quest <highlight2>" + objective.getObjectiveHolder().getIdentifier()+ "</highlight2> have been removed!"));
                }));


        final Command.Builder<CommandSender> editRewardsBuilder = builder.literal("edit")
                .argument(IntegerArgument.<CommandSender>newBuilder("Reward ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Reward ID]", "[...]");

                                    ArrayList<String> completions = new ArrayList<>();

                                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                                    for (final Action action : objective.getRewards()) {
                                        completions.add("" + action.getActionID());
                                    }

                                    return completions;
                                }
                        ).withParser((context, lastString) -> { //TODO: Fix this parser. It isn't run at all.
                            final int ID = context.get("Reward ID");
                            final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                            final Action foundReward = objective.getRewardFromID(ID);
                            if (foundReward == null) {
                                return ArgumentParseResult.failure(new IllegalArgumentException("Reward with the ID '" + ID + "' does not belong to Objective with ID " + objective.getObjectiveID() + " of Quest '" + objective.getObjectiveHolder().getIdentifier() + "'!"));
                            } else {
                                return ArgumentParseResult.success(ID);
                            }
                        })
                        , ArgumentDescription.of("Reward ID"));
        handleObjectiveEditRewards(editRewardsBuilder, level);
    }

    public void handleObjectiveEditRewards(final Command.Builder<CommandSender> builder, final int level) {
        manager.command(builder.literal("info")
                .meta(CommandMeta.DESCRIPTION, "Shows everything there is to know about this reward.")
                .handler((context) -> {
                    final int ID = context.get("Reward ID");
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }

                    context.getSender().sendMessage(main.parse(
                            "<main>Reward <highlight>" + ID + "</highlight> for Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> of Quest <highlight2>" + objective.getObjectiveHolder().getIdentifier() + "</highlight2>:"
                    ));

                    if(context.getSender() instanceof Player player){
                        context.getSender().sendMessage(main.parse(
                                "<unimportant>--</unimportant> <main>" + foundReward.getActionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))
                        ));
                    }else{
                        context.getSender().sendMessage(main.parse(
                                "<unimportant>--</unimportant> <main>" + foundReward.getActionDescription(null)
                        ));
                    }


                }));

        manager.command(builder.literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes the reward from the Quest.")
                .handler((context) -> {
                    final int ID = context.get("Reward ID");
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    objective.removeReward(foundReward, true);
                    context.getSender().sendMessage(main.parse(
                            "<success>The reward with the ID <highlight>" + ID + "</highlight> has been removed from the Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> of Quest <highlight2>"
                                    + objective.getObjectiveHolder().getIdentifier()+ "</highlight2>!"
                    ));
                }));

        manager.command(builder.literal("displayName")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current reward Display Name.")
                .handler((context) -> {
                    final int ID = context.get("Reward ID");
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    if (foundReward.getActionName().isBlank()) {
                        context.getSender().sendMessage(main.parse(
                                "<main>This reward has no display name set."
                        ));
                    } else {
                        context.getSender().sendMessage(main.parse(
                                "<main>Reward display name: <highlight>" + foundReward.getActionName() + "</highlight>"
                        ));
                    }
                }));

        manager.command(builder.literal("displayName")
                .literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes current reward Display Name.")
                .handler((context) -> {
                    final int ID = context.get("Reward ID");
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    foundReward.removeActionName();
                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".rewards." + ID + ".displayName", null);
                    objective.getObjectiveHolder().saveConfig();
                    context.getSender().sendMessage(main.parse(
                            "<success>Display Name of reward with the ID <highlight>" + ID + "</highlight> has been removed successfully."
                    ));
                }));

        manager.command(builder.literal("displayName")
                .literal("set")
                .argument(MiniMessageSelector.<CommandSender>newBuilder("DisplayName", main).withPlaceholders().build(), ArgumentDescription.of("Reward display name"))
                .meta(CommandMeta.DESCRIPTION, "Sets new reward Display Name. Only rewards with a Display Name will be displayed.")
                .handler((context) -> {
                    final int ID = context.get("Reward ID");
                  final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }

                    final String displayName = String.join(" ", (String[]) context.get("DisplayName"));


                    foundReward.setActionName(displayName);
                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath()+ ".objectives." + objective.getObjectiveID() + ".rewards." + ID + ".displayName", foundReward.getActionName());
                    objective.getObjectiveHolder().saveConfig();
                    context.getSender().sendMessage(main.parse(
                            "<success>Display Name successfully added to reward with ID <highlight>" + ID + "</highlight>! New display name: <highlight2>"
                                    + foundReward.getActionName()
                    ));
                }));
    }

    public void handleEditRewards(final Command.Builder<CommandSender> builder) {
        manager.command(builder.literal("info")
                .meta(CommandMeta.DESCRIPTION, "Shows everything there is to know about this reward.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int ID = context.get("Reward ID");
                    final Action foundReward = quest.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }

                    context.getSender().sendMessage(main.parse(
                            "<main>Reward <highlight>" + ID + "</highlight> for Quest <highlight2>" + quest.getIdentifier() + "</highlight2>:"
                    ));

                    if(context.getSender() instanceof Player player){
                        context.getSender().sendMessage(main.parse(
                                "<unimportant>--</unimportant> <main>" + foundReward.getActionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))
                        ));
                    }else{
                        context.getSender().sendMessage(main.parse(
                                "<unimportant>--</unimportant> <main>" + foundReward.getActionDescription(null)
                        ));
                    }


                }));

        manager.command(builder.literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes the reward from the Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int ID = context.get("Reward ID");
                    final Action foundReward = quest.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    quest.removeReward(foundReward);
                    context.getSender().sendMessage(main.parse(
                            "<success>The reward with the ID <highlight>" + ID + "</highlight> has been removed from the Quest <highlight2>"
                                    + quest.getIdentifier() + "</highlight2>!"
                    ));
                }));

        manager.command(builder.literal("displayName")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current reward Display Name.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int ID = context.get("Reward ID");
                    final Action foundReward = quest.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    if (foundReward.getActionName().isBlank()) {
                        context.getSender().sendMessage(main.parse(
                                "<main>This reward has no display name set."
                        ));
                    } else {
                        context.getSender().sendMessage(main.parse(
                                "<main>Reward display name: <highlight>" + foundReward.getActionName() + "</highlight>"
                        ));
                    }
                }));

        manager.command(builder.literal("displayName")
                .literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes current reward Display Name.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int ID = context.get("Reward ID");
                    final Action foundReward = quest.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    foundReward.removeActionName();
                    foundReward.getCategory().getQuestsConfig().set("quests." + quest.getIdentifier() + ".rewards." + ID + ".displayName", null);
                    foundReward.getCategory().saveQuestsConfig();
                    context.getSender().sendMessage(main.parse(
                            "<success>Display Name of reward with the ID <highlight>" + ID + "</highlight> has been removed successfully."
                    ));
                }));

        manager.command(builder.literal("displayName")
                .literal("set")
                .argument(MiniMessageSelector.<CommandSender>newBuilder("DisplayName", main).withPlaceholders().build(), ArgumentDescription.of("Reward display name"))
                .meta(CommandMeta.DESCRIPTION, "Sets new reward Display Name. Only rewards with a Display Name will be displayed.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int ID = context.get("Reward ID");
                    final Action foundReward = quest.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }

                    final String displayName = String.join(" ", (String[]) context.get("DisplayName"));


                    foundReward.setActionName(displayName);
                    foundReward.getCategory().getQuestsConfig().set("quests." + quest.getIdentifier() + ".rewards." + ID + ".displayName", foundReward.getActionName());
                    foundReward.getCategory().saveQuestsConfig();
                    context.getSender().sendMessage(main.parse(
                            "<success>Display Name successfully added to reward with ID <highlight>" + ID + "</highlight>! New display name: <highlight2>"
                                    + foundReward.getActionName()
                    ));
                }));
    }

    public void handleTriggers(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each trigger
        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Removes all the triggers this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    quest.clearTriggers();
                    context.getSender().sendMessage(main.parse(
                            "<success>All Triggers of Quest <highlight>" + quest.getIdentifier() + "</highlight> have been removed!"
                    ));

                }));

        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the triggers this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");


                    context.getSender().sendMessage(main.parse("<highlight>Triggers for Quest <highlight2>" + quest.getIdentifier() + "</highlight2>:"));

                    for (Trigger trigger : quest.getTriggers()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + trigger.getTriggerID() + ".</highlight> Type: <main>" + trigger.getTriggerType()));


                        final String triggerDescription = trigger.getTriggerDescription();
                        if (triggerDescription != null && !triggerDescription.isBlank()) {
                            context.getSender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + triggerDescription));
                        }

                        context.getSender().sendMessage(main.parse("<unimportant>--- Action Name:</unimportant> <main>" + trigger.getTriggerAction().getActionName()));
                        if(context.getSender() instanceof Player player){
                            context.getSender().sendMessage(main.parse("<unimportant>------ Description:</unimportant> <main>" + trigger.getTriggerAction().getActionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));

                        }else{
                            context.getSender().sendMessage(main.parse("<unimportant>------ Description:</unimportant> <main>" + trigger.getTriggerAction().getActionDescription(null)));

                        }
                        context.getSender().sendMessage(main.parse("<unimportant>--- Amount of triggers needed for first execution:</unimportant> <main>" + trigger.getAmountNeeded()));

                        if (trigger.getApplyOn() == 0) {
                            context.getSender().sendMessage(main.parse("<unimportant>--- Apply on:</unimportant> <main>Quest"));

                        } else {
                            context.getSender().sendMessage(main.parse("<unimportant>--- Apply on:</unimportant> <main>Objective " + trigger.getApplyOn()));
                        }

                        if (trigger.getWorldName() == null || trigger.getWorldName().isBlank() || trigger.getWorldName().equalsIgnoreCase("ALL")) {
                            context.getSender().sendMessage(main.parse("<unimportant>--- In World:</unimportant> <main>Any World"));
                        } else {
                            context.getSender().sendMessage(main.parse("<unimportant>--- In World:</unimportant> <main>" + trigger.getWorldName()));
                        }

                    }

                }));


        manager.command(builder.literal("remove", "delete")
                .argument(IntegerArgument.<CommandSender>newBuilder("Trigger ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Trigger ID]", "");

                                    ArrayList<String> completions = new ArrayList<>();

                                    final Quest quest = context.get("quest");
                                    for (final Trigger trigger : quest.getTriggers()) {
                                        completions.add("" + trigger.getTriggerID());
                                    }

                                    return completions;
                                }
                        ).withParser((context, lastString) -> { //TODO: Fix this parser. It isn't run at all.
                            final int ID = context.get("Trigger ID");
                            final Quest quest = context.get("quest");
                            final Trigger foundTrigger = quest.getTriggerFromID(ID);
                            if (foundTrigger == null) {
                                return ArgumentParseResult.failure(new IllegalArgumentException("Trigger with the ID '" + ID + "' does not belong to Quest '" + quest.getIdentifier() + "'!"));
                            } else {
                                return ArgumentParseResult.success(ID);
                            }
                        })
                        , ArgumentDescription.of("Trigger ID"))
                .meta(CommandMeta.DESCRIPTION, "Removes all the triggers this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    final int triggerID = context.get("Trigger ID");

                    final Trigger trigger = quest.getTriggerFromID(triggerID);

                    if(trigger == null){
                        context.getSender().sendMessage(main.parse(
                                "<error> Error: Trigger with the ID <highlight>" + triggerID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    context.getSender().sendMessage(main.parse(
                            quest.removeTrigger(trigger)
                    ));

                }));

    }

}
