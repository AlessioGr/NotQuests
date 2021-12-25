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
import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;
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
                .argument(IntegerArgument.<CommandSender>newBuilder("minutes").withMin(-1).withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    completions.add("<cooldown in minutes>");
                    return completions;
                }).build(), ArgumentDescription.of("New accept cooldown in minutes. Set it to -1, to disable the quest accept cooldown."))
                .meta(CommandMeta.DESCRIPTION, "Sets the time players have to wait between accepting quests.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    int cooldownInMinutes = context.get("minutes");
                    if (cooldownInMinutes > 0) {
                        quest.setAcceptCooldown(cooldownInMinutes);
                        context.getSender().sendMessage(main.parse(
                                "<success>Cooldown for Quest <highlight>" + quest.getQuestName() + "</highlight> has been set to <highlight2>"
                                        + cooldownInMinutes + "</highlight2> minutes!"
                        ));
                    } else {
                        quest.setAcceptCooldown(-1);
                        context.getSender().sendMessage(main.parse(
                                "<success>Cooldown for Quest <highlight>" + quest.getQuestName() + "</highlight> has been <highlight2>disabled</highlight2>!"
                        ));
                    }
                }));


        final Command.Builder<CommandSender> armorstandBuilder = editBuilder.literal("armorstands");
        handleArmorStands(armorstandBuilder);


        manager.command(editBuilder.literal("description")
                .argument(StringArrayArgument.of("Description",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter new Quest description>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            if (lastString.startsWith("{")) {
                                completions.addAll(main.getCommandManager().getAdminCommands().placeholders);
                            } else {
                                completions.add("<Enter new Quest description>");
                                completions.add("clear");
                            }
                            return completions;
                        }
                ), ArgumentDescription.of("Quest description"))
                .meta(CommandMeta.DESCRIPTION, "Sets the new description of the Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    final String description = String.join(" ", (String[]) context.get("Description"));

                    if (description.equalsIgnoreCase("clear")) {
                        quest.setQuestDescription("");
                        context.getSender().sendMessage(main.parse("<success>Description successfully removed from quest <highlight>"
                                + quest.getQuestName() + "</highlight>!"
                        ));
                    } else {
                        quest.setQuestDescription(description);
                        context.getSender().sendMessage(main.parse("<success>Description successfully added to quest <highlight>"
                                + quest.getQuestName() + "</highlight>! New description: <highlight2>"
                                + quest.getQuestDescription()
                        ));
                    }
                }));

        manager.command(editBuilder.literal("displayName")
                .argument(StringArrayArgument.of("DisplayName",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter new Quest display name>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            if (lastString.startsWith("{")) {
                                completions.addAll(main.getCommandManager().getAdminCommands().placeholders);
                            } else {
                                completions.add("<Enter new Quest display name>");
                                completions.add("clear");
                            }
                            return completions;
                        }
                ), ArgumentDescription.of("Quest display name"))
                .meta(CommandMeta.DESCRIPTION, "Sets the new display name of the Quest.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    final String displayName = String.join(" ", (String[]) context.get("DisplayName"));

                    if (displayName.equalsIgnoreCase("clear")) {
                        quest.setQuestDisplayName("");
                        context.getSender().sendMessage(main.parse("<success>Display name successfully removed from quest <highlight>"
                                + quest.getQuestName() + "</highlight>!"
                        ));
                    } else {
                        quest.setQuestDisplayName(displayName);
                        context.getSender().sendMessage(main.parse("<success>Display name successfully added to quest <highlight>"
                                + quest.getQuestName() + "</highlight>! New display name: <highlight2>"
                                + quest.getQuestDisplayName()
                        ));
                    }
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
                            takeItem = new ItemStack(Material.valueOf(materialOrHand.material), 1);
                        }
                    }
                    if (glow) {
                        takeItem.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
                        ItemMeta meta = takeItem.getItemMeta();
                        if (meta == null) {
                            meta = Bukkit.getItemFactory().getItemMeta(takeItem.getType());
                            ;
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
                    quest.removeAllNPCs();
                    context.getSender().sendMessage(main.parse("<success>All NPCs of Quest <highlight>" + quest.getQuestName() + "</highlight> have been removed!"));

                }));

        manager.command(builder.literal("list")
                .meta(CommandMeta.DESCRIPTION, "Lists all Citizens NPCs which have this Quest attached.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    context.getSender().sendMessage(main.parse("<highlight>NPCs bound to quest <highlight2>" + quest.getQuestName() + "</highlight2> with Quest showing:"));
                    int counter = 1;
                    for (final NPC npc : quest.getAttachedNPCsWithQuestShowing()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>ID:</man> <highlight2>" + npc.getId()));
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
                    quest.removeAllObjectives();
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
                    int counter = 1;
                    for (Condition condition : objective.getConditions()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + condition.getConditionType() + "</main>"));
                        context.getSender().sendMessage(main.parse("<main>>" + condition.getConditionDescription()));
                        counter += 1;
                    }

                    if (counter == 1) {
                        context.getSender().sendMessage(main.parse("<warn>This objective has no conditions!"));
                    }
                }));
       /* manager.command(builder.literal("dependencies")
                .literal("add")
                .meta(CommandMeta.DESCRIPTION, "Adds an objective as a dependency (needs to be completed before this one)")
                .argument(IntegerArgument.<CommandSender>newBuilder("Depending Objective ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    final context.getSender() context.getSender() = main.adventure().sender(context.getSender());
                                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Depending Objective ID]", "");

                                    ArrayList<String> completions = new ArrayList<>();

                                    final Quest quest = context.get("quest");
                                    for (final Objective objective : quest.getObjectives()) {
                                        if (objective.getObjectiveID() != (int) context.get("Objective ID")) {
                                            completions.add("" + objective.getObjectiveID());
                                        }
                                    }

                                    return completions;
                                }
                        ).withParser((context, lastString) -> {
                            final int ID = context.get("Depending Objective ID");
                            if (ID == (int) context.get("Depending Objective ID")) {
                                return ArgumentParseResult.failure(new IllegalArgumentException("An objective cannot depend on itself!"));
                            }
                            final Quest quest = context.get("quest");
                            final Objective foundObjective = quest.getObjectiveFromID(ID);
                            if (foundObjective == null) {
                                return ArgumentParseResult.failure(new IllegalArgumentException("Objective with the ID '" + ID + "' does not belong to Quest '" + quest.getQuestName() + "'!"));
                            } else {
                                return ArgumentParseResult.success(ID);
                            }
                        })
                        .build(), ArgumentDescription.of("Depending Objective ID"))
                .handler((context) -> {
                    final context.getSender() context.getSender() = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final int dependingObjectiveID = context.get("Depending Objective ID");
                    final Objective dependingObjective = quest.getObjectiveFromID(dependingObjectiveID);
                    assert dependingObjective != null; //Shouldn't be null

                    if (dependingObjective != objective) {
                        objective.addDependantObjective(dependingObjective, true);
                        context.getSender().sendMessage(main.parse(
                                successGradient + "The objective with the ID " + highlightGradient + dependingObjectiveID
                                        + "</gradient> has been added as a dependency to the objective with the ID " + highlight2Gradient + objectiveID
                                        + "</gradient>!</gradient>"
                        ));
                    } else {
                        context.getSender().sendMessage(main.parse(errorGradient + "Error: You cannot set an objective to depend on itself!"));
                    }
                }));
        manager.command(builder.literal("dependencies")
                .literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes an objective as a dependency (needs to be completed before this one)")
                .argument(IntegerArgument.<CommandSender>newBuilder("Depending Objective ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    final context.getSender() context.getSender() = main.adventure().sender(context.getSender());
                                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Depending Objective ID]", "");

                                    ArrayList<String> completions = new ArrayList<>();

                                    final Quest quest = context.get("quest");
                                    final int objectiveID = context.get("Objective ID");
                                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                                    assert objective != null;

                                    for (final Objective dependingObjective : objective.getDependantObjectives()) {
                                        completions.add(dependingObjective.getObjectiveID() + "");
                                    }

                                    return completions;
                                }
                        ).withParser((context, lastString) -> {
                            final Quest quest = context.get("quest");
                            final int objectiveID = context.get("Objective ID");
                            final Objective objective = quest.getObjectiveFromID(objectiveID);
                            assert objective != null;

                            final int ID = context.get("Depending Objective ID");
                            if (ID == (int) context.get("Depending Objective ID")) {
                                return ArgumentParseResult.failure(new IllegalArgumentException("An objective cannot depend on itself!"));
                            }
                            final Objective foundObjective = quest.getObjectiveFromID(ID);
                            if (foundObjective == null) {
                                return ArgumentParseResult.failure(new IllegalArgumentException("Objective with the ID '" + ID + "' does not belong to Quest '" + quest.getQuestName() + "'!"));
                            } else {
                                if (objective.getDependantObjectives().contains(foundObjective)) {
                                    return ArgumentParseResult.success(ID);
                                } else {
                                    return ArgumentParseResult.failure(new IllegalArgumentException("Objective with the ID '" + ID + "' is not a dependant of objective with the ID  '" + objectiveID + "'!"));
                                }

                            }
                        })
                        .build(), ArgumentDescription.of("Depending Objective ID"))
                .handler((context) -> {
                    final context.getSender() context.getSender() = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final int dependingObjectiveID = context.get("Depending Objective ID");
                    final Objective dependingObjective = quest.getObjectiveFromID(dependingObjectiveID);
                    assert dependingObjective != null; //Shouldn't be null

                    objective.removeDependantObjective(dependingObjective, true);
                    context.getSender().sendMessage(main.parse(
                            successGradient + "The objective with the ID " + highlightGradient + dependingObjectiveID
                                    + "</gradient> has been removed as a dependency from the objective with the ID " + highlight2Gradient + objectiveID
                                    + "</gradient>!</gradient>"
                    ));

                }));


        manager.command(builder.literal("dependencies")
                .literal("list")
                .meta(CommandMeta.DESCRIPTION, "Lists all dependencies of this objective.")
                .handler((context) -> {
                    final context.getSender() context.getSender() = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null


                    context.getSender().sendMessage(main.parse(
                            highlightGradient + "Depending objectives of objective with ID " + highlight2Gradient + objectiveID
                                    + "</gradient> " + unimportant + "(What needs to be completed BEFORE this objective can be started)" + unimportantClose + ":</gradient>"
                    ));
                    int counter = 1;
                    for (final Objective dependantObjective : objective.getDependantObjectives()) {
                        context.getSender().sendMessage(main.parse(
                                highlightGradient + counter + ".</gradient> " + mainGradient + " Objective ID: </gradient>"
                                        + highlight2Gradient + dependantObjective.getObjectiveID() + "</gradient>"
                        ));
                        counter++;
                    }
                    if (counter == 1) {
                        context.getSender().sendMessage(main.parse(warningGradient + "No depending objectives found!"));
                    }
                    context.getSender().sendMessage(main.parse(unimportant + "------" + unimportantClose));

                    context.getSender().sendMessage(main.parse(
                            highlightGradient + "Objectives where this objective with ID " + highlight2Gradient + objectiveID
                                    + "</gradient> is a dependant on  " + unimportant + "(What can only be started AFTER this objective is completed)" + unimportantClose + ":</gradient>"
                    ));
                    int counter2 = 1;
                    for (final Objective otherObjective : quest.getObjectives()) {
                        if (otherObjective.getDependantObjectives().contains(objective)) {
                            context.getSender().sendMessage(main.parse(
                                    highlightGradient + counter2 + ".</gradient> " + mainGradient + " Objective ID: </gradient>"
                                            + highlight2Gradient + otherObjective.getObjectiveID() + "</gradient>"
                            ));
                            counter2++;
                        }
                    }
                    if (counter2 == 1) {
                        context.getSender().sendMessage(main.parse(warningGradient + "No objectives where this objective is a dependant of found!"));
                    }

                }));*/


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
                                    + objective.getObjectiveDescription()
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

                    objective.removeObjectiveDescription(true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Description successfully removed from objective with ID <highlight>" + objectiveID + "</highlight>! New description: <highlight2>"
                                    + objective.getObjectiveDescription()
                    ));
                }));

        manager.command(builder.literal("description")
                .literal("set")
                .argument(StringArrayArgument.of("Objective Description",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter new Objective description>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            if (lastString.startsWith("{")) {
                                completions.addAll(main.getCommandManager().getAdminCommands().placeholders);
                            } else {
                                completions.add("<Enter new Objective description>");
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
                    objective.setObjectiveDescription(description, true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Description successfully added to objective with ID <highlight>" + objectiveID + "</highlight>! New description: <highlight2>"
                                    + objective.getObjectiveDescription()
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
                                    + objective.getObjectiveDisplayName()
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

                    objective.removeObjectiveDisplayName(true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Displayname successfully removed from objective with ID <highlight>" + objectiveID + "</highlight>! New displayname: <highlight2>"
                                    + objective.getObjectiveDescription()
                    ));
                }));

        manager.command(builder.literal("displayname")
                .literal("set")
                .argument(StringArrayArgument.of("Objective Displayname",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter new Objective displayname>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            if (lastString.startsWith("{")) {
                                completions.addAll(main.getCommandManager().getAdminCommands().placeholders);
                            } else {
                                completions.add("<Enter new Objective displayname>");
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
                    objective.setObjectiveDisplayName(description, true);
                    context.getSender().sendMessage(main.parse(
                            "<main>Displayname successfully added to objective with ID <highlight>" + objectiveID + "</highlight>! New displayname: <highlight2>"
                                    + objective.getObjectiveDisplayName()
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
                            "<highlight>Objective DisplayName: <main>" + objective.getObjectiveDisplayName()
                    ));
                    context.getSender().sendMessage(main.parse(
                            "<highlight>Objective Description: <main>" + objective.getObjectiveDescription()
                    ));

                    context.getSender().sendMessage(main.parse(
                            "<highlight>Objective Conditions:"
                    ));
                    int counter = 1;
                    for (final Condition condition : objective.getConditions()) {
                        context.getSender().sendMessage(main.parse(
                                "    <highlight>" + counter + ". Description: " + condition.getConditionDescription()
                        ));
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
    }


    public void handleRequirements(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each requirement


        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the requirements this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.getSender().sendMessage(main.parse("<highlight>Requirements for Quest <highlight2>" + quest.getQuestName() + "</highlight2>:"));
                    int counter = 1;
                    for (Condition condition : quest.getRequirements()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + condition.getConditionType()));
                        context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription()));
                        counter += 1;
                    }
                }));

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Clears all the requirements this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    quest.removeAllRequirements();
                    context.getSender().sendMessage(main.parse("<main>All requirements of Quest <highlight>" + quest.getQuestName() + "</highlight> have been removed!"));
                }));

    }

    public void handleRewards(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each reward

        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the rewards this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    context.getSender().sendMessage(main.parse("<highlight>Rewards for Quest <highlight2>" + quest.getQuestName() + "</highlight2>:"));
                    int counter = 1;
                    for (final Action action : quest.getRewards()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + action.getActionType()));
                        context.getSender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + action.getActionDescription()));
                        counter++;
                    }

                }));

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Clears all the rewards this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    quest.removeAllRewards();
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
                                        completions.add("" + (quest.getRewards().indexOf(action) + 1));
                                    }

                                    return completions;
                                }
                        ).withParser((context, lastString) -> { //TODO: Fix this parser. It isn't run at all.
                            final int ID = context.get("Reward ID");
                            final Quest quest = context.get("quest");
                            final Action foundReward = quest.getRewards().get(ID - 1);
                            if (foundReward == null) {
                                return ArgumentParseResult.failure(new IllegalArgumentException("Reward with the ID '" + ID + "' does not belong to Quest '" + quest.getQuestName() + "'!"));
                            } else {
                                return ArgumentParseResult.success(ID);
                            }
                        })
                        , ArgumentDescription.of("Reward ID"));
        handleEditRewards(editRewardsBuilder);


    }

    public void handleEditRewards(final Command.Builder<CommandSender> builder) {
        manager.command(builder.literal("info")
                .meta(CommandMeta.DESCRIPTION, "Shows everything there is to know about this reward.")
                .handler((context) -> {
                    final int ID = (int) context.get("Reward ID") - 1;
                    final Quest quest = context.get("quest");
                    final Action foundReward = quest.getRewards().get(ID);
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
                    context.getSender().sendMessage(main.parse(
                            "<unimportant>--</unimportant> <main>" + foundReward.getActionDescription()
                    ));

                }));

        manager.command(builder.literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes the reward from the Quest.")
                .handler((context) -> {
                    final int ID = (int) context.get("Reward ID") - 1;
                    final Quest quest = context.get("quest");
                    final Action foundReward = quest.getRewards().get(ID);
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
                    final int ID = (int) context.get("Reward ID") - 1;
                    final Quest quest = context.get("quest");
                    final Action foundReward = quest.getRewards().get(ID);
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
                    final int ID = (int) context.get("Reward ID") - 1;
                    final Quest quest = context.get("quest");
                    final Action foundReward = quest.getRewards().get(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }
                    foundReward.removeActionName();
                    main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".rewards." + (ID + 1) + ".displayName", null);
                    context.getSender().sendMessage(main.parse(
                            "<success>Display Name of reward with the ID <highlight>" + ID + "</highlight> has been removed successfully."
                    ));
                }));

        manager.command(builder.literal("displayname")
                .literal("set")
                .argument(StringArrayArgument.of("DisplayName",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter new Reward display name>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            if (lastString.startsWith("{")) {
                                completions.addAll(main.getCommandManager().getAdminCommands().placeholders);
                            } else {
                                completions.add("<Enter new Reward display name>");
                            }
                            return completions;
                        }
                ), ArgumentDescription.of("Reward display name"))
                .meta(CommandMeta.DESCRIPTION, "Sets new reward Display Name. Only rewards with a Display Name will be displayed.")
                .handler((context) -> {
                    final int ID = (int) context.get("Reward ID") - 1;
                    final Quest quest = context.get("quest");
                    final Action foundReward = quest.getRewards().get(ID);
                    context.getSender().sendMessage(Component.empty());
                    if (foundReward == null) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Invalid reward."
                        ));
                        return;
                    }

                    final String displayName = String.join(" ", (String[]) context.get("DisplayName"));


                    foundReward.setActionName(displayName);
                    main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".rewards." + (ID + 1) + ".displayName", foundReward.getActionName());
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

                    quest.removeAllTriggers();
                    context.getSender().sendMessage(main.parse(
                            "<success>All Triggers of Quest <highlight>" + quest.getQuestName() + "</highlight> have been removed!"
                    ));

                }));

        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the triggers this Quest has.")
                .handler((context) -> {
                    final Quest quest = context.get("quest");


                    context.getSender().sendMessage(main.parse("<highlight>Triggers for Quest <highlight2>" + quest.getQuestName() + "</highlight2>:"));

                    int counter = 1;
                    for (Trigger trigger : quest.getTriggers()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> Type: <main>" + trigger.getTriggerType()));


                        final String triggerDescription = trigger.getTriggerDescription();
                        if (triggerDescription != null && !triggerDescription.isBlank()) {
                            context.getSender().sendMessage(main.parse("<unimportant>--</unimportant> <main>" + triggerDescription));
                        }

                        context.getSender().sendMessage(main.parse("<unimportant>--- Action Name:</unimportant> <main>" + trigger.getTriggerAction().getActionName()));
                        context.getSender().sendMessage(main.parse("<unimportant>------ Description:</unimportant> <main>" + trigger.getTriggerAction().getActionDescription()));
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

                        counter++;
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

                    context.getSender().sendMessage(main.parse(
                            quest.removeTrigger(triggerID)
                    ));

                }));

    }

}
