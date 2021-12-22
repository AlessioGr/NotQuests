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
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.Commands.newCMDs.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.Commands.newCMDs.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Actions.Action;
import rocks.gravili.notquests.Structs.Conditions.Condition;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.Triggers.Trigger;

import java.util.ArrayList;
import java.util.List;

import static rocks.gravili.notquests.Commands.NotQuestColors.*;

public class AdminEditCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    protected final MiniMessage miniMessage = MiniMessage.miniMessage();
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

        if (main.getIntegrationsManager().isCitizensEnabled()) {
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

                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of item displayed in the Quest take GUI."))
                .flag(
                        manager.flagBuilder("glow")
                                .withDescription(ArgumentDescription.of("Makes the item have the enchanted glow."))
                )
                .meta(CommandMeta.DESCRIPTION, "Sets the item displayed in the Quest take GUI (default: book).")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final boolean glow = context.flags().isPresent("glow");

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack takeItem;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            takeItem = player.getInventory().getItemInMainHand();
                        } else {
                            audience.sendMessage(MiniMessage.miniMessage().parse(
                                    NotQuestColors.errorGradient + "This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        takeItem = new ItemStack(materialOrHand.material, 1);
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
                    audience.sendMessage(miniMessage.parse(
                            successGradient + "Take Item Material for Quest " + highlightGradient + quest.getQuestName()
                                    + "</gradient> has been set to " + highlight2Gradient + takeItem.getType().name() + "</gradient>!</gradient>"
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
                            successGradient + "All objectives of Quest " + highlightGradient + quest.getQuestName()
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


        handleEditObjectives(main.getCommandManager().getEditObjectivesBuilder());


    }

    public void handleEditObjectives(final Command.Builder<CommandSender> builder) {
        manager.command(builder.literal("completionNPC")
                .literal("show", "view")
                .meta(CommandMeta.DESCRIPTION, "Shows the completionNPC of an objective.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "The completionNPCID of the objective with the ID " + highlightGradient + objectiveID + "</gradient> is "
                                    + highlight2Gradient + objective.getCompletionNPCID() + "</gradient>!</gradient>"
                    ));
                    if (objective.getCompletionArmorStandUUID() != null) {
                        audience.sendMessage(miniMessage.parse(
                                mainGradient + "The completionNPCUUID (for armor stands) of the objective with the ID " + highlightGradient + objectiveID + "</gradient> is "
                                        + highlight2Gradient + objective.getCompletionArmorStandUUID() + "</gradient>!</gradient>"
                        ));
                    } else {
                        audience.sendMessage(miniMessage.parse(
                                mainGradient + "The completionNPCUUID (for armor stands) of the objective with the ID " + highlightGradient + objectiveID + "</gradient> is "
                                        + highlight2Gradient + "null</gradient>!</gradient>"
                        ));
                    }
                }));
        manager.command(builder.literal("completionNPC")
                .literal("set")
                .argument(StringArgument.<CommandSender>newBuilder("Completion NPC").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Completion NPC ID / 'armorstand']", "");

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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final String completionNPC = context.get("Completion NPC");

                    if (completionNPC.equalsIgnoreCase("-1")) {

                        objective.setCompletionNPCID(-1, true);
                        objective.setCompletionArmorStandUUID(null, true);
                        audience.sendMessage(miniMessage.parse(
                                successGradient + "The completionNPC of the objective with the ID " + highlightGradient + objectiveID + "</gradient> has been removed!</gradient>"
                        ));

                    } else if (!completionNPC.equalsIgnoreCase("armorstand")) {
                        final int completionNPCID = Integer.parseInt(completionNPC);

                        objective.setCompletionNPCID(completionNPCID, true);
                        audience.sendMessage(miniMessage.parse(
                                successGradient + "The completionNPC of the objective with the ID " + highlightGradient + objectiveID
                                        + "</gradient> has been set to the NPC with the ID " + highlight2Gradient + completionNPCID + "</gradient>!</gradient>"
                        ));

                    } else { //Armor Stands

                        if (context.getSender() instanceof final Player player) {
                            ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                            //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                            NamespacedKey key = new NamespacedKey(main, "notquests-item");
                            NamespacedKey QuestNameKey = new NamespacedKey(main, "notquests-questname");
                            NamespacedKey ObjectiveIDKey = new NamespacedKey(main, "notquests-objectiveid");

                            ItemMeta itemMeta = itemStack.getItemMeta();
                            //Only paper List<Component> lore = new ArrayList<>();
                            List<String> lore = new ArrayList<>();

                            assert itemMeta != null;

                            itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());
                            itemMeta.getPersistentDataContainer().set(ObjectiveIDKey, PersistentDataType.INTEGER, objectiveID);
                            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 6);


                            //Only paper itemMeta.displayName(Component.text("§dCheck Armor Stand", NamedTextColor.LIGHT_PURPLE));
                            itemMeta.setDisplayName("§dSet completionNPC of Quest §b" + quest.getQuestName() + " §dto this Armor Stand");
                            //Only paper lore.add(Component.text("§fRight-click an Armor Stand to see which Quests are attached to it."));
                            lore.add("§fRight-click an Armor Stand to set it as the completionNPC of Quest §b" + quest.getQuestName() + " §fand ObjectiveID §b" + objectiveID + "§f.");

                            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                            //Only paper itemMeta.lore(lore);

                            itemMeta.setLore(lore);
                            itemStack.setItemMeta(itemMeta);

                            player.getInventory().addItem(itemStack);

                            audience.sendMessage(miniMessage.parse(
                                    successGradient + "You have been given an item with which you can add the completionNPC of this Objective to an armor stand. Check your inventory!"
                            ));

                        } else {
                            audience.sendMessage(miniMessage.parse(errorGradient + "Error: this command can only be run as a player."));
                        }


                    }
                }));


        manager.command(builder.literal("conditions")
                .literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Removes all conditions from this objective.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    objective.clearConditions();
                    audience.sendMessage(miniMessage.parse(
                            successGradient + "All conditions of objective with ID " + highlightGradient + objectiveID
                                    + "</gradient> have been removed!</gradient>"
                    ));

                }));

        manager.command(builder.literal("conditions")
                .literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all conditions of this objective.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null


                    audience.sendMessage(miniMessage.parse(
                            highlightGradient + "Conditions of objective with ID " + highlight2Gradient + objectiveID
                                    + "</gradient>:</gradient>"
                    ));
                    int counter = 1;
                    for (Condition condition : objective.getConditions()) {
                        audience.sendMessage(miniMessage.parse(highlightGradient + counter + ". </gradient>" + mainGradient + condition.getConditionType() + "</gradient>"));
                        audience.sendMessage(miniMessage.parse(mainGradient + condition.getConditionDescription()));
                        counter += 1;
                    }

                    if (counter == 1) {
                        audience.sendMessage(miniMessage.parse(warningGradient + "This objective has no conditions!"));
                    }
                }));
       /* manager.command(builder.literal("dependencies")
                .literal("add")
                .meta(CommandMeta.DESCRIPTION, "Adds an objective as a dependency (needs to be completed before this one)")
                .argument(IntegerArgument.<CommandSender>newBuilder("Depending Objective ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    final Audience audience = main.adventure().sender(context.getSender());
                                    main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Depending Objective ID]", "");

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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final int dependingObjectiveID = context.get("Depending Objective ID");
                    final Objective dependingObjective = quest.getObjectiveFromID(dependingObjectiveID);
                    assert dependingObjective != null; //Shouldn't be null

                    if (dependingObjective != objective) {
                        objective.addDependantObjective(dependingObjective, true);
                        audience.sendMessage(miniMessage.parse(
                                successGradient + "The objective with the ID " + highlightGradient + dependingObjectiveID
                                        + "</gradient> has been added as a dependency to the objective with the ID " + highlight2Gradient + objectiveID
                                        + "</gradient>!</gradient>"
                        ));
                    } else {
                        audience.sendMessage(miniMessage.parse(errorGradient + "Error: You cannot set an objective to depend on itself!"));
                    }
                }));
        manager.command(builder.literal("dependencies")
                .literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes an objective as a dependency (needs to be completed before this one)")
                .argument(IntegerArgument.<CommandSender>newBuilder("Depending Objective ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    final Audience audience = main.adventure().sender(context.getSender());
                                    main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Depending Objective ID]", "");

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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final int dependingObjectiveID = context.get("Depending Objective ID");
                    final Objective dependingObjective = quest.getObjectiveFromID(dependingObjectiveID);
                    assert dependingObjective != null; //Shouldn't be null

                    objective.removeDependantObjective(dependingObjective, true);
                    audience.sendMessage(miniMessage.parse(
                            successGradient + "The objective with the ID " + highlightGradient + dependingObjectiveID
                                    + "</gradient> has been removed as a dependency from the objective with the ID " + highlight2Gradient + objectiveID
                                    + "</gradient>!</gradient>"
                    ));

                }));


        manager.command(builder.literal("dependencies")
                .literal("list")
                .meta(CommandMeta.DESCRIPTION, "Lists all dependencies of this objective.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null


                    audience.sendMessage(miniMessage.parse(
                            highlightGradient + "Depending objectives of objective with ID " + highlight2Gradient + objectiveID
                                    + "</gradient> " + unimportant + "(What needs to be completed BEFORE this objective can be started)" + unimportantClose + ":</gradient>"
                    ));
                    int counter = 1;
                    for (final Objective dependantObjective : objective.getDependantObjectives()) {
                        audience.sendMessage(miniMessage.parse(
                                highlightGradient + counter + ".</gradient> " + mainGradient + " Objective ID: </gradient>"
                                        + highlight2Gradient + dependantObjective.getObjectiveID() + "</gradient>"
                        ));
                        counter++;
                    }
                    if (counter == 1) {
                        audience.sendMessage(miniMessage.parse(warningGradient + "No depending objectives found!"));
                    }
                    audience.sendMessage(miniMessage.parse(unimportant + "------" + unimportantClose));

                    audience.sendMessage(miniMessage.parse(
                            highlightGradient + "Objectives where this objective with ID " + highlight2Gradient + objectiveID
                                    + "</gradient> is a dependant on  " + unimportant + "(What can only be started AFTER this objective is completed)" + unimportantClose + ":</gradient>"
                    ));
                    int counter2 = 1;
                    for (final Objective otherObjective : quest.getObjectives()) {
                        if (otherObjective.getDependantObjectives().contains(objective)) {
                            audience.sendMessage(miniMessage.parse(
                                    highlightGradient + counter2 + ".</gradient> " + mainGradient + " Objective ID: </gradient>"
                                            + highlight2Gradient + otherObjective.getObjectiveID() + "</gradient>"
                            ));
                            counter2++;
                        }
                    }
                    if (counter2 == 1) {
                        audience.sendMessage(miniMessage.parse(warningGradient + "No objectives where this objective is a dependant of found!"));
                    }

                }));*/


        manager.command(builder.literal("description")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current objective description.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Current description of objective with ID " + highlightGradient + objectiveID + "</gradient>: "
                                    + highlight2Gradient + objective.getObjectiveDescription() + "</gradient></gradient>"
                    ));
                }));
        manager.command(builder.literal("description")
                .literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes current objective description.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    objective.removeObjectiveDescription(true);
                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Description successfully removed from objective with ID " + highlightGradient + objectiveID + "</gradient>! New description: "
                                    + highlight2Gradient + objective.getObjectiveDescription() + "</gradient></gradient>"
                    ));
                }));

        manager.command(builder.literal("description")
                .literal("set")
                .argument(StringArrayArgument.of("Objective Description",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Enter new Objective description>", "");
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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final String description = String.join(" ", (String[]) context.get("Objective Description"));
                    objective.setObjectiveDescription(description, true);
                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Description successfully added to objective with ID " + highlightGradient + objectiveID + "</gradient>! New description: "
                                    + highlight2Gradient + objective.getObjectiveDescription() + "</gradient></gradient>"
                    ));
                }));


        manager.command(builder.literal("displayname")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current objective displayname.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Current displayname of objective with ID " + highlightGradient + objectiveID + "</gradient>: "
                                    + highlight2Gradient + objective.getObjectiveDisplayName() + "</gradient></gradient>"
                    ));
                }));
        manager.command(builder.literal("displayname")
                .literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes current objective displayname.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    objective.removeObjectiveDisplayName(true);
                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Displayname successfully removed from objective with ID " + highlightGradient + objectiveID + "</gradient>! New displayname: "
                                    + highlight2Gradient + objective.getObjectiveDescription() + "</gradient></gradient>"
                    ));
                }));

        manager.command(builder.literal("displayname")
                .literal("set")
                .argument(StringArrayArgument.of("Objective Displayname",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Enter new Objective displayname>", "");
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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    final String description = String.join(" ", (String[]) context.get("Objective Displayname"));
                    objective.setObjectiveDisplayName(description, true);
                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Displayname successfully added to objective with ID " + highlightGradient + objectiveID + "</gradient>! New displayname: "
                                    + highlight2Gradient + objective.getObjectiveDisplayName() + "</gradient></gradient>"
                    ));
                }));


        manager.command(builder.literal("info")
                .meta(CommandMeta.DESCRIPTION, "Shows everything there is to know about this objective.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(
                            highlightGradient + "Information of objective with the ID " + highlight2Gradient + objectiveID
                                    + "</gradient> from Quest " + highlight2Gradient + quest.getQuestName() + "</gradient>:</gradient>"
                    ));
                    audience.sendMessage(miniMessage.parse(
                            highlightGradient + "Objective Type: " + mainGradient + main.getObjectiveManager().getObjectiveType(objective.getClass()) + "</gradient></gradient>"
                    ));
                    audience.sendMessage(miniMessage.parse(
                            highlightGradient + "Objective Content:</gradient>"
                    ));

                    audience.sendMessage(miniMessage.parse(main.getQuestManager().getObjectiveTaskDescription(objective, false, null)));

                    audience.sendMessage(miniMessage.parse(
                            highlightGradient + "Objective DisplayName: " + mainGradient + objective.getObjectiveDisplayName() + "</gradient></gradient>"
                    ));
                    audience.sendMessage(miniMessage.parse(
                            highlightGradient + "Objective Description: " + mainGradient + objective.getObjectiveDescription() + "</gradient></gradient>"
                    ));

                    audience.sendMessage(miniMessage.parse(
                            highlightGradient + "Objective Conditions:</gradient>"
                    ));
                    int counter = 1;
                    for (final Condition condition : objective.getConditions()) {
                        audience.sendMessage(miniMessage.parse(
                                highlightGradient + "    " + counter + ". Description: " + condition.getConditionDescription()
                        ));
                        counter++;
                    }
                }));

        manager.command(builder.literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes the objective from the Quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    quest.removeObjective(objective);
                    audience.sendMessage(miniMessage.parse(
                            successGradient + "Objective with the ID " + highlightGradient + objectiveID + "</gradient> has been successfully removed from Quest "
                                    + highlight2Gradient + quest.getQuestName() + "</gradient>!</gradient>"
                    ));
                }));
    }


    public void handleRequirements(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each requirement


        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the requirements this Quest has.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    audience.sendMessage(miniMessage.parse(highlightGradient + "Requirements for Quest " + highlight2Gradient + quest.getQuestName() + "</gradient>:</gradient>"));
                    int counter = 1;
                    for (Condition condition : quest.getRequirements()) {
                        audience.sendMessage(miniMessage.parse(highlightGradient + counter + ". </gradient>" + mainGradient + condition.getConditionType() + "</gradient>"));
                        audience.sendMessage(miniMessage.parse(mainGradient + condition.getConditionDescription()));
                        counter += 1;
                    }
                }));

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Clears all the requirements this Quest has.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    quest.removeAllRequirements();
                    audience.sendMessage(miniMessage.parse(successGradient + "All requirements of Quest " + highlightGradient + quest.getQuestName() + "</gradient> have been removed!</gradient>"));
                }));

    }

    public void handleRewards(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each reward

        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the rewards this Quest has.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    audience.sendMessage(miniMessage.parse(highlightGradient + "Rewards for Quest " + highlight2Gradient + quest.getQuestName() + "</gradient>:</gradient>"));
                    int counter = 1;
                    for (final Action action : quest.getRewards()) {
                        audience.sendMessage(miniMessage.parse(highlightGradient + counter + ". </gradient>" + mainGradient + action.getActionType() + "</gradient>"));
                        audience.sendMessage(miniMessage.parse(unimportant + "-- " + unimportantClose + mainGradient + action.getActionDescription() + "</gradient>"));
                        counter++;
                    }

                }));

        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Clears all the rewards this Quest has.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    quest.removeAllRewards();
                    audience.sendMessage(miniMessage.parse(successGradient + "All rewards of Quest " + highlightGradient + quest.getQuestName() + "</gradient> have been removed!</gradient>"));
                }));


        final Command.Builder<CommandSender> editRewardsBuilder = builder.literal("edit")
                .argument(IntegerArgument.<CommandSender>newBuilder("Reward ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    final Audience audience = main.adventure().sender(context.getSender());
                                    main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Reward ID]", "[...]");

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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final int ID = (int) context.get("Reward ID") - 1;
                    final Quest quest = context.get("quest");
                    final Action foundReward = quest.getRewards().get(ID);
                    audience.sendMessage(Component.empty());
                    if (foundReward == null) {
                        audience.sendMessage(miniMessage.parse(
                                errorGradient + "Invalid reward."
                        ));
                        return;
                    }

                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Reward " + highlightGradient + ID + "</gradient> for Quest " + highlight2Gradient + quest.getQuestName() + "</gradient>:</gradient>"
                    ));
                    audience.sendMessage(miniMessage.parse(
                            unimportant + "-- " + unimportantClose + mainGradient + foundReward.getActionDescription() + "</gradient>"
                    ));

                }));

        manager.command(builder.literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes the reward from the Quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final int ID = (int) context.get("Reward ID") - 1;
                    final Quest quest = context.get("quest");
                    final Action foundReward = quest.getRewards().get(ID);
                    audience.sendMessage(Component.empty());
                    if (foundReward == null) {
                        audience.sendMessage(miniMessage.parse(
                                errorGradient + "Invalid reward."
                        ));
                        return;
                    }
                    quest.removeReward(foundReward);
                    audience.sendMessage(miniMessage.parse(
                            successGradient + "The reward with the ID " + highlightGradient + ID + "</gradient> has been removed from the Quest "
                                    + highlight2Gradient + quest.getQuestName() + "</gradient>!</gradient>"
                    ));
                }));

        manager.command(builder.literal("displayname")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current reward Display Name.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final int ID = (int) context.get("Reward ID") - 1;
                    final Quest quest = context.get("quest");
                    final Action foundReward = quest.getRewards().get(ID);
                    audience.sendMessage(Component.empty());
                    if (foundReward == null) {
                        audience.sendMessage(miniMessage.parse(
                                errorGradient + "Invalid reward."
                        ));
                        return;
                    }
                    if (foundReward.getActionName().isBlank()) {
                        audience.sendMessage(miniMessage.parse(
                                mainGradient + "This reward has no display name set."
                        ));
                    } else {
                        audience.sendMessage(miniMessage.parse(
                                mainGradient + "Reward display name: " + highlightGradient + foundReward.getActionName() + "</gradient></gradient>"
                        ));
                    }
                }));

        manager.command(builder.literal("displayname")
                .literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes current reward Display Name.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final int ID = (int) context.get("Reward ID") - 1;
                    final Quest quest = context.get("quest");
                    final Action foundReward = quest.getRewards().get(ID);
                    audience.sendMessage(Component.empty());
                    if (foundReward == null) {
                        audience.sendMessage(miniMessage.parse(
                                errorGradient + "Invalid reward."
                        ));
                        return;
                    }
                    foundReward.removeActionName();
                    main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".rewards." + (ID + 1) + ".displayName", null);
                    audience.sendMessage(miniMessage.parse(
                            successGradient + "Display Name of reward with the ID " + highlightGradient + ID + "</gradient> has been removed successfully.</gradient>"
                    ));
                }));

        manager.command(builder.literal("displayname")
                .literal("set")
                .argument(StringArrayArgument.of("DisplayName",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Enter new Reward display name>", "");
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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final int ID = (int) context.get("Reward ID") - 1;
                    final Quest quest = context.get("quest");
                    final Action foundReward = quest.getRewards().get(ID);
                    audience.sendMessage(Component.empty());
                    if (foundReward == null) {
                        audience.sendMessage(miniMessage.parse(
                                errorGradient + "Invalid reward."
                        ));
                        return;
                    }

                    final String displayName = String.join(" ", (String[]) context.get("DisplayName"));


                    foundReward.setActionName(displayName);
                    main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".rewards." + (ID + 1) + ".displayName", foundReward.getActionName());
                    audience.sendMessage(miniMessage.parse(
                            successGradient + "Display Name successfully added to reward with ID " + highlightGradient + ID + "</gradient>! New display name: "
                                    + highlight2Gradient + foundReward.getActionName() + "</gradient></gradient>"
                    ));
                }));
    }

    public void handleTriggers(final Command.Builder<CommandSender> builder) {
        //Add is handled individually by each trigger
        manager.command(builder.literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Removes all the triggers this Quest has.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    quest.removeAllTriggers();
                    audience.sendMessage(miniMessage.parse(
                            successGradient + "All Triggers of Quest " + highlightGradient + quest.getQuestName() + "</gradient> have been removed!</gradient>"
                    ));

                }));

        manager.command(builder.literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all the triggers this Quest has.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");


                    audience.sendMessage(miniMessage.parse(highlightGradient + "Triggers for Quest " + highlight2Gradient + quest.getQuestName() + "</gradient>:</gradient>"));

                    int counter = 1;
                    for (Trigger trigger : quest.getTriggers()) {
                        audience.sendMessage(miniMessage.parse(highlightGradient + counter + ". </gradient> Type: " + mainGradient + trigger.getTriggerType() + "</gradient>"));


                        final String triggerDescription = trigger.getTriggerDescription();
                        if (triggerDescription != null && !triggerDescription.isBlank()) {
                            audience.sendMessage(miniMessage.parse(unimportant + "-- " + unimportantClose + mainGradient + triggerDescription + "</gradient>"));
                        }

                        audience.sendMessage(miniMessage.parse(unimportant + "--- Action Name: " + unimportantClose + mainGradient + trigger.getTriggerAction().getActionName() + "</gradient>"));
                        audience.sendMessage(miniMessage.parse(unimportant + "------ Description: " + unimportantClose + mainGradient + trigger.getTriggerAction().getActionDescription() + "</gradient>"));
                        audience.sendMessage(miniMessage.parse(unimportant + "--- Amount of triggers needed for first execution: " + unimportantClose + mainGradient + trigger.getAmountNeeded() + "</gradient>"));

                        if (trigger.getApplyOn() == 0) {
                            audience.sendMessage(miniMessage.parse(unimportant + "--- Apply on: " + unimportantClose + mainGradient + "Quest</gradient>"));

                        } else {
                            audience.sendMessage(miniMessage.parse(unimportant + "--- Apply on: " + unimportantClose + mainGradient + "Objective " + trigger.getApplyOn() + "</gradient>"));
                        }

                        if (trigger.getWorldName() == null || trigger.getWorldName().isBlank() || trigger.getWorldName().equalsIgnoreCase("ALL")) {
                            audience.sendMessage(miniMessage.parse(unimportant + "--- In World: " + unimportantClose + mainGradient + "Any World</gradient>"));
                        } else {
                            audience.sendMessage(miniMessage.parse(unimportant + "--- In World: " + unimportantClose + mainGradient + trigger.getWorldName() + "</gradient>"));
                        }

                        counter++;
                    }

                }));


        manager.command(builder.literal("remove", "delete")
                .argument(IntegerArgument.<CommandSender>newBuilder("Trigger ID").withMin(1).withSuggestionsProvider(
                                (context, lastString) -> {
                                    final List<String> allArgs = context.getRawInput();
                                    final Audience audience = main.adventure().sender(context.getSender());
                                    main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Trigger ID]", "");

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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    final int triggerID = context.get("Trigger ID");

                    audience.sendMessage(miniMessage.parse(
                            quest.removeTrigger(triggerID)
                    ));

                }));

    }

}
