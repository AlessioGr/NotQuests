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

package rocks.gravili.notquests.Commands;

import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.Conversation.ConversationPlayer;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.ActiveObjective;
import rocks.gravili.notquests.Structs.ActiveQuest;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandNotQuests implements CommandExecutor, TabCompleter {
    private final NotQuests main;

    private final List<String> completions = new ArrayList<>();

    private final Component firstLevelCommands;

    public CommandNotQuests(NotQuests main) {
        this.main = main;

        firstLevelCommands = Component.text("NotQuests Player Commands:", NamedTextColor.BLUE, TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("/nquests §6take §3[Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/nquests take ")).hoverEvent(HoverEvent.showText(Component.text("Takes/Starts a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/nquests §6abort §3[Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/nquests abort ")).hoverEvent(HoverEvent.showText(Component.text("Fails a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/nquests §6preview §3[Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/nquests preview ")).hoverEvent(HoverEvent.showText(Component.text("Shows more information about a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/nquests §6activeQuests", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/nquests activeQuests")).hoverEvent(HoverEvent.showText(Component.text("Shows all your active Quests", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/nquests §6progress §3[Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/nquests progress ")).hoverEvent(HoverEvent.showText(Component.text("Shows the progress of an active Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/nquests §6questPoints", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/nquests questPoints")).hoverEvent(HoverEvent.showText(Component.text("Shows how many Quest Points you have", NamedTextColor.GREEN))))
                .append(Component.newline()

                );


    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof final Player player) {
            final boolean guiEnabled = main.getDataManager().getConfiguration().isUserCommandsUseGUI();
            if (sender.hasPermission("notquests.use")) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer((player.getUniqueId()));
                if (args.length == 0) {
                    if (guiEnabled) {
                        String[] guiSetup = {
                                "zxxxxxxxx",
                                "x0123456x",
                                "x789abcdx",
                                "xefghijkx",
                                "xlmnopqrx",
                                "xxxxxxxxx"
                        };
                        InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.main.title", player), guiSetup);
                        gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                        gui.addElement(new StaticGuiElement('8',
                                new ItemStack(Material.CHEST),
                                0,
                                click -> {
                                    player.chat("/notquests take");

                                    return true; // returning true will cancel the click event and stop taking the item
                                },
                                main.getLanguageManager().getString("gui.main.button.takequest.text", player)

                        ));
                        gui.addElement(new StaticGuiElement('a',
                                new ItemStack(Material.REDSTONE_BLOCK),
                                0,
                                click -> {
                                    player.chat("/notquests abort");
                                    return true; // returning true will cancel the click event and stop taking the item
                                },
                                main.getLanguageManager().getString("gui.main.button.abortquest.text", player)
                        ));
                        gui.addElement(new StaticGuiElement('c',
                                new ItemStack(Material.SPYGLASS),
                                0,
                                click -> {
                                    player.chat("/notquests preview");
                                    return true; // returning true will cancel the click event and stop taking the item
                                },
                                main.getLanguageManager().getString("gui.main.button.previewquest.text", player)
                        ));

                        gui.addElement(new StaticGuiElement('o',
                                new ItemStack(Material.LADDER),
                                0,
                                click -> {
                                    player.chat("/notquests activeQuests");
                                    return true; // returning true will cancel the click event and stop taking the item
                                },
                                main.getLanguageManager().getString("gui.main.button.activequests.text", player)
                        ));
                        if (questPlayer != null) {
                            gui.addElement(new StaticGuiElement('z',
                                    new ItemStack(Material.SUNFLOWER),
                                    0,
                                    click -> {
                                        return true; // returning true will cancel the click event and stop taking the item
                                    },
                                    main.getLanguageManager().getString("gui.main.button.questpoints.text", player, questPlayer)
                            ));
                        } else {
                            gui.addElement(new StaticGuiElement('z',
                                    new ItemStack(Material.SUNFLOWER),
                                    0,
                                    click -> {
                                        return true; // returning true will cancel the click event and stop taking the item
                                    },
                                    main.getLanguageManager().getString("gui.main.button.questpoints.text", player).replace("%QUESTPOINTS%", "??")

                            ));
                        }

                        gui.show(player);
                    } else {
                        sender.sendMessage("");
                        main.adventure().sender(sender).sendMessage(firstLevelCommands);
                    }
                } else if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("activequests")) {
                        if (questPlayer != null) {
                            if (guiEnabled) {
                                String[] guiSetup = {
                                        "zxxxxxxxx",
                                        "xgggggggx",
                                        "xgggggggx",
                                        "xgggggggx",
                                        "xgggggggx",
                                        "pxxxxxxxn"
                                };
                                InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.activeQuests.title", player), guiSetup);
                                gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this


                                int count = 0;
                                GuiElementGroup group = new GuiElementGroup('g');

                                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                                    final Material materialToUse;
                                    if (!activeQuest.isCompleted()) {
                                        materialToUse = activeQuest.getQuest().getTakeItem();
                                    } else {
                                        materialToUse = Material.EMERALD_BLOCK;
                                    }

                                    if (main.getDataManager().getConfiguration().showQuestItemAmount) {
                                        count++;
                                    }



                                    group.addElement(new StaticGuiElement('e',
                                            new ItemStack(materialToUse),
                                            count, // Display a number as the item count
                                            click -> {
                                                player.chat("/notquests progress " + activeQuest.getQuest().getQuestName());
                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            main.getLanguageManager().getString("gui.activeQuests.button.activeQuestButton.text", player, activeQuest)


                                    ));

                                }


                                gui.addElement(group);

                                // Previous page
                                gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));

                                // Next page
                                gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));


                                gui.show(player);
                            } else {
                                sender.sendMessage(main.getLanguageManager().getString("chat.active-quests-label", player));
                                int counter = 1;
                                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                    sender.sendMessage("§a" + counter + ". §e" + activeQuest.getQuest().getQuestFinalName());
                                    counter += 1;
                                }
                            }

                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.no-quests-accepted", player));
                        }
                    } else if (args[0].equalsIgnoreCase("take")) {
                        if (guiEnabled) {
                            String[] guiSetup = {
                                    "zxxxxxxxx",
                                    "xgggggggx",
                                    "xgggggggx",
                                    "xgggggggx",
                                    "xgggggggx",
                                    "pxxxxxxxn"
                            };
                            InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.takeQuestChoose.title", player), guiSetup);
                            gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                            int count = 0;
                            GuiElementGroup group = new GuiElementGroup('g');

                            for (final Quest quest : main.getQuestManager().getAllQuests()) {
                                if (quest.isTakeEnabled()) {
                                    final Material materialToUse = quest.getTakeItem();

                                    if (main.getDataManager().getConfiguration().showQuestItemAmount) {
                                        count++;
                                    }

                                    String displayName = quest.getQuestFinalName();

                                    displayName = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.questNamePrefix", player) + displayName;

                                    if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                                        displayName += main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.acceptedSuffix", player);
                                    }
                                    String description = "";
                                    if (!quest.getQuestDescription().isBlank()) {
                                        description = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.questDescriptionPrefix", player) + quest.getQuestDescription(50);
                                    }

                                    group.addElement(new StaticGuiElement('e',
                                            new ItemStack(materialToUse),
                                            count, // Display a number as the item count
                                            click -> {
                                                player.chat("/notquests preview " + quest.getQuestName());
                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            displayName,
                                            description,
                                            main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.bottomText", player)


                                    ));

                                }


                            }


                            gui.addElement(group);

                            // Previous page
                            gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));

                            // Next page
                            gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));


                            gui.show(player);
                        } else {
                            sender.sendMessage("§cPlease specify the §bname of the quest§c you wish to take.");
                            sender.sendMessage("§e/nquests §6take §3[Quest Name]");
                        }

                    } else if (args[0].equalsIgnoreCase("abort")) {
                        if (questPlayer != null) {
                            if (guiEnabled) {
                                String[] guiSetup = {
                                        "zxxxxxxxx",
                                        "xgggggggx",
                                        "xgggggggx",
                                        "xgggggggx",
                                        "xgggggggx",
                                        "pxxxxxxxn"
                                };
                                InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.abortQuestChoose.title", player), guiSetup);
                                gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                                GuiElementGroup group = new GuiElementGroup('g');


                                int count = 0;

                                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                                    final Material materialToUse;
                                    if (!activeQuest.isCompleted()) {
                                        materialToUse = activeQuest.getQuest().getTakeItem();
                                    } else {
                                        materialToUse = Material.EMERALD_BLOCK;
                                    }


                                    if (main.getDataManager().getConfiguration().showQuestItemAmount) {
                                        count++;
                                    }
                                    String displayName = activeQuest.getQuest().getQuestFinalName();

                                    group.addElement(new StaticGuiElement('e',
                                            new ItemStack(materialToUse),
                                            count, // Display a number as the item count
                                            click -> {
                                                player.chat("/notquests abort " + activeQuest.getQuest().getQuestName());
                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            main.getLanguageManager().getString("gui.abortQuestChoose.button.abortQuestPreview.text", player, activeQuest)


                                    ));

                                }


                                gui.addElement(group);

                                // Previous page
                                gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));

                                // Next page
                                gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));


                                gui.show(player);
                            } else {

                                sender.sendMessage("§cPlease specify the §bname of the quest§c you wish to abort (fail).");
                                sender.sendMessage("§e/nquests §6abort §3[Quest Name]");
                            }
                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.no-quests-accepted", player));
                        }


                    } else if (args[0].equalsIgnoreCase("preview")) {
                        if (guiEnabled) {
                            String[] guiSetup = {
                                    "zxxxxxxxx",
                                    "xgggggggx",
                                    "xgggggggx",
                                    "xgggggggx",
                                    "xgggggggx",
                                    "pxxxxxxxn"
                            };
                            InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.previewQuestChoose.title", player), guiSetup);
                            gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                            int count = 0;
                            GuiElementGroup group = new GuiElementGroup('g');

                            for (final Quest quest : main.getQuestManager().getAllQuests()) {
                                if (quest.isTakeEnabled()) {
                                    final Material materialToUse = quest.getTakeItem();


                                    if (main.getDataManager().getConfiguration().showQuestItemAmount) {
                                        count++;
                                    }
                                    String displayName = quest.getQuestFinalName();

                                    displayName = main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.questNamePrefix", player) + displayName;

                                    if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                                        displayName += main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.acceptedSuffix", player);
                                    }
                                    String description = "";
                                    if (!quest.getQuestDescription().isBlank()) {
                                        description = main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.questDescriptionPrefix", player) + quest.getQuestDescription(50);
                                    }

                                    group.addElement(new StaticGuiElement('e',
                                            new ItemStack(materialToUse),
                                            count, // Display a number as the item count
                                            click -> {
                                                player.chat("/notquests preview " + quest.getQuestName());
                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            displayName,
                                            description,
                                            main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.bottomText", player)


                                    ));

                                }

                            }


                            gui.addElement(group);

                            // Previous page
                            gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));

                            // Next page
                            gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));


                            gui.show(player);
                        } else {
                            sender.sendMessage("§cPlease specify the §bname of the quest§c you wish to preview.");
                            sender.sendMessage("§e/nquests §6preview §3[Quest Name]");
                        }

                    } else if (args[0].equalsIgnoreCase("progress")) {
                        sender.sendMessage("§cPlease specify the §bname of the quest§c you wish to see your progress for.");
                        sender.sendMessage("§e/nquests §6progress §3[Quest Name]");
                    } else if (args[0].equalsIgnoreCase("questPoints")) {
                        if (questPlayer != null) {
                            sender.sendMessage(main.getLanguageManager().getString("chat.questpoints.query", player, questPlayer));
                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.questpoints.none", player));
                        }
                    } else {
                        sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage", player));
                    }
                } else if (args.length >= 2 && args[0].equalsIgnoreCase("continueConversation")) {
                    if (args[0].equalsIgnoreCase("continueConversation")) {
                        handleConversation(player, args);
                        return true;
                    }
                } else if (args.length == 2) {
                    //Accept Quest
                    if (args[0].equalsIgnoreCase("take")) {
                        final Quest quest = main.getQuestManager().getQuest(args[1]);
                        if (quest != null) {

                            if (!quest.isTakeEnabled() && !main.getQuestManager().isPlayerCloseToCitizenOrArmorstandWithQuest(player, quest)) {
                                sender.sendMessage(main.getLanguageManager().getString("chat.take-disabled-accept", player, quest));
                                return true;
                            }
                            final String result = main.getQuestPlayerManager().acceptQuest(player, quest, true, true);
                            if (!result.equals("accepted")) {
                                sender.sendMessage(result);
                            } else {
                                sender.sendMessage(main.getLanguageManager().getString("chat.quest-successfully-accepted", player, quest));
                                if (!quest.getQuestDescription().isBlank()) {
                                    sender.sendMessage(main.getLanguageManager().getString("chat.quest-description", player, quest));
                                } else {
                                    sender.sendMessage(main.getLanguageManager().getString("chat.missing-quest-description", player));
                                }
                            }
                            return true;

                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.quest-does-not-exist", player).replace("%QUESTNAME%", args[1]));
                        }
                    } else if (args[0].equalsIgnoreCase("abort")) {

                        if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                            final String activeQuestName = args[1];
                            AtomicBoolean failedSuccessfully = new AtomicBoolean(false);
                            final ArrayList<ActiveQuest> questsToFail = new ArrayList<>();
                            for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                if (activeQuest.getQuest().getQuestName().equalsIgnoreCase(activeQuestName)) {
                                    questsToFail.add(activeQuest);
                                }
                            }

                            if (questsToFail.isEmpty()) {
                                sender.sendMessage(main.getLanguageManager().getString("chat.quest-not-active-error", player).replace("%QUESTNAME%", activeQuestName));
                                return true;
                            }

                            if (guiEnabled) {
                                String[] guiSetup = {
                                        "zxxxxxxxx",
                                        "x0123456x",
                                        "x789abcdx",
                                        "xefghijkx",
                                        "xlmnopqrx",
                                        "xxxxxxxxx"
                                };
                                InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.abortQuest.title", player), guiSetup);
                                gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                                final ActiveQuest activeQuest = questsToFail.get(0);


                                gui.addElement(new StaticGuiElement('9',
                                        new ItemStack(Material.GREEN_WOOL),
                                        1, // Display a number as the item count
                                        click -> {
                                            questPlayer.failQuest(activeQuest);
                                            failedSuccessfully.set(true);
                                            questsToFail.clear();
                                            if (!failedSuccessfully.get()) {
                                                sender.sendMessage(main.getLanguageManager().getString("chat.quest-not-active-error", player, activeQuest));
                                            }
                                            sender.sendMessage(main.getLanguageManager().getString("chat.quest-aborted", player, activeQuest));

                                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                            gui.close();
                                            return true; // returning true will cancel the click event and stop taking the item

                                        },
                                        main.getLanguageManager().getString("gui.abortQuest.button.confirmAbort.text", player, activeQuest)


                                ));
                                gui.addElement(new StaticGuiElement('b',
                                        new ItemStack(Material.RED_WOOL),
                                        1, // Display a number as the item count
                                        click -> {
                                            gui.close();
                                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                            return true; // returning true will cancel the click event and stop taking the item

                                        },
                                        main.getLanguageManager().getString("gui.abortQuest.button.cancelAbort.text", player)


                                ));


                                gui.show(player);
                            } else {

                                for (final ActiveQuest activeQuest : questsToFail) {
                                    questPlayer.failQuest(activeQuest);
                                    failedSuccessfully.set(true);
                                    sender.sendMessage(main.getLanguageManager().getString("chat.quest-aborted", player, activeQuest));

                                }
                                questsToFail.clear();
                                if (!failedSuccessfully.get()) {
                                    sender.sendMessage(main.getLanguageManager().getString("chat.quest-not-active-error", player).replace("%QUESTNAME%", activeQuestName));
                                }
                            }

                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.no-quests-accepted", player));
                        }


                    } else if (args[0].equalsIgnoreCase("preview")) {
                        Quest quest = main.getQuestManager().getQuest(args[1]);
                        if (quest != null) {

                            if (!quest.isTakeEnabled() && !main.getQuestManager().isPlayerCloseToCitizenOrArmorstandWithQuest(player, quest)) {
                                sender.sendMessage(main.getLanguageManager().getString("chat.take-disabled-preview", player, quest));
                                return true;
                            }
                            if (guiEnabled) {
                                String[] guiSetup = {
                                        "zxxxxxxxx",
                                        "x0123456x",
                                        "x789abcdx",
                                        "xefghijkx",
                                        "xlmnopqrx",
                                        "xxxxxxxxx"
                                };


                                InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.previewQuest.title", player, quest, questPlayer), guiSetup);
                                gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this


                                if (main.getDataManager().getConfiguration().isGuiQuestPreviewDescription_enabled()) {
                                    String description = main.getLanguageManager().getString("gui.previewQuest.button.description.empty", player, questPlayer);
                                    if (!quest.getQuestDescription().isBlank()) {
                                        description = quest.getQuestDescription(50);
                                    }
                                    gui.addElement(new StaticGuiElement(main.getDataManager().getConfiguration().getGuiQuestPreviewDescription_slot(),
                                            new ItemStack(Material.BOOKSHELF),
                                            1, // Display a number as the item count
                                            click -> {

                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            main.getLanguageManager().getString("gui.previewQuest.button.description.text", player, questPlayer)
                                                    .replace("%QUESTDESCRIPTION%", description)


                                    ));
                                }


                                if (main.getDataManager().getConfiguration().isGuiQuestPreviewRewards_enabled()) {
                                    String rewards = main.getQuestManager().getQuestRewards(quest);
                                    if (rewards.isBlank()) {
                                        rewards = main.getLanguageManager().getString("gui.previewQuest.button.rewards.empty", player);
                                    }

                                    gui.addElement(new StaticGuiElement(main.getDataManager().getConfiguration().getGuiQuestPreviewRewards_slot(),
                                            new ItemStack(Material.EMERALD),
                                            1, // Display a number as the item count
                                            click -> {

                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            main.getLanguageManager().getString("gui.previewQuest.button.rewards.text", player, quest, questPlayer)
                                                    .replace("%QUESTREWARDS%", rewards)


                                    ));
                                }


                                if (main.getDataManager().getConfiguration().isGuiQuestPreviewRequirements_enabled()) {
                                    String requirements = main.getQuestManager().getQuestRequirements(quest);
                                    if (requirements.isBlank()) {
                                        requirements = main.getLanguageManager().getString("gui.previewQuest.button.requirements.empty", player, questPlayer);
                                    }

                                    gui.addElement(new StaticGuiElement(main.getDataManager().getConfiguration().getGuiQuestPreviewRequirements_slot(),
                                            new ItemStack(Material.IRON_BARS),
                                            1, // Display a number as the item count
                                            click -> {

                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            main.getLanguageManager().getString("gui.previewQuest.button.requirements.text", player, quest, questPlayer)
                                                    .replace("%QUESTREQUIREMENTS%", requirements)


                                    ));
                                }


                                gui.addElement(new StaticGuiElement('g',
                                        new ItemStack(Material.GREEN_WOOL),
                                        1, // Display a number as the item count
                                        click -> {
                                            player.chat("/notquests take " + quest.getQuestName());

                                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                            gui.close();
                                            return true; // returning true will cancel the click event and stop taking the item

                                        },
                                        main.getLanguageManager().getString("gui.previewQuest.button.confirmTake.text", player, quest, questPlayer)

                                ));
                                gui.addElement(new StaticGuiElement('i',
                                        new ItemStack(Material.RED_WOOL),
                                        1, // Display a number as the item count
                                        click -> {
                                            gui.close();
                                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                            return true; // returning true will cancel the click event and stop taking the item

                                        },
                                        main.getLanguageManager().getString("gui.previewQuest.button.cancelTake.text", player, quest, questPlayer)

                                ));


                                gui.show(player);
                            } else {
                                main.getQuestManager().sendSingleQuestPreview(player, quest);
                            }

                            return true;

                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.quest-does-not-exist", player, questPlayer).replace("%QUESTNAME%", args[1]));
                        }
                    } else if (args[0].equalsIgnoreCase("progress")) {
                        ActiveQuest requestedActiveQuest = null;
                        if (questPlayer != null) {
                            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                if (activeQuest.getQuest().getQuestName().equalsIgnoreCase(args[1])) {
                                    requestedActiveQuest = activeQuest;
                                }
                            }
                            if (requestedActiveQuest != null) {
                                if (guiEnabled) {
                                    String[] guiSetup = {
                                            "zxxxxxxxx",
                                            "xgggggggx",
                                            "xgggggggx",
                                            "xgggggggx",
                                            "xgggggggx",
                                            "pxxxxxxxn"
                                    };
                                    String displayName = requestedActiveQuest.getQuest().getQuestFinalName();

                                    InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.progress.title", player, requestedActiveQuest, questPlayer), guiSetup);
                                    gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this


                                    //int count = 0;
                                    GuiElementGroup group = new GuiElementGroup('g');

                                    for (final ActiveObjective activeObjective : requestedActiveQuest.getActiveObjectives()) {

                                        final Material materialToUse = Material.PAPER;

                                        //count++;
                                        int count = activeObjective.getObjectiveID();
                                        if (!main.getDataManager().getConfiguration().showObjectiveItemAmount) {
                                            count = 0;
                                        }
                                        if (activeObjective.isUnlocked()) {

                                            String descriptionToDisplay = main.getLanguageManager().getString("gui.progress.button.unlockedObjective.description-empty", player);
                                            if (!activeObjective.getObjective().getObjectiveDescription().isBlank()) {
                                                descriptionToDisplay = activeObjective.getObjective().getObjectiveDescription(50);
                                            }

                                            group.addElement(new StaticGuiElement('e',
                                                    new ItemStack(materialToUse),
                                                    count, // Display a number as the item count
                                                    click -> {
                                                        //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                        return true; // returning true will cancel the click event and stop taking the item

                                                    },
                                                    main.getLanguageManager().getString("gui.progress.button.unlockedObjective.text", player, activeObjective, questPlayer)
                                                            .replace("%OBJECTIVEDESCRIPTION%", descriptionToDisplay)
                                                            .replace("%ACTIVEOBJECTIVEDESCRIPTION%", main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), false, player))


                                            ));
                                        } else {


                                            group.addElement(new StaticGuiElement('e',
                                                    new ItemStack(materialToUse),
                                                    activeObjective.getObjectiveID(), // Display a number as the item count
                                                    click -> {
                                                        //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                        return true; // returning true will cancel the click event and stop taking the item

                                                    },
                                                    main.getLanguageManager().getString("gui.progress.button.lockedObjective.text", player, activeObjective)


                                            ));
                                        }


                                    }
                                    //count++;
                                    for (final ActiveObjective activeObjective : requestedActiveQuest.getCompletedObjectives()) {

                                        final Material materialToUse = Material.FILLED_MAP;

                                        //count++;

                                        int count = activeObjective.getObjectiveID();
                                        if (!main.getDataManager().getConfiguration().showObjectiveItemAmount) {
                                            count = 0;
                                        }
                                        final String nameToDisplay = activeObjective.getObjective().getObjectiveFinalName();


                                        String descriptionToDisplay = main.getLanguageManager().getString("gui.progress.button.completedObjective.description-empty", player, activeObjective, questPlayer);
                                        if (!activeObjective.getObjective().getObjectiveDescription().isBlank()) {
                                            descriptionToDisplay = activeObjective.getObjective().getObjectiveDescription(50);
                                        }


                                        group.addElement(new StaticGuiElement('e',
                                                new ItemStack(materialToUse),
                                                count, // Display a number as the item count
                                                click -> {
                                                    //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                    return true; // returning true will cancel the click event and stop taking the item

                                                },
                                                main.getLanguageManager().getString("gui.progress.button.completedObjective.text", player, activeObjective, questPlayer)
                                                        .replace("%OBJECTIVEDESCRIPTION%", descriptionToDisplay)
                                                        .replace("%COMPLETEDOBJECTIVEDESCRIPTION%", main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), true, player))


                                        ));


                                    }


                                    gui.addElement(group);

                                    // Previous page
                                    gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, main.getLanguageManager().getString("gui.progress.button.previousPage.text", player)));

                                    // Next page
                                    gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, main.getLanguageManager().getString("gui.progress.button.nextPage.text", player)));


                                    gui.show(player);
                                } else {
                                    sender.sendMessage("§aCompleted Objectives for Quest §b" + requestedActiveQuest.getQuest().getQuestFinalName() + "§e:");
                                    main.getQuestManager().sendCompletedObjectivesAndProgress(player, requestedActiveQuest);
                                    sender.sendMessage("§eActive Objectives for Quest §b" + requestedActiveQuest.getQuest().getQuestFinalName() + "§e:");
                                    main.getQuestManager().sendActiveObjectivesAndProgress(player, requestedActiveQuest);
                                }

                            } else {
                                sender.sendMessage(main.getLanguageManager().getString("chat.quest-not-found-or-does-not-exist", player, questPlayer).replace("%QUESTNAME%", args[1]));
                                sender.sendMessage(main.getLanguageManager().getString("chat.active-quests-label", player, questPlayer));
                                int counter = 1;
                                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                    sender.sendMessage("§a" + counter + ". §e" + activeQuest.getQuest().getQuestFinalName());
                                    counter += 1;
                                }
                            }
                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.no-quests-accepted", player));
                        }

                    } else {
                        sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage", player));
                    }

                } else {
                    sender.sendMessage(main.getLanguageManager().getString("chat.too-many-arguments", player));
                }
            } else {
                sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage", player).replace("%PERMISSION%", "notquests.use"));
            }
        } else {
            sender.sendMessage("§cOnly players can run this command! Try §b/notquestsadmin§c.");
        }

        return true;
    }



    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        completions.clear();
        main.getDataManager().partialCompletions.clear();

        if (sender instanceof Player player) {
            if (sender.hasPermission("notquests.use")) {


                if (args.length == 1) {
                    completions.addAll(Arrays.asList("take", "abort", "preview", "activeQuests", "progress", "questPoints"));
                    StringUtil.copyPartialMatches(args[args.length - 1], completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;

                } else if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("take")) {
                        for (Quest quest : main.getQuestManager().getAllQuests()) {
                            if (quest.isTakeEnabled()) {
                                completions.add(quest.getQuestName());
                            }
                        }
                        StringUtil.copyPartialMatches(args[args.length - 1], completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[0].equalsIgnoreCase("abort")) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            for (final ActiveQuest quest : questPlayer.getActiveQuests()) {
                                completions.add(quest.getQuest().getQuestName());
                            }
                        }

                        StringUtil.copyPartialMatches(args[args.length - 1], completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[0].equalsIgnoreCase("preview")) {
                        for (Quest quest : main.getQuestManager().getAllQuests()) {
                            if (quest.isTakeEnabled()) {
                                completions.add(quest.getQuestName());
                            }
                        }
                        StringUtil.copyPartialMatches(args[args.length - 1], completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[0].equalsIgnoreCase("progress")) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            for (ActiveQuest quest : questPlayer.getActiveQuests()) {
                                completions.add(quest.getQuest().getQuestName());
                            }
                            StringUtil.copyPartialMatches(args[args.length - 1], completions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        }

                    }

                }

            }
        }


        StringUtil.copyPartialMatches(args[args.length - 1], completions, main.getDataManager().partialCompletions);
        return main.getDataManager().partialCompletions; //returns the possibility's to the client


    }


    private void handleConversation(Player player, String[] args) {
        final StringBuilder option = new StringBuilder();

        int counter = 0;
        for (String arg : args) {
            counter++;
            if (counter == args.length) {
                option.append(arg);
            } else if (counter > 1) {
                option.append(arg).append(" ");
            }
        }

        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId()) == null) {
            return;
        }


        //Check if the player has an open conversation
        final ConversationPlayer conversationPlayer = main.getConversationManager().getOpenConversation(player.getUniqueId());
        if (conversationPlayer != null) {
            conversationPlayer.chooseOption(option.toString());
        } else {
            questPlayer.sendDebugMessage("Tried to choose conversation option, but the conversationPlayer was not found! Active conversationPlayers count: " + NotQuestColors.highlightGradient + main.getConversationManager().getOpenConversations().size());
            questPlayer.sendDebugMessage("All active conversationPlayers: " + NotQuestColors.highlightGradient + main.getConversationManager().getOpenConversations().toString());
            questPlayer.sendDebugMessage("Current QuestPlayer: " + questPlayer);


        }

    }


}

