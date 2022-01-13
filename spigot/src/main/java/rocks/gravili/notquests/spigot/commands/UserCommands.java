package rocks.gravili.notquests.spigot.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.commands.arguments.ActiveQuestSelector;
import rocks.gravili.notquests.spigot.commands.arguments.QuestSelector;
import rocks.gravili.notquests.spigot.structs.ActiveObjective;
import rocks.gravili.notquests.spigot.structs.ActiveQuest;
import rocks.gravili.notquests.spigot.structs.Quest;
import rocks.gravili.notquests.spigot.structs.QuestPlayer;


public class UserCommands {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> builder;
    private final Component firstLevelCommands;


    public UserCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        this.main = main;
        this.manager = manager;
        this.builder = builder;

        firstLevelCommands = Component.text("NotQuests Player Commands:", NamedTextColor.BLUE, TextDecoration.BOLD)
                .append(Component.newline())
                .append(miniMessage.parse("<YELLOW>/nquests <GOLD>take <DARK_AQUA>[Quest Name]").clickEvent(ClickEvent.suggestCommand("/nquests take ")).hoverEvent(HoverEvent.showText(Component.text("Takes/Starts a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(miniMessage.parse("<YELLOW>/nquests <GOLD>abort <DARK_AQUA>[Quest Name]").clickEvent(ClickEvent.suggestCommand("/nquests abort ")).hoverEvent(HoverEvent.showText(Component.text("Fails a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(miniMessage.parse("<YELLOW>/nquests <GOLD>preview <DARK_AQUA>[Quest Name]").clickEvent(ClickEvent.suggestCommand("/nquests preview ")).hoverEvent(HoverEvent.showText(Component.text("Shows more information about a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(miniMessage.parse("<YELLOW>/nquests <GOLD>activeQuests").clickEvent(ClickEvent.runCommand("/nquests activeQuests")).hoverEvent(HoverEvent.showText(Component.text("Shows all your active Quests", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(miniMessage.parse("<YELLOW>/nquests <GOLD>progress <DARK_AQUA>[Quest Name]").clickEvent(ClickEvent.suggestCommand("/nquests progress ")).hoverEvent(HoverEvent.showText(Component.text("Shows the progress of an active Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(miniMessage.parse("<YELLOW>/nquests <GOLD>questPoints").clickEvent(ClickEvent.runCommand("/nquests questPoints")).hoverEvent(HoverEvent.showText(Component.text("Shows how many Quest Points you have", NamedTextColor.GREEN))))
                .append(Component.newline()

                );

        if (main.getConfiguration().isUserCommandsUseGUI()) {
            constructGUICommands();
        } else {
            constructTextCommands();
        }
        constructCommands();
    }

    public final String convert(final String old) { //Converts MiniMessage to legacy
        return ChatColor.translateAlternateColorCodes('&', main.getUtilManager().miniMessageToLegacyWithSpigotRGB(old)) ;
    }

    public void constructCommands() {
        manager.command(builder.literal("take")
                .senderType(Player.class)
                .argument(QuestSelector.<CommandSender>newBuilder("Quest Name", main).takeEnabledOnly().build(), ArgumentDescription.of("Quest Name"))
                .meta(CommandMeta.DESCRIPTION, "Starts a Quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("Quest Name");

                    final Player player = (Player) context.getSender();

                    final String result = main.getQuestPlayerManager().acceptQuest(player, quest, true, true);
                    if (!result.equals("accepted")) {
                        audience.sendMessage(miniMessage.parse(
                                result
                        ));
                    } else {
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.quest-successfully-accepted", player, quest)
                        ));
                        if (!quest.getQuestDescription().isBlank()) {
                            audience.sendMessage(miniMessage.parse(
                                    main.getLanguageManager().getString("chat.quest-description", player, quest)
                            ));
                        } else {
                            audience.sendMessage(miniMessage.parse(
                                    main.getLanguageManager().getString("chat.missing-quest-description", player)
                            ));
                        }
                    }
                }));


        manager.command(builder.literal("questPoints")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Starts a Quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Player player = (Player) context.getSender();
                    QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

                    if (questPlayer != null) {
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.questpoints.query", player, questPlayer)
                        ));
                    } else {
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.questpoints.none", player)
                        ));
                    }
                }));
    }


    public void constructGUICommands() {
        manager.command(builder
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Opens NotQuests GUI.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();

                    String[] guiSetup = {
                            "zxxxxxxxx",
                            "x0123456x",
                            "x789abcdx",
                            "xefghijkx",
                            "xlmnopqrx",
                            "xxxxxxxxx"
                    };
                    InventoryGui gui = new InventoryGui(main.getMain(), player, convert(main.getLanguageManager().getString("gui.main.title", player)), guiSetup);
                    gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                    gui.addElement(new StaticGuiElement('9',
                            new ItemStack(Material.CHEST),
                            0,
                            click -> {
                                player.chat("/notquests take");
                                return true;
                            },
                            convert(main.getLanguageManager().getString("gui.main.button.takequest.name", player))

                    ));
                    gui.addElement(new StaticGuiElement('b',
                            new ItemStack(Material.REDSTONE_BLOCK),
                            0,
                            click -> {
                                player.chat("/notquests abort");
                                return true;
                            },
                            convert(main.getLanguageManager().getString("gui.main.button.abortquest.name", player))
                    ));
                    /*gui.addElement(new StaticGuiElement('c',
                            new ItemStack(Material.SPYGLASS),
                            0,
                            click -> {
                                player.chat("/notquests preview");
                                return true;
                            },
                            convert(main.getLanguageManager().getString("gui.main.button.previewquest.name", player))
                    ));*/

                    gui.addElement(new StaticGuiElement('o',
                            new ItemStack(Material.LADDER),
                            0,
                            click -> {
                                player.chat("/notquests activeQuests");
                                return true;
                            },
                            convert(main.getLanguageManager().getString("gui.main.button.activequests.name", player))
                    ));
                    QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    if (questPlayer != null) {
                        gui.addElement(new StaticGuiElement('z',
                                new ItemStack(Material.SUNFLOWER),
                                0,
                                click -> {
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.main.button.questpoints.name", player, questPlayer))
                        ));
                    } else {
                        gui.addElement(new StaticGuiElement('z',
                                new ItemStack(Material.SUNFLOWER),
                                0,
                                click -> {
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.main.button.questpoints.name", player).replace("%QUESTPOINTS%", "??"))
                        ));
                    }

                    gui.show(player);
                }));

        manager.command(builder.literal("take")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Starts a Quest.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();

                    String[] guiSetup = {
                            "zxxxxxxxx",
                            "xgggggggx",
                            "xgggggggx",
                            "xgggggggx",
                            "xgggggggx",
                            "pxxxxxxxn"
                    };
                    InventoryGui gui = new InventoryGui(main.getMain(), player, convert(main.getLanguageManager().getString("gui.takeQuestChoose.title", player)), guiSetup);
                    gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                    int count = 0;
                    GuiElementGroup group = new GuiElementGroup('g');

                    for (final Quest quest : main.getQuestManager().getAllQuests()) {
                        if (quest.isTakeEnabled()) {
                            final ItemStack materialToUse = quest.getTakeItem();

                            if (main.getConfiguration().showQuestItemAmount) {
                                count++;
                            }


                            String displayName = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.name-if-not-accepted", player, quest);

                            QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

                            if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                                displayName = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.name-if-accepted", player, questPlayer, quest);
                            }
                            String description = "";
                            if (!quest.getQuestDescription().isBlank()) {
                                description = quest.getQuestDescription(main.getConfiguration().guiQuestDescriptionMaxLineLength);

                                /*description = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.questDescriptionPrefix", player, quest)
                                        + quest.getQuestDescription(main.getConfiguration().guiQuestDescriptionMaxLineLength
                                );*/
                            }

                            group.addElement(new StaticGuiElement('e',
                                    materialToUse,
                                    count,
                                    click -> {
                                        player.chat("/notquests preview " + quest.getQuestName());
                                        return true;
                                    },
                                    convert(displayName),
                                    convert(description)//,
                                    //convert(main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.bottomText", player))
                            ));

                        }
                    }

                    gui.addElement(group);
                    // Previous page
                    gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));
                    // Next page
                    gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));

                    gui.show(player);
                }));

        manager.command(builder.literal("activeQuests")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Shows your active Quests.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

                    main.getGuiManager().showActiveQuests(questPlayer, player);
                }));


        manager.command(builder.literal("abort")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Aborts an active Quest.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    final Audience audience = main.adventure().player(player);
                    if (questPlayer != null) {
                        String[] guiSetup = {
                                "zxxxxxxxx",
                                "xgggggggx",
                                "xgggggggx",
                                "xgggggggx",
                                "xgggggggx",
                                "pxxxxxxxn"
                        };
                        InventoryGui gui = new InventoryGui(main.getMain(), player, convert(main.getLanguageManager().getString("gui.abortQuestChoose.title", player)), guiSetup);
                        gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                        GuiElementGroup group = new GuiElementGroup('g');

                        int count = 0;

                        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                            final ItemStack materialToUse;
                            if (!activeQuest.isCompleted()) {
                                materialToUse = activeQuest.getQuest().getTakeItem();
                            } else {
                                materialToUse = new ItemStack(Material.EMERALD_BLOCK);
                            }


                            if (main.getConfiguration().showQuestItemAmount) {
                                count++;
                            }

                            group.addElement(new StaticGuiElement('e',
                                    materialToUse,
                                    count,
                                    click -> {
                                        player.chat("/notquests abort " + activeQuest.getQuest().getQuestName());
                                        return true;
                                    },
                                    convert(main.getLanguageManager().getString("gui.abortQuestChoose.button.abortQuestPreview.name", player, activeQuest))
                            ));

                        }

                        gui.addElement(group);
                        // Previous page
                        gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));
                        // Next page
                        gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));

                        gui.show(player);
                    } else {
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.no-quests-accepted", player)
                        ));
                    }
                }));


        manager.command(builder.literal("preview")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Shows a Preview for a Quest.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    String[] guiSetup = {
                            "zxxxxxxxx",
                            "xgggggggx",
                            "xgggggggx",
                            "xgggggggx",
                            "xgggggggx",
                            "pxxxxxxxn"
                    };
                    InventoryGui gui = new InventoryGui(main.getMain(), player, convert(main.getLanguageManager().getString("gui.previewQuestChoose.title", player)), guiSetup);
                    gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                    int count = 0;
                    GuiElementGroup group = new GuiElementGroup('g');

                    for (final Quest quest : main.getQuestManager().getAllQuests()) {
                        if (quest.isTakeEnabled()) {
                            final ItemStack materialToUse = quest.getTakeItem();

                            if (main.getConfiguration().showQuestItemAmount) {
                                count++;
                            }
                            String displayName = quest.getQuestFinalName();

                            displayName = main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.questNamePrefix", player) + displayName;

                            if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                                displayName += main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.acceptedSuffix", player);
                            }
                            String description = "";
                            if (!quest.getQuestDescription().isBlank()) {
                                description = main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.questDescriptionPrefix", player)
                                        + quest.getQuestDescription(main.getConfiguration().guiQuestDescriptionMaxLineLength);
                            }

                            group.addElement(new StaticGuiElement('e',
                                    materialToUse,
                                    count,
                                    click -> {
                                        player.chat("/notquests preview " + quest.getQuestName());
                                        return true;
                                    },
                                    convert(displayName),
                                    convert(description),
                                    convert(main.getLanguageManager().getString("gui.previewQuestChoose.button.questPreview.bottomText", player))
                            ));

                        }
                    }

                    gui.addElement(group);
                    // Previous page
                    gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));
                    // Next page
                    gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));

                    gui.show(player);
                }));


        manager.command(builder.literal("abort")
                .senderType(Player.class)
                .argument(ActiveQuestSelector.of("Active Quest", main, null), ArgumentDescription.of("Name of the active Quest which should be aborted/failed"))
                .meta(CommandMeta.DESCRIPTION, "Aborts an active Quest")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final Audience audience = main.adventure().player(player);
                    QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                        final ActiveQuest activeQuest = context.get("Active Quest");

                        String[] guiSetup = {
                                "zxxxxxxxx",
                                "x0123456x",
                                "x789abcdx",
                                "xefghijkx",
                                "xlmnopqrx",
                                "xxxxxxxxx"
                        };
                        InventoryGui gui = new InventoryGui(main.getMain(), player, convert(main.getLanguageManager().getString("gui.abortQuest.title", player)), guiSetup);
                        gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this


                        gui.addElement(new StaticGuiElement('9',
                                new ItemStack(Material.GREEN_WOOL),
                                1,
                                click -> {
                                    questPlayer.failQuest(activeQuest);
                                    audience.sendMessage(miniMessage.parse(
                                            main.getLanguageManager().getString("chat.quest-aborted", player, activeQuest)
                                    ));
                                    gui.close();
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.abortQuest.button.confirmAbort.name", player, activeQuest))
                        ));
                        gui.addElement(new StaticGuiElement('b',
                                new ItemStack(Material.RED_WOOL),
                                1,
                                click -> {
                                    gui.close();
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.abortQuest.button.cancelAbort.name", player))
                        ));

                        gui.show(player);

                    } else {
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.no-quests-accepted", player)
                        ));
                    }
                }));


        manager.command(builder.literal("preview")
                .senderType(Player.class)
                .argument(QuestSelector.<CommandSender>newBuilder("Quest Name", main).takeEnabledOnly().build(), ArgumentDescription.of("Quest Name"))
                .meta(CommandMeta.DESCRIPTION, "Previews a Quest")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer((player.getUniqueId()));
                    final Quest quest = context.get("Quest Name");

                    String[] guiSetup = {
                            "zxxxxxxxx",
                            "x0123456x",
                            "x789abcdx",
                            "xefghijkx",
                            "xlmnopqrx",
                            "xxxxxxxxx"
                    };

                    InventoryGui gui = new InventoryGui(main.getMain(), player, convert(main.getLanguageManager().getString("gui.previewQuest.title", player, quest, questPlayer)), guiSetup);
                    gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                    if (main.getConfiguration().isGuiQuestPreviewDescription_enabled()) {
                        String description = main.getLanguageManager().getString("gui.previewQuest.button.description.empty", player, questPlayer);
                        if (!quest.getQuestDescription().isBlank()) {
                            description = quest.getQuestDescription(main.getConfiguration().guiQuestDescriptionMaxLineLength);
                        }
                        gui.addElement(new StaticGuiElement(main.getConfiguration().getGuiQuestPreviewDescription_slot(),
                                new ItemStack(Material.BOOKSHELF),
                                1,
                                click -> {
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.previewQuest.button.description.name", player, questPlayer)
                                        .replace("%QUESTDESCRIPTION%", description))
                        ));
                    }

                    if (main.getConfiguration().isGuiQuestPreviewRewards_enabled()) {
                        String rewards = main.getQuestManager().getQuestRewards(quest);
                        if (rewards.isBlank()) {
                            rewards = main.getLanguageManager().getString("gui.previewQuest.button.rewards.empty", player);
                        }
                        gui.addElement(new StaticGuiElement(main.getConfiguration().getGuiQuestPreviewRewards_slot(),
                                new ItemStack(Material.EMERALD),
                                1,
                                click -> {
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.previewQuest.button.rewards.name", player, quest, questPlayer)
                                        .replace("%QUESTREWARDS%", rewards))
                        ));
                    }

                    if (main.getConfiguration().isGuiQuestPreviewRequirements_enabled()) {
                        String requirements = main.getQuestManager().getQuestRequirements(quest);
                        if (requirements.isBlank()) {
                            requirements = main.getLanguageManager().getString("gui.previewQuest.button.requirements.empty", player, questPlayer);
                        }

                        gui.addElement(new StaticGuiElement(main.getConfiguration().getGuiQuestPreviewRequirements_slot(),
                                new ItemStack(Material.IRON_BARS),
                                1,
                                click -> {
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.previewQuest.button.requirements.name", player, quest, questPlayer)
                                        .replace("%QUESTREQUIREMENTS%", requirements))


                        ));
                    }


                    gui.addElement(new StaticGuiElement('g',
                            new ItemStack(Material.GREEN_WOOL),
                            1,
                            click -> {
                                player.chat("/notquests take " + quest.getQuestName());
                                gui.close();
                                return true;

                            },
                            convert(main.getLanguageManager().getString("gui.previewQuest.button.confirmTake.name", player, quest, questPlayer))

                    ));
                    gui.addElement(new StaticGuiElement('i',
                            new ItemStack(Material.RED_WOOL),
                            1,
                            click -> {
                                gui.close();
                                return true;

                            },
                            convert(main.getLanguageManager().getString("gui.previewQuest.button.cancelTake.name", player, quest, questPlayer))

                    ));

                    gui.show(player);
                }));


        manager.command(builder.literal("progress")
                .senderType(Player.class)
                .argument(ActiveQuestSelector.of("Active Quest", main, null), ArgumentDescription.of("Name of the active Quest of which you want to see the progress"))
                .meta(CommandMeta.DESCRIPTION, "Shows progress for an active Quest")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final Audience audience = main.adventure().player(player);
                    QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                        final ActiveQuest activeQuest = context.get("Active Quest");

                        String[] guiSetup = {
                                "zxxxxxxxx",
                                "xgggggggx",
                                "xgggggggx",
                                "xgggggggx",
                                "xgggggggx",
                                "pxxxxxxxn"
                        };

                        InventoryGui gui = new InventoryGui(main.getMain(), player, convert(main.getLanguageManager().getString("gui.progress.title", player, activeQuest, questPlayer)), guiSetup);
                        gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                        GuiElementGroup group = new GuiElementGroup('g');

                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {

                            final Material materialToUse = Material.PAPER;

                            int count = activeObjective.getObjectiveID();
                            if (!main.getConfiguration().showObjectiveItemAmount) {
                                count = 0;
                            }
                            if (activeObjective.isUnlocked()) {
                                String descriptionToDisplay = main.getLanguageManager().getString("gui.progress.button.unlockedObjective.description-empty", player);
                                if (!activeObjective.getObjective().getObjectiveDescription().isBlank()) {
                                    descriptionToDisplay = activeObjective.getObjective().getObjectiveDescription(main.getConfiguration().guiObjectiveDescriptionMaxLineLength);
                                }

                                group.addElement(new StaticGuiElement('e',
                                        new ItemStack(materialToUse),
                                        count,
                                        click -> {
                                            return true;
                                        },
                                        convert(
                                                main.getLanguageManager().getString("gui.progress.button.unlockedObjective.name", player, activeObjective, questPlayer)
                                                        .replace("%OBJECTIVEDESCRIPTION%", descriptionToDisplay)
                                                        .replace("%ACTIVEOBJECTIVEDESCRIPTION%", main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), false, player))
                                        )
                                ));
                            } else {
                                group.addElement(new StaticGuiElement('e',
                                        new ItemStack(materialToUse),
                                        activeObjective.getObjectiveID(),
                                        click -> {
                                            return true;
                                        },
                                        convert(main.getLanguageManager().getString("gui.progress.button.lockedObjective.name", player, activeObjective))
                                ));
                            }
                        }

                        for (final ActiveObjective activeObjective : activeQuest.getCompletedObjectives()) {

                            final Material materialToUse = Material.FILLED_MAP;

                            int count = activeObjective.getObjectiveID();
                            if (!main.getConfiguration().showObjectiveItemAmount) {
                                count = 0;
                            }

                            String descriptionToDisplay = main.getLanguageManager().getString("gui.progress.button.completedObjective.description-empty", player, activeObjective, questPlayer);
                            if (!activeObjective.getObjective().getObjectiveDescription().isBlank()) {
                                descriptionToDisplay = activeObjective.getObjective().getObjectiveDescription(main.getConfiguration().guiObjectiveDescriptionMaxLineLength);
                            }

                            group.addElement(new StaticGuiElement('e',
                                    new ItemStack(materialToUse),
                                    count,
                                    click -> {
                                        return true;
                                    },
                                    convert(
                                            main.getLanguageManager().getString("gui.progress.button.completedObjective.name", player, activeObjective, questPlayer)
                                                    .replace("%OBJECTIVEDESCRIPTION%", descriptionToDisplay)
                                                    .replace("%COMPLETEDOBJECTIVEDESCRIPTION%", main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), true, player))
                                    )
                            ));
                        }

                        gui.addElement(group);
                        // Previous page
                        gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, convert(main.getLanguageManager().getString("gui.progress.button.previousPage.name", player))));
                        // Next page
                        gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, convert(main.getLanguageManager().getString("gui.progress.button.nextPage.name", player))));
                        gui.show(player);

                    } else {
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.no-quests-accepted", player)
                        ));
                    }
                }));
    }

    public void constructTextCommands() {
        manager.command(builder
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Opens NotQuests GUI.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    audience.sendMessage(Component.empty());
                    audience.sendMessage(firstLevelCommands);
                }));


        manager.command(builder.literal("take")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Starts a Quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    audience.sendMessage(miniMessage.parse(
                            "<RED>Please specify the <AQUA>name of the quest</AQUA> you wish to take.\n"
                                    + "<YELLOW>/nquests <GOLD>take <DARK_AQUA>[Quest Name]"
                    ));
                }));


        manager.command(builder.literal("activeQuests")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Shows your active Quests.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    final Audience audience = main.adventure().player(player);
                    if (questPlayer != null) {
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.active-quests-label", player)
                        ));
                        int counter = 1;
                        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            audience.sendMessage(miniMessage.parse(
                                    "<GREEN>" + counter + ". <YELLOW>" + activeQuest.getQuest().getQuestFinalName()
                            ));
                            counter += 1;
                        }

                    } else {
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.no-quests-accepted", player)
                        ));
                    }
                }));


        manager.command(builder.literal("abort")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Aborts an active Quest.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    final Audience audience = main.adventure().player(player);
                    if (questPlayer != null) {
                        audience.sendMessage(miniMessage.parse(
                                "<RED>Please specify the <AQUA>name of the quest</AQUA> you wish to abort (fail).\n"
                                        + "<YELLOW>/nquests <GOLD>abort <DARK_AQUA>[Quest Name]"
                        ));
                    } else {
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.no-quests-accepted", player)
                        ));
                    }
                }));

        manager.command(builder.literal("preview")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Shows a Preview for a Quest.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final Audience audience = main.adventure().player(player);

                    audience.sendMessage(miniMessage.parse(
                            "<RED>Please specify the <AQUA>name of the quest</AQUA> you wish to preview.\n"
                                    + "<YELLOW>/nquests <GOLD>preview <DARK_AQUA>[Quest Name]"
                    ));
                }));


        manager.command(builder.literal("abort")
                .senderType(Player.class)
                .argument(ActiveQuestSelector.of("Active Quest", main, null), ArgumentDescription.of("Name of the active Quest which should be aborted/failed"))
                .meta(CommandMeta.DESCRIPTION, "Aborts an active Quest")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final Audience audience = main.adventure().player(player);
                    QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                        final ActiveQuest activeQuest = context.get("Active Quest");

                        questPlayer.failQuest(activeQuest);
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.quest-aborted", player, activeQuest)
                        ));

                    } else {
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.no-quests-accepted", player)
                        ));
                    }
                }));


        manager.command(builder.literal("preview")
                .senderType(Player.class)
                .argument(QuestSelector.<CommandSender>newBuilder("Quest Name", main).takeEnabledOnly().build(), ArgumentDescription.of("Quest Name"))
                .meta(CommandMeta.DESCRIPTION, "Previews a Quest")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();

                    final Quest quest = context.get("Quest Name");

                    main.getQuestManager().sendSingleQuestPreview(player, quest);
                }));

        manager.command(builder.literal("progress")
                .senderType(Player.class)
                .argument(ActiveQuestSelector.of("Active Quest", main, null), ArgumentDescription.of("Name of the active Quest of which you want to see the progress"))
                .meta(CommandMeta.DESCRIPTION, "Shows progress for an active Quest")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final Audience audience = main.adventure().player(player);
                    QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                        final ActiveQuest activeQuest = context.get("Active Quest");

                        audience.sendMessage(miniMessage.parse(
                                "<GREEN>Completed Objectives for Quest <AQUA>" + activeQuest.getQuest().getQuestFinalName() + "<YELLOW>:"
                        ));
                        main.getQuestManager().sendCompletedObjectivesAndProgress(player, activeQuest);
                        audience.sendMessage(miniMessage.parse(
                                "<GREEN>Active Objectives for Quest <AQUA>" + activeQuest.getQuest().getQuestFinalName() + "<YELLOW>:"
                        ));
                        main.getQuestManager().sendActiveObjectivesAndProgress(player, activeQuest);

                    } else {
                        audience.sendMessage(miniMessage.parse(
                                main.getLanguageManager().getString("chat.no-quests-accepted", player)
                        ));
                    }
                }));

    }
}
