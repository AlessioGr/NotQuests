package rocks.gravili.notquests.paper.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.ActiveQuestSelector;
import rocks.gravili.notquests.paper.commands.arguments.QuestSelector;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.UUID;


public class UserCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> builder;
    private final Component firstLevelCommands;

    private final ItemStack chest, abort, coins, books, info;


    public UserCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        this.main = main;
        this.manager = manager;
        this.builder = builder;

        chest = new ItemStack(Material.PLAYER_HEAD);
        {
            SkullMeta meta = (SkullMeta) chest.getItemMeta();
            PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
            prof.getProperties().add(new ProfileProperty("textures",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTcxNDUxNmU2NTY1YjgxMmZmNWIzOWVhMzljZDI4N2FmZWM4ZmNjMDZkOGYzMDUyMWNjZDhmMWI0Y2JmZGM2YiJ9fX0="
            ));
            meta.setPlayerProfile(prof);
            chest.setItemMeta(meta);
        }

        abort = new ItemStack(Material.PLAYER_HEAD);
        {
            SkullMeta meta = (SkullMeta) abort.getItemMeta();
            PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
            prof.getProperties().add(new ProfileProperty("textures",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQwYTE0MjA4NDRjZTIzN2E0NWQyZTdlNTQ0ZDEzNTg0MWU5ZjgyZDA5ZTIwMzI2N2NmODg5NmM4NTE1ZTM2MCJ9fX0="
            ));
            meta.setPlayerProfile(prof);
            abort.setItemMeta(meta);
        }

        coins = new ItemStack(Material.PLAYER_HEAD);
        {
            SkullMeta meta = (SkullMeta) coins.getItemMeta();
            PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
            prof.getProperties().add(new ProfileProperty("textures",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODM4MWM1MjlkNTJlMDNjZDc0YzNiZjM4YmI2YmEzZmRlMTMzN2FlOWJmNTAzMzJmYWE4ODllMGEyOGU4MDgxZiJ9fX0="
            ));
            meta.setPlayerProfile(prof);
            coins.setItemMeta(meta);
        }

        books = new ItemStack(Material.PLAYER_HEAD);
        {
            SkullMeta meta = (SkullMeta) books.getItemMeta();
            PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
            prof.getProperties().add(new ProfileProperty("textures",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWVlOGQ2ZjVjYjdhMzVhNGRkYmRhNDZmMDQ3ODkxNWRkOWViYmNlZjkyNGViOGNhMjg4ZTkxZDE5YzhjYiJ9fX0="
            ));
            meta.setPlayerProfile(prof);
            books.setItemMeta(meta);
        }

        info = new ItemStack(Material.PLAYER_HEAD);
        {
            SkullMeta meta = (SkullMeta) info.getItemMeta();
            PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
            prof.getProperties().add(new ProfileProperty("textures",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Q5MWY1MTI2NmVkZGM2MjA3ZjEyYWU4ZDdhNDljNWRiMDQxNWFkYTA0ZGFiOTJiYjc2ODZhZmRiMTdmNGQ0ZSJ9fX0="
            ));
            meta.setPlayerProfile(prof);
            info.setItemMeta(meta);
        }

        firstLevelCommands = Component.text("NotQuests Player Commands:", NamedTextColor.BLUE, TextDecoration.BOLD)
                .append(Component.newline())
                .append(main.parse("<YELLOW>/nquests <GOLD>take <DARK_AQUA>[Quest Name]").clickEvent(ClickEvent.suggestCommand("/nquests take ")).hoverEvent(HoverEvent.showText(Component.text("Takes/Starts a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(main.parse("<YELLOW>/nquests <GOLD>abort <DARK_AQUA>[Quest Name]").clickEvent(ClickEvent.suggestCommand("/nquests abort ")).hoverEvent(HoverEvent.showText(Component.text("Fails a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(main.parse("<YELLOW>/nquests <GOLD>preview <DARK_AQUA>[Quest Name]").clickEvent(ClickEvent.suggestCommand("/nquests preview ")).hoverEvent(HoverEvent.showText(Component.text("Shows more information about a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(main.parse("<YELLOW>/nquests <GOLD>activeQuests").clickEvent(ClickEvent.runCommand("/nquests activeQuests")).hoverEvent(HoverEvent.showText(Component.text("Shows all your active Quests", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(main.parse("<YELLOW>/nquests <GOLD>progress <DARK_AQUA>[Quest Name]").clickEvent(ClickEvent.suggestCommand("/nquests progress ")).hoverEvent(HoverEvent.showText(Component.text("Shows the progress of an active Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(main.parse("<YELLOW>/nquests <GOLD>questPoints").clickEvent(ClickEvent.runCommand("/nquests questPoints")).hoverEvent(HoverEvent.showText(Component.text("Shows how many Quest Points you have", NamedTextColor.GREEN))))
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
        //main.getLogManager().info("Old: " + old);
        //main.getLogManager().info("New: " + main.getUtilManager().miniMessageToLegacyWithSpigotRGB(old));

        return main.getUtilManager().miniMessageToLegacyWithSpigotRGB(old);
        //return main.getUtilManager().miniMessageToLegacyWithSpigotRGB(old);
    }

    public void constructCommands() {
        manager.command(builder.literal("take")
                .senderType(Player.class)
                .argument(QuestSelector.<CommandSender>newBuilder("Quest Name", main).takeEnabledOnly().build(), ArgumentDescription.of("Quest Name"))
                .meta(CommandMeta.DESCRIPTION, "Starts a Quest.")
                .handler((context) -> {

                    final Quest quest = context.get("Quest Name");

                    final Player player = (Player) context.getSender();

                    final String result = main.getQuestPlayerManager().acceptQuest(player, quest, true, true);
                    if (!result.equals("accepted")) {
                        main.sendMessage(context.getSender(), result);
                    }
                }));


        manager.command(builder.literal("questPoints")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Starts a Quest.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

                    if (questPlayer != null) {
                        context.getSender().sendMessage(main.parse(
                                main.getLanguageManager().getString("chat.questpoints.query", player, questPlayer)
                        ));
                    } else {
                        context.getSender().sendMessage(main.parse(
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




                    gui.addElement(new StaticGuiElement('8',
                            chest,
                            0,
                            click -> {
                                player.chat("/notquests take");
                                return true;
                            },
                            convert(main.getLanguageManager().getString("gui.main.button.takequest.text", player))

                    ));

                    gui.addElement(new StaticGuiElement('a',
                            abort,
                            0,
                            click -> {
                                player.chat("/notquests abort");
                                return true;
                            },
                            convert(main.getLanguageManager().getString("gui.main.button.abortquest.text", player))
                    ));
                    gui.addElement(new StaticGuiElement('c',
                            info,
                            0,
                            click -> {
                                player.chat("/notquests preview");
                                return true;
                            },
                            convert(main.getLanguageManager().getString("gui.main.button.previewquest.text", player))
                    ));

                    gui.addElement(new StaticGuiElement('o',
                            books,
                            0,
                            click -> {
                                player.chat("/notquests activeQuests");
                                return true;
                            },
                            convert(main.getLanguageManager().getString("gui.main.button.activequests.text", player))
                    ));
                    QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    if (questPlayer != null) {
                        gui.addElement(new StaticGuiElement('z',
                                coins,
                                0,
                                click -> {
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.main.button.questpoints.text", player, questPlayer))
                        ));
                    } else {
                        gui.addElement(new StaticGuiElement('z',
                                coins,
                                0,
                                click -> {
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.main.button.questpoints.text", player).replace("%QUESTPOINTS%", "??"))
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

                            String displayName = quest.getQuestFinalName();

                            displayName = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.questNamePrefix", player, quest) + displayName;

                            QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

                            if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                                displayName += main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.acceptedSuffix", player, quest);
                            }
                            String description = "";
                            if (!quest.getQuestDescription().isBlank()) {

                                description = main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.questDescriptionPrefix", player, quest)
                                        + quest.getQuestDescription(main.getConfiguration().guiQuestDescriptionMaxLineLength
                                );
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
                                    convert(main.getLanguageManager().getString("gui.takeQuestChoose.button.questPreview.bottomText", player))
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
                                    convert(main.getLanguageManager().getString("gui.abortQuestChoose.button.abortQuestPreview.text", player, activeQuest))
                            ));

                        }

                        gui.addElement(group);
                        // Previous page
                        gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));
                        // Next page
                        gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));

                        gui.show(player);
                    } else {
                        context.getSender().sendMessage(main.parse(
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
                                    main.sendMessage(context.getSender(), main.getLanguageManager().getString("chat.quest-aborted", player, activeQuest));

                                    gui.close();
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.abortQuest.button.confirmAbort.text", player, activeQuest))
                        ));
                        gui.addElement(new StaticGuiElement('b',
                                new ItemStack(Material.RED_WOOL),
                                1,
                                click -> {
                                    gui.close();
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.abortQuest.button.cancelAbort.text", player))
                        ));

                        gui.show(player);

                    } else {
                        context.getSender().sendMessage(main.parse(
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
                                convert(main.getLanguageManager().getString("gui.previewQuest.button.description.text", player, questPlayer)
                                        .replace("%QUESTDESCRIPTION%", description))
                        ));
                    }

                    if (main.getConfiguration().isGuiQuestPreviewRewards_enabled()) {
                        String rewards = main.getQuestManager().getQuestRewards(quest, player);
                        if (rewards.isBlank()) {
                            rewards = main.getLanguageManager().getString("gui.previewQuest.button.rewards.empty", player);
                        }
                        gui.addElement(new StaticGuiElement(main.getConfiguration().getGuiQuestPreviewRewards_slot(),
                                new ItemStack(Material.EMERALD),
                                1,
                                click -> {
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.previewQuest.button.rewards.text", player, quest, questPlayer)
                                        .replace("%QUESTREWARDS%", rewards))
                        ));
                    }

                    if (main.getConfiguration().isGuiQuestPreviewRequirements_enabled()) {
                        String requirements = main.getQuestManager().getQuestRequirements(quest, player);
                        if (requirements.isBlank()) {
                            requirements = main.getLanguageManager().getString("gui.previewQuest.button.requirements.empty", player, questPlayer);
                        }

                        gui.addElement(new StaticGuiElement(main.getConfiguration().getGuiQuestPreviewRequirements_slot(),
                                new ItemStack(Material.IRON_BARS),
                                1,
                                click -> {
                                    return true;
                                },
                                convert(main.getLanguageManager().getString("gui.previewQuest.button.requirements.text", player, quest, questPlayer)
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
                            convert(main.getLanguageManager().getString("gui.previewQuest.button.confirmTake.text", player, quest, questPlayer))

                    ));
                    gui.addElement(new StaticGuiElement('i',
                            new ItemStack(Material.RED_WOOL),
                            1,
                            click -> {
                                gui.close();
                                return true;

                            },
                            convert(main.getLanguageManager().getString("gui.previewQuest.button.cancelTake.text", player, quest, questPlayer))

                    ));

                    gui.show(player);
                }));


        manager.command(builder.literal("progress")
                .senderType(Player.class)
                .argument(ActiveQuestSelector.of("Active Quest", main, null), ArgumentDescription.of("Name of the active Quest of which you want to see the progress"))
                .meta(CommandMeta.DESCRIPTION, "Shows progress for an active Quest")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
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
                                                main.getLanguageManager().getString("gui.progress.button.unlockedObjective.text", player, activeObjective, questPlayer)
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
                                        convert(main.getLanguageManager().getString("gui.progress.button.lockedObjective.text", player, activeObjective))
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
                                            main.getLanguageManager().getString("gui.progress.button.completedObjective.text", player, activeObjective, questPlayer)
                                                    .replace("%OBJECTIVEDESCRIPTION%", descriptionToDisplay)
                                                    .replace("%COMPLETEDOBJECTIVEDESCRIPTION%", main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), true, player))
                                    )
                            ));
                        }

                        gui.addElement(group);
                        // Previous page
                        gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, convert(main.getLanguageManager().getString("gui.progress.button.previousPage.text", player))));
                        // Next page
                        gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, convert(main.getLanguageManager().getString("gui.progress.button.nextPage.text", player))));
                        gui.show(player);

                    } else {
                        context.getSender().sendMessage(main.parse(
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
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(firstLevelCommands);
                }));


        manager.command(builder.literal("take")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Starts a Quest.")
                .handler((context) -> {
                    context.getSender().sendMessage(main.parse(
                            "<RED>Please specify the <highlight>name of the quest</highlight> you wish to take.\n"
                                    + "<YELLOW>/nquests <GOLD>take <DARK_AQUA>[Quest Name]"
                    ));
                }));


        manager.command(builder.literal("activeQuests")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Shows your active Quests.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());if (questPlayer != null) {
                        context.getSender().sendMessage(main.parse(
                                main.getLanguageManager().getString("chat.active-quests-label", player)
                        ));
                        int counter = 1;
                        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            context.getSender().sendMessage(main.parse(
                                    "<GREEN>" + counter + ". <YELLOW>" + activeQuest.getQuest().getQuestFinalName()
                            ));
                            counter += 1;
                        }

                    } else {
                        context.getSender().sendMessage(main.parse(
                                main.getLanguageManager().getString("chat.no-quests-accepted", player)
                        ));
                    }
                }));


        manager.command(builder.literal("abort")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Aborts an active Quest.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());if (questPlayer != null) {
                        context.getSender().sendMessage(main.parse(
                                "<RED>Please specify the <highlight>name of the quest</highlight> you wish to abort (fail).\n"
                                        + "<YELLOW>/nquests <GOLD>abort <DARK_AQUA>[Quest Name]"
                        ));
                    } else {
                        context.getSender().sendMessage(main.parse(
                                main.getLanguageManager().getString("chat.no-quests-accepted", player)
                        ));
                    }
                }));

        manager.command(builder.literal("preview")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Shows a Preview for a Quest.")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    context.getSender().sendMessage(main.parse(
                            "<RED>Please specify the <highlight>name of the quest</highlight> you wish to preview.\n"
                                    + "<YELLOW>/nquests <GOLD>preview <DARK_AQUA>[Quest Name]"
                    ));
                }));


        manager.command(builder.literal("abort")
                .senderType(Player.class)
                .argument(ActiveQuestSelector.of("Active Quest", main, null), ArgumentDescription.of("Name of the active Quest which should be aborted/failed"))
                .meta(CommandMeta.DESCRIPTION, "Aborts an active Quest")
                .handler((context) -> {
                    final Player player = (Player) context.getSender();
                    QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                        final ActiveQuest activeQuest = context.get("Active Quest");

                        questPlayer.failQuest(activeQuest);
                        main.sendMessage(context.getSender(), main.getLanguageManager().getString("chat.quest-aborted", player, activeQuest));


                    } else {
                        context.getSender().sendMessage(main.parse(
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
                    QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                    if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                        final ActiveQuest activeQuest = context.get("Active Quest");

                        context.getSender().sendMessage(main.parse(
                                "<GREEN>Completed Objectives for Quest <highlight>" + activeQuest.getQuest().getQuestFinalName() + "<YELLOW>:"
                        ));
                        main.getQuestManager().sendCompletedObjectivesAndProgress(player, activeQuest);
                        context.getSender().sendMessage(main.parse(
                                "<GREEN>Active Objectives for Quest <highlight>" + activeQuest.getQuest().getQuestFinalName() + "<YELLOW>:"
                        ));
                        main.getQuestManager().sendActiveObjectivesAndProgress(player, activeQuest);

                    } else {
                        context.getSender().sendMessage(main.parse(
                                main.getLanguageManager().getString("chat.no-quests-accepted", player)
                        ));
                    }
                }));

    }
}
