package notquests.notquests.Commands;

import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.md_5.bungee.api.chat.*;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.ActiveObjective;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Quest;
import notquests.notquests.Structs.QuestPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandNotQuests implements CommandExecutor, TabCompleter {
    private final NotQuests main;

    private final List<String> completions = new ArrayList<String>(); //makes a ArrayList
    private final List<String> standardPlayerCompletions = new ArrayList<String>(); //makes a ArrayList

    private final BaseComponent firstLevelCommands;

    public CommandNotQuests(NotQuests main) {
        this.main = main;

       /* firstLevelCommands = Component.text("NotQuests Player Commands:", NamedTextColor.BLUE, TextDecoration.BOLD)
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

                );*/ //Paper only

        firstLevelCommands = new TextComponent("§9§lNotQuests Player Commands:\n");


        TextComponent line1 = new TextComponent("§e/nquests §6take §3[Quest Name]\n");
        line1.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/nquests take "));
        line1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aTakes/Starts a Quest").create()));

        TextComponent line2 = new TextComponent("§e/nquests §6abort §3[Quest Name]\n");
        line2.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/nquests abort "));
        line2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aFails a Quest").create()));

        TextComponent line3 = new TextComponent("§e/nquests §6preview §3[Quest Name]\n");
        line3.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/nquests preview "));
        line3.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aShows more information about a Quest").create()));

        TextComponent line4 = new TextComponent("§e/nquests §6activeQuests\n");
        line4.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nquests activeQuests"));
        line4.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aShows all your active Quests").create()));

        TextComponent line5 = new TextComponent("§e/nquests §6progress §3[Quest Name]\n");
        line5.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/nquests progress"));
        line5.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aShows the progress of an active Quest").create()));

        TextComponent line6 = new TextComponent("§e/nquests §6questPoints");
        line6.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nquests questPoints"));
        line6.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aShows how many Quest Points you have").create()));

        firstLevelCommands.addExtra(line1);
        firstLevelCommands.addExtra(line2);
        firstLevelCommands.addExtra(line3);
        firstLevelCommands.addExtra(line4);
        firstLevelCommands.addExtra(line5);
        firstLevelCommands.addExtra(line6);


    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            boolean guiEnabled = main.getDataManager().getConfiguration().isUserCommandsUseGUI();
            //if (label.equalsIgnoreCase("qg")) {
            //guiEnabled = false;
            //player.sendMessage("§aOpening NotQuests GUI §1[BETA]");
            //}
            if (sender.hasPermission("notnot.quests.use")) {
                QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer((player.getUniqueId()));
                sender.sendMessage("");
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
                                    player.chat("/q take");

                                    return true; // returning true will cancel the click event and stop taking the item
                                },
                                main.getLanguageManager().getString("gui.main.button.takequest.text")

                        ));
                        gui.addElement(new StaticGuiElement('a',
                                new ItemStack(Material.REDSTONE_BLOCK),
                                0, // Display a number as the item count
                                click -> {
                                    player.chat("/q abort");
                                    gui.close();
                                    return true; // returning true will cancel the click event and stop taking the item
                                },
                                main.getLanguageManager().getString("gui.main.button.abortquest.text")
                        ));
                        gui.addElement(new StaticGuiElement('c',
                                new ItemStack(Material.SPYGLASS),
                                0, // Display a number as the item count
                                click -> {
                                    player.chat("/q preview");
                                    gui.close();
                                    return true; // returning true will cancel the click event and stop taking the item
                                },
                                main.getLanguageManager().getString("gui.main.button.previewquest.text")
                        ));

                        gui.addElement(new StaticGuiElement('o',
                                new ItemStack(Material.LADDER),
                                0, // Display a number as the item count
                                click -> {
                                    player.chat("/q activeQuests");
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
                        //only paper sender.sendMessage(firstLevelCommands);
                        sender.spigot().sendMessage(firstLevelCommands);
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
                                InventoryGui gui = new InventoryGui(main, player, "             §9Active Quests", guiSetup);
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

                                    String displayName = activeQuest.getQuest().getQuestName();
                                    if (!activeQuest.getQuest().getQuestDisplayName().equals("")) {
                                        displayName = activeQuest.getQuest().getQuestDisplayName();
                                    }
                                    group.addElement(new StaticGuiElement('e',
                                            new ItemStack(materialToUse),
                                            count, // Display a number as the item count
                                            click -> {
                                                player.chat("/q progress " + activeQuest.getQuest().getQuestName());
                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            "§b" + displayName + " §a[ACTIVE]",
                                            "§7Progress: §a" + activeQuest.getCompletedObjectives().size() + " §f/ " + activeQuest.getQuest().getObjectives().size(),
                                            "§fClick to see details!"


                                    ));

                                }


                                gui.addElement(group);

                                // Previous page
                                gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));

                                // Next page
                                gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));


                                gui.show(player);
                            } else {
                                sender.sendMessage("§eActive quests:");
                                int counter = 1;
                                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                    sender.sendMessage("§a" + counter + ". §e" + activeQuest.getQuest().getQuestName());
                                    counter += 1;
                                }
                            }

                        } else {
                            sender.sendMessage("§cSeems like you did not accept any active quests!");
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
                            InventoryGui gui = new InventoryGui(main, player, "§9Which Quest do you want to take?", guiSetup);
                            gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                            int count = 0;
                            GuiElementGroup group = new GuiElementGroup('g');

                            for (final Quest quest : main.getQuestManager().getAllQuests()) {
                                if (quest.isTakeEnabled()) {
                                    final Material materialToUse = Material.BOOK;


                                    count++;

                                    String displayName = quest.getQuestName();
                                    if (!quest.getQuestDisplayName().equals("")) {
                                        displayName = quest.getQuestDisplayName();
                                    }
                                    displayName = "§b" + displayName;

                                    if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                                        displayName += " §a[ACCEPTED]";
                                    }
                                    String description = "";
                                    if (!quest.getQuestDescription().equals("")) {
                                        description = "§8" + quest.getQuestDescription();
                                    }

                                    group.addElement(new StaticGuiElement('e',
                                            new ItemStack(materialToUse),
                                            count, // Display a number as the item count
                                            click -> {
                                                player.chat("/q preview " + quest.getQuestName());
                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            displayName,
                                            description,
                                            "§aClick to preview Quest!"


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
                                InventoryGui gui = new InventoryGui(main, player, "  §9Active Quests you can abort", guiSetup);
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

                                    String displayName = activeQuest.getQuest().getQuestName();
                                    if (!activeQuest.getQuest().getQuestDisplayName().equals("")) {
                                        displayName = activeQuest.getQuest().getQuestDisplayName();
                                    }
                                    group.addElement(new StaticGuiElement('e',
                                            new ItemStack(materialToUse),
                                            count, // Display a number as the item count
                                            click -> {
                                                player.chat("/q abort " + activeQuest.getQuest().getQuestName());
                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            "§b" + displayName + " §a[ACTIVE]",
                                            "§7Progress: §a" + activeQuest.getCompletedObjectives().size() + " §f/ " + activeQuest.getQuest().getObjectives().size(),
                                            "§cClick to abort Quest!"


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
                            sender.sendMessage("§cSeems like you did not accept any active quests!");
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
                            InventoryGui gui = new InventoryGui(main, player, "§9Choose Quest to preview", guiSetup);
                            gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                            int count = 0;
                            GuiElementGroup group = new GuiElementGroup('g');

                            for (final Quest quest : main.getQuestManager().getAllQuests()) {
                                if (quest.isTakeEnabled()) {
                                    final Material materialToUse = Material.BOOK;


                                    count++;

                                    String displayName = quest.getQuestName();
                                    if (!quest.getQuestDisplayName().equals("")) {
                                        displayName = quest.getQuestDisplayName();
                                    }
                                    displayName = "§b" + displayName;

                                    if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                                        displayName += " §a[ACCEPTED]";
                                    }
                                    String description = "";
                                    if (!quest.getQuestDescription().equals("")) {
                                        description = "§8" + quest.getQuestDescription();
                                    }

                                    group.addElement(new StaticGuiElement('e',
                                            new ItemStack(materialToUse),
                                            count, // Display a number as the item count
                                            click -> {
                                                player.chat("/q preview " + quest.getQuestName());
                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            displayName,
                                            description,
                                            "§aClick to preview Quest!"


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
                            sender.sendMessage(main.getLanguageManager().getString("chat.questpoints").replaceAll("%QUESTPOINTS%", "" + questPlayer.getQuestPoints()));
                        } else {
                            sender.sendMessage("§cSeems like you don't have any quest points!");
                        }
                    } else {
                        sender.sendMessage("§cWrong command usage!");
                    }
                } else if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("take")) {
                        final Quest quest = main.getQuestManager().getQuest(args[1]);
                        if (quest != null) {
                            if (quest.isTakeEnabled()) {
                                final String result = main.getQuestPlayerManager().acceptQuest(player, quest, true, true);
                                if (!result.equals("accepted")) {
                                    sender.sendMessage(result);
                                } else {
                                    sender.sendMessage(main.getLanguageManager().getString("chat.quest-successfully-accepted").replaceAll("%QUESTNAME%", quest.getQuestName()));
                                    if (!quest.getQuestDescription().equals("")) {
                                        sender.sendMessage("§eQuest description: §7" + quest.getQuestDescription());

                                    } else {
                                        sender.sendMessage(main.getLanguageManager().getString("chat.missing-quest-description"));
                                    }


                                }

                            } else {
                                if(main.getQuestManager().isPlayerCloseToCitizenOrArmorstandWithQuest(player, quest)){

                                    final String result = main.getQuestPlayerManager().acceptQuest(player, quest, true, true);
                                    if (!result.equals("accepted")) {
                                        sender.sendMessage(result);
                                    } else {
                                        sender.sendMessage(main.getLanguageManager().getString("chat.quest-successfully-accepted").replaceAll("%QUESTNAME%", quest.getQuestName()));
                                        if (!quest.getQuestDescription().equals("")) {
                                            sender.sendMessage("§eQuest description: §7" + quest.getQuestDescription());
                                        } else {
                                            sender.sendMessage(main.getLanguageManager().getString("chat.missing-quest-description"));
                                        }
                                    }
                                    return true;

                                }


                                sender.sendMessage("§cAccepting the quest §b" + quest.getQuestName() + " §cis disabled with the /nquests take command.");
                            }

                        } else {
                            sender.sendMessage("§cQuest does not exist!");
                        }
                    } else if (args[0].equalsIgnoreCase("abort")) {

                        if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                            final String activeQuestName = args[1];
                            AtomicBoolean failedSuccessfully = new AtomicBoolean(false);
                            final ArrayList<ActiveQuest> questsToFail = new ArrayList<ActiveQuest>();
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
                                InventoryGui gui = new InventoryGui(main, player, "         §cAbort Confirmation", guiSetup);
                                gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

                                final ActiveQuest activeQuest = questsToFail.get(0);

                                String displayName = activeQuest.getQuest().getQuestName();
                                if (!activeQuest.getQuest().getQuestDisplayName().equals("")) {
                                    displayName = activeQuest.getQuest().getQuestDisplayName();
                                }
                                String finalDisplayName = displayName;
                                gui.addElement(new StaticGuiElement('9',
                                        new ItemStack(Material.GREEN_WOOL),
                                        1, // Display a number as the item count
                                        click -> {
                                            questPlayer.failQuest(activeQuest);
                                            failedSuccessfully.set(true);
                                            questsToFail.clear();
                                            if (!failedSuccessfully.get()) {
                                                sender.sendMessage("§cError: §b" + finalDisplayName + " §cis not an active Quest!");
                                            }
                                            sender.sendMessage(main.getLanguageManager().getString("chat.quest-aborted").replaceAll("%QUESTNAME%", finalDisplayName));

                                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                            gui.close();
                                            return true; // returning true will cancel the click event and stop taking the item

                                        },
                                        "§b" + finalDisplayName + " §a[ACTIVE]",
                                        "§7Progress: §a" + activeQuest.getCompletedObjectives().size() + " §f/ " + activeQuest.getQuest().getObjectives().size(),
                                        "§cClick to abort the quest §b" + finalDisplayName + "§c!"


                                ));
                                gui.addElement(new StaticGuiElement('b',
                                        new ItemStack(Material.RED_WOOL),
                                        1, // Display a number as the item count
                                        click -> {
                                            gui.close();
                                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                            return true; // returning true will cancel the click event and stop taking the item

                                        },
                                        "§aClick to NOT abort this quest",
                                        "§aand cancel this action"


                                ));


                                gui.show(player);
                            } else {

                                for (final ActiveQuest activeQuest : questsToFail) {
                                    questPlayer.failQuest(activeQuest);
                                    failedSuccessfully.set(true);
                                    sender.sendMessage(main.getLanguageManager().getString("chat.quest-aborted").replaceAll("%QUESTNAME%", activeQuest.getQuest().getQuestName()));

                                }
                                questsToFail.clear();
                                if (!failedSuccessfully.get()) {
                                    sender.sendMessage("§cError: §b" + activeQuestName + " §cis not an active Quest!");
                                }
                            }

                        } else {
                            sender.sendMessage("§cYou don't seem to not have accepted any quests!");
                        }


                    } else if (args[0].equalsIgnoreCase("preview")) {
                        Quest quest = main.getQuestManager().getQuest(args[1]);
                        if (quest != null) {
                            if (quest.isTakeEnabled()) {
                                if (guiEnabled) {
                                    String[] guiSetup = {
                                            "zxxxxxxxx",
                                            "x0123456x",
                                            "x789abcdx",
                                            "xefghijkx",
                                            "xlmnopqrx",
                                            "xxxxxxxxx"
                                    };

                                    String displayName = quest.getQuestName();
                                    if (!quest.getQuestDisplayName().equals("")) {
                                        displayName = quest.getQuestDisplayName();
                                    }

                                    InventoryGui gui = new InventoryGui(main, player, "§9Preview for Quest §b" + displayName, guiSetup);
                                    gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this


                                    String description = "§8???";
                                    if (!quest.getQuestDescription().equals("")) {
                                        description = "§8" + quest.getQuestDescription();
                                    }
                                    gui.addElement(new StaticGuiElement('1',
                                            new ItemStack(Material.BOOKSHELF),
                                            1, // Display a number as the item count
                                            click -> {

                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            "§eDescription",
                                            description


                                    ));

                                    String requirements = main.getQuestManager().getQuestRequirements(quest);


                                    gui.addElement(new StaticGuiElement('5',
                                            new ItemStack(Material.IRON_BARS),
                                            1, // Display a number as the item count
                                            click -> {

                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            "§cRequirements",
                                            requirements


                                    ));

                                    String finalDisplayName = displayName;
                                    gui.addElement(new StaticGuiElement('g',
                                            new ItemStack(Material.GREEN_WOOL),
                                            1, // Display a number as the item count
                                            click -> {
                                                player.chat("/q take " + quest.getQuestName());

                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                gui.close();
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            "§b" + finalDisplayName,
                                            "§aClick to take the quest §b" + finalDisplayName + "§c!"


                                    ));
                                    gui.addElement(new StaticGuiElement('i',
                                            new ItemStack(Material.RED_WOOL),
                                            1, // Display a number as the item count
                                            click -> {
                                                gui.close();
                                                //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                return true; // returning true will cancel the click event and stop taking the item

                                            },
                                            "§cClick to NOT take this quest",
                                            "§cand cancel this action"


                                    ));


                                    gui.show(player);
                                } else {
                                    main.getQuestManager().sendSingleQuestPreview((Player) sender, quest);
                                }

                                return true;
                            } else {

                                if(main.getQuestManager().isPlayerCloseToCitizenOrArmorstandWithQuest(player, quest)){
                                    if (guiEnabled) {
                                        String[] guiSetup = {
                                                "zxxxxxxxx",
                                                "x0123456x",
                                                "x789abcdx",
                                                "xefghijkx",
                                                "xlmnopqrx",
                                                "xxxxxxxxx"
                                        };

                                        String displayName = quest.getQuestName();
                                        if (!quest.getQuestDisplayName().equals("")) {
                                            displayName = quest.getQuestDisplayName();
                                        }

                                        InventoryGui gui = new InventoryGui(main, player, "§9Preview for Quest §b" + displayName, guiSetup);
                                        gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this


                                        String description = "§8???";
                                        if (!quest.getQuestDescription().equals("")) {
                                            description = "§8" + quest.getQuestDescription();
                                        }
                                        gui.addElement(new StaticGuiElement('1',
                                                new ItemStack(Material.BOOKSHELF),
                                                1, // Display a number as the item count
                                                click -> {

                                                    return true; // returning true will cancel the click event and stop taking the item

                                                },
                                                "§eDescription",
                                                description


                                        ));

                                        String requirements = main.getQuestManager().getQuestRequirements(quest);


                                        gui.addElement(new StaticGuiElement('5',
                                                new ItemStack(Material.IRON_BARS),
                                                1, // Display a number as the item count
                                                click -> {

                                                    return true; // returning true will cancel the click event and stop taking the item

                                                },
                                                "§cRequirements",
                                                requirements


                                        ));

                                        String finalDisplayName = displayName;
                                        gui.addElement(new StaticGuiElement('g',
                                                new ItemStack(Material.GREEN_WOOL),
                                                1, // Display a number as the item count
                                                click -> {
                                                    player.chat("/q take " + quest.getQuestName());

                                                    //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                    gui.close();
                                                    return true; // returning true will cancel the click event and stop taking the item

                                                },
                                                "§b" + finalDisplayName,
                                                "§aClick to take the quest §b" + finalDisplayName + "§c!"


                                        ));
                                        gui.addElement(new StaticGuiElement('i',
                                                new ItemStack(Material.RED_WOOL),
                                                1, // Display a number as the item count
                                                click -> {
                                                    gui.close();
                                                    //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                    return true; // returning true will cancel the click event and stop taking the item

                                                },
                                                "§cClick to NOT take this quest",
                                                "§cand cancel this action"


                                        ));


                                        gui.show(player);
                                    } else {
                                        main.getQuestManager().sendSingleQuestPreview((Player) sender, quest);
                                    }
                                    return true;

                                }

                                sender.sendMessage("§cPreviewing the quest §b" + quest.getQuestName() + " §cis disabled with the /nquests take command.");
                            }
                        } else {
                            sender.sendMessage("§cQuest does not exist!");
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
                                    String displayName = requestedActiveQuest.getQuest().getQuestName();
                                    if (!requestedActiveQuest.getQuest().getQuestDisplayName().equals("")) {
                                        displayName = requestedActiveQuest.getQuest().getQuestDisplayName();
                                    }
                                    InventoryGui gui = new InventoryGui(main, player, "§9Details for Quest §b" + displayName, guiSetup);
                                    gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this


                                    int count = 0;
                                    GuiElementGroup group = new GuiElementGroup('g');

                                    for (final ActiveObjective activeObjective : requestedActiveQuest.getActiveObjectives()) {

                                        final Material materialToUse = Material.PAPER;

                                        count++;
                                        if (activeObjective.isUnlocked()) {
                                            String nameToDisplay = "";
                                            if (!activeObjective.getObjective().getObjectiveDisplayName().equals("")) {
                                                nameToDisplay = activeObjective.getObjective().getObjectiveDisplayName();
                                            } else {
                                                nameToDisplay = activeObjective.getObjective().getObjectiveType().toString();
                                            }
                                            String descriptionToDisplay = "";
                                            if (!activeObjective.getObjective().getObjectiveDescription().equals("")) {
                                                descriptionToDisplay = activeObjective.getObjective().getObjectiveDescription();
                                            }

                                            group.addElement(new StaticGuiElement('e',
                                                    new ItemStack(materialToUse),
                                                    activeObjective.getObjectiveID(), // Display a number as the item count
                                                    click -> {
                                                        //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                        return true; // returning true will cancel the click event and stop taking the item

                                                    },
                                                    "§e" + activeObjective.getObjectiveID() + ". §b" + nameToDisplay,
                                                    "§6§lACTIVE",
                                                    "§8" + descriptionToDisplay,
                                                    main.getQuestManager().getActiveObjectiveDescription(activeObjective),
                                                    "",
                                                    "§7Progress: §a" + activeObjective.getCurrentProgress() + " §f/ " + activeObjective.getProgressNeeded()


                                            ));
                                        } else {


                                            group.addElement(new StaticGuiElement('e',
                                                    new ItemStack(materialToUse),
                                                    activeObjective.getObjectiveID(), // Display a number as the item count
                                                    click -> {
                                                        //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                        return true; // returning true will cancel the click event and stop taking the item

                                                    },
                                                    "§e" + activeObjective.getObjectiveID() + ". §7§l[HIDDEN]",
                                                    "§eThis objective has not yet",
                                                    "§ebeen unlocked!"


                                            ));
                                        }


                                    }
                                    count++;
                                    for (final ActiveObjective activeObjective : requestedActiveQuest.getCompletedObjectives()) {

                                        final Material materialToUse = Material.FILLED_MAP;

                                        count++;


                                        String nameToDisplay = "";
                                        if (!activeObjective.getObjective().getObjectiveDisplayName().equals("")) {
                                            nameToDisplay = activeObjective.getObjective().getObjectiveDisplayName();
                                        } else {
                                            nameToDisplay = activeObjective.getObjective().getObjectiveType().toString();
                                        }
                                        String descriptionToDisplay = "";
                                        if (!activeObjective.getObjective().getObjectiveDescription().equals("")) {
                                            descriptionToDisplay = activeObjective.getObjective().getObjectiveDescription();
                                        }


                                        group.addElement(new StaticGuiElement('e',
                                                new ItemStack(materialToUse),
                                                activeObjective.getObjectiveID(), // Display a number as the item count
                                                click -> {
                                                    //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                                                    return true; // returning true will cancel the click event and stop taking the item

                                                },
                                                "§a§m" + activeObjective.getObjectiveID() + ". §2§m" + nameToDisplay,
                                                "§a§lCOMPLETED",
                                                "§8§m" + descriptionToDisplay,
                                                main.getQuestManager().getCompletedObjectiveDescription(activeObjective),
                                                "",
                                                "§7§mProgress: §a§m" + activeObjective.getCurrentProgress() + " §f§m/ " + activeObjective.getProgressNeeded()


                                        ));


                                    }


                                    gui.addElement(group);

                                    // Previous page
                                    gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));

                                    // Next page
                                    gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));


                                    gui.show(player);
                                } else {
                                    sender.sendMessage("§aCompleted Objectives for Quest §b" + requestedActiveQuest.getQuest().getQuestName() + "§e:");
                                    main.getQuestManager().sendCompletedObjectivesAndProgress(sender, requestedActiveQuest);
                                    sender.sendMessage("§eActive Objectives for Quest §b" + requestedActiveQuest.getQuest().getQuestName() + "§e:");
                                    main.getQuestManager().sendActiveObjectivesAndProgress(sender, requestedActiveQuest);
                                }

                            } else {
                                sender.sendMessage("§cQuest §b" + args[1] + " §cnot found or not active!");
                                sender.sendMessage("§cActive quests:");
                                int counter = 1;
                                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                    sender.sendMessage("§a" + counter + ". §e" + activeQuest.getQuest().getQuestName());
                                    counter += 1;
                                }
                            }
                        } else {
                            sender.sendMessage("§cSeems like you did not accept any active quests!");
                        }

                    } else {
                        sender.sendMessage("§cWrong command usage!");
                    }

                } else {
                    sender.sendMessage("§cToo many arguments!");
                }
            } else {
                sender.sendMessage("§cNo permission! Required permission node: §enotnot.quests.use");
            }
        } else {
            sender.sendMessage("§cOnly players can run this command! Try §b/notquestsadmin§c.");
        }

        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        completions.clear();
        standardPlayerCompletions.clear();
        main.getDataManager().partialCompletions.clear();

        if (sender instanceof Player) {
            if (sender.hasPermission("notnot.quests.use")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    standardPlayerCompletions.add(player.getName());
                }

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
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(((Player) sender).getUniqueId());
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
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(((Player) sender).getUniqueId());
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

