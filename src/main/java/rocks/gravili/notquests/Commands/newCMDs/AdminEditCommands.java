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

package rocks.gravili.notquests.Commands.newCMDs;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.bukkit.parsers.MaterialArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;

import java.util.ArrayList;
import java.util.List;

import static rocks.gravili.notquests.Commands.NotQuestColors.*;

public class AdminEditCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    protected final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Command.Builder<CommandSender> editBuilder;

    //TODO: /qa2 edit ... objectives | requirements | rewards | triggers

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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    int cooldownInMinutes = context.get("minutes");
                    if (cooldownInMinutes > 0) {
                        quest.setAcceptCooldown(cooldownInMinutes);
                        audience.sendMessage(miniMessage.parse(
                                successGradient + "Cooldown for Quest " + highlightGradient + quest.getQuestName() + "</gradient> has been set to "
                                        + highlight2Gradient + cooldownInMinutes + "</gradient> minutes!"
                        ));
                    } else {
                        quest.setAcceptCooldown(-1);
                        audience.sendMessage(miniMessage.parse(
                                successGradient + "Cooldown for Quest " + highlightGradient + quest.getQuestName() + "</gradient> has been "
                                        + highlight2Gradient + "disabled</gradient>!"
                        ));
                    }
                }));


        final Command.Builder<CommandSender> armorstandBuilder = editBuilder.literal("armorstands");
        handleArmorStands(armorstandBuilder);


        manager.command(editBuilder.literal("description")
                .argument(StringArrayArgument.of("Description",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Enter new Quest description>", "");
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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    final String description = String.join(" ", (String[]) context.get("Description"));

                    if (description.equalsIgnoreCase("clear")) {
                        quest.setQuestDescription("");
                        audience.sendMessage(miniMessage.parse(successGradient + "Description successfully removed from quest "
                                + highlightGradient + quest.getQuestName() + "</gradient>!"
                        ));
                    } else {
                        quest.setQuestDescription(description);
                        audience.sendMessage(miniMessage.parse(successGradient + "Description successfully added to quest "
                                + highlightGradient + quest.getQuestName() + "</gradient>! New description: "
                                + highlight2Gradient + quest.getQuestDescription()
                        ));
                    }
                }));

        manager.command(editBuilder.literal("displayName")
                .argument(StringArrayArgument.of("DisplayName",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Enter new Quest display name>", "");
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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    final String displayName = String.join(" ", (String[]) context.get("DisplayName"));

                    if (displayName.equalsIgnoreCase("clear")) {
                        quest.setQuestDisplayName("");
                        audience.sendMessage(miniMessage.parse(successGradient + "Display name successfully removed from quest "
                                + highlightGradient + quest.getQuestName() + "</gradient>!"
                        ));
                    } else {
                        quest.setQuestDisplayName(displayName);
                        audience.sendMessage(miniMessage.parse(successGradient + "Display name successfully added to quest "
                                + highlightGradient + quest.getQuestName() + "</gradient>! New display name: "
                                + highlight2Gradient + quest.getQuestDisplayName()
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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    int maxAccepts = context.get("max. accepts");
                    if (maxAccepts > 0) {
                        quest.setMaxAccepts(maxAccepts);
                        audience.sendMessage(miniMessage.parse(
                                successGradient + "Maximum amount of accepts for Quest " + highlightGradient + quest.getQuestName() + "</gradient> has been set to "
                                        + highlight2Gradient + maxAccepts + "</gradient>!"
                        ));
                    } else {
                        quest.setMaxAccepts(-1);
                        audience.sendMessage(miniMessage.parse(
                                successGradient + "Maximum amount of accepts for Quest " + highlightGradient + quest.getQuestName() + "</gradient> has been set to "
                                        + highlight2Gradient + "unlimited (default)</gradient>!"
                        ));
                    }
                }));

        if (main.isCitizensEnabled()) {
            final Command.Builder<CommandSender> citizensNPCsBuilder = editBuilder.literal("npcs");
            handleCitizensNPCs(citizensNPCsBuilder);
        }

        manager.command(editBuilder.literal("takeEnabled")
                .argument(BooleanArgument.<CommandSender>newBuilder("Take Enabled").withLiberal(true).build(),
                        ArgumentDescription.of("Enabled by default. Yes / no"))
                .meta(CommandMeta.DESCRIPTION, "Sets if players can accept the Quest using /notquests take.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    boolean takeEnabled = context.get("Take Enabled");
                    quest.setTakeEnabled(takeEnabled);
                    if (takeEnabled) {
                        audience.sendMessage(miniMessage.parse(
                                successGradient + "Quest taking (/notquests take) for the Quest "
                                        + highlightGradient + quest.getQuestName() + "</gradient> has been set to "
                                        + highlight2Gradient + "enabled</gradient>!</gradient>"
                        ));
                    } else {
                        audience.sendMessage(miniMessage.parse(
                                successGradient + "Quest taking (/notquests take) for the Quest "
                                        + highlightGradient + quest.getQuestName() + "</gradient> has been set to "
                                        + highlight2Gradient + "disabled</gradient>!</gradient>"
                        ));
                    }
                }));

        manager.command(editBuilder.literal("takeItem")
                .argument(MaterialArgument.of("material"), ArgumentDescription.of("Material of item displayed in the Quest take GUI."))
                .meta(CommandMeta.DESCRIPTION, "Sets the item displayed in the Quest take GUI (default: book).")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    final Material takeItem = context.get("material");
                    quest.setTakeItem(takeItem);
                    audience.sendMessage(miniMessage.parse(
                            successGradient + "Take Item Material for Quest " + highlightGradient + quest.getQuestName()
                                    + "</gradient> has been set to " + highlight2Gradient + takeItem.name() + "</gradient>!</gradient>"
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
                    final Audience audience = main.adventure().sender(context.getSender());
                    main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[ID of the NPC you wish to add]", "(optional: --hideInNPC)");

                    return completions;
                }).build(), ArgumentDescription.of("ID of the Citizens NPC to whom the Quest should be attached."))
                .flag(
                        manager.flagBuilder("hideInNPC")
                                .withDescription(ArgumentDescription.of("Makes the Quest hidden from in the NPC."))
                )
                .meta(CommandMeta.DESCRIPTION, "Attaches the Quest to a Citizens NPC.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    boolean showInNPC = !context.flags().isPresent("hideInNPC");

                    int npcID = context.get("npc ID");

                    final NPC npc = CitizensAPI.getNPCRegistry().getById(npcID);
                    if (npc != null) {
                        if (!quest.getAttachedNPCsWithQuestShowing().contains(npc) && !quest.getAttachedNPCsWithoutQuestShowing().contains(npc)) {
                            quest.bindToNPC(npc, showInNPC);
                            audience.sendMessage(miniMessage.parse(
                                    successGradient + "Quest " + highlightGradient + quest.getQuestName()
                                            + "</gradient> has been bound to the NPC with the ID " + highlight2Gradient + npcID
                                            + "</gradient>! Showing Quest: " + highlightGradient + showInNPC + "</gradient>."
                            ));
                        } else {
                            audience.sendMessage(miniMessage.parse(
                                    warningGradient + "Quest " + highlightGradient + quest.getQuestName()
                                            + "</gradient> has already been bound to the NPC with the ID " + highlight2Gradient + npcID
                                            + "</gradient>!"
                            ));
                        }

                    } else {
                        audience.sendMessage(miniMessage.parse(errorGradient + "NPC with the ID " + highlightGradient + npcID + "</gradient> was not found!"));
                    }

                }));

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "De-attaches this Quest from all Citizens NPCs.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    quest.removeAllNPCs();
                    audience.sendMessage(miniMessage.parse(successGradient + "All NPCs of Quest " + highlightGradient + quest.getQuestName() + "</gradient> have been removed!"));

                }));

        manager.command(builder.literal("list")
                .meta(CommandMeta.DESCRIPTION, "Lists all Citizens NPCs which have this Quest attached.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    audience.sendMessage(miniMessage.parse(highlightGradient + "NPCs bound to quest " + highlight2Gradient + quest.getQuestName() + "</gradient> with Quest showing:</gradient>"));
                    int counter = 1;
                    for (final NPC npc : quest.getAttachedNPCsWithQuestShowing()) {
                        audience.sendMessage(miniMessage.parse(highlightGradient + counter + ".</gradient> " + mainGradient + "ID:</gradient> " + highlight2Gradient + npc.getId()));
                        counter++;
                    }
                    counter = 1;
                    audience.sendMessage(miniMessage.parse(highlightGradient + "NPCs bound to quest " + highlight2Gradient + quest.getQuestName() + "</gradient> without Quest showing:</gradient>"));
                    for (NPC npc : quest.getAttachedNPCsWithoutQuestShowing()) {
                        audience.sendMessage(miniMessage.parse(highlightGradient + counter + ".</gradient> " + mainGradient + "ID:</gradient> " + highlight2Gradient + npc.getId()));
                        counter++;
                    }
                }));
    }


    public void handleArmorStands(final Command.Builder<CommandSender> builder) {

        manager.command(builder.literal("check")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Gives you an item with which you check what Quests are attached to an armor stand.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Player player = (Player) context.getSender();

                    ItemStack itemStack = new ItemStack(Material.LEATHER, 1);
                    //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                    NamespacedKey key = new NamespacedKey(main, "notquests-item");

                    ItemMeta itemMeta = itemStack.getItemMeta();
                    //Only paper List<Component> lore = new ArrayList<>();
                    List<String> lore = new ArrayList<>();

                    assert itemMeta != null;


                    itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 4);

                    //Only paper itemMeta.displayName(Component.text("§dCheck Armor Stand", NamedTextColor.LIGHT_PURPLE));
                    itemMeta.setDisplayName("§dCheck Armor Stand");
                    //Only paper lore.add(Component.text("§fRight-click an Armor Stand to see which Quests are attached to it."));
                    lore.add("§fRight-click an Armor Stand to see which Quests are attached to it.");

                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


                    //Only paper itemMeta.lore(lore);

                    itemMeta.setLore(lore);

                    itemStack.setItemMeta(itemMeta);

                    player.getInventory().addItem(itemStack);

                    audience.sendMessage(miniMessage.parse(successGradient + "You have been given an item with which you can check armor stands!"));
                }));


        manager.command(builder.literal("add")
                .senderType(Player.class)
                .flag(
                        manager.flagBuilder("hideInArmorStand")
                                .withDescription(ArgumentDescription.of("Makes the Quest hidden from armor stands"))
                )
                .meta(CommandMeta.DESCRIPTION, "Gives you an item with which you can add the quest to an armor stand.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    boolean showInArmorStand = !context.flags().isPresent("hideInArmorStand");
                    final Player player = (Player) context.getSender();

                    ItemStack itemStack = new ItemStack(Material.GHAST_TEAR, 1);
                    //give a specialitem. clicking an armorstand with that special item will give it the pdb.

                    NamespacedKey key = new NamespacedKey(main, "notquests-item");
                    NamespacedKey QuestNameKey = new NamespacedKey(main, "notquests-questname");
                    ItemMeta itemMeta = itemStack.getItemMeta();

                    if (itemMeta == null) {
                        audience.sendMessage(miniMessage.parse(errorGradient + "Error: ItemMeta is null."));
                        return;
                    }
                    //only paper List<Component> lore = new ArrayList<>();
                    List<String> lore = new ArrayList<>();

                    if (showInArmorStand) {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 0);
                        //only paper itemMeta.displayName(Component.text("§6Add showing Quest §b" + quest.getQuestName() + " §6to Armor Stand", NamedTextColor.GOLD));
                        itemMeta.setDisplayName("§6Add showing Quest §b" + quest.getQuestName() + " §6to Armor Stand");
                        //only paper lore.add(Component.text("§fHit an armor stand to add the showing Quest §b" + quest.getQuestName() + " §fto it."));
                        lore.add("§fHit an armor stand to add the showing Quest §b" + quest.getQuestName() + " §fto it.");

                    } else {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
                        //only paper itemMeta.displayName(Component.text("§6Add non-showing Quest §b" + quest.getQuestName() + " §6to Armor Stand", NamedTextColor.GOLD));
                        itemMeta.setDisplayName("§6Add non-showing Quest §b" + quest.getQuestName() + " §6to Armor Stand");
                        //only paper lore.add(Component.text("§fHit an armor stand to add the non-showing Quest §b" + quest.getQuestName() + " §fto it."));

                        lore.add("§fHit an armor stand to add the non-showing Quest §b" + quest.getQuestName() + " §fto it.");

                    }
                    itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());
                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


                    //only paper itemMeta.lore(lore);
                    itemMeta.setLore(lore);
                    itemStack.setItemMeta(itemMeta);
                    player.getInventory().addItem(itemStack);
                    audience.sendMessage(miniMessage.parse(successGradient + "You have been given an item with which you can add this quest to armor stands!"));
                }));

        manager.command(builder.literal("clear")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "This command is not done yet.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(miniMessage.parse(errorGradient + "Sorry, this command is not done yet! I'll add it in future versions."));
                }));

        manager.command(builder.literal("list")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "This command is not done yet.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(miniMessage.parse(errorGradient + "Sorry, this command is not done yet! I'll add it in future versions."));
                }));


        manager.command(builder.literal("remove")
                .senderType(Player.class)
                .flag(
                        manager.flagBuilder("hideInArmorStand")
                                .withDescription(ArgumentDescription.of("Sets if you want to remove the Quest which is hidden in an armor stand."))
                )
                .meta(CommandMeta.DESCRIPTION, "Gives you an item with which you can remove the quest from an armor stand.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    boolean showInArmorStand = !context.flags().isPresent("hideInArmorStand");
                    final Player player = (Player) context.getSender();

                    ItemStack itemStack = new ItemStack(Material.NETHER_STAR, 1);
                    //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                    NamespacedKey key = new NamespacedKey(main, "notquests-item");
                    NamespacedKey QuestNameKey = new NamespacedKey(main, "notquests-questname");

                    ItemMeta itemMeta = itemStack.getItemMeta();

                    if (itemMeta == null) {
                        audience.sendMessage(miniMessage.parse(errorGradient + "Error: ItemMeta is null."));
                        return;
                    }

                    //only paper List<Component> lore = new ArrayList<>();
                    List<String> lore = new ArrayList<>();

                    if (showInArmorStand) {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 2);
                        //itemMeta.displayName(Component.text("§cRemove showing Quest §b" + quest.getQuestName() + " §cfrom Armor Stand", NamedTextColor.RED));
                        itemMeta.setDisplayName("§cRemove showing Quest §b" + quest.getQuestName() + " §cfrom Armor Stand");

                        //only paper lore.add(Component.text("§fHit an armor stand to remove the showing Quest §b" + quest.getQuestName() + " §ffrom it."));
                        lore.add("§fHit an armor stand to remove the showing Quest §b" + quest.getQuestName() + " §ffrom it.");
                    } else {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 3);
                        //only paper itemMeta.displayName(Component.text("§cRemove non-showing Quest §b" + quest.getQuestName() + " §cfrom Armor Stand", NamedTextColor.RED));
                        itemMeta.setDisplayName("§cRemove non-showing Quest §b" + quest.getQuestName() + " §cfrom Armor Stand");
                        //only paper lore.add(Component.text("§fHit an armor stand to remove the non-showing Quest §b" + quest.getQuestName() + " §ffrom it."));
                        lore.add("§fHit an armor stand to remove the non-showing Quest §b" + quest.getQuestName() + " §ffrom it.");
                    }
                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                    itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());


                    //only paper itemMeta.lore(lore);
                    itemMeta.setLore(lore);

                    itemStack.setItemMeta(itemMeta);

                    player.getInventory().addItem(itemStack);

                    audience.sendMessage(miniMessage.parse(successGradient + "You have been given an item with which you can remove this quest from armor stands!"));
                }));
    }

    public void handleObjectives(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each objective

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Removes all objectives from a Quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    quest.removeAllObjectives();
                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(
                            successGradient + "All objectives of Quest" + highlightGradient + quest.getQuestName()
                                    + "</gradient> have been removed!</gradient>"
                    ));
                }));
        manager.command(builder.literal("list")
                .meta(CommandMeta.DESCRIPTION, "Lists all objectives of a Quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(highlightGradient + "Objectives for Quest " + highlight2Gradient + quest.getQuestName() + "</gradient>:</gradient>"));
                    main.getQuestManager().sendObjectivesAdmin(audience, quest);
                }));

        final Command.Builder<CommandSender> editObjectivesBuilder = builder.literal("edit")
                .argument(IntegerArgument.<CommandSender>newBuilder("Objective ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    final Audience audience = main.adventure().sender(context.getSender());
                                    main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Objective ID]", "[...]");

                                    ArrayList<String> completions = new ArrayList<>();

                                    final Quest quest = context.get("quest");
                                    for (final Objective objective : quest.getObjectives()) {
                                        completions.add("" + objective.getObjectiveID());
                                    }


                                    return completions;
                                }
                        ).withParser((context, lastString) -> {
                            final int ID = context.get("Objective ID");
                            final Quest quest = context.get("quest");
                            final Objective foundObjective = quest.getObjectiveFromID(ID);
                            if (foundObjective == null) {
                                return ArgumentParseResult.failure(new IllegalArgumentException("Objective with the ID '" + ID + "' does not belong to Quest '" + quest.getQuestName() + "'!"));
                            } else {
                                return ArgumentParseResult.success(ID);
                            }
                        })
                        .build(), ArgumentDescription.of("Objective ID"));
        handleEditObjectives(editObjectivesBuilder);


    }

    public void handleEditObjectives(final Command.Builder<CommandSender> builder) {
        manager.command(builder.literal("completionNPC")
                .literal("show", "view")
                .meta(CommandMeta.DESCRIPTION, "Shows the completionNPC of an objective.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final Objective objective = quest.getObjectiveFromID(context.get("Objective ID"));
                    assert objective != null; //Shouldn't be null

                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "The completionNPCID of the objective with the ID " + highlightGradient + objective + "</gradient> is "
                                    + highlight2Gradient + objective.getCompletionNPCID() + "</gradient>!</gradient>"
                    ));
                    if (objective.getCompletionArmorStandUUID() != null) {
                        audience.sendMessage(miniMessage.parse(
                                mainGradient + "The completionNPCUUID (for armor stands)  of the objective with the ID " + highlightGradient + objective + "</gradient> is "
                                        + highlight2Gradient + objective.getCompletionArmorStandUUID() + "</gradient>!</gradient>"
                        ));
                    } else {
                        audience.sendMessage(miniMessage.parse(
                                mainGradient + "The completionNPCUUID (for armor stands)  of the objective with the ID " + highlightGradient + objective + "</gradient> is "
                                        + highlight2Gradient + "null</gradient>!</gradient>"
                        ));
                    }
                }));
    }


    public void handleRequirements(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each requirement

    }

    public void handleRewards(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each reward

    }

    public void handleTriggers(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each trigger

    }

}
