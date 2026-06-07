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

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.bukkit.parser.WorldParser.worldParser;
import static org.incendo.cloud.parser.standard.BooleanParser.booleanParser;
import static org.incendo.cloud.parser.standard.DurationParser.durationParser;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static rocks.gravili.notquests.paper.commands.arguments.CategoryParser.categoryParser;
import static rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionParser.itemStackSelectionParser;
import static rocks.gravili.notquests.paper.commands.arguments.NQNPCParser.nqNPCParser;
import static rocks.gravili.notquests.paper.commands.arguments.ObjectiveParser.objectiveParser;

public class AdminEditCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;

    public AdminEditCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> editBuilder) {
        this.main = main;
        this.manager = manager;


        manager.command(editBuilder.literal("acceptCooldown").literal("complete")
                .literal("set")
                .required("duration", durationParser(), Description.of("New accept cooldown measured by quest completion time.")).commandDescription(Description.of("Sets the time players have to wait between accepting quests."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final Duration durationCooldown = context.get("duration");
                    final long cooldownInMinutes = durationCooldown.toMinutes();

                    quest.setAcceptCooldownComplete(cooldownInMinutes, true);
                    context.sender().sendMessage(main.parse(
                            "<success>Complete acceptCooldown for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                    + durationCooldown.toDaysPart() + " days, " + durationCooldown.toHoursPart() + " hours, " + durationCooldown.toMinutesPart() + " minutes" + "</highlight2>!"
                    ));
                }));

        manager.command(editBuilder.literal("acceptCooldown").literal("complete")
                .literal("disable").commandDescription(Description.of("Disables the wait time for players between accepting quests."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    quest.setAcceptCooldownComplete(-1, true);
                    context.sender().sendMessage(main.parse(
                            "<success>Complete acceptCooldown for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>disabled</highlight2>!"
                    ));
                }));


        final Command.Builder<CommandSender> armorstandBuilder = editBuilder.literal("armorstands");
        handleArmorStands(armorstandBuilder);


        manager.command(editBuilder.literal("description")
                .literal("show").commandDescription(Description.of("Shows current Quest description."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.sender().sendMessage(main.parse(
                            "<main>Current description of Quest <highlight>" + quest.getIdentifier() + "</highlight>: <highlight2>"
                                    + quest.getObjectiveHolderDescription()
                    ));
                }));
        manager.command(editBuilder.literal("description")
                .literal("remove").commandDescription(Description.of("Removes current Quest description."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    quest.removeQuestDescription(true);
                    context.sender().sendMessage(main.parse("<success>Description successfully removed from quest <highlight>"
                            + quest.getIdentifier() + "</highlight>!"
                    ));
                }));


        manager.command(editBuilder.literal("description")
                .literal("set")
                .required("description", greedyStringParser(), Description.of("Sets the new description of the Quest."), main.getCommandManager().miniMessageSuggestions())
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    final String description = (String) context.get("description");

                    quest.setQuestDescription(description, true);
                    context.sender().sendMessage(main.parse("<success>Description successfully added to quest <highlight>"
                            + quest.getIdentifier() + "</highlight>! New description: <highlight2>"
                            + quest.getObjectiveHolderDescription()
                    ));
                }));

        manager.command(editBuilder.literal("displayName")
                .literal("show").commandDescription(Description.of("Shows current Quest display name."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.sender().sendMessage(main.parse(
                            "<main>Current display name of Quest <highlight>" + quest.getIdentifier() + "</highlight>: <highlight2>"
                                    + quest.getQuestDisplayName()
                    ));
                }));
        manager.command(editBuilder.literal("displayName")
                .literal("remove").commandDescription(Description.of("Removes current Quest display name."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");


                    quest.removeQuestDisplayName(true);
                    context.sender().sendMessage(main.parse("<success>Display name successfully removed from quest <highlight>"
                            + quest.getIdentifier() + "</highlight>!"
                    ));
                }));

        manager.command(editBuilder.literal("displayName")
                .literal("set")
                .required("display-name", greedyStringParser(), Description.of("Quest display name"), main.getCommandManager().miniMessageSuggestions())
                .commandDescription(Description.of("Sets the new display name of the Quest."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    final String displayName = (String) context.get("display-name");

                    quest.setQuestDisplayName(displayName, true);
                    context.sender().sendMessage(main.parse("<success>Display name successfully added to quest <highlight>"
                            + quest.getIdentifier() + "</highlight>! New display name: <highlight2>"
                            + quest.getQuestDisplayName()
                    ));
                }));


        manager.command(editBuilder.literal("limits").literal("completions")
                .required("max-completions", integerParser(-1), Description.of("Maximum amount of completions. Set to -1 for unlimited (default)."), (context, input) -> {
                    ArrayList<Suggestion> completions = new ArrayList<>();
                    completions.add(Suggestion.suggestion("<amount of maximum completions>"));
                    return CompletableFuture.completedFuture(completions);
                })
                .commandDescription(Description.of("Sets the maximum amount of times you can complete this Quest."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int maxCompletions = context.get("max-completions");
                    if (maxCompletions > 0) {
                        quest.setMaxCompletions(maxCompletions, true);
                        context.sender().sendMessage(main.parse(
                                "<success>Maximum amount of completions for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + maxCompletions + "</highlight2>!"
                        ));
                    } else {
                        quest.setMaxCompletions(-1, true);
                        context.sender().sendMessage(main.parse(
                                "<success>Maximum amount of completions for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + "unlimited (default)</highlight2>!"
                        ));
                    }
                }));

        manager.command(editBuilder.literal("limits").literal("accepts")
                .required("max-accepts", integerParser(-1), Description.of("Maximum amount of accepts. Set to -1 for unlimited (default)."), (context, input) -> {
                    ArrayList<Suggestion> completions = new ArrayList<>();
                    completions.add(Suggestion.suggestion("<amount of maximum accepts>"));
                    return CompletableFuture.completedFuture(completions);
                })
                .commandDescription(Description.of("Sets the maximum amount of times you can accept this Quest."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int maxAccepts = context.get("max-accepts");
                    if (maxAccepts > 0) {
                        quest.setMaxAccepts(maxAccepts, true);
                        context.sender().sendMessage(main.parse(
                                "<success>Maximum amount of accepts for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + maxAccepts + "</highlight2>!"
                        ));
                    } else {
                        quest.setMaxAccepts(-1, true);
                        context.sender().sendMessage(main.parse(
                                "<success>Maximum amount of accepts for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + "unlimited (default)</highlight2>!"
                        ));
                    }
                }));

        manager.command(editBuilder.literal("limits").literal("fails")
                .required("max-fails", integerParser(-1), Description.of("Maximum amount of fails. Set to -1 for unlimited (default)."), (context, input) -> {
                    ArrayList<Suggestion> completions = new ArrayList<>();
                    completions.add(Suggestion.suggestion("<amount of maximum fails>"));
                    return CompletableFuture.completedFuture(completions);
                })
                .commandDescription(Description.of("Sets the maximum amount of times you can fail this Quest."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int maxFails = context.get("max-fails");
                    if (maxFails > 0) {
                        quest.setMaxFails(maxFails, true);
                        context.sender().sendMessage(main.parse(
                                "<success>Maximum amount of fails for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + maxFails + "</highlight2>!"
                        ));
                    } else {
                        quest.setMaxFails(-1, true);
                        context.sender().sendMessage(main.parse(
                                "<success>Maximum amount of fails for Quest <highlight>" + quest.getIdentifier() + "</highlight> has been set to <highlight2>"
                                        + "unlimited (default)</highlight2>!"
                        ));
                    }
                }));


        manager.command(editBuilder.literal("takeEnabled")
                .required("take-enabled", booleanParser(true), Description.of("Enabled by default. Yes / no"))
                .commandDescription(Description.of("Sets if players can accept the Quest using /notquests take."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    boolean takeEnabled = context.get("take-enabled");
                    quest.setTakeEnabled(takeEnabled, true);
                    if (takeEnabled) {
                        context.sender().sendMessage(main.parse(
                                "<success>Quest taking (/notquests take) for the Quest <highlight>"
                                        + quest.getIdentifier() + "</highlight> has been set to <highlight2>enabled</highlight2>!"
                        ));
                    } else {
                        context.sender().sendMessage(main.parse(
                                "<success>Quest taking (/notquests take) for the Quest <highlight>"
                                        + quest.getIdentifier() + "</highlight> has been set to <highlight2>disabled</highlight2>!"
                        ));
                    }
                }));

        manager.command(editBuilder.literal("abortEnabled")
                .required("abort-enabled", booleanParser(true), Description.of("Enabled by default. Yes / no")).commandDescription(Description.of("Sets if players can abort the Quest using /notquests abort."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    boolean abortEnabled = context.get("abort-enabled");
                    quest.setAbortEnabled(abortEnabled, true);
                    if (abortEnabled) {
                        context.sender().sendMessage(main.parse(
                                "<success>Quest aborting (/notquests abort) for the Quest <highlight>"
                                        + quest.getIdentifier() + "</highlight> has been set to <highlight2>enabled</highlight2>!"
                        ));
                    } else {
                        context.sender().sendMessage(main.parse(
                                "<success>Quest aborting (/notquests Abort) for the Quest <highlight>"
                                        + quest.getIdentifier() + "</highlight> has been set to <highlight2>disabled</highlight2>!"
                        ));
                    }
                }));

        manager.command(editBuilder.literal("guiItem")
                .required("material", itemStackSelectionParser(main), Description.of("Material of item displayed in the Quest take GUI."))
                .flag(
                        manager.flagBuilder("glow")
                                .withDescription(Description.of("Makes the item have the enchanted glow."))
                ).commandDescription(Description.of("Sets the item displayed in the Quest take GUI (default: book)."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final boolean glow = context.flags().isPresent("glow");

                    final ItemStackSelection itemStackSelection = context.get("material");
                    ItemStack guiItem = itemStackSelection.toFirstItemStack();
                    if (guiItem == null) {
                        guiItem = new ItemStack(Material.BOOK, 1);
                    }

                    if (glow) {
                        guiItem.addUnsafeEnchantment(Enchantment.SHARPNESS, 1);
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
                    context.sender().sendMessage(main.parse(
                            "<success>Take Item Material for Quest <highlight>" + quest.getIdentifier()
                                    + "</highlight> has been set to <highlight2>" + guiItem.getType().name() + "</highlight2>!"
                    ));


                }));

        final Command.Builder<CommandSender> objectivesBuilder = editBuilder.literal("objectives", "o");
        //qa edit questname objectives

        final String objectiveIDIdentifier = "objectiveId";
        //qa edit questname objectives edit <objectiveID> objectives
        final Command.Builder<CommandSender> objectivesBuilderLevel1 =
                objectivesBuilder
                        .literal("edit")
                        .required(objectiveIDIdentifier,
                                objectiveParser(main, 0),
                                Description.of(objectiveIDIdentifier))
                        .literal("objectives", "o");


        final String objectiveIDIdentifier2 = "objectiveId2";
        final Command.Builder<CommandSender> objectivesBuilderLevel2 =
                objectivesBuilderLevel1
                        .literal("edit")
                        .required(objectiveIDIdentifier2,
                                objectiveParser(main, 1),
                                Description.of(objectiveIDIdentifier2))
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

    public void handleNPCs(final Command.Builder<CommandSender> builder) {
        manager.command(
                builder
                        .literal("add")
                        .required("npc", nqNPCParser(main, false, true), Description.of("ID of the Citizens NPC to whom the Quest should be attached."))
                        .flag(
                                manager
                                        .flagBuilder("hideInNPC")
                                        .withDescription(
                                                Description.of("Makes the Quest hidden from in the NPC."))).commandDescription(Description.of("Attaches the Quest to a Citizens NPC."))
                        .handler(
                                (context) -> {
                                    final Quest quest = context.get("quest");
                                    final boolean showInNPC = !context.flags().isPresent("hideInNPC");

                                    final NQNPCResult nqnpcResult = context.get("npc");

                                    if (nqnpcResult.isRightClickSelect()) {//Armor Stands
                                        if (context.sender() instanceof final Player player) {
                                            main.getNPCManager().handleRightClickNQNPCSelectionWithAction(
                                                    (nqnpc) -> {
                                                        if (!quest.getAttachedNPCsWithQuestShowing().contains(nqnpc)
                                                                && !quest.getAttachedNPCsWithoutQuestShowing().contains(nqnpc)) {
                                                            final String result = quest.bindToNPC(nqnpc, showInNPC);
                                                            if (!result.isBlank()) {
                                                                player.sendMessage(main.parse(result));
                                                                return;
                                                            }
                                                            context
                                                                    .sender()
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
                                                                    .sender()
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
                                            context.sender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                                        }

                                    } else {
                                        final NQNPC nqnpc = nqnpcResult.getNQNPC();
                                        if (nqnpc == null) {
                                            context.sender().sendMessage(main.parse(
                                                    "<error>Error: NPC does not exist"
                                            ));
                                            return;
                                        }
                                        if (!quest.getAttachedNPCsWithQuestShowing().contains(nqnpc)
                                                && !quest.getAttachedNPCsWithoutQuestShowing().contains(nqnpc)) {
                                            quest.bindToNPC(nqnpc, showInNPC);
                                            context.sender().sendMessage(main.parse(
                                                    "<success>Quest <highlight>"
                                                            + quest.getIdentifier()
                                                            + "</highlight> has been bound to the NPC with the ID <highlight2>"
                                                            + nqnpc.getID().toString()
                                                            + "</highlight2>! Showing Quest: <highlight>"
                                                            + showInNPC
                                                            + "</highlight>.")
                                            );
                                        } else {
                                            context.sender().sendMessage(main.parse(
                                                    "<warn>Quest <highlight>"
                                                            + quest.getIdentifier()
                                                            + "</highlight> has already been bound to the NPC with the ID <highlight2>"
                                                            + nqnpc.getID().toString()
                                                            + "</highlight2>!")
                                            );
                                        }
                                    }

                                }));

        manager.command(builder.literal("clear").commandDescription(Description.of("De-attaches this Quest from all NPCs."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    quest.clearNPCs();
                    context.sender().sendMessage(main.parse(
                            "<success>All NPCs of Quest <highlight>"
                                    + quest.getIdentifier()
                                    + "</highlight> have been removed!")
                    );
                }));

        manager.command(builder.literal("list")
                .commandDescription(Description.of("Lists all NPCs which have this Quest attached."))
                .handler(
                        (context) -> {
                            final Quest quest = context.get("quest");
                            context.sender().sendMessage(main.parse(
                                    "<highlight>NPCs bound to quest <highlight2>"
                                            + quest.getIdentifier()
                                            + "</highlight2> with Quest showing:")
                            );
                            int counter = 1;
                            for (final NQNPC nqNPC : quest.getAttachedNPCsWithQuestShowing()) {
                                context.sender().sendMessage(main.parse(
                                        "<highlight>"
                                                + counter
                                                + ".</highlight> <main>ID:</main> <highlight2>"
                                                + nqNPC.getNPCType() + ":" + nqNPC.getID().getEitherAsString())
                                );
                                counter++;
                            }
                            counter = 1;
                            context.sender().sendMessage(main.parse(
                                    "<highlight>NPCs bound to quest <highlight2>"
                                            + quest.getIdentifier()
                                            + "</highlight2> without Quest showing:")
                            );
                            for (final NQNPC nqNPC : quest.getAttachedNPCsWithoutQuestShowing()) {
                                context.sender().sendMessage(main.parse(
                                        "<highlight>"
                                                + counter
                                                + ".</highlight> <main>ID:</main> <highlight2>"
                                                + nqNPC.getNPCType() + ":" + nqNPC.getID().getEitherAsString())
                                );
                                counter++;
                            }
                        }));
    }


    public void handleCategories(final Command.Builder<CommandSender> builder) {
        manager.command(builder.literal("show").commandDescription(Description.of("Shows the current category of this Quest."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.sender().sendMessage(main.parse(
                            "<main>Category for Quest <highlight>" + quest.getIdentifier() + "</highlight>: <highlight2>"
                                    + quest.getCategory().getCategoryFullName() + "</highlight2>."
                    ));
                }));

        manager.command(builder.literal("set")
                .required("category", categoryParser(main), Description.of("New category for this Quest."))
                .commandDescription(Description.of("Changes the current category of this Quest."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final Category category = context.get("category");
                    if (quest.getCategory().getCategoryFullName().equalsIgnoreCase(category.getCategoryFullName())) {
                        context.sender().sendMessage(main.parse(
                                "<error> Error: The quest <highlight>" + quest.getIdentifier() + "</highlight> already has the category <highlight2>" + quest.getCategory().getCategoryFullName() + "</highlight2>."
                        ));
                        return;
                    }


                    context.sender().sendMessage(main.parse(
                            "<success>Category for Quest <highlight>" + quest.getIdentifier() + "</highlight> has successfully been changed from <highlight2>"
                                    + quest.getCategory().getCategoryFullName() + "</highlight2> to <highlight2>" + category.getCategoryFullName() + "</highlight2>!"
                    ));

                    quest.switchCategory(category);

                }));
    }


    public void handleArmorStands(final Command.Builder<CommandSender> builder) {

        manager.command(builder.literal("check")
                .senderType(Player.class).commandDescription(Description.of("Gives you an item with which you check what Quests are attached to an armor stand."))
                .handler((context) -> {
                    final Player player = (Player) context.sender();

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

                    context.sender().sendMessage(main.parse("<success>You have been given an item with which you can check armor stands!"));
                }));


        manager.command(builder.literal("add")
                .senderType(Player.class)
                .flag(manager.flagBuilder("hideInArmorStand").withDescription(Description.of("Makes the Quest hidden from armor stands")).build())
                .commandDescription(Description.of("Gives you an item with which you can add the quest to an armor stand."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    boolean showInArmorStand = !context.flags().isPresent("hideInArmorStand");
                    final Player player = context.sender();

                    ItemStack itemStack = new ItemStack(Material.GHAST_TEAR, 1);
                    //give a specialitem. clicking an armorstand with that special item will give it the pdb.

                    NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-item");
                    NamespacedKey QuestNameKey = new NamespacedKey(main.getMain(), "notquests-questname");
                    ItemMeta itemMeta = itemStack.getItemMeta();

                    if (itemMeta == null) {
                        context.sender().sendMessage(main.parse("<error>Error: ItemMeta is null."));
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
                    context.sender().sendMessage(main.parse("<success>You have been given an item with which you can add this quest to armor stands!"));
                }));

        manager.command(builder.literal("clear")
                .senderType(Player.class).commandDescription(Description.of("This command is not done yet."))
                .handler((context) -> context.sender().sendMessage(main.parse("<error>Sorry, this command is not done yet! I'll add it in future versions."))));

        manager.command(builder.literal("list")
                .senderType(Player.class).commandDescription(Description.of("This command is not done yet."))
                .handler((context) -> context.sender().sendMessage(main.parse("<error>Sorry, this command is not done yet! I'll add it in future versions."))));


        manager.command(builder.literal("remove")
                .senderType(Player.class)
                .flag(manager.flagBuilder("hideInArmorStand")
                        .withDescription(Description.of("Sets if you want to remove the Quest which is hidden in an armor stand."))
                        .build())
                .commandDescription(Description.of("Gives you an item with which you can remove the quest from an armor stand."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    boolean showInArmorStand = !context.flags().isPresent("hideInArmorStand");
                    final Player player = (Player) context.sender();

                    ItemStack itemStack = new ItemStack(Material.NETHER_STAR, 1);
                    //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                    NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-item");
                    NamespacedKey QuestNameKey = new NamespacedKey(main.getMain(), "notquests-questname");

                    ItemMeta itemMeta = itemStack.getItemMeta();

                    if (itemMeta == null) {
                        context.sender().sendMessage(main.parse("<errorError: ItemMeta is null."));
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

                    context.sender().sendMessage(main.parse("<success>You have been given an item with which you can remove this quest from armor stands!"));
                }));
    }

    public void handleObjectives(final Command.Builder<CommandSender> builder, final int level) {
        //Add is handled individually by each objective

        //Builder: qa edit questname objectives edit <objectiveID> objectives

        main.getLogManager().debug("Handling objectives for level <highlight>" + level + "</highlight>...");

        final Command.Builder<CommandSender> predefinedProgressOrderBuilder = builder.literal("predefinedProgressOrder");

        manager.command(predefinedProgressOrderBuilder.literal("show").commandDescription(Description.of("Shows the current predefined order in which the objectives need to be progressed for your quest."))
                .handler((context) -> {
                    final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);
                    context.sender().sendMessage(Component.empty());
                    final String predefinedProgressOrderString = objectiveHolder.getPredefinedProgressOrder() != null ? (objectiveHolder.getPredefinedProgressOrder().getReadableString()) : "None";
                    context.sender().sendMessage(main.parse("<success>Current predefined progress order of Quest <highlight>" + objectiveHolder.getIdentifier() + "</highlight>: <highlight2>" + predefinedProgressOrderString
                    ));
                }));

        manager.command(predefinedProgressOrderBuilder.literal("set")
                .literal("none").commandDescription(Description.of("Sets a predefined order in which the objectives need to be progressed for your quest."))
                .handler((context) -> {
                    final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);
                    objectiveHolder.setPredefinedProgressOrder(null, true);
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(main.parse("<success>Predefined progress order of Quest <highlight>" + objectiveHolder.getIdentifier() + "</highlight> have been removed!"
                    ));
                }));

        manager.command(predefinedProgressOrderBuilder.literal("set")
                .literal("firstToLast").commandDescription(Description.of("Sets a predefined order in which the objectives need to be progressed for your quest."))
                .handler((context) -> {
                    final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);
                    objectiveHolder.setPredefinedProgressOrder(PredefinedProgressOrder.firstToLast(), true);
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(main.parse("<success>Predefined progress order of Quest <highlight>" + objectiveHolder.getIdentifier() + "</highlight> have been set to first to last!"
                    ));
                }));

        manager.command(predefinedProgressOrderBuilder.literal("set")
                .literal("lastToFirst").commandDescription(Description.of("Sets a predefined order in which the objectives need to be progressed for your quest."))
                .handler((context) -> {
                    final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);
                    objectiveHolder.setPredefinedProgressOrder(PredefinedProgressOrder.lastToFirst(), true);
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(main.parse("<success>Predefined progress order of Quest <highlight>" + objectiveHolder.getIdentifier() + "</highlight> have been set to last to first!"
                    ));
                }));

        manager.command(predefinedProgressOrderBuilder.literal("set")
                .literal("custom")
                .required("order", greedyStringParser(), Description.of("Custom order. Example: 2 1 3 4 5 6 7 9 8"), (context, input) -> {
                            main.getUtilManager().sendFancyCommandCompletion(context.sender(), input.input().split(" "), "<Enter custom order (numbers of objective IDs separated by space)>", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);

                            for (final Objective objective : objectiveHolder.getObjectives()) {
                                completions.add(Suggestion.suggestion(objective.getObjectiveID() + ""));
                            }

                            return CompletableFuture.completedFuture(completions);
                        }
                )
                .commandDescription(Description.of("Sets a predefined order in which the objectives need to be progressed for your quest."))
                .handler((context) -> {
                    final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);

                    final String orderString = context.get("order");
                    final String[] order = orderString.split(" ");
                    final ArrayList<String> orderParsed = new ArrayList<>();
                    Collections.addAll(orderParsed, order);

                    objectiveHolder.setPredefinedProgressOrder(PredefinedProgressOrder.custom(orderParsed), true);
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(main.parse(
                            "<success>Predefined progress order of Quest <highlight>" + objectiveHolder.getIdentifier()
                                    + "</highlight> have been set to custom with this order: " + orderString
                    ));
                }));

        manager.command(builder.literal("clear").commandDescription(Description.of("Removes all objectives from a Quest."))
                .handler((context) -> {
                    final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);

                    objectiveHolder.clearObjectives();
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(main.parse(
                            "<success>All objectives of Quest <highlight>" + objectiveHolder.getIdentifier()
                                    + "</highlight> have been removed!"
                    ));
                }));
        manager.command(builder.literal("list").commandDescription(Description.of("Lists all objectives of a Quest."))
                .handler((context) -> {
                    final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);

                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(main.parse("<highlight>Objectives for Quest <highlight2>" + objectiveHolder.getIdentifier() + "</highlight2>:"));
                    main.getQuestManager().sendObjectivesAdmin(context.sender(), objectiveHolder);
                }));


        final String objectiveIDIdentifier = (level == 0 ? "objectiveId" : "objectiveId" + (level + 1));

        //Builder: qa edit questname objectives edit <Objective ID> objectives
        //adminEditObjectivesBuilderWithLevels: qa edit questname objectives edit <Objective ID> objectives edit <Objective ID 2>

        final Command.Builder<CommandSender> adminEditObjectivesBuilderWithLevels = builder.literal("edit").required(objectiveIDIdentifier, objectiveParser(main, level), Description.of(objectiveIDIdentifier));
        handleEditObjectives(adminEditObjectivesBuilderWithLevels, level);
    }

    public void handleEditObjectives(final Command.Builder<CommandSender> builder, final int level) {

        final String objectiveIDIdentifier;
        if (level == 0) {
            objectiveIDIdentifier = "objectiveId";
        } else {
            objectiveIDIdentifier = "objectiveId" + (level + 1);
        }

        main.getLogManager().debug("Handling EDIT objectives for level <highlight>" + level + "</highlight>... objectiveIDIdentifier: " + objectiveIDIdentifier);


        manager.command(builder.literal("location")
                .literal("enable").commandDescription(Description.of("Shows the location to the player."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                    objective.setShowLocation(true, true);
                    context.sender().sendMessage(main.parse("<main>The objective with ID <highlight>" + objective.getObjectiveID() + "</highlight> is now showing the location to the player!"));
                }));

        manager.command(builder.literal("location")
                .literal("disables").commandDescription(Description.of("Disables showing the location to the player."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    objective.setShowLocation(false, true);

                    context.sender().sendMessage(main.parse(
                            "<main>The objective with ID <highlight>" + objective.getObjectiveID() + "</highlight> is now no longer showing the location to the player!"
                    ));
                }));

        manager.command(builder.literal("location")
                .literal("set")
                .required("world", worldParser(), Description.of("World name"))
                /* .argumentTriplet(
                         "coords",
                         TypeToken.get(Vector.class),
                         Triplet.of("x", "y", "z"),
                         Triplet.of(Integer.class, Integer.class, Integer.class),
                         (sender, triplet) -> new Vector(triplet.getFirst(), triplet.getSecond(),
                                 triplet.getThird()
                         ),
                         Description.of("Coordinates")
                 )*/ //Commented out, because this somehow breaks flags
                .required("x", integerParser(0, 5000), Description.of("X coordinate"))
                .required("y", integerParser(0, 312), Description.of("Y coordinate"))
                .required("z", integerParser(0, 5000), Description.of("Z coordinate"))
                .commandDescription(Description.of("Disables showing the location to the player."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                    final World world = context.get("world");
                    final Vector coordinates = new Vector(context.get("x"), context.get("y"), context.get("z"));
                    final Location location = coordinates.toLocation(world);
                    objective.setLocation(location, true);
                    objective.setShowLocation(true, true);
                    context.sender().sendMessage(main.parse("<main>The objective with ID <highlight>" + objective.getObjectiveID() + "</highlight> is now has a location!"
                    ));
                }));

        manager.command(builder.literal("completionNPC")
                .literal("show", "view").commandDescription(Description.of("Shows the completionNPC of an objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    context.sender().sendMessage(main.parse(
                            "<main>The completionNPCID of the objective with the ID <highlight>" + objective.getObjectiveID() + "</highlight> is <highlight2>"
                                    + (objective.getCompletionNPC() != null ? objective.getCompletionNPC().getID() : "null") + "</highlight2>!"
                    ));
                }));
        manager.command(builder.literal("completionNPC")
                .literal("set")
                .required("Completion NPC", nqNPCParser(main, true, true), Description.of("Completion NPC")).commandDescription(Description.of("Sets the completionNPC of an objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                    final NQNPCResult completionNPCResult = context.get("Completion NPC");
                    if (completionNPCResult.isNone()) {
                        objective.setCompletionNPC(null, true);
                        context.sender().sendMessage(main.parse("<success>The completionNPC of the objective with the ID <highlight>" + objective.getObjectiveID() + "</highlight> has been removed!"
                        ));

                    } else if (completionNPCResult.isRightClickSelect()) {//Armor Stands
                        if (context.sender() instanceof final Player player) {
                            main.getNPCManager().handleRightClickNQNPCSelectionWithAction(
                                    (nqnpc) -> {
                                        objective.setCompletionNPC(nqnpc, true);
                                        player.sendMessage(main.parse(
                                                "<success>The completionArmorStandUUID of the objective with the ID <highlight>" + objective.getObjectiveID() + "</highlight> has been set to the NPC with the ID <highlight2>" + nqnpc.getID().toString() + "</highlight2> and name <highlight2>" + "todo" + "</highlight2>!"
                                        ));
                                    },
                                    player,
                                    "<success>You have been given an item with which you can add the completionNPC of this Objective to an NPC. Check your inventory!",
                                    "<LIGHT_PURPLE>Set completionNPC of Quest <highlight>" + objective.getObjectiveHolder().getIdentifier() + "</highlight> to this NPC",
                                    "<WHITE>Right-click an NPC to set it as the completionNPC of Quest <highlight>" + objective.getObjectiveHolder().getIdentifier() + "</highlight> and ObjectiveID <highlight>" + objective.getObjectiveID() + "</highlight>."
                            );
                        } else {
                            context.sender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                        }

                    } else {
                        objective.setCompletionNPC(completionNPCResult.getNQNPC(), true);
                        context.sender().sendMessage(main.parse(
                                "<success>The completionNPC of the objective with the ID <highlight>" + objective.getObjectiveID()
                                        + "</highlight> has been set to the NPC with the ID <highlight2>" + (completionNPCResult.getNQNPC() != null ? completionNPCResult.getNQNPC().getID() : "null") + "</highlight2>!"
                        ));
                    }
                }));


        final Command.Builder<CommandSender> editObjectiveConditionsUnlockBuilder = builder.literal("conditions").literal("unlock");
        handleEditObjectivesUnlockConditions(editObjectiveConditionsUnlockBuilder, level);

        final Command.Builder<CommandSender> editObjectiveConditionsProgressBuilder = builder.literal("conditions").literal("progress");
        handleEditObjectivesProgressConditions(editObjectiveConditionsProgressBuilder, level);

        final Command.Builder<CommandSender> editObjectiveConditionsCompleteBuilder = builder.literal("conditions").literal("complete");
        handleEditObjectivesCompleteConditions(editObjectiveConditionsCompleteBuilder, level);


        manager.command(builder.literal("description")
                .literal("show").commandDescription(Description.of("Shows current objective description."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                    context.sender().sendMessage(main.parse(
                            "<main>Current description of objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>: <highlight2>"
                                    + objective.getObjectiveHolderDescription()
                    ));
                }));
        manager.command(builder.literal("description")
                .literal("remove").commandDescription(Description.of("Removes current objective description."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                    objective.removeDescription(true);
                    context.sender().sendMessage(main.parse("<main>Description successfully removed from objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New description: <highlight2>"
                            + objective.getObjectiveHolderDescription()
                    ));
                }));

        manager.command(builder.literal("taskDescription")
                .literal("show").commandDescription(Description.of("Shows current objective task description."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                    context.sender().sendMessage(main.parse(
                            "<main>Current task description of objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>: <highlight2>"
                                    + objective.getTaskDescriptionProvided()
                    ));
                }));
        manager.command(builder.literal("taskDescription")
                .literal("remove").commandDescription(Description.of("Removes current objective task description."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    objective.removeTaskDescription(true);
                    context.sender().sendMessage(main.parse(
                            "<main>Task description successfully removed from objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New description: <highlight2>"
                                    + objective.getTaskDescriptionProvided()
                    ));
                }));

        manager.command(builder.literal("description")
                .literal("set")
                .required("Objective Description", greedyStringParser(), Description.of("Objective description"), main.getCommandManager().miniMessageSuggestions())
                .commandDescription(Description.of("Sets current objective description."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    final String description = (String) context.get("Objective Description");
                    objective.setDescription(description, true);
                    context.sender().sendMessage(main.parse(
                            "<main>Description successfully added to objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New description: <highlight2>"
                                    + objective.getObjectiveHolderDescription()
                    ));
                }));

        manager.command(builder.literal("taskDescription")
                .literal("set")
                .required("Task Description", greedyStringParser(), Description.of("Objective task description"), main.getCommandManager().miniMessageSuggestions())
                .commandDescription(Description.of("Sets current objective task description."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                    final String taskDescription = (String) context.get("Task Description");
                    objective.setTaskDescription(taskDescription, true);
                    context.sender().sendMessage(main.parse(
                            "<main>Task Description successfully added to objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New description: <highlight2>"
                                    + objective.getTaskDescriptionProvided()
                    ));
                }));


        manager.command(builder.literal("displayName")
                .literal("show").commandDescription(Description.of("Shows current objective displayname."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                    context.sender().sendMessage(main.parse("<main>Current displayname of objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>: <highlight2>" + objective.getDisplayName()));
                }));
        manager.command(builder.literal("displayName")
                .literal("remove").commandDescription(Description.of("Removes current objective displayname."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    objective.removeDisplayName(true);
                    context.sender().sendMessage(main.parse(
                            "<main>Displayname successfully removed from objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New displayname: <highlight2>"
                                    + objective.getObjectiveHolderDescription()
                    ));
                }));

        manager.command(builder.literal("displayName")
                .literal("set")
                .required("DisplayName", greedyStringParser(), Description.of("Quest display name"), main.getCommandManager().miniMessageSuggestions())
                .commandDescription(Description.of("Sets current objective displayname."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                    final String displayName = (String) context.get("DisplayName");
                    objective.setDisplayName(displayName, true);
                    context.sender().sendMessage(main.parse("<main>Displayname successfully added to objective with ID <highlight>" + objective.getObjectiveID() + "</highlight>! New displayname: <highlight2>" + objective.getDisplayName()
                    ));
                }));


        manager.command(builder.literal("info").commandDescription(Description.of("Shows everything there is to know about this objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(main.parse("<highlight>Information of objective with the ID <highlight2>" + objective.getObjectiveID() + "</highlight2> from Quest <highlight2>" + objective.getObjectiveHolder().getIdentifier() + "</highlight2>:"));
                    context.sender().sendMessage(main.parse("<highlight>Objective Type: <main>" + main.getObjectiveManager().getObjectiveType(objective.getClass())));
                    context.sender().sendMessage(main.parse("<highlight>Objective Content:</highlight>"));
                    context.sender().sendMessage(main.parse(main.getQuestManager().getObjectiveTaskDescription(objective, false, null)));
                    context.sender().sendMessage(main.parse("<highlight>Objective DisplayName: <main>" + objective.getDisplayName()));
                    context.sender().sendMessage(main.parse("<highlight>Objective Description: <main>" + objective.getObjectiveHolderDescription()));
                    {
                        context.sender().sendMessage(main.parse("<highlight>Objective unlock conditions:"));
                        int counter = 1;
                        for (final Condition condition : objective.getUnlockConditions()) {
                            if (context.sender() instanceof Player player) {
                                context.sender().sendMessage(main.parse("    <highlight>" + counter + ". Description: " + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                            } else {
                                context.sender().sendMessage(main.parse("    <highlight>" + counter + ". Description: " + condition.getConditionDescription(null)));
                            }
                            counter++;
                        }
                    }

                    {
                        context.sender().sendMessage(main.parse("<highlight>Objective progress conditions:"));
                        int counter = 1;
                        for (final Condition condition : objective.getProgressConditions()) {
                            if (context.sender() instanceof Player player) {
                                context.sender().sendMessage(main.parse("    <highlight>" + counter + ". Description: " + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                            } else {
                                context.sender().sendMessage(main.parse("    <highlight>" + counter + ". Description: " + condition.getConditionDescription(null)));
                            }
                            counter++;
                        }
                    }

                    {
                        context.sender().sendMessage(main.parse("<highlight>Objective complete conditions:"
                        ));
                        int counter = 1;
                        for (final Condition condition : objective.getCompleteConditions()) {
                            if (context.sender() instanceof Player player) {
                                context.sender().sendMessage(main.parse("    <highlight>" + counter + ". Description: " + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))
                                ));
                            } else {
                                context.sender().sendMessage(main.parse("    <highlight>" + counter + ". Description: " + condition.getConditionDescription(null)
                                ));
                            }
                            counter++;
                        }
                    }

                }));

        manager.command(builder.literal("remove", "delete").commandDescription(Description.of("Removes the objective from the Quest."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                    objective.getObjectiveHolder().removeObjective(objective);
                    context.sender().sendMessage(main.parse("<success>Objective with the ID <highlight>" + objective.getObjectiveID() + "</highlight> has been successfully removed from Quest <highlight2>" + objective.getObjectiveHolder().getIdentifier() + "</highlight2>!"
                    ));
                }));


        final Command.Builder<CommandSender> rewardsBuilder = builder.literal("rewards");
        handleObjectiveRewards(rewardsBuilder, level);
    }


    public void handleRequirements(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each requirement


        manager.command(builder.literal("list", "show").commandDescription(Description.of("Lists all the requirements this Quest has."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    context.sender().sendMessage(main.parse("<highlight>Requirements for Quest <highlight2>" + quest.getIdentifier() + "</highlight2>:"));
                    for (final Condition condition : quest.getRequirements()) {
                        context.sender().sendMessage(main.parse("<highlight>" + condition.getConditionID() + ".</highlight> <main>" + condition.getConditionType()));
                        if (context.sender() instanceof Player player) {
                            context.sender().sendMessage(main.parse("<main>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        } else {
                            context.sender().sendMessage(main.parse("<main>" + condition.getConditionDescription(null)));
                        }
                    }
                }));

        manager.command(builder.literal("clear").commandDescription(Description.of("Clears all the requirements this Quest has."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    quest.clearRequirements();
                    context.sender().sendMessage(main.parse("<main>All requirements of Quest <highlight>" + quest.getIdentifier() + "</highlight> have been removed!"));
                }));

        final Command.Builder<CommandSender> editQuestRequirementsBuilder = builder.literal("edit")
                .required("Requirement ID", integerParser(1), (context, input) -> {
                    main.getUtilManager().sendFancyCommandCompletion(context.sender(), input.input().split(" "), "[Requirement ID]", "[...]");
                    ArrayList<Suggestion> completions = new ArrayList<>();
                    final Quest quest = context.get("quest");
                    for (final Condition condition : quest.getRequirements()) {
                        completions.add(Suggestion.suggestion("" + condition.getConditionID()));
                    }
                    return CompletableFuture.completedFuture(completions);
                });

        manager.command(editQuestRequirementsBuilder.literal("delete", "remove")
                .commandDescription(Description.of("Removes a requirement from this Quest."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int conditionID = context.get("Requirement ID");
                    Condition condition = quest.getRequirementFromID(conditionID);
                    if (condition == null) {
                        context.sender().sendMessage(main.parse("<error>Requirement with the ID <highlight>" + conditionID + "</highlight> was not found!"));
                        return;
                    }
                    quest.removeRequirement(condition);
                    context.sender().sendMessage(main.parse("<main>The requirement with the ID <highlight>" + conditionID + "</highlight> of Quest <highlight2>" + quest.getIdentifier() + "</highlight2> has been removed!"));
                }));


        manager.command(editQuestRequirementsBuilder.literal("description")
                .literal("set")
                .required("description", greedyStringParser(), Description.of("Quest requirementdescription"), main.getCommandManager().miniMessageSuggestions()).commandDescription(Description.of("Sets the new description of the Quest requirement."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int conditionID = context.get("Requirement ID");
                    Condition condition = quest.getRequirementFromID(conditionID);
                    if (condition == null) {
                        context.sender().sendMessage(main.parse("<error>Requirement with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String description = (String) context.get("description");
                    condition.setDescription(description);

                    quest.getCategory().getQuestsConfig().set("quests." + quest.getIdentifier() + ".requirements." + conditionID + ".description", description);
                    quest.getCategory().saveQuestsConfig();
                    context.sender().sendMessage(main.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of quest <highlight2>" + quest.getIdentifier() + "</highlight2>! New description: <highlight2>" + condition.getDescription()
                    ));
                }));


        manager.command(editQuestRequirementsBuilder.literal("hidden")
                .literal("set")
                .required("hiddenStatusExpression", stringParser(), Description.of("Expression"))
                .commandDescription(Description.of("Sets the new hidden status of the Quest requirement."))
                //.required("hiddenStatusExpression", booleanVariableValueParser("hiddenStatusExpression", null, ((context, input) -> CompletableFuture.completedFuture(new ArrayList<>()))), Description.of("Expression")).commandDescription(Description.of("Sets the new hidden status of the Quest requirement."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int conditionID = context.get("Requirement ID");
                    Condition condition = quest.getRequirementFromID(conditionID);
                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Requirement with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String hiddenStatusExpression = context.get("hiddenStatusExpression");
                    final NumberExpression hiddenExpression = new NumberExpression(main, hiddenStatusExpression);

                    condition.setHidden(hiddenExpression);

                    quest.getCategory().getQuestsConfig().set("quests." + quest.getIdentifier() + ".requirements." + conditionID + ".hiddenStatusExpression", hiddenStatusExpression);
                    quest.getCategory().saveQuestsConfig();

                    context.sender().sendMessage(main.parse("<success>Hidden status successfully added to condition with ID <highlight>" + conditionID + "</highlight> of quest <highlight2>"
                            + quest.getIdentifier() + "</highlight2>! New hidden status: <highlight2>"
                            + condition.getHiddenExpression()
                    ));
                }));

        manager.command(editQuestRequirementsBuilder.literal("description")
                .literal("remove", "delete").commandDescription(Description.of("Removes the description of the Quest requirement."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int conditionID = context.get("Requirement ID");
                    Condition condition = quest.getRequirementFromID(conditionID);
                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Requirement with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    condition.removeDescription();

                    quest.getCategory().getQuestsConfig().set("quests." + quest.getIdentifier() + ".requirements." + conditionID + ".description", "");
                    quest.getCategory().saveQuestsConfig();


                    context.sender().sendMessage(main.parse("<success>Description successfully removed from condition with ID <highlight>" + conditionID + "</highlight> of quest <highlight2>"
                            + quest.getIdentifier() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editQuestRequirementsBuilder.literal("description")
                .literal("show", "check").commandDescription(Description.of("Shows the description of the Quest requirement."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int conditionID = context.get("Requirement ID");
                    Condition condition = quest.getRequirementFromID(conditionID);
                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Requirement with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    context.sender().sendMessage(main.parse("<main>Description of condition with ID <highlight>" + conditionID + "</highlight> of quest <highlight2>"
                            + quest.getIdentifier() + "</highlight2>:\n"
                            + condition.getDescription()
                    ));
                }));

    }

    public void handleEditObjectivesUnlockConditions(final Command.Builder<CommandSender> builder, final int level) {

        manager.command(builder
                .literal("clear").commandDescription(Description.of("Removes all unlock conditions from this objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    objective.clearUnlockConditions();
                    context.sender().sendMessage(main.parse(
                            "<success>All unlock conditions of objective with ID <highlight>" + objective.getObjectiveID()
                                    + "</highlight> have been removed!"
                    ));

                }));

        manager.command(builder
                .literal("list", "show").commandDescription(Description.of("Lists all unlock conditions of this objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    context.sender().sendMessage(main.parse(
                            "<highlight>Unlock conditions of objective with ID <highlight2>" + objective.getObjectiveID()
                                    + "</highlight2>:"
                    ));
                    for (Condition condition : objective.getUnlockConditions()) {
                        context.sender().sendMessage(main.parse("<highlight>" + condition.getConditionID() + ".</highlight> <main>" + condition.getConditionType() + "</main>"));
                        if (context.sender() instanceof Player player) {
                            context.sender().sendMessage(main.parse("<main>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        } else {
                            context.sender().sendMessage(main.parse("<main>" + condition.getConditionDescription(null)));
                        }
                    }

                    if (objective.getUnlockConditions().size() == 0) {
                        context.sender().sendMessage(main.parse("<warn>This objective has no unlock conditions!"));
                    }
                }));


        final Command.Builder<CommandSender> editObjectiveConditionsBuilder = builder
                .literal("edit")
                .required("Condition ID", integerParser(1), Description.of("Condition ID"), (context, input) -> {
                            main.getUtilManager().sendFancyCommandCompletion(context.sender(), input.input().split(" "), "[Condition ID]", "[...]");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                            for (final Condition condition : objective.getUnlockConditions()) {
                                completions.add(Suggestion.suggestion("" + condition.getConditionID()));
                            }
                            return CompletableFuture.completedFuture(completions);
                        }
                );

        manager.command(editObjectiveConditionsBuilder
                .literal("delete", "remove").commandDescription(Description.of("Removes an unlock condition from this Objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getUnlockConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Unlock condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    objective.removeUnlockCondition(condition, true);
                    context.sender().sendMessage(main.parse("<main>The unlock condition with the ID <highlight>" + conditionID + "</highlight> of Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> has been removed!"));
                }));


        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("set")
                .required("description", greedyStringParser(), Description.of("Objective condition description"), main.getCommandManager().miniMessageSuggestions()).commandDescription(Description.of("Sets the new description of the Objective unlock condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getUnlockConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String description = (String) context.get("description");

                    condition.setDescription(description);


                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", description);
                    objective.getObjectiveHolder().saveConfig();

                    context.sender().sendMessage(main.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editObjectiveConditionsBuilder
                .literal("hidden")
                .literal("set")
                .required("hiddenStatusExpression", stringParser(), Description.of("Expression"))
                .commandDescription(Description.of("Sets the new hidden status of the Objective unlock condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getUnlockConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String hiddenStatusExpression = context.get("hiddenStatusExpression");
                    final NumberExpression hiddenExpression = new NumberExpression(main, hiddenStatusExpression);

                    condition.setHidden(hiddenExpression);

                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".hiddenStatusExpression", hiddenStatusExpression);
                    objective.getObjectiveHolder().saveConfig();

                    context.sender().sendMessage(main.parse("<success>Hidden status successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>! New hidden status: <highlight2>"
                            + condition.getHiddenExpression()
                    ));
                }));

        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("remove", "delete").commandDescription(Description.of("Removes the description of the objective unlock condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getUnlockConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Unlock condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    condition.removeDescription();

                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", "");
                    objective.getObjectiveHolder().saveConfig();

                    context.sender().sendMessage(main.parse("<success>Description successfully removed from unlock condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("show", "check").commandDescription(Description.of("Shows the description of the objective unlock condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getUnlockConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Unlock condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    context.sender().sendMessage(main.parse("<main>Description of unlock condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>:\n"
                            + condition.getDescription()
                    ));
                }));
    }

    public void handleEditObjectivesProgressConditions(final Command.Builder<CommandSender> builder, final int level) {
        manager.command(builder
                .literal("clear").commandDescription(Description.of("Removes all progress conditions from this objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    objective.clearProgressConditions();
                    context.sender().sendMessage(main.parse(
                            "<success>All progress conditions of objective with ID <highlight>" + objective.getObjectiveID()
                                    + "</highlight> have been removed!"
                    ));

                }));

        manager.command(builder
                .literal("list", "show").commandDescription(Description.of("Lists all progress conditions of this objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    context.sender().sendMessage(main.parse(
                            "<highlight>Progress conditions of objective with ID <highlight2>" + objective.getObjectiveID()
                                    + "</highlight2>:"
                    ));
                    for (Condition condition : objective.getProgressConditions()) {
                        context.sender().sendMessage(main.parse("<highlight>" + condition.getConditionID() + ".</highlight> <main>" + condition.getConditionType() + "</main>"));
                        if (context.sender() instanceof Player player) {
                            context.sender().sendMessage(main.parse("<main>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        } else {
                            context.sender().sendMessage(main.parse("<main>" + condition.getConditionDescription(null)));
                        }
                    }

                    if (objective.getProgressConditions().size() == 0) {
                        context.sender().sendMessage(main.parse("<warn>This objective has no progress conditions!"));
                    }
                }));


        final Command.Builder<CommandSender> editObjectiveConditionsBuilder = builder
                .literal("edit")
                .required("Condition ID", integerParser(1), Description.of("Condition ID"), (context, input) -> {
                            main.getUtilManager().sendFancyCommandCompletion(context.sender(), input.input().split(" "), "[Condition ID]", "[...]");

                            ArrayList<Suggestion> completions = new ArrayList<>();
                            final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);
                            for (final Condition condition : objective.getProgressConditions()) {
                                completions.add(Suggestion.suggestion("" + condition.getConditionID()));
                            }
                            return CompletableFuture.completedFuture(completions);
                        }
                );

        manager.command(editObjectiveConditionsBuilder
                .literal("delete", "remove").commandDescription(Description.of("Removes an progress condition from this Objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getProgressConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Progress condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    objective.removeProgressCondition(condition, true);
                    context.sender().sendMessage(main.parse("<main>The progress condition with the ID <highlight>" + conditionID + "</highlight> of Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> has been removed!"));
                }));


        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("set")
                .required("description", greedyStringParser(), Description.of("Objective condition description"), main.getCommandManager().miniMessageSuggestions()).commandDescription(Description.of("Sets the new description of the Objective progress condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getProgressConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String description = (String) context.get("description");

                    condition.setDescription(description);

                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", description);
                    objective.getObjectiveHolder().saveConfig();

                    context.sender().sendMessage(main.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editObjectiveConditionsBuilder.literal("hidden")
                .literal("set")
                .required("hiddenStatusExpression", stringParser(), Description.of("Expression"))
                .commandDescription(Description.of("Sets the new hidden status of the Objective progress condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getProgressConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String hiddenStatusExpression = context.get("hiddenStatusExpression");
                    final NumberExpression hiddenExpression = new NumberExpression(main, hiddenStatusExpression);

                    condition.setHidden(hiddenExpression);

                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".hiddenStatusExpression", hiddenStatusExpression);
                    objective.getObjectiveHolder().saveConfig();

                    context.sender().sendMessage(main.parse("<success>Hidden status successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>! New hidden status: <highlight2>"
                            + condition.getHiddenExpression()
                    ));
                }));

        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("remove", "delete").commandDescription(Description.of("Removes the description of the objective progress condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getProgressConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Progress condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    condition.removeDescription();

                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", "");
                    objective.getObjectiveHolder().saveConfig();

                    context.sender().sendMessage(main.parse("<success>Description successfully removed from progress condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("show", "check").commandDescription(Description.of("Shows the description of the objective progress condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getProgressConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Progress condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    context.sender().sendMessage(main.parse("<main>Description of progress condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>:\n"
                            + condition.getDescription()
                    ));
                }));
    }

    public void handleEditObjectivesCompleteConditions(final Command.Builder<CommandSender> builder, final int level) {
        manager.command(builder
                .literal("clear").commandDescription(Description.of("Removes all complete conditions from this objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    objective.clearCompleteConditions();
                    context.sender().sendMessage(main.parse(
                            "<success>All complete conditions of objective with ID <highlight>" + objective.getObjectiveID()
                                    + "</highlight> have been removed!"
                    ));

                }));

        manager.command(builder
                .literal("list", "show").commandDescription(Description.of("Lists all complete conditions of this objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    context.sender().sendMessage(main.parse(
                            "<highlight>Complete conditions of objective with ID <highlight2>" + objective.getObjectiveID()
                                    + "</highlight2>:"
                    ));
                    for (Condition condition : objective.getCompleteConditions()) {
                        context.sender().sendMessage(main.parse("<highlight>" + condition.getConditionID() + ".</highlight> <main>" + condition.getConditionType() + "</main>"));
                        if (context.sender() instanceof Player player) {
                            context.sender().sendMessage(main.parse("<main>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        } else {
                            context.sender().sendMessage(main.parse("<main>" + condition.getConditionDescription(null)));
                        }
                    }

                    if (objective.getCompleteConditions().size() == 0) {
                        context.sender().sendMessage(main.parse("<warn>This objective has no complete conditions!"));
                    }
                }));


        final Command.Builder<CommandSender> editObjectiveConditionsBuilder = builder
                .literal("edit")
                .required("Condition ID", integerParser(1), Description.of("Condition ID"), (context, input) -> {
                            main.getUtilManager().sendFancyCommandCompletion(context.sender(), input.input().split(" "), "[Condition ID]", "[...]");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                            for (final Condition condition : objective.getCompleteConditions()) {
                                completions.add(Suggestion.suggestion("" + condition.getConditionID()));
                            }

                            return CompletableFuture.completedFuture(completions);
                        }
                );

        manager.command(editObjectiveConditionsBuilder
                .literal("delete", "remove").commandDescription(Description.of("Removes an complete condition from this Objective."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getCompleteConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Complete condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    objective.removeCompleteCondition(condition, true);
                    context.sender().sendMessage(main.parse("<main>The complete condition with the ID <highlight>" + conditionID + "</highlight> of Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> has been removed!"));
                }));


        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("set")
                .required("description", greedyStringParser(), Description.of("Objective condition description"), main.getCommandManager().miniMessageSuggestions()).commandDescription(Description.of("Sets the new description of the Objective complete condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getCompleteConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String description = (String) context.get("description");

                    condition.setDescription(description);

                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", description);
                    objective.getObjectiveHolder().saveConfig();

                    context.sender().sendMessage(main.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editObjectiveConditionsBuilder.literal("hidden")
                .literal("set")
                .required("hiddenStatusExpression", stringParser(), Description.of("Expression"))
                .commandDescription(Description.of("Sets the new hidden status of the Objective complete condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getCompleteConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String hiddenStatusExpression = context.get("hiddenStatusExpression");
                    final NumberExpression hiddenExpression = new NumberExpression(main, hiddenStatusExpression);

                    condition.setHidden(hiddenExpression);

                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".hiddenStatusExpression", hiddenStatusExpression);
                    objective.getObjectiveHolder().saveConfig();

                    context.sender().sendMessage(main.parse("<success>Hidden status successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>! New hidden status: <highlight2>"
                            + condition.getHiddenExpression()
                    ));
                }));

        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("remove", "delete").commandDescription(Description.of("Removes the description of the objective complete condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getCompleteConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Complete condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    condition.removeDescription();

                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions." + conditionID + ".description", "");
                    objective.getObjectiveHolder().saveConfig();

                    context.sender().sendMessage(main.parse("<success>Description successfully removed from complete condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("show", "check").commandDescription(Description.of("Shows the description of the objective complete condition."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getCompleteConditionFromID(conditionID);

                    if (condition == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Complete condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    context.sender().sendMessage(main.parse("<main>Description of complete condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objective.getObjectiveID() + "</highlight2>:\n"
                            + condition.getDescription()
                    ));
                }));
    }

    public void handleRewards(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each reward

        manager.command(builder.literal("list", "show").commandDescription(Description.of("Lists all the rewards this Quest has."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.sender().sendMessage(main.parse("<highlight>Rewards for Quest <highlight2>" + quest.getIdentifier() + "</highlight2>:"));
                    for (final Action action : quest.getRewards()) {
                        context.sender().sendMessage(main.parse("<highlight>" + action.getActionID() + ".</highlight> <main>" + action.getActionType()));
                        if (context.sender() instanceof Player player) {
                            context.sender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + action.getActionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        } else {
                            context.sender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + action.getActionDescription(null)));
                        }
                    }

                }));

        manager.command(builder.literal("clear").commandDescription(Description.of("Clears all the rewards this Quest has."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    quest.clearRewards();
                    context.sender().sendMessage(main.parse("<success>All rewards of Quest <highlight>" + quest.getIdentifier() + "</highlight> have been removed!"));
                }));


        final Command.Builder<CommandSender> editRewardsBuilder = builder.literal("edit")
                .required("reward-id", integerParser(1),
                        (context, input) -> {
                            main.getUtilManager().sendFancyCommandCompletion(context.sender(), input.input().split(" "), "[reward-id]", "[...]");

                            ArrayList<Suggestion> completions = new ArrayList<>();

                            final Quest quest = context.get("quest");
                            for (final Action action : quest.getRewards()) {
                                completions.add(Suggestion.suggestion("" + action.getActionID()));
                            }

                            return CompletableFuture.completedFuture(completions);
                        }
                );/*.withParser((context, input) -> { //TODO: Fix this parser. It isn't run at all.
                            final int ID = context.get("reward-id");
                            final Quest quest = context.get("quest");
                            final Action foundReward = quest.getRewards().get(ID - 1);
                            if (foundReward == null) {
                                return ArgumentParseResult.failure(new IllegalArgumentException("Reward with the ID '" + ID + "' does not belong to Quest '" + quest.getIdentifier() + "'!"));
                            } else {
                                return ArgumentParseResult.success(ID);
                            }
                        })
                        , Description.of("reward-id")*/
        handleEditRewards(editRewardsBuilder);
    }

    public void handleObjectiveRewards(final Command.Builder<CommandSender> builder, final int level) {
        //Add is handled individually by each reward

        manager.command(builder.literal("list", "show").commandDescription(Description.of("Lists all the rewards this Objective has."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    context.sender().sendMessage(main.parse("<highlight>Rewards for Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> of Quest <highlight2>" + objective.getObjectiveHolder().getIdentifier() + "</highlight2>:"));
                    for (final Action action : objective.getRewards()) {
                        context.sender().sendMessage(main.parse("<highlight>" + action.getActionID() + ".</highlight> <main>" + action.getActionType()));
                        if (context.sender() instanceof Player player) {
                            context.sender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + action.getActionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        } else {
                            context.sender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + action.getActionDescription(null)));
                        }
                    }
                }));

        manager.command(builder.literal("clear").commandDescription(Description.of("Clears all the rewards this Objective has."))
                .handler((context) -> {
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    objective.clearRewards();
                    context.sender().sendMessage(main.parse("<success>All rewards of Objective with ID <highlight>" + objective.getObjectiveID() + "</highlight> of Quest <highlight2>" + objective.getObjectiveHolder().getIdentifier() + "</highlight2> have been removed!"));
                }));


        final Command.Builder<CommandSender> editRewardsBuilder = builder.literal("edit")
                .required("reward-id", integerParser(1), Description.of("reward-id"), (context, input) -> {
                            main.getUtilManager().sendFancyCommandCompletion(context.sender(), input.input().split(" "), "[reward-id]", "[...]");

                            ArrayList<Suggestion> completions = new ArrayList<>();

                            final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                            for (final Action action : objective.getRewards()) {
                                completions.add(Suggestion.suggestion("" + action.getActionID()));
                            }

                            return CompletableFuture.completedFuture(completions);
                        }
                );
        handleObjectiveEditRewards(editRewardsBuilder, level);
    }

    public void handleObjectiveEditRewards(final Command.Builder<CommandSender> builder, final int level) {
        manager.command(builder.literal("info").commandDescription(Description.of("Shows everything there is to know about this reward."))
                .handler((context) -> {
                    final int ID = context.get("reward-id");
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.sender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }

                    context.sender().sendMessage(main.parse(
                            "<main>Reward <highlight>" + ID + "</highlight> for Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> of Quest <highlight2>" + objective.getObjectiveHolder().getIdentifier() + "</highlight2>:"
                    ));

                    if (context.sender() instanceof Player player) {
                        context.sender().sendMessage(main.parse(
                                "<unimportant>--</unimportant> <main>" + foundReward.getActionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))
                        ));
                    } else {
                        context.sender().sendMessage(main.parse(
                                "<unimportant>--</unimportant> <main>" + foundReward.getActionDescription(null)
                        ));
                    }


                }));

        manager.command(builder.literal("remove").commandDescription(Description.of("Removes the reward from the Quest."))
                .handler((context) -> {
                    final int ID = context.get("reward-id");
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.sender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    objective.removeReward(foundReward, true);
                    context.sender().sendMessage(main.parse(
                            "<success>The reward with the ID <highlight>" + ID + "</highlight> has been removed from the Objective with ID <highlight2>" + objective.getObjectiveID() + "</highlight2> of Quest <highlight2>"
                                    + objective.getObjectiveHolder().getIdentifier() + "</highlight2>!"
                    ));
                }));

        manager.command(builder.literal("displayName")
                .literal("show").commandDescription(Description.of("Shows current reward Display Name."))
                .handler((context) -> {
                    final int ID = context.get("reward-id");
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.sender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    if (foundReward.getActionName().isBlank()) {
                        context.sender().sendMessage(main.parse(
                                "<main>This reward has no display name set."
                        ));
                    } else {
                        context.sender().sendMessage(main.parse(
                                "<main>Reward display name: <highlight>" + foundReward.getActionName() + "</highlight>"
                        ));
                    }
                }));

        manager.command(builder.literal("displayName")
                .literal("remove", "delete").commandDescription(Description.of("Removes current reward Display Name."))
                .handler((context) -> {
                    final int ID = context.get("reward-id");
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.sender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    foundReward.removeActionName();
                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".rewards." + ID + ".displayName", null);
                    objective.getObjectiveHolder().saveConfig();
                    context.sender().sendMessage(main.parse(
                            "<success>Display Name of reward with the ID <highlight>" + ID + "</highlight> has been removed successfully."
                    ));
                }));

        manager.command(builder.literal("displayName")
                .literal("set")
                .required("display-name", greedyStringParser(), Description.of("Reward display name"), main.getCommandManager().miniMessageSuggestions()).commandDescription(Description.of("Sets new reward Display Name. Only rewards with a Display Name will be displayed."))
                .handler((context) -> {
                    final int ID = context.get("reward-id");
                    final Objective objective = main.getCommandManager().getObjectiveFromContextAndLevel(context, level);

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.sender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }

                    final String displayName = (String) context.get("display-name");


                    foundReward.setActionName(displayName);
                    objective.getObjectiveHolder().getConfig().set(objective.getObjectiveHolder().getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".rewards." + ID + ".displayName", foundReward.getActionName());
                    objective.getObjectiveHolder().saveConfig();
                    context.sender().sendMessage(main.parse(
                            "<success>Display Name successfully added to reward with ID <highlight>" + ID + "</highlight>! New display name: <highlight2>"
                                    + foundReward.getActionName()
                    ));
                }));
    }

    public void handleEditRewards(final Command.Builder<CommandSender> builder) {
        manager.command(builder.literal("info").commandDescription(Description.of("Shows everything there is to know about this reward."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int ID = context.get("reward-id");
                    final Action foundReward = quest.getRewardFromID(ID);
                    context.sender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }

                    context.sender().sendMessage(main.parse(
                            "<main>Reward <highlight>" + ID + "</highlight> for Quest <highlight2>" + quest.getIdentifier() + "</highlight2>:"
                    ));

                    if (context.sender() instanceof Player player) {
                        context.sender().sendMessage(main.parse(
                                "<unimportant>--</unimportant> <main>" + foundReward.getActionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))
                        ));
                    } else {
                        context.sender().sendMessage(main.parse(
                                "<unimportant>--</unimportant> <main>" + foundReward.getActionDescription(null)
                        ));
                    }


                }));

        manager.command(builder.literal("remove").commandDescription(Description.of("Removes the reward from the Quest."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int ID = context.get("reward-id");
                    final Action foundReward = quest.getRewardFromID(ID);
                    context.sender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    quest.removeReward(foundReward);
                    context.sender().sendMessage(main.parse(
                            "<success>The reward with the ID <highlight>" + ID + "</highlight> has been removed from the Quest <highlight2>"
                                    + quest.getIdentifier() + "</highlight2>!"
                    ));
                }));

        manager.command(builder.literal("displayName")
                .literal("show").commandDescription(Description.of("Shows current reward Display Name."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int ID = context.get("reward-id");
                    final Action foundReward = quest.getRewardFromID(ID);
                    context.sender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    if (foundReward.getActionName().isBlank()) {
                        context.sender().sendMessage(main.parse(
                                "<main>This reward has no display name set."
                        ));
                    } else {
                        context.sender().sendMessage(main.parse(
                                "<main>Reward display name: <highlight>" + foundReward.getActionName() + "</highlight>"
                        ));
                    }
                }));

        manager.command(builder.literal("displayName")
                .literal("remove", "delete").commandDescription(Description.of("Removes current reward Display Name."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int ID = context.get("reward-id");
                    final Action foundReward = quest.getRewardFromID(ID);
                    context.sender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    foundReward.removeActionName();
                    foundReward.getCategory().getQuestsConfig().set("quests." + quest.getIdentifier() + ".rewards." + ID + ".displayName", null);
                    foundReward.getCategory().saveQuestsConfig();
                    context.sender().sendMessage(main.parse(
                            "<success>Display Name of reward with the ID <highlight>" + ID + "</highlight> has been removed successfully."
                    ));
                }));

        manager.command(builder.literal("displayName")
                .literal("set")
                .required("display-name", greedyStringParser(), Description.of("Reward display name"), main.getCommandManager().miniMessageSuggestions()).commandDescription(Description.of("Sets new reward Display Name. Only rewards with a Display Name will be displayed."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int ID = context.get("reward-id");
                    final Action foundReward = quest.getRewardFromID(ID);
                    context.sender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }

                    final String displayName = (String) context.get("display-name");


                    foundReward.setActionName(displayName);
                    foundReward.getCategory().getQuestsConfig().set("quests." + quest.getIdentifier() + ".rewards." + ID + ".displayName", foundReward.getActionName());
                    foundReward.getCategory().saveQuestsConfig();
                    context.sender().sendMessage(main.parse(
                            "<success>Display Name successfully added to reward with ID <highlight>" + ID + "</highlight>! New display name: <highlight2>"
                                    + foundReward.getActionName()
                    ));
                }));
    }

    public void handleTriggers(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each trigger
        manager.command(builder.literal("clear").commandDescription(Description.of("Removes all the triggers this Quest has."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    quest.clearTriggers();
                    context.sender().sendMessage(main.parse(
                            "<success>All Triggers of Quest <highlight>" + quest.getIdentifier() + "</highlight> have been removed!"
                    ));

                }));

        manager.command(builder.literal("list", "show").commandDescription(Description.of("Lists all the triggers this Quest has."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");


                    context.sender().sendMessage(main.parse("<highlight>Triggers for Quest <highlight2>" + quest.getIdentifier() + "</highlight2>:"));

                    for (Trigger trigger : quest.getTriggers()) {
                        context.sender().sendMessage(main.parse("<highlight>" + trigger.getTriggerID() + ".</highlight> Type: <main>" + trigger.getTriggerType()));


                        final String triggerDescription = trigger.getTriggerDescription();
                        if (triggerDescription != null && !triggerDescription.isBlank()) {
                            context.sender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + triggerDescription));
                        }

                        context.sender().sendMessage(main.parse("<unimportant>--- Action Name:</unimportant> <main>" + trigger.getTriggerAction().getActionName()));
                        if (context.sender() instanceof Player player) {
                            context.sender().sendMessage(main.parse("<unimportant>------ Description:</unimportant> <main>" + trigger.getTriggerAction().getActionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));

                        } else {
                            context.sender().sendMessage(main.parse("<unimportant>------ Description:</unimportant> <main>" + trigger.getTriggerAction().getActionDescription(null)));

                        }
                        context.sender().sendMessage(main.parse("<unimportant>--- Amount of triggers needed for first execution:</unimportant> <main>" + trigger.getAmountNeeded()));

                        if (trigger.getApplyOn() == 0) {
                            context.sender().sendMessage(main.parse("<unimportant>--- Apply on:</unimportant> <main>Quest"));

                        } else {
                            context.sender().sendMessage(main.parse("<unimportant>--- Apply on:</unimportant> <main>Objective " + trigger.getApplyOn()));
                        }

                        if (trigger.getWorldName() == null || trigger.getWorldName().isBlank() || trigger.getWorldName().equalsIgnoreCase("ALL")) {
                            context.sender().sendMessage(main.parse("<unimportant>--- In World:</unimportant> <main>Any World"));
                        } else {
                            context.sender().sendMessage(main.parse("<unimportant>--- In World:</unimportant> <main>" + trigger.getWorldName()));
                        }

                    }

                }));


        manager.command(builder.literal("remove", "delete")
                .required("trigger-id", integerParser(1), Description.of("Trigger ID"), (context, input) -> {
                            main.getUtilManager().sendFancyCommandCompletion(context.sender(), input.input().split(" "), "[Trigger ID]", "");

                            ArrayList<Suggestion> completions = new ArrayList<>();

                            final Quest quest = context.get("quest");
                            for (final Trigger trigger : quest.getTriggers()) {
                                completions.add(Suggestion.suggestion("" + trigger.getTriggerID()));
                            }

                            return CompletableFuture.completedFuture(completions);
                        }

                ).commandDescription(Description.of("Removes all the triggers this Quest has."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    final int triggerID = context.get("trigger-id");

                    final Trigger trigger = quest.getTriggerFromID(triggerID);

                    if (trigger == null) {
                        context.sender().sendMessage(main.parse(
                                "<error> Error: Trigger with the ID <highlight>" + triggerID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    context.sender().sendMessage(main.parse(
                            quest.removeTrigger(trigger)
                    ));

                }));

    }

}
