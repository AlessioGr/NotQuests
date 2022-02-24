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

package rocks.gravili.notquests.paper.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.standard.*;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
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
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.MiniMessageSelector;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


public class AdminEditCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> editBuilder;


    public AdminEditCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> editBuilder) {
        this.main = main;
        this.manager = manager;
        this.editBuilder = editBuilder;



        manager.command(editBuilder.literal("acceptCooldown", "cooldown")
                .literal("set")
                .argument(DurationArgument.of("duration"), ArgumentDescription.of("New accept cooldown."))
                .meta(CommandMeta.DESCRIPTION, "Sets the time players have to wait between accepting quests.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final Duration durationCooldown = context.get("duration");
                    final long cooldownInMinutes = durationCooldown.toMinutes();

                    quest.setAcceptCooldown(cooldownInMinutes);
                    context.getSender().sendMessage(main.parse(
                            "<success>Cooldown for Quest <highlight>" + quest.getQuestName() + "</highlight> has been set to <highlight2>"
                                    + durationCooldown.toDaysPart() + " days, " + durationCooldown.toHoursPart() + " hours, " + durationCooldown.toMinutesPart() + " minutes" + "</highlight2>!"
                    ));
                }));

        manager.command(editBuilder.literal("acceptCooldown", "cooldown")
                .literal("disable")
                .meta(CommandMeta.DESCRIPTION, "Disables the wait time for players between accepting quests.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    quest.setAcceptCooldown(-1);
                    context.getSender().sendMessage(main.parse(
                            "<success>Cooldown for Quest <highlight>" + quest.getQuestName() + "</highlight> has been set to <highlight2>disabled</highlight2>!"
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
                            "<main>Current description of Quest <highlight>" + quest.getQuestName() + "</highlight>: <highlight2>"
                                    + quest.getQuestDescription()
                    ));
                }));
        manager.command(editBuilder.literal("description")
                .literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes current Quest description.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    quest.removeQuestDescription(true);
                    context.getSender().sendMessage(main.parse("<success>Description successfully removed from quest <highlight>"
                            + quest.getQuestName() + "</highlight>!"
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
                            + quest.getQuestName() + "</highlight>! New description: <highlight2>"
                            + quest.getQuestDescription()
                    ));
                }));

        manager.command(editBuilder.literal("displayName")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current Quest display name.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.getSender().sendMessage(main.parse(
                            "<main>Current display name of Quest <highlight>" + quest.getQuestName() + "</highlight>: <highlight2>"
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
                            + quest.getQuestName() + "</highlight>!"
                    ));
                }));

        manager.command(editBuilder.literal("displayName")
        .literal("set")
                .argument(StringArrayArgument.of("DisplayName",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter new Quest display name>", "");
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
                                    completions.add("<Enter new Quest display name>");
                                }
                            }
                            return completions;
                        }
                ), ArgumentDescription.of("Quest display name"))
                .meta(CommandMeta.DESCRIPTION, "Sets the new display name of the Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    final String displayName = String.join(" ", (String[]) context.get("DisplayName"));

                    quest.setQuestDisplayName(displayName, true);
                    context.getSender().sendMessage(main.parse("<success>Display name successfully added to quest <highlight>"
                            + quest.getQuestName() + "</highlight>! New display name: <highlight2>"
                            + quest.getQuestDisplayName()
                    ));
                }));


        manager.command(editBuilder.literal("maxAccepts")
                .argument(IntegerArgument.<CommandSender>newBuilder("max. accepts").withMin(-1).withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    completions.add("<amount of maximum accepts>");
                    return completions;
                }).build(), ArgumentDescription.of("Maximum amount of accepts. Set to -1 for unlimited (default)."))
                .meta(CommandMeta.DESCRIPTION, "Sets the maximum amount of times you can start/accept this Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int maxAccepts = context.get("max. accepts");
                    if (maxAccepts > 0) {
                        quest.setMaxAccepts(maxAccepts);
                        context.getSender().sendMessage(main.parse(
                                "<success>Maximum amount of accepts for Quest <highlight>" + quest.getQuestName() + "</highlight> has been set to <highlight2>"
                                        + maxAccepts + "</highlight2>!"
                        ));
                    } else {
                        quest.setMaxAccepts(-1);
                        context.getSender().sendMessage(main.parse(
                                "<success>Maximum amount of accepts for Quest <highlight>" + quest.getQuestName() + "</highlight> has been set to <highlight2>"
                                        + "unlimited (default)</highlight2>!"
                        ));
                    }
                }));

        if (main.getIntegrationsManager().isCitizensEnabled()) {
            final Command.Builder<CommandSender> citizensNPCsBuilder = editBuilder.literal("npcs");
            handleCitizensNPCs(citizensNPCsBuilder);
        }

        manager.command(editBuilder.literal("takeEnabled")
                .argument(BooleanArgument.<CommandSender>newBuilder("Take Enabled").withLiberal(true).build(),
                        ArgumentDescription.of("Enabled by default. Yes / no"))
                .meta(CommandMeta.DESCRIPTION, "Sets if players can accept the Quest using /notquests take.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    boolean takeEnabled = context.get("Take Enabled");
                    quest.setTakeEnabled(takeEnabled);
                    if (takeEnabled) {
                        context.getSender().sendMessage(main.parse(
                                 "<success>Quest taking (/notquests take) for the Quest <highlight>"
                                        + quest.getQuestName() + "</highlight> has been set to <highlight2>enabled</highlight2>!"
                        ));
                    } else {
                        context.getSender().sendMessage(main.parse(
                                "<success>Quest taking (/notquests take) for the Quest <highlight>"
                                        + quest.getQuestName() + "</highlight> has been set to <highlight2>disabled</highlight2>!"
                        ));
                    }
                }));

        manager.command(editBuilder.literal("takeItem")

                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of item displayed in the Quest take GUI."))
                .flag(
                        manager.flagBuilder("glow")
                                .withDescription(ArgumentDescription.of("Makes the item have the enchanted glow."))
                )
                .meta(CommandMeta.DESCRIPTION, "Sets the item displayed in the Quest take GUI (default: book).")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final boolean glow = context.flags().isPresent("glow");

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack takeItem;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            takeItem = player.getInventory().getItemInMainHand();
                        } else {
                            context.getSender().sendMessage(main.parse(
                                    "<error>This command must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        if (materialOrHand.material.equalsIgnoreCase("any")) {
                            takeItem = new ItemStack(Material.BOOK, 1);
                        } else {
                            takeItem = main.getItemsManager().getItemStack(materialOrHand.material);
                        }
                    }
                    if (glow) {
                        takeItem.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
                        ItemMeta meta = takeItem.getItemMeta();
                        if (meta == null) {
                            meta = Bukkit.getItemFactory().getItemMeta(takeItem.getType());
                        }
                        if (meta != null) {
                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            takeItem.setItemMeta(meta);
                        }

                    }


                    quest.setTakeItem(takeItem);
                    context.getSender().sendMessage(main.parse(
                            "<success>Take Item Material for Quest <highlight>" + quest.getQuestName()
                                    + "</highlight> has been set to <highlight2>" + takeItem.getType().name() + "</highlight2>!"
                    ));


                }));

        final Command.Builder<CommandSender> objectivesBuilder = editBuilder.literal("objectives");
        handleObjectives(objectivesBuilder);
        final Command.Builder<CommandSender> requirementsBuilder = editBuilder.literal("requirements");
        handleRequirements(requirementsBuilder);
        final Command.Builder<CommandSender> rewardsBuilder = editBuilder.literal("rewards");
        handleRewards(rewardsBuilder);
        final Command.Builder<CommandSender> triggersBuilder = editBuilder.literal("triggers");
        handleTriggers(triggersBuilder);

        final Command.Builder<CommandSender> categoryBuilder = editBuilder.literal("category");
        handleCategories(categoryBuilder);
    }

    public void handleCategories(final Command.Builder<CommandSender> builder){
        manager.command(builder.literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows the current category of this Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.getSender().sendMessage(main.parse(
                            "<main>Category for Quest <highlight>" + quest.getQuestName() + "</highlight>: <highlight2>"
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
                                "<error> Error: The quest <highlight>" + quest.getQuestName() + "</highlight> already has the category <highlight2>" + quest.getCategory().getCategoryFullName() + "</highlight2>."
                        ));
                        return;
                    }


                    context.getSender().sendMessage(main.parse(
                            "<success>Category for Quest <highlight>" + quest.getQuestName() + "</highlight> has successfully been changed from <highlight2>"
                                    + quest.getCategory().getCategoryFullName() + "</highlight2> to <highlight2>" + category.getCategoryFullName() + "</highlight2>!"
                    ));

                    quest.switchCategory(category);

                }));
    }


    public void handleCitizensNPCs(final Command.Builder<CommandSender> builder) {
        manager.command(builder.literal("add")
                .argument(IntegerArgument.<CommandSender>newBuilder("npc ID").withMin(0).withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                        completions.add("" + npc.getId());
                    }
                    final List<String> allArgs = context.getRawInput();
                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[ID of the NPC you wish to add]", "(optional: --hideInNPC)");

                    return completions;
                }).build(), ArgumentDescription.of("ID of the Citizens NPC to whom the Quest should be attached."))
                .flag(
                        manager.flagBuilder("hideInNPC")
                                .withDescription(ArgumentDescription.of("Makes the Quest hidden from in the NPC."))
                )
                .meta(CommandMeta.DESCRIPTION, "Attaches the Quest to a Citizens NPC.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    boolean showInNPC = !context.flags().isPresent("hideInNPC");

                    int npcID = context.get("npc ID");

                    final NPC npc = CitizensAPI.getNPCRegistry().getById(npcID);
                    if (npc != null) {
                        if (!quest.getAttachedNPCsWithQuestShowing().contains(npc) && !quest.getAttachedNPCsWithoutQuestShowing().contains(npc)) {
                            quest.bindToNPC(npc, showInNPC);
                            context.getSender().sendMessage(main.parse(
                                    "<success>Quest <highlight>" + quest.getQuestName()
                                            + "</highlight> has been bound to the NPC with the ID <highlight2>" + npcID
                                            + "</highlight2>! Showing Quest: <highlight>" + showInNPC + "</highlight>."
                            ));
                        } else {
                            context.getSender().sendMessage(main.parse(
                                    "<warn>Quest <highlight>" + quest.getQuestName()
                                            + "</highlight> has already been bound to the NPC with the ID <highlight2>" + npcID
                                            + "</highlight2>!"
                            ));
                        }

                    } else {
                        context.getSender().sendMessage(main.parse("<error>NPC with the ID <highlight>" + npcID + "</highlight> was not found!"));
                    }

                }));

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "De-attaches this Quest from all Citizens NPCs.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    quest.clearNPCs();
                    context.getSender().sendMessage(main.parse("<success>All NPCs of Quest <highlight>" + quest.getQuestName() + "</highlight> have been removed!"));

                }));

        manager.command(builder.literal("list")
                .meta(CommandMeta.DESCRIPTION, "Lists all Citizens NPCs which have this Quest attached.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    context.getSender().sendMessage(main.parse("<highlight>NPCs bound to quest <highlight2>" + quest.getQuestName() + "</highlight2> with Quest showing:"));
                    int counter = 1;
                    for (final NPC npc : quest.getAttachedNPCsWithQuestShowing()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>ID:</main> <highlight2>" + npc.getId()));
                        counter++;
                    }
                    counter = 1;
                    context.getSender().sendMessage(main.parse( "<highlight>NPCs bound to quest <highlight2>" + quest.getQuestName() + "</highlight2> without Quest showing:"));
                    for (NPC npc : quest.getAttachedNPCsWithoutQuestShowing()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>ID:</main> <highlight2>" + npc.getId()));
                        counter++;
                    }
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
                                "<GOLD>Add showing Quest <highlight>" + quest.getQuestName() + "</highlight> to Armor Stand"
                        ));
                        lore.add(main.parse(
                                "<WHITE>Hit an armor stand to add the showing Quest <highlight>" + quest.getQuestName() + "</highlight> to it."
                        ));

                    } else {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
                        itemMeta.displayName(main.parse(
                                "<GOLD>Add non-showing Quest </highlight>" + quest.getQuestName() + "</highlight> to Armor Stand"
                        ));
                        lore.add(main.parse(
                                "<WHITE>Hit an armor stand to add the non-showing Quest <highlight>" + quest.getQuestName() + "</highlight> to it."
                        ));

                    }
                    itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());
                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


                    itemMeta.lore(lore);
                    itemStack.setItemMeta(itemMeta);
                    player.getInventory().addItem(itemStack);
                    context.getSender().sendMessage(main.parse("<success>You have been given an item with which you can add this quest to armor stands!"));
                }));

        manager.command(builder.literal("clear")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "This command is not done yet.")
                .handler((context) -> {
                    context.getSender().sendMessage(main.parse("<error>Sorry, this command is not done yet! I'll add it in future versions."));
                }));

        manager.command(builder.literal("list")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "This command is not done yet.")
                .handler((context) -> {
                    context.getSender().sendMessage(main.parse("<error>Sorry, this command is not done yet! I'll add it in future versions."));
                }));


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
                                "<RED>Remove showing Quest <highlight>" + quest.getQuestName() + "</highlight> from Armor Stand"
                        ));

                        lore.add(main.parse(
                                "<WHITE>Hit an armor stand to remove the showing Quest <highlight>" + quest.getQuestName() + "</highlight> from it."
                        ));

                    } else {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 3);

                        itemMeta.displayName(main.parse(
                                "<RED>Remove non-showing Quest <highlight>" + quest.getQuestName() + "</highlight> from Armor Stand"
                        ));

                        lore.add(main.parse(
                                "<WHITE>Hit an armor stand to remove the non-showing Quest <highlight>" + quest.getQuestName() + "</highlight> from it."
                        ));
                    }
                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                    itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());


                    itemMeta.lore(lore);

                    itemStack.setItemMeta(itemMeta);

                    player.getInventory().addItem(itemStack);

                    context.getSender().sendMessage(main.parse("<success>You have been given an item with which you can remove this quest from armor stands!"));
                }));
    }

    public void handleObjectives(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each objective

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Removes all objectives from a Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    quest.clearObjectives();
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse(
                            "<success>All objectives of Quest <highlight>" + quest.getQuestName()
                                    + "</highlight> have been removed!"
                    ));
                }));
        manager.command(builder.literal("list")
                .meta(CommandMeta.DESCRIPTION, "Lists all objectives of a Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<highlight>Objectives for Quest <highlight2>" + quest.getQuestName() + "</highlight2>:"));
                    main.getQuestManager().sendObjectivesAdmin(context.getSender(), quest);
                }));


        handleEditObjectives(main.getCommandManager().getAdminEditObjectivesBuilder());


    }

    public void handleEditObjectives(final Command.Builder<CommandSender> builder) {
        manager.command(builder.literal("location")
                .literal("enable")
                .meta(CommandMeta.DESCRIPTION, "Shows the location to the player.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    objective.setShowLocation(true, true);

                    context.getSender().sendMessage(main.parse(
                            "<main>The objective with ID <highlight>" + objectiveID + "</highlight> is now showing the location to the player!"
                    ));
                }));

        manager.command(builder.literal("location")
                .literal("disables")
                .meta(CommandMeta.DESCRIPTION, "Disables showing the location to the player.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    objective.setShowLocation(false, true);

                    context.getSender().sendMessage(main.parse(
                            "<main>The objective with ID <highlight>" + objectiveID + "</highlight> is now no longer showing the location to the player!"
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
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final World world = context.get("world");
                    final Vector coordinates = new Vector(context.get("x"), context.get("y"), context.get("z"));
                    final Location location = coordinates.toLocation(world);

                    objective.setLocation(location, true);
                    objective.setShowLocation(true, true);

                    context.getSender().sendMessage(main.parse(
                            "<main>The objective with ID <highlight>" + objectiveID + "</highlight> is now has a location!"
                    ));
                }));

        manager.command(builder.literal("completionNPC")
                .literal("show", "view")
                .meta(CommandMeta.DESCRIPTION, "Shows the completionNPC of an objective.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    context.getSender().sendMessage(main.parse(
                            "<main>The completionNPCID of the objective with the ID <highlight>" + objectiveID + "</highlight> is <highlight2>"
                                    + objective.getCompletionNPCID() + "</highlight2>!"
                    ));
                    if (objective.getCompletionArmorStandUUID() != null) {
                        context.getSender().sendMessage(main.parse(
                                "<main>The completionNPCUUID (for armor stands) of the objective with the ID <highlight>" + objectiveID + "</highlight> is <highlight2>"
                                        + objective.getCompletionArmorStandUUID() + "</highlight2>!"
                        ));
                    } else {
                        context.getSender().sendMessage(main.parse(
                                "<main>The completionNPCUUID (for armor stands) of the objective with the ID <highlight>" + objectiveID + "</highlight> is <highlight2>null</highlight2>!"
                        ));
                    }
                }));
        manager.command(builder.literal("completionNPC")
                .literal("set")
                .argument(StringArgument.<CommandSender>newBuilder("Completion NPC").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Completion NPC ID / 'armorstand']", "");

                            ArrayList<String> completions = new ArrayList<>();

                            for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                                completions.add("" + npc.getId());
                            }
                            completions.add("armorstand");
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Completion NPC"))
                .meta(CommandMeta.DESCRIPTION, "Sets the completionNPC of an objective.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final String completionNPC = context.get("Completion NPC");

                    if (completionNPC.equalsIgnoreCase("-1")) {

                        objective.setCompletionNPCID(-1, true);
                        objective.setCompletionArmorStandUUID(null, true);
                        context.getSender().sendMessage(main.parse(
                                "<success>The completionNPC of the objective with the ID <highlight>" + objectiveID + "</highlight> has been removed!"
                        ));

                    } else if (!completionNPC.equalsIgnoreCase("armorstand")) {
                        final int completionNPCID = Integer.parseInt(completionNPC);

                        objective.setCompletionNPCID(completionNPCID, true);
                        context.getSender().sendMessage(main.parse(
                                "<success>The completionNPC of the objective with the ID <highlight>" + objectiveID
                                        + "</highlight> has been set to the NPC with the ID <highlight2>" + completionNPCID + "</highlight2>!"
                        ));

                    } else { //Armor Stands

                        if (context.getSender() instanceof final Player player) {
                            ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                            //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                            NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-item");
                            NamespacedKey QuestNameKey = new NamespacedKey(main.getMain(), "notquests-questname");
                            NamespacedKey ObjectiveIDKey = new NamespacedKey(main.getMain(), "notquests-objectiveid");

                            ItemMeta itemMeta = itemStack.getItemMeta();
                            List<Component> lore = new ArrayList<>();

                            assert itemMeta != null;

                            itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());
                            itemMeta.getPersistentDataContainer().set(ObjectiveIDKey, PersistentDataType.INTEGER, objectiveID);
                            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 6);


                            itemMeta.displayName(main.parse(
                                    "<LIGHT_PURPLE>Set completionNPC of Quest <highlight>" + quest.getQuestName() + "</highlight> to this Armor Stand"
                            ));
                            //Only paper lore.add(Component.text("§fRight-click an Armor Stand to see which Quests are attached to it."));
                            lore.add(main.parse(
                                    "<WHITE>Right-click an Armor Stand to set it as the completionNPC of Quest <highlight>" + quest.getQuestName() + "</highlight> and ObjectiveID <highlight>" + objectiveID + "</highlight>."
                            ));

                            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                            itemMeta.lore(lore);

                            itemStack.setItemMeta(itemMeta);

                            player.getInventory().addItem(itemStack);

                            context.getSender().sendMessage(main.parse(
                                    "<success>You have been given an item with which you can add the completionNPC of this Objective to an armor stand. Check your inventory!"
                            ));

                        } else {
                            context.getSender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                        }


                    }
                }));


        manager.command(builder.literal("conditions")
                .literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Removes all conditions from this objective.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    objective.clearConditions();
                    context.getSender().sendMessage(main.parse(
                            "<success>All conditions of objective with ID <highlight>" + objectiveID
                                    + "</highlight> have been removed!"
                    ));

                }));

        manager.command(builder.literal("conditions")
                .literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all conditions of this objective.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null


                    context.getSender().sendMessage(main.parse(
                            "<highlight>Conditions of objective with ID <highlight2>" + objectiveID
                                    + "</highlight2>:"
                    ));
                    for (Condition condition : objective.getConditions()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + condition.getConditionID() + ".</highlight> <main>" + condition.getConditionType() + "</main>"));
                        if(context.getSender() instanceof Player player){
                            context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        }else{
                            context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(null)));
                        }
                    }

                    if (objective.getConditions().size() == 0) {
                        context.getSender().sendMessage(main.parse("<warn>This objective has no conditions!"));
                    }
                }));


        final Command.Builder<CommandSender> editObjectiveConditionsBuilder = builder
                .literal("conditions")
                .literal("edit")
                .argument(IntegerArgument.<CommandSender>newBuilder("Condition ID").withMin(1).withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Condition ID]", "[...]");

                            ArrayList<String> completions = new ArrayList<>();

                            final Quest quest = context.get("quest");
                            final int objectiveID = context.get("Objective ID");
                            final Objective objective = quest.getObjectiveFromID(objectiveID);
                            assert objective != null; //Shouldn't be null

                             for (final Condition condition : objective.getConditions()) {
                                completions.add("" + condition.getConditionID());
                            }

                            return completions;
                        }
                ));

        manager.command(editObjectiveConditionsBuilder.literal("delete", "remove")
                .meta(CommandMeta.DESCRIPTION, "Removes a condition from this Objective.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getConditionFromID(conditionID);

                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    objective.removeCondition(condition, true);
                    context.getSender().sendMessage(main.parse("<main>The condition with the ID <highlight>" + conditionID + "</highlight> of Objective with ID <highlight2>" + objectiveID + "</highlight2> has been removed!"));
                }));


        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("set")
                .argument(MiniMessageSelector.<CommandSender>newBuilder("description", main).withPlaceholders().build(), ArgumentDescription.of("Objective condition description"))
                .meta(CommandMeta.DESCRIPTION, "Sets the new description of the Objective condition.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getConditionFromID(conditionID);

                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String description = String.join(" ", (String[]) context.get("description"));

                    condition.setDescription(description);

                    quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + objectiveID + ".conditions." + conditionID + ".description", description);
                    quest.getCategory().saveQuestsConfig();

                    context.getSender().sendMessage(main.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objectiveID + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes the description of the Quest requirement.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getConditionFromID(conditionID);

                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    condition.removeDescription();

                    quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + objectiveID + ".conditions." + conditionID + ".description", "");
                    quest.getCategory().saveQuestsConfig();

                    context.getSender().sendMessage(main.parse("<success>Description successfully removed from condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objectiveID + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editObjectiveConditionsBuilder.literal("description")
                .literal("show", "check")
                .meta(CommandMeta.DESCRIPTION, "Shows the description of the Quest requirement.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    int conditionID = context.get("Condition ID");
                    Condition condition = objective.getConditionFromID(conditionID);

                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    context.getSender().sendMessage(main.parse("<main>Description of condition with ID <highlight>" + conditionID + "</highlight> of objective with ID <highlight2>"
                            + objectiveID + "</highlight2>:\n"
                            + condition.getDescription()
                    ));
                }));













        manager.command(builder.literal("description")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current objective description.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    context.getSender().sendMessage(main.parse(
                            "<main>Current description of objective with ID <highlight>" + objectiveID + "</highlight>: <highlight2>"
                                    + objective.getDescription()
                    ));
                }));
        manager.command(builder.literal("description")
                .literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes current objective description.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    objective.removeDescription(true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Description successfully removed from objective with ID <highlight>" + objectiveID + "</highlight>! New description: <highlight2>"
                                    + objective.getDescription()
                    ));
                }));

        manager.command(builder.literal("description")
                .literal("set")
                .argument(StringArrayArgument.of("Objective Description",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter new Objective description>", "");
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
                                    completions.add("<Enter new Objective description>");
                                }
                            }

                            return completions;
                        }
                ), ArgumentDescription.of("Objective description"))
                .meta(CommandMeta.DESCRIPTION, "Sets current objective description.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final String description = String.join(" ", (String[]) context.get("Objective Description"));
                    objective.setDescription(description, true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Description successfully added to objective with ID <highlight>" + objectiveID + "</highlight>! New description: <highlight2>"
                                    + objective.getDescription()
                    ));
                }));


        manager.command(builder.literal("displayname")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current objective displayname.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    context.getSender().sendMessage(main.parse(
                            "<main>Current displayname of objective with ID <highlight>" + objectiveID + "</highlight>: <highlight2>"
                                    + objective.getDisplayName()
                    ));
                }));
        manager.command(builder.literal("displayname")
                .literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes current objective displayname.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    objective.removeDisplayName(true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Displayname successfully removed from objective with ID <highlight>" + objectiveID + "</highlight>! New displayname: <highlight2>"
                                    + objective.getDescription()
                    ));
                }));

        manager.command(builder.literal("displayname")
                .literal("set")
                .argument(StringArrayArgument.of("Objective Displayname",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter new Objective displayname>", "");
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
                                    completions.add("<Enter new Objective display name>");
                                }
                            }

                            return completions;
                        }
                ), ArgumentDescription.of("Objective displayname"))
                .meta(CommandMeta.DESCRIPTION, "Sets current objective displayname.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final String description = String.join(" ", (String[]) context.get("Objective Displayname"));
                    objective.setDisplayName(description, true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Displayname successfully added to objective with ID <highlight>" + objectiveID + "</highlight>! New displayname: <highlight2>"
                                    + objective.getDisplayName()
                    ));
                }));


        manager.command(builder.literal("info")
                .meta(CommandMeta.DESCRIPTION, "Shows everything there is to know about this objective.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse(
                            "<highlight>Information of objective with the ID <highlight2>" + objectiveID
                                    + "</highlight2> from Quest <highlight2>" + quest.getQuestName() + "</highlight2>:"
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
                            "<highlight>Objective Description: <main>" + objective.getDescription()
                    ));

                    context.getSender().sendMessage(main.parse(
                            "<highlight>Objective Conditions:"
                    ));
                    int counter = 1;
                    for (final Condition condition : objective.getConditions()) {
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
                }));

        manager.command(builder.literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes the objective from the Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    quest.removeObjective(objective);
                    context.getSender().sendMessage(main.parse(
                            "<success>Objective with the ID <highlight>" + objectiveID + "</highlight> has been successfully removed from Quest <highlight2>"
                                    + quest.getQuestName() + "</highlight2>!"
                    ));
                }));


        final Command.Builder<CommandSender> rewardsBuilder = builder.literal("rewards");
        handleObjectiveRewards(rewardsBuilder);
    }


    public void handleRequirements(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each requirement


        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the requirements this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.getSender().sendMessage(main.parse("<highlight>Requirements for Quest <highlight2>" + quest.getQuestName() + "</highlight2>:"));
                    for (Condition condition : quest.getRequirements()) {
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
                    context.getSender().sendMessage(main.parse("<main>All requirements of Quest <highlight>" + quest.getQuestName() + "</highlight> have been removed!"));
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


                    context.getSender().sendMessage(main.parse("<main>The requirement with the ID <highlight>" + conditionID + "</highlight> of Quest <highlight2>" + quest.getQuestName() + "</highlight2> has been removed!"));
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

                    quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".requirements." + conditionID + ".description", description);
                    quest.getCategory().saveQuestsConfig();

                   // foundReward.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".rewards." + (ID + 1) + ".displayName", foundReward.getActionName());
                    //foundReward.getCategory().saveQuestsConfig();

                    context.getSender().sendMessage(main.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of quest <highlight2>"
                            + quest.getQuestName() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
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

                    quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".requirements." + conditionID + ".description", "");
                    quest.getCategory().saveQuestsConfig();


                    context.getSender().sendMessage(main.parse("<success>Description successfully removed from condition with ID <highlight>" + conditionID + "</highlight> of quest <highlight2>"
                            + quest.getQuestName() + "</highlight2>! New description: <highlight2>"
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
                            + quest.getQuestName() + "</highlight2>:\n"
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

                    context.getSender().sendMessage(main.parse("<highlight>Rewards for Quest <highlight2>" + quest.getQuestName() + "</highlight2>:"));
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
                    context.getSender().sendMessage(main.parse("<success>All rewards of Quest <highlight>" + quest.getQuestName() + "</highlight> have been removed!"));
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
                                return ArgumentParseResult.failure(new IllegalArgumentException("Reward with the ID '" + ID + "' does not belong to Quest '" + quest.getQuestName() + "'!"));
                            } else {
                                return ArgumentParseResult.success(ID);
                            }
                        })
                        , ArgumentDescription.of("Reward ID")*/);
        handleEditRewards(editRewardsBuilder);
    }

    public void handleObjectiveRewards(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each reward

        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the rewards this Objective has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    context.getSender().sendMessage(main.parse("<highlight>Rewards for Objective with ID <highlight2>" + objectiveID + "</highlight2> of Quest <highlight2>" + quest.getQuestName() + "</highlight2>:"));
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
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    objective.clearRewards();
                    context.getSender().sendMessage(main.parse("<success>All rewards of Objective with ID <highlight>" + objectiveID + "</highlight> of Quest <highlight2>" + quest.getQuestName() + "</highlight2> have been removed!"));
                }));


        final Command.Builder<CommandSender> editRewardsBuilder = builder.literal("edit")
                .argument(IntegerArgument.<CommandSender>newBuilder("Reward ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Reward ID]", "[...]");

                                    ArrayList<String> completions = new ArrayList<>();

                                    final Quest quest = context.get("quest");
                                    final int objectiveID = context.get("Objective ID");
                                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                                    assert objective != null; //Shouldn't be null

                                    for (final Action action : objective.getRewards()) {
                                        completions.add("" + action.getActionID());
                                    }

                                    return completions;
                                }
                        ).withParser((context, lastString) -> { //TODO: Fix this parser. It isn't run at all.
                            final int ID = context.get("Reward ID");
                            final Quest quest = context.get("quest");
                            final int objectiveID = context.get("Objective ID");
                            final Objective objective = quest.getObjectiveFromID(objectiveID);
                            assert objective != null; //Shouldn't be null

                            final Action foundReward = objective.getRewardFromID(ID);
                            if (foundReward == null) {
                                return ArgumentParseResult.failure(new IllegalArgumentException("Reward with the ID '" + ID + "' does not belong to Objective with ID " + objectiveID + " of Quest '" + quest.getQuestName() + "'!"));
                            } else {
                                return ArgumentParseResult.success(ID);
                            }
                        })
                        , ArgumentDescription.of("Reward ID"));
        handleObjectiveEditRewards(editRewardsBuilder);
    }

    public void handleObjectiveEditRewards(final Command.Builder<CommandSender> builder) {
        manager.command(builder.literal("info")
                .meta(CommandMeta.DESCRIPTION, "Shows everything there is to know about this reward.")
                .handler((context) -> {
                    final int ID = context.get("Reward ID");
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }

                    context.getSender().sendMessage(main.parse(
                            "<main>Reward <highlight>" + ID + "</highlight> for Objective with ID <highlight2>" + objectiveID + "</highlight2> of Quest <highlight2>" + quest.getQuestName() + "</highlight2>:"
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
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

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
                            "<success>The reward with the ID <highlight>" + ID + "</highlight> has been removed from the Objective with ID <highlight2>" + objectiveID + "</highlight2> of Quest <highlight2>"
                                    + quest.getQuestName() + "</highlight2>!"
                    ));
                }));

        manager.command(builder.literal("displayname")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current reward Display Name.")
                .handler((context) -> {
                    final int ID = context.get("Reward ID");
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

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

        manager.command(builder.literal("displayname")
                .literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes current reward Display Name.")
                .handler((context) -> {
                    final int ID = context.get("Reward ID");
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final Action foundReward = objective.getRewardFromID(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    foundReward.removeActionName();
                    foundReward.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".rewards." + ID + ".displayName", null);
                    foundReward.getCategory().saveQuestsConfig();
                    context.getSender().sendMessage(main.parse(
                            "<success>Display Name of reward with the ID <highlight>" + ID + "</highlight> has been removed successfully."
                    ));
                }));

        manager.command(builder.literal("displayname")
                .literal("set")
                .argument(MiniMessageSelector.<CommandSender>newBuilder("DisplayName", main).withPlaceholders().build(), ArgumentDescription.of("Reward display name"))
                .meta(CommandMeta.DESCRIPTION, "Sets new reward Display Name. Only rewards with a Display Name will be displayed.")
                .handler((context) -> {
                    final int ID = context.get("Reward ID");
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

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
                    foundReward.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".rewards." + ID + ".displayName", foundReward.getActionName());
                    foundReward.getCategory().saveQuestsConfig();
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
                            "<main>Reward <highlight>" + ID + "</highlight> for Quest <highlight2>" + quest.getQuestName() + "</highlight2>:"
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
                                    + quest.getQuestName() + "</highlight2>!"
                    ));
                }));

        manager.command(builder.literal("displayname")
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

        manager.command(builder.literal("displayname")
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
                    foundReward.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".rewards." + ID + ".displayName", null);
                    foundReward.getCategory().saveQuestsConfig();
                    context.getSender().sendMessage(main.parse(
                            "<success>Display Name of reward with the ID <highlight>" + ID + "</highlight> has been removed successfully."
                    ));
                }));

        manager.command(builder.literal("displayname")
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
                    foundReward.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".rewards." + ID + ".displayName", foundReward.getActionName());
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
                            "<success>All Triggers of Quest <highlight>" + quest.getQuestName() + "</highlight> have been removed!"
                    ));

                }));

        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the triggers this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");


                    context.getSender().sendMessage(main.parse("<highlight>Triggers for Quest <highlight2>" + quest.getQuestName() + "</highlight2>:"));

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
                                return ArgumentParseResult.failure(new IllegalArgumentException("Trigger with the ID '" + ID + "' does not belong to Quest '" + quest.getQuestName() + "'!"));
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
