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

package rocks.gravili.Commands;

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
import rocks.gravili.NotQuests;
import rocks.gravili.Structs.ActiveObjective;
import rocks.gravili.Structs.ActiveQuest;
import rocks.gravili.Structs.Quest;
import rocks.gravili.Structs.QuestPlayer;

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
                        InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.main.title"), guiSetup);
                        gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                        gui.addElement(new StaticGuiElement('8',
                                new ItemStack(Material.CHEST),
                                0, // Display a number as the item count
                                click -> {
                                    player.chat("/notquests take");

                                    return true; // returning true will cancel the click event and stop taking the item
                                },
                                main.getLanguageManager().getString("gui.main.button.takequest.text")

                        ));
                        gui.addElement(new StaticGuiElement('a',
                                new ItemStack(Material.REDSTONE_BLOCK),
                                0, // Display a number as the item count
                                click -> {
                                    player.chat("/notquests abort");
                                    return true; // returning true will cancel the click event and stop taking the item
                                },
                                main.getLanguageManager().getString("gui.main.button.abortquest.text")
                        ));
                        gui.addElement(new StaticGuiElement('c',
                                new ItemStack(Material.SPYGLASS),
                                0, // Display a number as the item count
                                click -> {
                                    player.chat("/notquests preview");
                                    return true; // returning true will cancel the click event and stop taking the item
                                },
                                main.getLanguageManager().getString("gui.main.button.previewquest.text")
                        ));

                        gui.addElement(new StaticGuiElement('o',
                                new ItemStack(Material.LADDER),
                                0, // Display a number as the item count
                                click -> {
                                    player.chat("/notquests activeQuests");
                                    return true; // returning true will cancel the click event and stop taking the item
                                },
                                main.getLanguageManager().getString("gui.main.button.activequests.text")
                        ));
                        if (questPlayer != null) {
                            gui.addElement(new StaticGuiElement('z',
                                    new ItemStack(Material.SUNFLOWER),
                                    0, // Display a number as the item count
                                    click -> {
                                        return true; // returning true will cancel the click event and stop taking the item
                                    },
                                    main.getLanguageManager().getString("gui.main.button.questpoints.text").replaceAll("%QUESTPOINTS%", "" + questPlayer.getQuestPoints())
                            ));
                        } else {
                            gui.addElement(new StaticGuiElement('z',
                                    new ItemStack(Material.SUNFLOWER),
                                    0, // Display a number as the item count
                                    click -> {
                                        return true; // returning true will cancel the click event and stop taking the item
                                    },
                                    main.getLanguageManager().getString("gui.main.button.questpoints.text").replaceAll("%QUESTPOINTS%", "??")

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
                                InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.activeQuests.title"), guiSetup);
                                gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this


                                int count = 0;
                                GuiElementGroup group = new GuiElementGroup('g');

                                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                                    final Material materialToUse;
                                    if (!activeQuest.isCompleted()) {
                                        materialToUse = Material.BOOK;
                                    } else {
                                        materialToUse = Material.EMERALD_BLOCK;
                                    }


                                    count++;

                                    String displayName = activeQuest.getQuest().getQuestFinalName();

                                    group.addElement(new StaticGuiElement('e',
                                            new ItemStack(materialToUse),
                                            count, // Display a number as the item count
                                            click -> {
                                                player.chat("/notquests progress " + activeQuest.getQuest().getQuestName());
                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            main.getLanguageManager().getString("gui.activeQuests.button.activeQuestButton.text")
                                                    .replaceAll("%QUESTNAME%", displayName)
                                                    .replaceAll("%COMPLETEDOBJECTIVESCOUNT%", "" + activeQuest.getCompletedObjectives().size())
                                                    .replaceAll("%ALLOBJECTIVESCOUNT%", "" + activeQuest.getQuest().getObjectives().size())


                                    ));

                                }


                                gui.addElement(group);

                                // Previous page
                                gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));

                                // Next page
                                gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));


                                gui.show(player);
                            } else {
                                sender.sendMessage(main.getLanguageManager().getString("chat.active-quests-label"));
                                int counter = 1;
                                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                    sender.sendMessage("§a" + counter + ". §e" + activeQuest.getQuest().getQuestFinalName());
                                    counter += 1;
                                }
                            }

                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.no-quests-accepted"));
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
                            InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.takeQuestChoose.title"), guiSetup);
                            gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                            int count = 0;
                            GuiElementGroup group = new GuiElementGroup('g');

                            for (final Quest quest : main.getQuestManager().getAllQuests()) {
                                if (quest.isTakeEnabled()) {
                                    final Material materialToUse = Material.BOOK;


                                    count++;

                                    String displayName = quest.getQuestFinalName();

                                    displayName = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.questNamePrefix") + displayName;

                                    if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                                        displayName += main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.acceptedSuffix");
                                    }
                                    String description = "";
                                    if (!quest.getQuestDescription().isBlank()) {
                                        description = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.questDescriptionPrefix") + quest.getQuestDescription(50);
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
                                            main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.bottomText")


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
                                InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.abortQuestChoose.title"), guiSetup);
                                gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                                GuiElementGroup group = new GuiElementGroup('g');


                                int count = 0;

                                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                                    final Material materialToUse;
                                    if (!activeQuest.isCompleted()) {
                                        materialToUse = Material.BOOK;
                                    } else {
                                        materialToUse = Material.EMERALD_BLOCK;
                                    }


                                    count++;

                                    String displayName = activeQuest.getQuest().getQuestFinalName();

                                    group.addElement(new StaticGuiElement('e',
                                            new ItemStack(materialToUse),
                                            count, // Display a number as the item count
                                            click -> {
                                                player.chat("/notquests abort " + activeQuest.getQuest().getQuestName());
                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            main.getLanguageManager().getString("gui.abortQuestChoose.button.abortQuestPreview.text")
                                                    .replaceAll("%QUESTNAME%", displayName)
                                                    .replaceAll("%COMPLETEDOBJECTIVESCOUNT%", "" + activeQuest.getCompletedObjectives().size())
                                                    .replaceAll("%ALLOBJECTIVESCOUNT%", "" + activeQuest.getQuest().getObjectives().size())


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
                            sender.sendMessage(main.getLanguageManager().getString("chat.no-quests-accepted"));
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
                            InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.previewQuestChoose.title"), guiSetup);
                            gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                            int count = 0;
                            GuiElementGroup group = new GuiElementGroup('g');

                            for (final Quest quest : main.getQuestManager().getAllQuests()) {
                                if (quest.isTakeEnabled()) {
                                    final Material materialToUse = Material.BOOK;


                                    count++;

                                    String displayName = quest.getQuestFinalName();

                                    displayName = main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.questNamePrefix") + displayName;

                                    if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                                        displayName += main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.acceptedSuffix");
                                    }
                                    String description = "";
                                    if (!quest.getQuestDescription().isBlank()) {
                                        description = main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.questDescriptionPrefix") + quest.getQuestDescription(50);
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
                                            main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.bottomText")


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
                            sender.sendMessage(main.getLanguageManager().getString("chat.questpoints.query").replaceAll("%QUESTPOINTS%", "" + questPlayer.getQuestPoints()));
                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.questpoints.none"));
                        }
                    } else {
                        sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                    }
                } else if (args.length == 2) {
                    //Accept Quest
                    if (args[0].equalsIgnoreCase("take")) {
                        final Quest quest = main.getQuestManager().getQuest(args[1]);
                        if (quest != null) {

                            if (!quest.isTakeEnabled() && !main.getQuestManager().isPlayerCloseToCitizenOrArmorstandWithQuest(player, quest)) {
                                sender.sendMessage(main.getLanguageManager().getString("chat.take-disabled-accept").replaceAll("%QUESTNAME%", quest.getQuestFinalName()));
                                return true;
                            }
                            final String result = main.getQuestPlayerManager().acceptQuest(player, quest, true, true);
                            if (!result.equals("accepted")) {
                                sender.sendMessage(result);
                            } else {
                                sender.sendMessage(main.getLanguageManager().getString("chat.quest-successfully-accepted").replaceAll("%QUESTNAME%", quest.getQuestFinalName()));
                                if (!quest.getQuestDescription().isBlank()) {
                                    sender.sendMessage(main.getLanguageManager().getString("chat.quest-description").replaceAll("%QUESTDESCRIPTION%", quest.getQuestDescription()));
                                } else {
                                    sender.sendMessage(main.getLanguageManager().getString("chat.missing-quest-description"));
                                }
                            }
                            return true;

                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.quest-does-not-exist").replaceAll("%QUESTNAME%", args[1]));
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
                            if (guiEnabled) {
                                String[] guiSetup = {
                                        "zxxxxxxxx",
                                        "x0123456x",
                                        "x789abcdx",
                                        "xefghijkx",
                                        "xlmnopqrx",
                                        "xxxxxxxxx"
                                };
                                InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.abortQuest.title"), guiSetup);
                                gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                                final ActiveQuest activeQuest = questsToFail.get(0);

                                String displayName = activeQuest.getQuest().getQuestFinalName();

                                gui.addElement(new StaticGuiElement('9',
                                        new ItemStack(Material.GREEN_WOOL),
                                        1, // Display a number as the item count
                                        click -> {
                                            questPlayer.failQuest(activeQuest);
                                            failedSuccessfully.set(true);
                                            questsToFail.clear();
                                            if (!failedSuccessfully.get()) {
                                                sender.sendMessage(main.getLanguageManager().getString("chat.quest-not-active-error").replaceAll("%QUESTNAME%", displayName));
                                            }
                                            sender.sendMessage(main.getLanguageManager().getString("chat.quest-aborted").replaceAll("%QUESTNAME%", displayName));

                                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                            gui.close();
                                            return true; // returning true will cancel the click event and stop taking the item

                                        },
                                        main.getLanguageManager().getString("gui.abortQuest.button.confirmAbort.text")
                                                .replaceAll("%QUESTNAME%", displayName)
                                                .replaceAll("%COMPLETEDOBJECTIVESCOUNT%", "" + activeQuest.getCompletedObjectives().size())
                                                .replaceAll("%ALLOBJECTIVESCOUNT%", "" + activeQuest.getQuest().getObjectives().size())


                                ));
                                gui.addElement(new StaticGuiElement('b',
                                        new ItemStack(Material.RED_WOOL),
                                        1, // Display a number as the item count
                                        click -> {
                                            gui.close();
                                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                            return true; // returning true will cancel the click event and stop taking the item

                                        },
                                        main.getLanguageManager().getString("gui.abortQuest.button.cancelAbort.text")


                                ));


                                gui.show(player);
                            } else {

                                for (final ActiveQuest activeQuest : questsToFail) {
                                    questPlayer.failQuest(activeQuest);
                                    failedSuccessfully.set(true);
                                    sender.sendMessage(main.getLanguageManager().getString("chat.quest-aborted").replaceAll("%QUESTNAME%", activeQuest.getQuest().getQuestFinalName()));

                                }
                                questsToFail.clear();
                                if (!failedSuccessfully.get()) {
                                    sender.sendMessage(main.getLanguageManager().getString("chat.quest-not-active-error").replaceAll("%QUESTNAME%", activeQuestName));

                                }
                            }

                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.no-quests-accepted"));
                        }


                    } else if (args[0].equalsIgnoreCase("preview")) {
                        Quest quest = main.getQuestManager().getQuest(args[1]);
                        if (quest != null) {

                            if (!quest.isTakeEnabled() && !main.getQuestManager().isPlayerCloseToCitizenOrArmorstandWithQuest(player, quest)) {
                                sender.sendMessage(main.getLanguageManager().getString("chat.take-disabled-preview").replaceAll("%QUESTNAME%", quest.getQuestFinalName()));
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

                                String displayName = quest.getQuestFinalName();

                                InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.previewQuest.title").replaceAll("%QUESTNAME%", displayName), guiSetup);
                                gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this


                                if (main.getDataManager().getConfiguration().isGuiQuestPreviewDescription_enabled()) {
                                    String description = main.getLanguageManager().getString("gui.previewQuest.button.description.empty");
                                    if (!quest.getQuestDescription().isBlank()) {
                                        description = quest.getQuestDescription(50);
                                    }
                                    gui.addElement(new StaticGuiElement(main.getDataManager().getConfiguration().getGuiQuestPreviewDescription_slot(),
                                            new ItemStack(Material.BOOKSHELF),
                                            1, // Display a number as the item count
                                            click -> {

                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            main.getLanguageManager().getString("gui.previewQuest.button.description.text")
                                                    .replaceAll("%QUESTDESCRIPTION%", description)


                                    ));
                                }


                                if (main.getDataManager().getConfiguration().isGuiQuestPreviewRewards_enabled()) {
                                    String rewards = main.getQuestManager().getQuestRewards(quest);
                                    if (rewards.isBlank()) {
                                        rewards = main.getLanguageManager().getString("gui.previewQuest.button.rewards.empty");
                                    }

                                    gui.addElement(new StaticGuiElement(main.getDataManager().getConfiguration().getGuiQuestPreviewRewards_slot(),
                                            new ItemStack(Material.EMERALD),
                                            1, // Display a number as the item count
                                            click -> {

                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            main.getLanguageManager().getString("gui.previewQuest.button.rewards.text")
                                                    .replaceAll("%QUESTREWARDS%", rewards)


                                    ));
                                }


                                if (main.getDataManager().getConfiguration().isGuiQuestPreviewRequirements_enabled()) {
                                    String requirements = main.getQuestManager().getQuestRequirements(quest);
                                    if (requirements.isBlank()) {
                                        requirements = main.getLanguageManager().getString("gui.previewQuest.button.requirements.empty");
                                    }

                                    gui.addElement(new StaticGuiElement(main.getDataManager().getConfiguration().getGuiQuestPreviewRequirements_slot(),
                                            new ItemStack(Material.IRON_BARS),
                                            1, // Display a number as the item count
                                            click -> {

                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            main.getLanguageManager().getString("gui.previewQuest.button.requirements.text")
                                                    .replaceAll("%QUESTREQUIREMENTS%", requirements)


                                    ));
                                }


                                String finalDisplayName = displayName;
                                gui.addElement(new StaticGuiElement('g',
                                        new ItemStack(Material.GREEN_WOOL),
                                        1, // Display a number as the item count
                                        click -> {
                                            player.chat("/notquests take " + quest.getQuestName());

                                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                            gui.close();
                                            return true; // returning true will cancel the click event and stop taking the item

                                        },
                                        main.getLanguageManager().getString("gui.previewQuest.button.confirmTake.text")
                                                .replaceAll("%QUESTNAME%", finalDisplayName)

                                ));
                                gui.addElement(new StaticGuiElement('i',
                                        new ItemStack(Material.RED_WOOL),
                                        1, // Display a number as the item count
                                        click -> {
                                            gui.close();
                                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                            return true; // returning true will cancel the click event and stop taking the item

                                        },
                                        main.getLanguageManager().getString("gui.previewQuest.button.cancelTake.text")

                                ));


                                gui.show(player);
                            } else {
                                main.getQuestManager().sendSingleQuestPreview(player, quest);
                            }

                            return true;

                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.quest-does-not-exist").replaceAll("%QUESTNAME%", args[1]));
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

                                    InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.progress.title").replaceAll("%QUESTNAME%", displayName), guiSetup);
                                    gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this


                                    //int count = 0;
                                    GuiElementGroup group = new GuiElementGroup('g');

                                    for (final ActiveObjective activeObjective : requestedActiveQuest.getActiveObjectives()) {

                                        final Material materialToUse = Material.PAPER;

                                        //count++;
                                        if (activeObjective.isUnlocked()) {
                                            String nameToDisplay = activeObjective.getObjective().getObjectiveFinalName();

                                            String descriptionToDisplay = main.getLanguageManager().getString("gui.progress.button.unlockedObjective.description-empty");
                                            if (!activeObjective.getObjective().getObjectiveDescription().isBlank()) {
                                                descriptionToDisplay = activeObjective.getObjective().getObjectiveDescription(50);
                                            }

                                            group.addElement(new StaticGuiElement('e',
                                                    new ItemStack(materialToUse),
                                                    activeObjective.getObjectiveID(), // Display a number as the item count
                                                    click -> {
                                                        //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                        return true; // returning true will cancel the click event and stop taking the item

                                                    },
                                                    main.getLanguageManager().getString("gui.progress.button.unlockedObjective.text")
                                                            .replaceAll("%ACTIVEOBJECTIVEID%", "" + activeObjective.getObjectiveID())
                                                            .replaceAll("%OBJECTIVENAME%", nameToDisplay)
                                                            .replaceAll("%OBJECTIVEDESCRIPTION%", descriptionToDisplay)
                                                            .replaceAll("%ACTIVEOBJECTIVEDESCRIPTION%", main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), false))
                                                            .replaceAll("%ACTIVEOBJECTIVEPROGRESS%", "" + activeObjective.getCurrentProgress())
                                                            .replaceAll("%OBJECTIVEPROGRESSNEEDED%", "" + activeObjective.getProgressNeeded())


                                            ));
                                        } else {


                                            group.addElement(new StaticGuiElement('e',
                                                    new ItemStack(materialToUse),
                                                    activeObjective.getObjectiveID(), // Display a number as the item count
                                                    click -> {
                                                        //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                        return true; // returning true will cancel the click event and stop taking the item

                                                    },
                                                    main.getLanguageManager().getString("gui.progress.button.lockedObjective.text")
                                                            .replaceAll("%ACTIVEOBJECTIVEID%", "" + activeObjective.getObjectiveID())


                                            ));
                                        }


                                    }
                                    //count++;
                                    for (final ActiveObjective activeObjective : requestedActiveQuest.getCompletedObjectives()) {

                                        final Material materialToUse = Material.FILLED_MAP;

                                        //count++;


                                        final String nameToDisplay = activeObjective.getObjective().getObjectiveFinalName();


                                        String descriptionToDisplay = main.getLanguageManager().getString("gui.progress.button.completedObjective.description-empty");
                                        if (!activeObjective.getObjective().getObjectiveDescription().isBlank()) {
                                            descriptionToDisplay = activeObjective.getObjective().getObjectiveDescription(50);
                                        }


                                        group.addElement(new StaticGuiElement('e',
                                                new ItemStack(materialToUse),
                                                activeObjective.getObjectiveID(), // Display a number as the item count
                                                click -> {
                                                    //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                    return true; // returning true will cancel the click event and stop taking the item

                                                },
                                                main.getLanguageManager().getString("gui.progress.button.completedObjective.text")
                                                        .replaceAll("%ACTIVEOBJECTIVEID%", "" + activeObjective.getObjectiveID())
                                                        .replaceAll("%OBJECTIVENAME%", nameToDisplay)
                                                        .replaceAll("%OBJECTIVEDESCRIPTION%", descriptionToDisplay)
                                                        .replaceAll("%COMPLETEDOBJECTIVEDESCRIPTION%", main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), true))
                                                        .replaceAll("%ACTIVEOBJECTIVEPROGRESS%", "" + activeObjective.getCurrentProgress())
                                                        .replaceAll("%OBJECTIVEPROGRESSNEEDED%", "" + activeObjective.getProgressNeeded())


                                        ));


                                    }


                                    gui.addElement(group);

                                    // Previous page
                                    gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, main.getLanguageManager().getString("gui.progress.button.previousPage.text")));

                                    // Next page
                                    gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, main.getLanguageManager().getString("gui.progress.button.nextPage.text")));


                                    gui.show(player);
                                } else {
                                    sender.sendMessage("§aCompleted Objectives for Quest §b" + requestedActiveQuest.getQuest().getQuestFinalName() + "§e:");
                                    main.getQuestManager().sendCompletedObjectivesAndProgress(sender, requestedActiveQuest);
                                    sender.sendMessage("§eActive Objectives for Quest §b" + requestedActiveQuest.getQuest().getQuestFinalName() + "§e:");
                                    main.getQuestManager().sendActiveObjectivesAndProgress(sender, requestedActiveQuest);
                                }

                            } else {
                                sender.sendMessage(main.getLanguageManager().getString("chat.quest-not-found-or-does-not-exist").replaceAll("%QUESTNAME%", args[1]));
                                sender.sendMessage(main.getLanguageManager().getString("chat.active-quests-label"));
                                int counter = 1;
                                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                    sender.sendMessage("§a" + counter + ". §e" + activeQuest.getQuest().getQuestFinalName());
                                    counter += 1;
                                }
                            }
                        } else {
                            sender.sendMessage(main.getLanguageManager().getString("chat.no-quests-accepted"));
                        }

                    } else {
                        sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                    }

                } else {
                    sender.sendMessage(main.getLanguageManager().getString("chat.too-many-arguments"));
                }
            } else {
                sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage").replaceAll("%PERMISSION%", "notquests.use"));
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
}

