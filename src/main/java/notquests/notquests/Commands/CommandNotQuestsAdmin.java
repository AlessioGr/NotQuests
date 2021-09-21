package notquests.notquests.Commands;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import notquests.notquests.Commands.AdminCommands.ObjectivesAdminCommand;
import notquests.notquests.Commands.AdminCommands.QuestPointsAdminCommand;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.*;
import notquests.notquests.Structs.Objectives.Objective;
import notquests.notquests.Structs.Objectives.TriggerCommandObjective;
import notquests.notquests.Structs.Requirements.*;
import notquests.notquests.Structs.Rewards.CommandReward;
import notquests.notquests.Structs.Rewards.QuestPointsReward;
import notquests.notquests.Structs.Rewards.Reward;
import notquests.notquests.Structs.Triggers.Action;
import notquests.notquests.Structs.Triggers.Trigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;


public class CommandNotQuestsAdmin implements CommandExecutor, TabCompleter {
    final ArrayList<ActiveQuest> questsToFail;
    private final NotQuests main;
    private final QuestPointsAdminCommand questPointsAdminCommand;
    private final ObjectivesAdminCommand objectivesAdminCommand;

    private final SimpleDateFormat simpleDateFormat;
    private final Date resultDate;

    private final Component firstLevelCommands;


    public CommandNotQuestsAdmin(NotQuests main) {
        this.main = main;
        simpleDateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm");


        firstLevelCommands = Component.text("NotQuests §b§lv" + main.getDescription().getVersion() + " §9§lAdmin Commands:", NamedTextColor.BLUE, TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("/qadmin §6create §3[Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin create ")).hoverEvent(HoverEvent.showText(Component.text("Create a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6delete §3[Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin delete ")).hoverEvent(HoverEvent.showText(Component.text("Delete a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6edit §3[Quest Name] §3...", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin edit ")).hoverEvent(HoverEvent.showText(Component.text("Edit a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6actions §3...", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin actions")).hoverEvent(HoverEvent.showText(Component.text("Manage Actions", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6give §3[Player Name] [Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin give ")).hoverEvent(HoverEvent.showText(Component.text("Make another player accept a Quest", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6forcegive §3[Player Name] [Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin give ")).hoverEvent(HoverEvent.showText(Component.text("Same as give, but bypasses max accepts, cooldown & requirements", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6questPoints §3[Player Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin questPoints ")).hoverEvent(HoverEvent.showText(Component.text("Manages the Quest Points of another player", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6activeQuests §3[Player Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin activeQuests ")).hoverEvent(HoverEvent.showText(Component.text("Shows active quests of another player", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6completedQuests §3[Player Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin activeQuests ")).hoverEvent(HoverEvent.showText(Component.text("Shows active quests of another player", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6completedQuests §3[Player Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin completedQuests ")).hoverEvent(HoverEvent.showText(Component.text("Shows completed quests of another player", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6progress §3[Player Name] [Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin progress ")).hoverEvent(HoverEvent.showText(Component.text("Shows progress for a quest of another player", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6failQuest §3[Player Name] [Active Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin failQuest ")).hoverEvent(HoverEvent.showText(Component.text("Fails an active quest for set player", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6completeQuest §3[Player Name] [Active Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin completeQuest ")).hoverEvent(HoverEvent.showText(Component.text("Force-completes an active quest for set player no matter if the objectives are completed", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6listObjectiveTypes", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/qadmin listObjectiveTypes")).hoverEvent(HoverEvent.showText(Component.text("Shows you a list of all available Objective Types", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6listRewardTypes", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/qadmin listRewardTypes")).hoverEvent(HoverEvent.showText(Component.text("Shows you a list of all available Reward Types", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6listRequirementTypes", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/qadmin listRequirementTypes")).hoverEvent(HoverEvent.showText(Component.text("Shows you a list of all available Requirement Types", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6listAllQuests", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/qadmin listAllQuests")).hoverEvent(HoverEvent.showText(Component.text("Shows you a list of all created Quests", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6listPlaceholders", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/qadmin listPlaceholders")).hoverEvent(HoverEvent.showText(Component.text("Shows you a list of all available Placeholders which can be used in Trigger or Action commands", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6triggerObjective §3[triggerName] [playerName]", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/qadmin triggerObjective ")).hoverEvent(HoverEvent.showText(Component.text("This triggers the Trigger Command which is needed to complete a TriggerObjective (don't mistake it with Triggers & actions)", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6resetAndRemoveQuestForAllPlayers §3[Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/qadmin resetAndRemoveQuestForAllPlayers ")).hoverEvent(HoverEvent.showText(Component.text("Removes the quest from all players, removes it from completed quests, resets the accept cooldown and basically everything else", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6resetAndFailQuestForAllPlayers §3[Quest Name]", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/qadmin resetAndFailQuestForAllPlayers ")).hoverEvent(HoverEvent.showText(Component.text("Fails the quest from all players, removes it from completed quests, resets the accept cooldown and basically everything else", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6load", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/qadmin load ")).hoverEvent(HoverEvent.showText(Component.text("Loads from the NotQuests configuration file", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text("/qadmin §6save", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/qadmin load ")).hoverEvent(HoverEvent.showText(Component.text("Saves the NotQuests configuration file", NamedTextColor.GREEN))))
                .append(Component.newline()
                );


        questsToFail = new ArrayList<>();
        resultDate = new Date();

        questPointsAdminCommand = new QuestPointsAdminCommand(main);
        objectivesAdminCommand = new ObjectivesAdminCommand(main);
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (sender.hasPermission("notnot.quests.admin")) {
            sender.sendMessage("");
            if (args.length == 0) {
                sender.sendMessage(firstLevelCommands);

            } else if (args.length >= 1 && args[0].equalsIgnoreCase("questPoints")) {
                questPointsAdminCommand.handleQuestPointsAdminCommand(sender, args);
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("load") || args[0].equalsIgnoreCase("reload")) {
                    main.getDataManager().loadGeneralConfig();
                    sender.sendMessage("§aNotQuests general.yml configuration has been re-loaded. §7If you want to reload more, please use the ServerUtils plugin (available on spigot) or restart the server. This reload command is not finished yet and does not reload the quests file or the database.");
                }else if (args[0].equalsIgnoreCase("save")) {
                    main.getDataManager().saveData();
                    sender.sendMessage("§aNotQuests configuration and player data has been saved");
                } else if (args[0].equalsIgnoreCase("listobjectivetypes")) {
                    sender.sendMessage("§eObjective Types:");
                    sender.sendMessage("§bBreakBlocks");
                    sender.sendMessage("§bCollectItems");
                    sender.sendMessage("§bKillMobs");
                    sender.sendMessage("§bTriggerCommand");
                    sender.sendMessage("§bOtherQuest");
                    sender.sendMessage("§bConsumeItems");
                    sender.sendMessage("§bDeliverItems");
                    sender.sendMessage("§bTalkToNPC");
                    sender.sendMessage("§bEscortNPC");
                } else if (args[0].equalsIgnoreCase("listrewardtypes")) {
                    sender.sendMessage("§eReward Types:");
                    sender.sendMessage("§bConsoleCommand");
                    sender.sendMessage("§bQuestPoints");
                } else if (args[0].equalsIgnoreCase("resetAndRemoveQuestForAllPlayers")) {
                    sender.sendMessage("§cMissing 2. argument §3[Quest Name]§c. Specify the §bname of the quest§c which should be reset and removed.");
                    sender.sendMessage("§e/qadmin §6resetAndRemoveQuestForAllPlayers §3[Quest Name] §7| Resets & removes specified quest for all players");
                } else if (args[0].equalsIgnoreCase("resetAndFailQuestForAllPlayers")) {
                    sender.sendMessage("§cMissing 2. argument §3[Quest Name]§c. Specify the §bname of the quest§c which should be reset and failed.");
                    sender.sendMessage("§e/qadmin §6resetAndFailQuestForAllPlayers §3[Quest Name] §7| Resets & fails specified quest for all players");
                } else if (args[0].equalsIgnoreCase("listrequirementtypes")) {
                    sender.sendMessage("§eRequirement Types:");
                    sender.sendMessage("§bOtherQuest");
                    sender.sendMessage("§bQuestPoints");
                    sender.sendMessage("§bPermission");
                    sender.sendMessage("§bPlaceholder (WIP)");
                    sender.sendMessage("§bMoney (WIP)");
                    sender.sendMessage("§bKarma (WIP)");
                } else if (args[0].equalsIgnoreCase("listallquests")) {
                    int counter = 1;
                    sender.sendMessage("§eAll Quests:");
                    for (Quest quest : main.getQuestManager().getAllQuests()) {
                        sender.sendMessage("§a" + counter + ". §e" + quest.getQuestName());
                        counter += 1;
                    }
                } else if (args[0].equalsIgnoreCase("listPlaceholders")) {
                    sender.sendMessage("§eAll Placeholders (Case-sensitive):");
                    sender.sendMessage("§b{PLAYER} §7 - Name of the player");
                    sender.sendMessage("§b{PLAYERUUID} §7 - UUID of the player");
                    sender.sendMessage("§b{PLAYERX} §7 - X coordinates of the player");
                    sender.sendMessage("§b{PLAYERY} §7 - Y coordinates of the player");
                    sender.sendMessage("§b{PLAYERZ} §7 - Z coordinates of the player");
                    sender.sendMessage("§b{WORLD} §7 - World name of the player");
                    sender.sendMessage("§b{QUEST} §7 - Relevant Quest Name");
                } else if (args[0].equalsIgnoreCase("create")) {
                    sender.sendMessage("§cMissing 2. argument §3[Quest Name]§c. Specify the §bname of the quest§c you would like to create.");
                    sender.sendMessage("§e/qadmin §6create §3[Quest Name]");
                } else if (args[0].equalsIgnoreCase("delete")) {
                    sender.sendMessage("§cMissing 2. argument §3[Quest Name]§c. Specify the §bname of the quest§c you would like to delete.");
                    sender.sendMessage("§e/qadmin §6delete §3[Quest Name]");
                } else if (args[0].equalsIgnoreCase("edit")) {
                    sender.sendMessage("§cMissing 2. argument §3[Quest Name]§c. Specify the §bname of the quest§c you would like to edit.");
                    sender.sendMessage("§e/qadmin §6edit §3[Quest Name] §3...");
                } else if (args[0].equalsIgnoreCase("activeQuests")) {
                    sender.sendMessage("§cMissing 2. argument §3[Player Name]§c. Specify the §bname of the player§c to see their active quests.");
                    sender.sendMessage("§e/qadmin §6activeQuests §3[Player Name] §7| Shows active quests of another player");
                } else if (args[0].equalsIgnoreCase("completedQuests")) {
                    sender.sendMessage("§cMissing 2. argument §3[Player Name]§c. Specify the §bname of the player§c to see their completed quests.");
                    sender.sendMessage("§e/qadmin §6completedQuests §3[Player Name] §7| Shows completed quests of another player");
                } else if (args[0].equalsIgnoreCase("progress")) {
                    sender.sendMessage("§cMissing 2. argument §3[Player Name]§c. Specify the §bname of the player§c to see their quest progress.");
                    sender.sendMessage("§e/qadmin §6progress §3[Player Name] [Quest Name] §7| Shows progress for a quest of another player");
                } else if (args[0].equalsIgnoreCase("actions")) {
                    sender.sendMessage("§e/qadmin §6actions add §3[Action Name] <Console Command>");
                    sender.sendMessage("§e/qadmin §6actions edit §3[Action Name] ...");
                    sender.sendMessage("§e/qadmin §6actions list");
                } else if (args[0].equalsIgnoreCase("forcegive")) {
                    sender.sendMessage("§cMissing 2. argument §3[Player Name]§c. Specify the §bname of the player§c to whom you want to give the quest");
                    sender.sendMessage("§e/qadmin §6forcegive §3[Player Name] [Quest Name] §7| Bypasses max accepts, cooldown & requirements");
                } else if (args[0].equalsIgnoreCase("failQuest")) {
                    sender.sendMessage("§cMissing 2. argument §3[Player Name]§c. Specify the §bname of the player§c to fail their quests.");
                    sender.sendMessage("§e/qadmin §6failQuest §3[Player Name] [Active Quest Name] §7| Fails an active quest for set player");
                } else if (args[0].equalsIgnoreCase("completeQuest")) {
                    sender.sendMessage("§cMissing 2. argument §3[Player Name]§c. Specify the §bname of the player§c to complete their quests.");
                    sender.sendMessage("§e/qadmin §6completeQuest §3[Player Name] [Active Quest Name] §7| Force-completes an active quest for set player");
                } else {
                    sender.sendMessage("§cWrong command usage!");
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("actions")) {
                    if (args[1].equalsIgnoreCase("add")) {
                        sender.sendMessage("§cMissing 2. argument §3[Action Name]§c. Specify a §bunique name§c of your new Action.");
                        sender.sendMessage("§e/qadmin §6actions add §3[Action Name] <Console Command>");
                    } else if (args[1].equalsIgnoreCase("edit")) {
                        sender.sendMessage("§cMissing 2. argument §3[Action Name]§c. Specify a §baction name§c you wish to edit.");
                        sender.sendMessage("§e/qadmin §6actions edit §3[Action Name] setCommand <new Console Command>");
                        sender.sendMessage("§e/qadmin §6actions edit §3[Action Name] delete");
                    } else if (args[1].equalsIgnoreCase("list")) {
                        int counter = 1;
                        sender.sendMessage("§eAll Actions:");
                        for (final Action action : main.getQuestManager().getAllActions()) {
                            sender.sendMessage("§a" + counter + ". §e" + action.getActionName());
                            sender.sendMessage("§7--- Command: §f" + action.getConsoleCommand());
                            counter += 1;
                        }
                    } else {
                        sender.sendMessage("§cWrong command usage!");
                    }
                } else if (args[0].equalsIgnoreCase("resetAndRemoveQuestForAllPlayers")) {
                    final Quest quest = main.getQuestManager().getQuest(args[1]);
                    if (quest != null) {
                        for (final QuestPlayer questPlayer : main.getQuestPlayerManager().getQuestPlayers()) {
                            final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
                            for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                if (activeQuest.getQuest().equals(quest)) {
                                    activeQuestsToRemove.add(activeQuest);
                                    sender.sendMessage("§aRemoved the quest as an active quest for the player with the UUID §b" + questPlayer.getUUID() + " §aand name §b" + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName());


                                }
                            }

                            questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

                            final ArrayList<CompletedQuest> completedQuestsToRemove = new ArrayList<>();

                            for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                                if (completedQuest.getQuest().equals(quest)) {
                                    completedQuestsToRemove.add(completedQuest);
                                    sender.sendMessage("§aRemoved the quest as a completed quest for the player with the UUID §b" + questPlayer.getUUID() + " §aand name §b" + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName());
                                }

                            }

                            questPlayer.getCompletedQuests().removeAll(completedQuestsToRemove);
                        }
                        sender.sendMessage("§aOperation done!");
                    } else {
                        sender.sendMessage("§cError: the quest §b" + args[0] + " §cwas not found!");
                    }

                } else if (args[0].equalsIgnoreCase("resetAndFailQuestForAllPlayers")) {
                    final Quest quest = main.getQuestManager().getQuest(args[1]);
                    if (quest != null) {
                        for (final QuestPlayer questPlayer : main.getQuestPlayerManager().getQuestPlayers()) {
                            final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
                            for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                if (activeQuest.getQuest().equals(quest)) {
                                    activeQuestsToRemove.add(activeQuest);
                                }
                            }

                            for (final ActiveQuest activeQuest : activeQuestsToRemove) {
                                questPlayer.failQuest(activeQuest);
                                sender.sendMessage("§aFailed the quest as an active quest for the player with the UUID §b" + questPlayer.getUUID() + " §aand name §b" + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName());

                            }

                            // questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

                            final ArrayList<CompletedQuest> completedQuestsToRemove = new ArrayList<>();

                            for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                                if (completedQuest.getQuest().equals(quest)) {
                                    completedQuestsToRemove.add(completedQuest);
                                    sender.sendMessage("§aRemoved the quest as a completed quest for the player with the UUID §b" + questPlayer.getUUID() + " §aand name §b" + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName());
                                }

                            }

                            questPlayer.getCompletedQuests().removeAll(completedQuestsToRemove);
                        }
                        sender.sendMessage("§aOperation done!");
                    } else {
                        sender.sendMessage("§cError: the quest §b" + args[0] + " §cwas not found!");
                    }

                } else if (args[0].equalsIgnoreCase("create")) {
                    sender.sendMessage(main.getQuestManager().createQuest(args[1]));
                } else if (args[0].equalsIgnoreCase("delete")) {
                    sender.sendMessage(main.getQuestManager().deleteQuest(args[1]));
                } else if (args[0].equalsIgnoreCase("edit")) {
                    if (main.getQuestManager().getQuest(args[1]) != null) {
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6rewards");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6npcs");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers");

                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6maxAccepts §3[Amount] §7| Sets the maximum amount of times you can start/accept this quest. Set to -1 for unlimited (default)");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6takeEnabled §3[yes/no] §7| Sets if players can accept the quest using /nquests take. Enabled by default");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6acceptCooldown §3[time in minutes] §7| Sets the time players have to wait between accepting quests");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6description §3<description> §7| Sets the quest description");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6displayName §3<display name> §7| Sets the name of the quest which will be displayed in, for example, the quest preview");

                    } else {
                        sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                    }
                } else if (args[0].equalsIgnoreCase("triggerObjective")) {
                    sender.sendMessage("§c9Please enter a §bplayer name§c!");
                    sender.sendMessage("§cCommand usage: §e/qadmin §6triggerObjective §3[triggerName] [playerName]");
                } else if (args[0].equalsIgnoreCase("activeQuests")) {
                    final String playerName = args[1];
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            sender.sendMessage("§eActive quests of player §b" + playerName + " §a(online)§e:");
                            int counter = 1;
                            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                sender.sendMessage("§a" + counter + ". §e" + activeQuest.getQuest().getQuestName());
                                counter += 1;
                            }
                            sender.sendMessage("§7Total active quests: §f" + (counter - 1) + " §7.");
                        } else {
                            sender.sendMessage("§cSeems like the player §b" + playerName + " §a(online) §cdid not accept any active quests!");
                        }
                    } else {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            sender.sendMessage("§eActive quests of player §b" + playerName + " §c(offline)§e:");
                            int counter = 1;
                            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                sender.sendMessage("§a" + counter + ". §e" + activeQuest.getQuest().getQuestName());
                                counter += 1;
                            }
                            sender.sendMessage("§7Total active quests: §f" + (counter - 1) + " §7.");
                        } else {
                            sender.sendMessage("§cSeems like the player §b" + playerName + " §c(offline) did not accept any active quests!");
                        }
                    }


                } else if (args[0].equalsIgnoreCase("completedQuests")) {
                    final String playerName = args[1];
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            sender.sendMessage("§eCompleted quests of player §b" + playerName + " §a(online)§e:");
                            int counter = 1;

                            for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                                resultDate.setTime(completedQuest.getTimeCompleted());
                                sender.sendMessage("§a" + counter + ". §e" + completedQuest.getQuest().getQuestName() + "§a. Completed: §b" + resultDate);
                                counter += 1;
                            }
                            sender.sendMessage("§7Total completed quests: §f" + (counter - 1) + " §7.");
                        } else {
                            sender.sendMessage("§cSeems like the player §b" + playerName + " §a(online) §cnever completed any quests!");
                        }
                    } else {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            sender.sendMessage("§eCompleted quests of player §b" + playerName + " §c(offline)§e:");
                            int counter = 1;
                            for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                                resultDate.setTime(completedQuest.getTimeCompleted());
                                sender.sendMessage("§a" + counter + ". §e" + completedQuest.getQuest().getQuestName() + "§a. Completed: §b" + resultDate);
                                counter += 1;
                            }
                            sender.sendMessage("§7Total completed quests: §f" + (counter - 1) + " §7.");
                        } else {
                            sender.sendMessage("§cSeems like the player §b" + playerName + " §c(offline) never completed any quests!");
                        }
                    }


                } else if (args[0].equalsIgnoreCase("progress")) {
                    sender.sendMessage("§cMissing 3. argument §3[Quest Name]§c. Specify the §bquest name§c of the quest you wish to see the player's progress for.");
                    sender.sendMessage("§e/qadmin §6progress §2" + args[1] + " §3[Quest Name] §7| Shows progress for a quest of another player");
                } else if (args[0].equalsIgnoreCase("forcegive")) {
                    sender.sendMessage("§cMissing 3. argument §3[Quest Name]§c. Specify the §bname of the quest§c the player should be forced to accept.");
                    sender.sendMessage("§e/qadmin §6forcegive §3[Player Name] [Quest Name] §7| Bypasses max accepts, cooldown & requirements");
                } else if (args[0].equalsIgnoreCase("failQuest")) {
                    sender.sendMessage("§cMissing 3. argument §3[Active Quest Name] §c. Specify the §bname of the active quest§c of the player they should fail.");
                    sender.sendMessage("§e/qadmin §6failQuest §3[Player Name] [Active Quest Name] §7| Fails an active quest for set player");
                } else if (args[0].equalsIgnoreCase("completeQuest")) {
                    sender.sendMessage("§cMissing 3. argument §3[Active Quest Name] §c. Specify the §bname of the active quest§c of the player they should complete.");
                    sender.sendMessage("§e/qadmin §6completeQuest §3[Player Name] [Active Quest Name] §7| Force-completes an active quest for set player");
                } else {
                    sender.sendMessage("§cWrong command usage!");
                }
            } else if (args.length >= 3 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("objectives")) {
                final Quest quest = main.getQuestManager().getQuest(args[1]);
                if (quest != null) {
                    objectivesAdminCommand.handleObjectivesAdminCommand(sender, args, quest);
                } else {
                    sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("actions")) {
                    if (args[1].equalsIgnoreCase("add")) {
                        sender.sendMessage("§cMissing 3. argument §3<Console Command>§c. Specify the §bcommand which will be executed from the Console§c of your new Action.");
                        sender.sendMessage("§e/qadmin §6actions add §3[Action Name] <Console Command>");
                    } else if (args[1].equalsIgnoreCase("edit")) {

                        sender.sendMessage("§cMissing 3. argument§c. Specify 'setCommand', 'setName' or 'delete'.");
                        sender.sendMessage("§e/qadmin §6actions edit §2" + args[2] + " §3setCommand <new Console Command>");
                        sender.sendMessage("§e/qadmin §6actions edit §2" + args[2] + " §3delete");
                    } else {
                        sender.sendMessage("§cWrong command usage!");
                    }
                } else if (args[0].equalsIgnoreCase("edit")) {
                    final Quest quest = main.getQuestManager().getQuest(args[1]);
                    if (quest != null) {
                        if (args[2].equalsIgnoreCase("requirements")) {
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements add §3[Requirement Type] ...");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements list");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements clear");
                        } else if (args[2].equalsIgnoreCase("rewards")) {
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6rewards add §3[Reward Type] ...");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6rewards list");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6rewards clear");
                        } else if (args[2].equalsIgnoreCase("npcs")) {
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6npcs add §3[NPCS ID] [ShowInNPC (true/false)]");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6npcs list");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6npcs clear");
                        } else if (args[2].equalsIgnoreCase("triggers")) {
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3[Action] [Trigger type] [Apply On] <extra options, depending on what trigger it is>");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers remove §3[Trigger ID (check the trigger list to get the ID)]");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers list");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers clear");
                        } else if (args[2].equalsIgnoreCase("maxAccepts")) {
                            sender.sendMessage("§cPlease enter an amount!");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6maxAccepts §3[Amount] §7| Sets the maximum amount of times you can start/accept this quest. Set to -1 for unlimited (default)");
                        } else if (args[2].equalsIgnoreCase("takeEnabled")) {
                            sender.sendMessage("§cPlease enter §b'yes' §cor §b'no§c!");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6takeEnabled §3[yes/no] §7| Sets if players can accept the quest using /nquests take. Enabled by default");
                        } else if (args[2].equalsIgnoreCase("acceptCooldown")) {
                            sender.sendMessage("§cPlease enter the amount of time in minutes!");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6acceptCooldown §3[time in minutes] §7| Sets the time players have to wait between accepting quests");
                        } else if (args[2].equalsIgnoreCase("description")) {
                            sender.sendMessage("§cPlease enter the quest description!");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6description §3<description> §7| Sets the quest description");
                            sender.sendMessage("§9Current quest description: §e" + quest.getQuestDescription());
                        } else if (args[2].equalsIgnoreCase("displayName")) {
                            sender.sendMessage("§cPlease enter the quest display name!");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6displayName §3<display name> §7| Sets the name of the quest which will be displayed in, for example, the quest preview");
                            sender.sendMessage("§9Current quest displayname: §e" + quest.getQuestDisplayName());
                        } else {
                            sender.sendMessage("§cWrong command usage!");
                        }
                    } else {
                        sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                    }
                } else if (args[0].equalsIgnoreCase("triggerObjective")) {
                    String triggerName = args[1];
                    String playerName = args[2];
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            if (questPlayer.getActiveQuests().size() > 0) {
                                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                    for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                                        if (activeObjective.isUnlocked()) {
                                            if (activeObjective.getObjective() instanceof TriggerCommandObjective) {
                                                if (((TriggerCommandObjective) activeObjective.getObjective()).getTriggerName().equalsIgnoreCase(triggerName)) {
                                                    activeObjective.addProgress(1, -1);

                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }

                    } else {
                        sender.sendMessage("§cObjective TriggerCommand failed. Player §b" + playerName + " §cis not online or was not found!");
                    }
                } else if (args[0].equalsIgnoreCase("give")) {
                    final String playerName = args[1];
                    final String questName = args[2];
                    final Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        final Quest quest = main.getQuestManager().getQuest(questName);
                        if (quest != null) {
                            sender.sendMessage(main.getQuestPlayerManager().acceptQuest(player, quest, true, true));
                        } else {
                            sender.sendMessage("§cQuest §b" + questName + " §cdoes not exist");
                        }
                    } else {
                        sender.sendMessage("§cPlayer §b" + playerName + " §cis not online or was not found!");
                    }
                } else if (args[0].equalsIgnoreCase("forcegive")) {
                    final String playerName = args[1];
                    final String questName = args[2];
                    final Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        final Quest quest = main.getQuestManager().getQuest(questName);
                        if (quest != null) {
                            UUID uuid = player.getUniqueId();
                            sender.sendMessage(main.getQuestPlayerManager().forceAcceptQuest(uuid, quest));
                        } else {
                            sender.sendMessage("§cQuest §b" + questName + " §cdoes not exist");
                        }
                    } else {
                        sender.sendMessage("§cPlayer §b" + playerName + " §cis not online or was not found!");
                    }
                } else if (args[0].equalsIgnoreCase("progress")) {
                    getProgress(sender, args[1], args[2]);
                } else if (args[0].equalsIgnoreCase("failQuest")) {
                    final String playerName = args[1];
                    final String activeQuestName = args[2];
                    final Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {

                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            boolean failedSuccessfully = false;
                            final ArrayList<ActiveQuest> questsToFail = new ArrayList<ActiveQuest>();
                            for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                if (activeQuest.getQuest().getQuestName().equalsIgnoreCase(activeQuestName)) {
                                    questsToFail.add(activeQuest);
                                }
                            }
                            for (final ActiveQuest activeQuest : questsToFail) {
                                questPlayer.failQuest(activeQuest);
                                failedSuccessfully = true;
                                sender.sendMessage("§aThe active quest §b" + activeQuest.getQuest().getQuestName() + " §ahas been failed for player §b" + player.getName() + " §a!");
                            }
                            questsToFail.clear();
                            if (!failedSuccessfully) {
                                sender.sendMessage("§cError: §b" + activeQuestName + " §cis not an active Quest of player §b" + player.getName() + " §c!");
                            }
                        } else {
                            sender.sendMessage("§cPlayer §b" + playerName + " §cseems to not have accepted any quests!");
                        }

                    } else {
                        sender.sendMessage("§cPlayer §b" + playerName + " §cis not online or was not found!");
                    }
                } else if (args[0].equalsIgnoreCase("completeQuest")) {
                    final String playerName = args[1];
                    final String activeQuestName = args[2];
                    final Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {

                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            boolean completedSuccessfully = false;
                            final ArrayList<ActiveQuest> questsToComplete = new ArrayList<ActiveQuest>();
                            for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                if (activeQuest.getQuest().getQuestName().equalsIgnoreCase(activeQuestName)) {
                                    questsToComplete.add(activeQuest);
                                }
                            }
                            for (final ActiveQuest activeQuest : questsToComplete) {
                                questPlayer.forceActiveQuestCompleted(activeQuest);
                                completedSuccessfully = true;
                                sender.sendMessage("§aThe active quest §b" + activeQuest.getQuest().getQuestName() + " §ahas been completed for player §b" + player.getName() + " §a!");
                            }
                            questsToComplete.clear();
                            if (!completedSuccessfully) {
                                sender.sendMessage("§cError: §b" + activeQuestName + " §cis not an active Quest of player §b" + player.getName() + " §c!");
                            }
                        } else {
                            sender.sendMessage("§cPlayer §b" + playerName + " §cseems to not have accepted any quests!");
                        }

                    } else {
                        sender.sendMessage("§cPlayer §b" + playerName + " §cis not online or was not found!");
                    }
                } else {
                    sender.sendMessage("§cWrong command usage!");
                }

            } else if (args.length >= 4 && ((args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("description")) || (args[0].equalsIgnoreCase("actions") && (args[1].equalsIgnoreCase("add"))))) {

                if (args[0].equalsIgnoreCase("actions")) {
                    if (args[1].equalsIgnoreCase("add")) {

                        final String actionName = args[2];

                        boolean alreadyExists = false;
                        for (final Action action : main.getQuestManager().getAllActions()) {
                            if (action.getActionName().equalsIgnoreCase(actionName)) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (!alreadyExists) {
                            StringBuilder consoleCommand = new StringBuilder();
                            for (int start = 3; start < args.length; start++) {
                                consoleCommand.append(args[start]);
                                if (start < args.length - 1) {
                                    consoleCommand.append(" ");
                                }

                            }


                            sender.sendMessage("§aTrying to create Action with the name §b" + actionName + " §aand console command §e" + consoleCommand + " §a...");
                            sender.sendMessage("§aStatus: §b" + main.getQuestManager().createAction(actionName, consoleCommand.toString()));


                        } else {
                            sender.sendMessage("§cError! An action with the name §b" + actionName + " §calready exists!");
                        }


                    } else {
                        sender.sendMessage("§cWrong command usage!");
                    }
                } else if (args[0].equalsIgnoreCase("edit")) {
                    final Quest quest = main.getQuestManager().getQuest(args[1]);
                    if (quest != null) {
                        if (args[3].equalsIgnoreCase("clear")) {
                            quest.setQuestDescription("");
                            sender.sendMessage("§aDescription successfully removed from quest §b" + quest.getQuestName() + "§a!");

                        } else {
                            StringBuilder description = new StringBuilder();
                            for (int start = 3; start < args.length; start++) {
                                description.append(args[start]);
                                if (start < args.length - 1) {
                                    description.append(" ");
                                }

                            }

                            quest.setQuestDescription(description.toString());
                            sender.sendMessage("§aDescription successfully added to quest §b" + quest.getQuestName() + "§a! New description: §e" + quest.getQuestDescription());
                        }


                    } else {
                        sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                    }
                } else {
                    sender.sendMessage("§cWrong command usage!");
                }


            } else if (args.length >= 4 && (args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("displayName"))) {
                final Quest quest = main.getQuestManager().getQuest(args[1]);
                if (quest != null) {
                    if (args[3].equalsIgnoreCase("clear")) {
                        quest.setQuestDisplayName("");
                        sender.sendMessage("§aDisplay name successfully removed from quest §b" + quest.getQuestName() + "§a!");

                    } else {
                        StringBuilder displayName = new StringBuilder();
                        for (int start = 3; start < args.length; start++) {
                            displayName.append(args[start]);
                            if(start < args.length-1){
                                displayName.append(" ");
                            }
                        }

                        quest.setQuestDisplayName(displayName.toString());
                        sender.sendMessage("§aDisplay name successfully added to quest §b" + quest.getQuestName() + "§a!");
                    }


                } else {
                    sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                }

            } else if (args.length == 4) {
                if (args[0].equalsIgnoreCase("edit")) {
                    final Quest quest = main.getQuestManager().getQuest(args[1]);
                    if (quest != null) {
                        if (args[2].equalsIgnoreCase("requirements")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements add §3[Requirement Type] ...");
                                sender.sendMessage("§cPlease specify a requirement type!");
                                sender.sendMessage("§eRequirement Types:");
                                sender.sendMessage("§bOtherQuest");
                                sender.sendMessage("§bQuestPoints");
                                sender.sendMessage("§bPermission");
                                sender.sendMessage("§bMoney");
                                sender.sendMessage("§bPlaceholder (WIP");
                                sender.sendMessage("§bKarma (WIP)");
                            } else if (args[3].equalsIgnoreCase("list")) {
                                sender.sendMessage("§9Requirements for quest §b" + quest.getQuestName() + "§9:");
                                int counter = 1;
                                for (Requirement requirement : quest.getRequirements()) {
                                    sender.sendMessage("§a" + counter + ". §e" + requirement.getRequirementType().toString());
                                    if (requirement instanceof OtherQuestRequirement) {
                                        sender.sendMessage("§7-- Finish Quest first: " + ((OtherQuestRequirement) requirement).getOtherQuestName());
                                    } else if (requirement instanceof QuestPointsRequirement) {
                                        sender.sendMessage("§7-- Quest points needed: " + ((QuestPointsRequirement) requirement).getQuestPointRequirement());
                                        sender.sendMessage("§7--- Will quest points be deducted?: " + ((QuestPointsRequirement) requirement).isDeductQuestPoints());
                                    } else if (requirement instanceof MoneyRequirement moneyRequirement) {
                                        sender.sendMessage("§7-- Money needed: " + moneyRequirement.getMoneyRequirement());
                                        sender.sendMessage("§7--- Will money be deducted?: " + moneyRequirement.isDeductMoney());
                                    } else if (requirement instanceof PermissionRequirement) {
                                        sender.sendMessage("§7-- Permission needed: " + ((PermissionRequirement) requirement).getRequiredPermission());
                                    }

                                    counter += 1;
                                }
                            } else if (args[3].equalsIgnoreCase("clear")) {
                                quest.removeAllRequirements();
                                sender.sendMessage("§aAll requirements of quest §b" + quest.getQuestName() + " §ahave been removed!");
                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }


                        } else if (args[2].equalsIgnoreCase("rewards")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6rewards add §3[Reward Type] ...");
                                sender.sendMessage("§cPlease specify a reward type!");
                                sender.sendMessage("§eReward Types:");
                                sender.sendMessage("§bConsoleCommand");
                                sender.sendMessage("§bQuestPoints");
                            } else if (args[3].equalsIgnoreCase("list")) {
                                sender.sendMessage("§9Rewards for quest §b" + quest.getQuestName() + "§9:");
                                int counter = 1;
                                for (Reward reward : quest.getRewards()) {
                                    sender.sendMessage("§a" + counter + ". §e" + reward.getRewardType().toString());
                                    if (reward instanceof CommandReward) {
                                        sender.sendMessage("§7-- Reward Command: " + ((CommandReward) reward).getConsoleCommand());
                                    } else if (reward instanceof QuestPointsReward) {
                                        sender.sendMessage("§7-- Quest points amount: " + ((QuestPointsReward) reward).getRewardedQuestPoints());
                                    }

                                    counter += 1;
                                }
                            } else if (args[3].equalsIgnoreCase("clear")) {
                                quest.removeAllRewards();
                                sender.sendMessage("§aAll rewards of quest §b" + quest.getQuestName() + " §ahave been removed!");
                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }


                        } else if (args[2].equalsIgnoreCase("npcs")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6npcs add §3[NPCS ID] [ShowInNPC (yes/no)]");
                            } else if (args[3].equalsIgnoreCase("list")) {
                                if(!main.isCitizensEnabled()){
                                    sender.sendMessage("§cError: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order for NPC stuff to work.");
                                    return true;
                                }
                                sender.sendMessage("§9NPCs bound to quest  §b" + quest.getQuestName() + "§9 with quest showing:");
                                int counter = 1;
                                for (NPC npc : quest.getAttachedNPCsWithQuestShowing()) {
                                    sender.sendMessage("§e" + counter + ". ID: §b" + npc.getId());
                                    counter++;
                                }
                                sender.sendMessage("§9NPCs bound to quest  §b" + quest.getQuestName() + "§9 without quest showing:");
                                for (NPC npc : quest.getAttachedNPCsWithoutQuestShowing()) {
                                    sender.sendMessage("§e" + counter + ". ID: §b" + npc.getId());
                                    counter++;
                                }
                            } else if (args[3].equalsIgnoreCase("clear")) {
                                if(!main.isCitizensEnabled()){
                                    sender.sendMessage("§cError: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order for NPC stuff to work.");
                                    return true;
                                }
                                quest.removeAllNPCs();
                                sender.sendMessage("§aAll NPCs of quest §b" + quest.getQuestName() + " §ahave been removed!");
                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }

                        } else if (args[2].equalsIgnoreCase("triggers")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                sender.sendMessage("§cMissing 5. argument §3[Action]§c. Specify the §baction§c which will be executed when the Trigger triggers.");
                                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3[Action] [Trigger type] [Apply On] [World Name/ALL] <extra options, depending on what trigger it is>");
                            } else if (args[3].equalsIgnoreCase("remove") || args[3].equalsIgnoreCase("delete")) {
                                sender.sendMessage("§cMissing 5. argument §3[Trigger ID to remove]§c. Specify the §bID of the trigger§c which should be removed from this quest.");
                                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3[Trigger ID to remove (check the trigger list to get the ID)]");
                            } else if (args[3].equalsIgnoreCase("list")) {
                                sender.sendMessage("§9Triggers for quest §b" + quest.getQuestName() + "§9:");
                                int counter = 1;
                                for (Trigger trigger : quest.getTriggers()) {

                                    sender.sendMessage("§e" + counter + ". Type: §b" + trigger.getTriggerType().toString());
                                    if (trigger.getTriggerType() == TriggerType.NPCDEATH) {
                                        sender.sendMessage("§7-- NPC to die ID: §f" + ((NPCDeathTrigger) trigger).getNpcToDieID());
                                    } else if (trigger.getTriggerType() == TriggerType.WORLDENTER) {
                                        sender.sendMessage("§7-- World to enter: §f" + ((WorldEnterTrigger) trigger).getWorldToEnterName());
                                    } else if (trigger.getTriggerType() ==TriggerType.WORLDLEAVE ) {
                                        sender.sendMessage("§7-- World to leave: §f" + ((WorldLeaveTrigger) trigger).getWorldToLeaveName());
                                    }
                                    sender.sendMessage("§7--- Action Name: §f" + trigger.getTriggerAction().getActionName());
                                    sender.sendMessage("§7------ Action console command: §f" + trigger.getTriggerAction().getConsoleCommand());
                                    sender.sendMessage("§7--- Amount of triggers needed for first execution: §f" + trigger.getAmountNeeded());
                                    if (trigger.getApplyOn() == 0) {
                                        sender.sendMessage("§7--- Apply on: §fQuest");
                                    } else {
                                        sender.sendMessage("§7--- Apply on: §fObjective " + trigger.getApplyOn());
                                    }

                                    if (trigger.getWorldName() == null || trigger.getWorldName().equals("") || trigger.getWorldName().equalsIgnoreCase("ALL")) {
                                        sender.sendMessage("§7--- In World: §fAll Worlds");
                                    } else {
                                        sender.sendMessage("§7--- In World: §f" + trigger.getWorldName());
                                    }

                                    counter++;
                                }
                            } else if (args[3].equalsIgnoreCase("clear")) {
                                quest.removeAllTriggers();
                                sender.sendMessage("§aAll Triggers of quest §b" + quest.getQuestName() + " §ahave been removed!");
                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }

                        } else if (args[2].equalsIgnoreCase("maxAccepts")) {
                            int maxAcceptsAmount = Integer.parseInt(args[3]);
                            if (maxAcceptsAmount >= 0) {
                                quest.setMaxAccepts(maxAcceptsAmount);
                                sender.sendMessage("§aMax quest accept amount for quest §b" + quest.getQuestName() + " §ahas been set to §b" + maxAcceptsAmount + "§a!");
                            } else {
                                quest.setMaxAccepts(-1);
                                sender.sendMessage("§aMax quest accept amount for quest §b" + quest.getQuestName() + " §ahas been set to §bunlimited§a!");
                            }
                        } else if (args[2].equalsIgnoreCase("takeEnabled")) {

                            boolean takeEnabled = true;
                            boolean didntspecify = true;
                            if (args[3].equalsIgnoreCase("yes") || args[3].equalsIgnoreCase("true")) {
                                takeEnabled = true;
                                didntspecify = false;
                            } else if (args[3].equalsIgnoreCase("no") || args[3].equalsIgnoreCase("false")) {
                                takeEnabled = false;
                                didntspecify = false;
                            }
                            if (!didntspecify) {
                                quest.setTakeEnabled(takeEnabled);
                                if (takeEnabled) {
                                    sender.sendMessage("§aQuest taking (/nquests take) for the quest §b" + quest.getQuestName() + " §ahas been set to §benabled§a!");
                                } else {
                                    sender.sendMessage("§aQuest taking (/nquests take) for the quest §b" + quest.getQuestName() + " §ahas been set to §bdisabled§a!");
                                }
                            } else {
                                sender.sendMessage("§cPlease enter §b'yes' §cor §b'no§c!");
                                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6takeEnabled §3[yes/no] §7| Sets if players can accept the quest using §/nquests take. Enabled by default");
                            }

                        } else if (args[2].equalsIgnoreCase("acceptCooldown") || args[2].equalsIgnoreCase("cooldown")) {
                            int cooldownInMinutes = Integer.parseInt(args[3]);
                            if (cooldownInMinutes > 0) {
                                quest.setAcceptCooldown(cooldownInMinutes);
                                sender.sendMessage("§aCooldown for quest §b" + quest.getQuestName() + " §ahas been set to §b" + cooldownInMinutes + "§a minutes!");
                            } else {
                                quest.setAcceptCooldown(-1);
                                sender.sendMessage("§aCooldown for quest §b" + quest.getQuestName() + " §ahas been §bdisabled§a!");
                            }
                        } else {
                            sender.sendMessage("§cWrong command usage!");
                        }
                    } else {
                        sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                    }
                } else if (args[0].equalsIgnoreCase("actions") && args[1].equalsIgnoreCase("edit")) {
                    if (args[3].equalsIgnoreCase("delete")) {
                        final String actionName = args[2];

                        Action foundAction = null;
                        for (final Action action : main.getQuestManager().getAllActions()) {
                            if (action.getActionName().equalsIgnoreCase(actionName)) {
                                foundAction = action;
                                break;
                            }
                        }

                        if (foundAction != null) {

                            main.getQuestManager().removeAction(foundAction);
                            sender.sendMessage("§aAction with the name §b" + foundAction.getActionName() + " §ahas been deleted.");


                        } else {
                            sender.sendMessage("§cError: Action with the name §b" + actionName + " §c does not exist.");
                        }


                    } else if (args[3].equalsIgnoreCase("setCommand")) {
                        sender.sendMessage("§cMissing 5. argument <new Console Command>§c. Specify the new console command here.");
                        sender.sendMessage("§e/qadmin §6actions edit §2" + args[2] + " §3setCommand <new Console Command>");
                    } else {
                        sender.sendMessage("§cWrong command usage!");
                    }


                } else {
                    sender.sendMessage("§cWrong command usage!");
                }
            } else if (args.length >= 5 && args[0].equalsIgnoreCase("actions") && args[1].equalsIgnoreCase("edit") && args[3].equalsIgnoreCase("setCommand")) {


                final String actionName = args[2];

                Action foundAction = null;
                for (final Action action : main.getQuestManager().getAllActions()) {
                    if (action.getActionName().equalsIgnoreCase(actionName)) {
                        foundAction = action;
                        break;
                    }
                }

                if (foundAction != null) {
                    StringBuilder consoleCommand = new StringBuilder();
                    for (int start = 4; start < args.length; start++) {
                        consoleCommand.append(args[start]);
                        if (start < args.length - 1) {
                            consoleCommand.append(" ");
                        }

                    }

                    foundAction.setConsoleCommand(consoleCommand.toString());
                    sender.sendMessage("§aConsole command of action §b" + foundAction.getActionName() + " §ahas been set to §e" + consoleCommand + " §a.");


                } else {
                    sender.sendMessage("§cError: Action with the name §b" + actionName + " §c does not exist.");
                }


            } else if (args.length == 5) {
                if (args[0].equalsIgnoreCase("edit")) {
                    final Quest quest = main.getQuestManager().getQuest(args[1]);
                    if (quest != null) {
                        if (args[2].equalsIgnoreCase("requirements")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                if (args[4].equalsIgnoreCase("OtherQuest")) {
                                    sender.sendMessage("§cMissing 6. argument §3[Other Quest Name]§c. Specify the §bname of the quest§c the player has to complete.");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements add §2OtherQuest §3[Other Quest Name] §3[amount of completions needed]");
                                } else if (args[4].equalsIgnoreCase("QuestPoints")) {
                                    sender.sendMessage("§cMissing 6. argument §3[Quest Point requirement amount]§c. Specify the §bamount of quest points§c the player needs to accept this quest.");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements add §2QuestPoints §3[Quest Point requirement amount] §3[Deduct quest points?]");
                                } else if (args[4].equalsIgnoreCase("Money")) {
                                    sender.sendMessage("§cMissing 6. argument §3[Money requirement amount]§c. Specify the §bamount of money§c the player needs to accept this quest.");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements add §2Money §3[Money requirement amount] §3[Deduct money?]");
                                } else if (args[4].equalsIgnoreCase("Permission")) {
                                    sender.sendMessage("§cMissing 6. argument §3<Required permission node>§c. Specify the §bpermission§c the player needs to accept this quest.");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements add §2Permission §3<Required permission node>");
                                } else {
                                    sender.sendMessage("§cInvalid Requirement Type");
                                }
                            } else {
                                sender.sendMessage("§cWrong command usage");
                            }
                        } else if (args[2].equalsIgnoreCase("rewards")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                if (args[4].equalsIgnoreCase("ConsoleCommand")) {
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6rewards add §2ConsoleCommand §3[Console Command]");
                                } else if (args[4].equalsIgnoreCase("QuestPoints")) {
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6rewards add §2QuestPoints §3[Quest point reward amount]");
                                } else {
                                    sender.sendMessage("§cInvalid Reward Type");
                                    sender.sendMessage("§eReward Types:");
                                    sender.sendMessage("§bConsoleCommand");
                                    sender.sendMessage("§bQuestPoints");
                                }
                            } else {
                                sender.sendMessage("§cWrong command usage");
                            }
                        } else if (args[2].equalsIgnoreCase("npcs")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                sender.sendMessage("§cError: Missing last argument §b[ShowInNPC (yes/no)]§c. Specify if the quest should auto-show when a player right clicks the NPC.");
                                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6npcs add §3[NPCS ID] [ShowInNPC (yes/no)]");
                            }
                        } else if (args[2].equalsIgnoreCase("triggers")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                sender.sendMessage("§cMissing 6. argument §3[Trigger Type]§c. Specify the §bTrigger type§c of the trigger.");
                                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3[Action] [Trigger type] [Apply On] [World Name/ALL] <extra options, depending on what trigger it is>");
                                sender.sendMessage("§eTrigger Types:");
                                sender.sendMessage("§bDEATH");
                                sender.sendMessage("§bNPCDEATH");
                                sender.sendMessage("§bFAIL");
                                sender.sendMessage("§bCOMPLETE");
                                sender.sendMessage("§bBEGIN");
                                sender.sendMessage("§bDISCONNECT");
                                sender.sendMessage("§bWORLDENTER");
                                sender.sendMessage("§bWORLDLEAVE");

                            } else if (args[3].equalsIgnoreCase("remove")) {

                                final int triggerID = Integer.parseInt(args[4]);

                                sender.sendMessage(quest.removeTrigger(triggerID));

                            } else {
                                sender.sendMessage("§cWrong command usage");
                            }
                        } else {
                            sender.sendMessage("§cWrong command usage!");
                        }
                    } else {
                        sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                    }
                } else {
                    sender.sendMessage("§cWrong command usage!");
                }
            } else if (args.length > 5 && args[2].equalsIgnoreCase("rewards") && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("ConsoleCommand")) {
                final Quest quest = main.getQuestManager().getQuest(args[1]);
                if (quest != null) {
                    StringBuilder rewardCommand = new StringBuilder();
                    for (int start = 4; start < args.length; start++) {
                        rewardCommand.append(args[start]).append(" ");
                    }
                    CommandReward commandReward = new CommandReward(main, rewardCommand.toString());
                    quest.addReward(commandReward);
                    sender.sendMessage("§aReward successfully added to quest §b" + quest.getQuestName() + "§a!");

                } else {
                    sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                }


            } else if (args.length == 6) {
                if (args[0].equalsIgnoreCase("edit")) {
                    final Quest quest = main.getQuestManager().getQuest(args[1]);
                    if (quest != null) {
                        if (args[2].equalsIgnoreCase("requirements")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                if (args[4].equalsIgnoreCase("OtherQuest")) {
                                    sender.sendMessage("§cMissing 7. argument §3[Amount of completions]§c. Specify the §bamount of times§c the player has complete the quest.");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements add §2OtherQuest §3[Other Quest name] [Amount of completions]");
                                } else if (args[4].equalsIgnoreCase("QuestPoints")) {
                                    sender.sendMessage("§cMissing 7. argument §3[Deduct quest points?]§c. Specify if you want to deduct the quest points from the player once he meets the requirements and accepts the quest.");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements add §2QuestPoints §3[Quest Point requirement amount] §3[Deduct quest points?]");
                                } else if (args[4].equalsIgnoreCase("Money")) {
                                    sender.sendMessage("§cMissing 7. argument §3[Deduct money?]§c. Specify if you want to deduct the money from the player once he meets the requirements and accepts the quest.");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements add §2Money §3[Money requirement amount] §3[Deduct money?]");
                                } else if (args[4].equalsIgnoreCase("Permission")) {
                                    String requiredPermission = args[5];
                                    PermissionRequirement permissionRequirement = new PermissionRequirement(main, requiredPermission);
                                    quest.addRequirement(permissionRequirement);
                                    sender.sendMessage("§aRequirement successfully added to quest §b" + quest.getQuestName() + "§a!");
                                } else {
                                    sender.sendMessage("§cWrong command usage!");
                                }
                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }

                        } else if (args[2].equalsIgnoreCase("rewards")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                if (args[4].equalsIgnoreCase("QuestPoints")) {

                                    final long questPointRewardAmount = Long.parseLong(args[5]);

                                    QuestPointsReward questPointsReward = new QuestPointsReward(main, questPointRewardAmount);
                                    quest.addReward(questPointsReward);
                                    sender.sendMessage("§aReward successfully added to quest §b" + quest.getQuestName() + "§a!");

                                } else {
                                    sender.sendMessage("§cWrong command usage!");
                                }
                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }
                        } else if (args[2].equalsIgnoreCase("npcs")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                if(!main.isCitizensEnabled()){
                                    sender.sendMessage("§cError: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order for NPC stuff to work.");
                                    return true;
                                }
                                int npcID = Integer.parseInt(args[4]);

                                boolean showQuest = true;
                                boolean setit = false;
                                if (args[5].equalsIgnoreCase("Yes") || args[5].equalsIgnoreCase("True")) {
                                    setit = true;
                                } else if (args[5].equalsIgnoreCase("No") || args[5].equalsIgnoreCase("False")) {
                                    showQuest = false;
                                    setit = true;
                                }
                                if (setit) {
                                    final NPC npc = CitizensAPI.getNPCRegistry().getById(npcID);
                                    if (npc != null) {
                                        if (!quest.getAttachedNPCsWithQuestShowing().contains(npc) && !quest.getAttachedNPCsWithoutQuestShowing().contains(npc)) {
                                            quest.bindToNPC(npc, showQuest);
                                            sender.sendMessage("§aQuest §b" + quest.getQuestName() + " §ahas been bound to the NPC with the ID §b" + npcID + "§a! Showing quest: §b" + showQuest);
                                        } else {
                                            sender.sendMessage("§cQuest §b" + quest.getQuestName() + " §chas already been bound to the NPC with the ID §b" + npcID + "§c!");
                                        }

                                    } else {
                                        sender.sendMessage("§cNPC with the ID §b" + npcID + " §cwas not found!");

                                    }
                                } else {
                                    sender.sendMessage("§cWrong last argument. Please enter Yes or No.");
                                }


                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }
                        } else if (args[2].equalsIgnoreCase("triggers")) {

                            if (args[3].equalsIgnoreCase("add")) {
                                if (args[5].equalsIgnoreCase("DEATH")) {

                                    sender.sendMessage("§cMissing 7. argument §3[Apply On]§c. Specify where the trigger will apply (Examples:  'Quest', 'O1', 'O2'). (O1 = Objective 1)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2DEATH §3[Apply On] [World Name/ALL] [Amount of Deaths]");

                                } else if (args[5].equalsIgnoreCase("FAIL")) {

                                    sender.sendMessage("§cMissing 7. argument §3[Apply On]§c. Specify where the trigger will apply (Examples:  'Quest', 'O1', 'O2'). (O1 = Objective 1)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2FAIL §3[Apply On] [World Name/ALL]");
                                } else if (args[5].equalsIgnoreCase("COMPLETE")) {

                                    sender.sendMessage("§cMissing 7. argument §3[Apply On]§c. Specify where the trigger will apply (Examples:  'Quest', 'O1', 'O2'). (O1 = Objective 1)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2COMPLETE §3[Apply On] [World Name/ALL]");
                                } else if (args[5].equalsIgnoreCase("BEGIN")) {

                                    sender.sendMessage("§cMissing 7. argument §3[Apply On]§c. Specify where the trigger will apply (Examples:  'Quest', 'O1', 'O2'). (O1 = Objective 1)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2BEGIN §3[Apply On] [World Name/ALL]");
                                } else if (args[5].equalsIgnoreCase("DISCONNECT")) {

                                    sender.sendMessage("§cMissing 7. argument §3[Apply On]§c. Specify where the trigger will apply (Examples:  'Quest', 'O1', 'O2'). (O1 = Objective 1)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2DISCONNECT §3[Apply On] [World Name/ALL]");
                                } else if (args[5].equalsIgnoreCase("NPCDEATH")) {

                                    sender.sendMessage("§cMissing 7. argument §3[Apply On]§c. Specify where the trigger will apply (Examples:  'Quest', 'O1', 'O2'). (O1 = Objective 1)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2NPCDEATH §3[Apply On] [World Name/ALL] [Amount of Deaths] [NPC ID]");

                                } else if (args[5].equalsIgnoreCase("WORLDENTER")) {

                                    sender.sendMessage("§cMissing 7. argument §3[Apply On]§c. Specify where the trigger will apply (Examples:  'Quest', 'O1', 'O2'). (O1 = Objective 1)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2WORLDENTER §3[Apply On] [World Name/ALL] [Amount of Enters] [World to Enter Name/ALL]");

                                } else if (args[5].equalsIgnoreCase("WORLDLEAVE")) {

                                    sender.sendMessage("§cMissing 7. argument §3[Apply On]§c. Specify where the trigger will apply (Examples:  'Quest', 'O1', 'O2'). (O1 = Objective 1)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2WORLDLEAVE §3[Apply On] [World Name/ALL] [Amount of Leaves] [World to Leave Name/ALL]");

                                } else {
                                    sender.sendMessage("§cWrong command usage!");
                                }
                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }

                        } else {
                            sender.sendMessage("§cWrong Command Usage!");
                        }
                    } else {
                        sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                    }
                } else {
                    sender.sendMessage("§cWrong command usage!");
                }
            } else if (args.length == 7) {
                if (args[0].equalsIgnoreCase("edit")) {
                    final Quest quest = main.getQuestManager().getQuest(args[1]);
                    if (quest != null) {
                        if (args[2].equalsIgnoreCase("requirements")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                if (args[4].equalsIgnoreCase("OtherQuest")) {
                                    String otherQuestName = args[5];
                                    int amountOfCompletions = Integer.parseInt(args[6]);
                                    OtherQuestRequirement otherQuestRequirement = new OtherQuestRequirement(main, otherQuestName, amountOfCompletions);
                                    quest.addRequirement(otherQuestRequirement);
                                    sender.sendMessage("§aRequirement successfully added to quest §b" + quest.getQuestName() + "§a!");

                                } else if (args[4].equalsIgnoreCase("QuestPoints")) {
                                    long questPointsNeeded = Long.parseLong(args[5]);

                                    boolean deductQuestPoints = false;
                                    boolean didntSpecify = true;
                                    if (args[6].equalsIgnoreCase("yes") || args[6].equalsIgnoreCase("true")) {
                                        deductQuestPoints = true;
                                        didntSpecify = false;
                                    } else if (args[6].equalsIgnoreCase("no") || args[6].equalsIgnoreCase("false")) {
                                        didntSpecify = false;
                                    }
                                    if (!didntSpecify) {
                                        QuestPointsRequirement questPointsRequirement = new QuestPointsRequirement(main, questPointsNeeded, deductQuestPoints);
                                        quest.addRequirement(questPointsRequirement);
                                        sender.sendMessage("§aRequirement successfully added to quest §b" + quest.getQuestName() + "§a!");
                                    } else {
                                        sender.sendMessage("§cWrong last argument. Specify §bYes §cor §b No");
                                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements add §2QuestPoints §3[Quest Point requirement amount] §3[Deduct quest points?]");
                                    }

                                } else if (args[4].equalsIgnoreCase("Money")) {
                                    long moneyNeeded = Long.parseLong(args[5]);

                                    boolean deductMoney = false;
                                    boolean didntSpecify = true;
                                    if (args[6].equalsIgnoreCase("yes") || args[6].equalsIgnoreCase("true")) {
                                        deductMoney = true;
                                        didntSpecify = false;
                                    } else if (args[6].equalsIgnoreCase("no") || args[6].equalsIgnoreCase("false")) {
                                        didntSpecify = false;
                                    }
                                    if (!didntSpecify) {

                                        //Cancel if Vault is not found
                                        if(!main.isVaultEnabled()){
                                            sender.sendMessage("§cError: cannot add a money requirement because Vault (needed for money stuff to work) is not installed on the server.");
                                            return true;
                                        }

                                        MoneyRequirement moneyRequirement = new MoneyRequirement(main, moneyNeeded, deductMoney);
                                        quest.addRequirement(moneyRequirement);
                                        sender.sendMessage("§aRequirement successfully added to quest §b" + quest.getQuestName() + "§a!");
                                    } else {
                                        sender.sendMessage("§cWrong last argument. Specify §bYes §cor §b No");
                                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6requirements add §2Money §3[Money requirement amount] §3[Deduct money?]");
                                    }

                                } else {
                                    sender.sendMessage("§cWrong command usage");
                                }
                            } else {
                                sender.sendMessage("§cWrong command usage");
                            }
                        } else if (args[2].equalsIgnoreCase("triggers")) {

                            if (args[3].equalsIgnoreCase("add")) {

                                if (args[5].equalsIgnoreCase("DEATH")) {

                                    sender.sendMessage("§cMissing 8. argument §3[[World Name/ALL] ]§c. Specify in which world the trigger will apply (Examples:  'Nelphguard', 'ALL', 'world').");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2DEATH §3[Apply On] [World Name/ALL] [Amount of Deaths]");

                                } else if (args[5].equalsIgnoreCase("FAIL")) {

                                    sender.sendMessage("§cMissing 8. argument §3[[World Name/ALL] ]§c. Specify in which world the trigger will apply (Examples:  'Nelphguard', 'ALL', 'world').");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2FAIL §3[Apply On] [World Name/ALL]");
                                } else if (args[5].equalsIgnoreCase("COMPLETE")) {

                                    sender.sendMessage("§cMissing 8. argument §3[[World Name/ALL] ]§c. Specify in which world the trigger will apply (Examples:  'Nelphguard', 'ALL', 'world').");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2COMPLETE §3[Apply On] [World Name/ALL]");
                                } else if (args[5].equalsIgnoreCase("BEGIN")) {

                                    sender.sendMessage("§cMissing 8. argument §3[[World Name/ALL] ]§c. Specify in which world the trigger will apply (Examples:  'Nelphguard', 'ALL', 'world').");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2BEGIN §3[Apply On] [World Name/ALL]");
                                } else if (args[5].equalsIgnoreCase("DISCONNECT")) {

                                    sender.sendMessage("§cMissing 8. argument §3[[World Name/ALL] ]§c. Specify in which world the trigger will apply (Examples:  'Nelphguard', 'ALL', 'world').");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2DISCONNECT §3[Apply On] [World Name/ALL]");
                                } else if (args[5].equalsIgnoreCase("NPCDEATH")) {

                                    sender.sendMessage("§cMissing 8. argument §3[[World Name/ALL] ]§c. Specify in which world the trigger will apply (Examples:  'Nelphguard', 'ALL', 'world').");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2NPCDEATH §3[Apply On] [World Name/ALL] [Amount of Deaths] [NPC ID]");

                                } else if (args[5].equalsIgnoreCase("WORLDENTER")) {

                                    sender.sendMessage("§cMissing 8. argument §3[[World Name/ALL] ]§c. Specify in which world the trigger will apply (Examples:  'Nelphguard', 'ALL', 'world').");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2WORLDENTER §3[Apply On] [World Name/ALL] [Amount of Enters] [World to Enter Name/ALL]");

                                } else if (args[5].equalsIgnoreCase("WORLDLEAVE")) {

                                    sender.sendMessage("§cMissing 8. argument §3[[World Name/ALL] ]§c. Specify in which world the trigger will apply (Examples:  'Nelphguard', 'ALL', 'world').");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2WORLDLEAVE §3[Apply On] [World Name/ALL] [Amount of Leaves] [World to Leave Name/ALL]");

                                } else {
                                    sender.sendMessage("§cWrong command usage!");
                                }


                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }
                        } else {
                            sender.sendMessage("§cWrong command usage");
                        }
                    } else {
                        sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                    }
                } else {
                    sender.sendMessage("§cWrong command usage!");
                }
            } else if (args.length == 8) {
                if (args[0].equalsIgnoreCase("edit")) {
                    final Quest quest = main.getQuestManager().getQuest(args[1]);
                    if (quest != null) {
                        if (args[2].equalsIgnoreCase("triggers")) {
                            if (args[3].equalsIgnoreCase("add")) {


                                if (args[5].equalsIgnoreCase("DEATH")) {

                                    sender.sendMessage("§cMissing 9. argument §3[Amount of Deaths]§c. Specify after which amount of deaths the trigger will start to trigger for every further death (or = amount of deaths)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2DEATH §3" + args[6] + " [World Name/ALL] [Amount of Deaths]");

                                } else if (args[5].equalsIgnoreCase("FAIL")) {
                                    final Action action = main.getQuestManager().getAction(args[4]);
                                    if (action != null) {
                                        int applyOn = -1;
                                        if (args[6].equalsIgnoreCase("quest")) {
                                            applyOn = 0;
                                        } else if (args[6].toUpperCase().startsWith("O")) {
                                            applyOn = Integer.parseInt(args[6].substring(1));
                                        }
                                        if (applyOn != -1) {
                                            final FailTrigger failTrigger = new FailTrigger(main, action, applyOn, args[7]);
                                            quest.addTrigger(failTrigger);
                                            sender.sendMessage("§aTrigger successfully added to quest §b" + quest.getQuestName() + "§a!");
                                        } else {
                                            sender.sendMessage("§cError: the argument 'applyON' was not valid: §b" + args[6]);
                                        }

                                    } else {
                                        sender.sendMessage("§cError: the following action was not found: §b" + args[4]);
                                    }

                                } else if (args[5].equalsIgnoreCase("COMPLETE")) {
                                    final Action action = main.getQuestManager().getAction(args[4]);
                                    if (action != null) {
                                        int applyOn = -1;
                                        if (args[6].equalsIgnoreCase("quest")) {
                                            applyOn = 0;
                                        } else if (args[6].toUpperCase().startsWith("O")) {
                                            applyOn = Integer.parseInt(args[6].substring(1));
                                        }
                                        if (applyOn != -1) {
                                            final CompleteTrigger completeTrigger = new CompleteTrigger(main, action, applyOn, args[7]);
                                            quest.addTrigger(completeTrigger);
                                            sender.sendMessage("§aTrigger successfully added to quest §b" + quest.getQuestName() + "§a!");
                                        } else {
                                            sender.sendMessage("§cError: the argument 'applyON' was not valid: §b" + args[6]);
                                        }

                                    } else {
                                        sender.sendMessage("§cError: the following action was not found: §b" + args[4]);
                                    }
                                } else if (args[5].equalsIgnoreCase("BEGIN")) {
                                    final Action action = main.getQuestManager().getAction(args[4]);
                                    if (action != null) {
                                        int applyOn = -1;
                                        if (args[6].equalsIgnoreCase("quest")) {
                                            applyOn = 0;
                                        } else if (args[6].toUpperCase().startsWith("O")) {
                                            applyOn = Integer.parseInt(args[6].substring(1));
                                        }
                                        if (applyOn != -1) {
                                            final BeginTrigger beginTrigger = new BeginTrigger(main, action, applyOn, args[7]);
                                            quest.addTrigger(beginTrigger);
                                            sender.sendMessage("§aTrigger successfully added to quest §b" + quest.getQuestName() + "§a!");
                                        } else {
                                            sender.sendMessage("§cError: the argument 'applyON' was not valid: §b" + args[6]);
                                        }

                                    } else {
                                        sender.sendMessage("§cError: the following action was not found: §b" + args[4]);
                                    }
                                } else if (args[5].equalsIgnoreCase("DISCONNECT")) {
                                    final Action action = main.getQuestManager().getAction(args[4]);
                                    if (action != null) {
                                        int applyOn = -1;
                                        if (args[6].equalsIgnoreCase("quest")) {
                                            applyOn = 0;
                                        } else if (args[6].toUpperCase().startsWith("O")) {
                                            applyOn = Integer.parseInt(args[6].substring(1));
                                        }
                                        if (applyOn != -1) {
                                            final DisconnectTrigger disconnectTrigger = new DisconnectTrigger(main, action, applyOn, args[7]);
                                            quest.addTrigger(disconnectTrigger);
                                            sender.sendMessage("§aTrigger successfully added to quest §b" + quest.getQuestName() + "§a!");
                                        } else {
                                            sender.sendMessage("§cError: the argument 'applyON' was not valid: §b" + args[6]);
                                        }

                                    } else {
                                        sender.sendMessage("§cError: the following action was not found: §b" + args[4]);
                                    }
                                } else if (args[5].equalsIgnoreCase("NPCDEATH")) {

                                    sender.sendMessage("§cMissing 8. argument §3[Amount of Deaths]§c. Specify after which amount of deaths the trigger will start to trigger for every further death (or = amount of deaths)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2NPCDEATH §3" + args[6] + " [World Name/ALL] [Amount of Deaths] [NPC ID]");

                                } else if (args[5].equalsIgnoreCase("WORLDENTER")) {

                                    sender.sendMessage("§cMissing 8. argument §3[Amount of Enters]§c. Specify after which amount of world enters the trigger will start to trigger for every further enter (or = amount of enters)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2WORLDENTER §3[Apply On] [World Name/ALL] [Amount of Enters] [World to Enter Name/ALL]");

                                } else if (args[5].equalsIgnoreCase("WORLDLEAVE")) {

                                    sender.sendMessage("§cMissing 8. argument §3[Amount of Deaths]§c. Specify after which amount of world leaves the trigger will start to trigger for every further leave (or = amount of leaves)");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2WORLDLEAVE §3[Apply On] [World Name/ALL] [Amount of Leaves] [World to Leave Name/ALL]");

                                } else {
                                    sender.sendMessage("§cWrong command usage!");
                                }


                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }
                        } else {
                            sender.sendMessage("§cWrong command usage!");
                        }
                    } else {
                        sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                    }
                } else {
                    sender.sendMessage("§cWrong command usage!");
                }

            } else if (args.length == 9) {
                if (args[0].equalsIgnoreCase("edit")) {
                    final Quest quest = main.getQuestManager().getQuest(args[1]);
                    if (quest != null) {
                        if (args[2].equalsIgnoreCase("triggers")) {
                            if (args[3].equalsIgnoreCase("add")) {


                                if (args[5].equalsIgnoreCase("DEATH")) {
                                    final Action action = main.getQuestManager().getAction(args[4]);
                                    if (action != null) {
                                        int applyOn = -1;
                                        if (args[6].equalsIgnoreCase("quest")) {
                                            applyOn = 0;
                                        } else if (args[6].toUpperCase().startsWith("O")) {
                                            applyOn = Integer.parseInt(args[6].substring(1));
                                        }
                                        if (applyOn != -1) {
                                            int amountOfDeaths = Integer.parseInt(args[8]);
                                            final DeathTrigger deathTrigger = new DeathTrigger(main, action, applyOn, args[7], amountOfDeaths);
                                            quest.addTrigger(deathTrigger);
                                            sender.sendMessage("§aTrigger successfully added to quest §b" + quest.getQuestName() + "§a!");
                                        } else {
                                            sender.sendMessage("§cError: the argument 'applyON' was not valid: §b" + args[6]);
                                        }

                                    } else {
                                        sender.sendMessage("§cError: the following action was not found: §b" + args[4]);
                                    }
                                } else if (args[5].equalsIgnoreCase("NPCDEATH")) {

                                    sender.sendMessage("§cMissing 10. argument §3[NPC ID]§c. Specify the ID of the NPC which has to die for the trigger to trigger");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2NPCDEATH §3" + args[6] + " [World Name/ALL] [Amount of Deaths] [NPC ID]");

                                } else if (args[5].equalsIgnoreCase("WORLDENTER")) {

                                    sender.sendMessage("§cMissing 10. argument §3[World to Enter Name/ALL]§c. Specify 'ALL' or the name of the world which the player has to enter for the trigger to trigger");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2WORLDENTER §3[Apply On] [World Name/ALL] [Amount of Enters] [World to Enter Name/ALL]");

                                } else if (args[5].equalsIgnoreCase("WORLDLEAVE")) {

                                    sender.sendMessage("§cMissing 10. argument §3[World to Enter Name/ALL]§c. Specify 'ALL' or the name of the world which the player has to leave for the trigger to trigger");
                                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6triggers add §3" + args[4] + " §2WORLDLEAVE §3[Apply On] [World Name/ALL] [Amount of Leaves] [World to Leave Name/ALL]");

                                } else {
                                    sender.sendMessage("§cWrong command usage!");
                                }


                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }
                        } else {
                            sender.sendMessage("§cWrong command usage!");
                        }
                    } else {
                        sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                    }
                } else {
                    sender.sendMessage("§cWrong command usage!");
                }

            } else if (args.length == 10) {
                if (args[0].equalsIgnoreCase("edit")) {
                    final Quest quest = main.getQuestManager().getQuest(args[1]);
                    if (quest != null) {
                        if (args[2].equalsIgnoreCase("triggers")) {
                            if (args[3].equalsIgnoreCase("add")) {
                                if (args[5].equalsIgnoreCase("NPCDEATH")) {
                                    if(!main.isCitizensEnabled()){
                                        sender.sendMessage("§cError: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order for NPC stuff to work.");
                                        return true;
                                    }
                                    final Action action = main.getQuestManager().getAction(args[4]);
                                    if (action != null) {
                                        int applyOn = -1;
                                        if (args[6].equalsIgnoreCase("quest")) {
                                            applyOn = 0;
                                        } else if (args[6].toUpperCase().startsWith("O")) {
                                            applyOn = Integer.parseInt(args[6].substring(1));
                                        }
                                        if (applyOn != -1) {
                                            int amountOfDeaths = Integer.parseInt(args[8]);
                                            int npcID = Integer.parseInt(args[9]);
                                            final NPC npc = CitizensAPI.getNPCRegistry().getById(npcID);
                                            if (npc != null) {
                                                final NPCDeathTrigger npcDeathTrigger = new NPCDeathTrigger(main, action, applyOn, args[7], amountOfDeaths, npcID);
                                                quest.addTrigger(npcDeathTrigger);
                                                sender.sendMessage("§aTrigger successfully added to quest §b" + quest.getQuestName() + "§a!");
                                            } else {
                                                sender.sendMessage("§cError: The NPC with the ID: §b" + npcID + " §cdoes not exist!");
                                            }

                                        } else {
                                            sender.sendMessage("§cError: the argument 'applyON' was not valid: §b" + args[6]);
                                        }

                                    } else {
                                        sender.sendMessage("§cError: the following action was not found: §b" + args[4]);
                                    }
                                } else if (args[5].equalsIgnoreCase("WORLDENTER")) {

                                    final Action action = main.getQuestManager().getAction(args[4]);
                                    if (action != null) {
                                        int applyOn = -1;
                                        if (args[6].equalsIgnoreCase("quest")) {
                                            applyOn = 0;
                                        } else if (args[6].toUpperCase().startsWith("O")) {
                                            applyOn = Integer.parseInt(args[6].substring(1));
                                        }
                                        if (applyOn != -1) {
                                            final int amountOfEnters = Integer.parseInt(args[8]);
                                            final String worldName = args[9];

                                            final WorldEnterTrigger worldEnterTrigger = new WorldEnterTrigger(main, action, applyOn, args[7], amountOfEnters, worldName);
                                            quest.addTrigger(worldEnterTrigger);
                                            sender.sendMessage("§aTrigger successfully added to quest §b" + quest.getQuestName() + "§a!");


                                        } else {
                                            sender.sendMessage("§cError: the argument 'applyON' was not valid: §b" + args[6]);
                                        }

                                    } else {
                                        sender.sendMessage("§cError: the following action was not found: §b" + args[4]);
                                    }

                                } else if (args[5].equalsIgnoreCase("WORLDLEAVE")) {

                                    final Action action = main.getQuestManager().getAction(args[4]);
                                    if (action != null) {
                                        int applyOn = -1;
                                        if (args[6].equalsIgnoreCase("quest")) {
                                            applyOn = 0;
                                        } else if (args[6].toUpperCase().startsWith("O")) {
                                            applyOn = Integer.parseInt(args[6].substring(1));
                                        }
                                        if (applyOn != -1) {
                                            final int amountOfLeaves = Integer.parseInt(args[8]);
                                            final String worldName = args[9];

                                            final WorldLeaveTrigger worldLeaveTrigger = new WorldLeaveTrigger(main, action, applyOn, args[7], amountOfLeaves, worldName);
                                            quest.addTrigger(worldLeaveTrigger);
                                            sender.sendMessage("§aTrigger successfully added to quest §b" + quest.getQuestName() + "§a!");


                                        } else {
                                            sender.sendMessage("§cError: the argument 'applyON' was not valid: §b" + args[6]);
                                        }

                                    } else {
                                        sender.sendMessage("§cError: the following action was not found: §b" + args[4]);
                                    }
                                } else {
                                    sender.sendMessage("§cWrong command usage!");
                                }
                            } else {
                                sender.sendMessage("§cWrong command usage!");
                            }
                        } else {
                            sender.sendMessage("§cWrong command usage!");
                        }
                    } else {
                        sender.sendMessage("§cQuest §b" + args[1] + " §cdoes not exist");
                    }
                } else {
                    sender.sendMessage("§cWrong command usage!");
                }

            } else {
                sender.sendMessage("§cToo many arguments!");
            }

        } else {
            sender.sendMessage("§cNo permission! Required permission node: §enotnot.quests.admin");
        }
        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        main.getDataManager().completions.clear();
        main.getDataManager().standardPlayerCompletions.clear();
        main.getDataManager().partialCompletions.clear();

        if (sender.hasPermission("notnot.quests.admin")) {

            for (Player player : Bukkit.getOnlinePlayers()) {
                main.getDataManager().standardPlayerCompletions.add(player.getName());
            }


            if (args.length == 1) {
                main.getDataManager().completions.addAll(Arrays.asList("create", "delete", "edit", "actions", "give", "forcegive", "questPoints", "activeQuests", "completedQuests", "progress", "failQuest", "completeQuest", "listObjectiveTypes", "listRewardTypes", "listRequirementTypes", "listAllQuests", "triggerObjective", "load", "save", "listPlaceholders", "resetAndRemoveQuestForAllPlayers", "resetAndFailQuestForAllPlayers"));
                StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                return main.getDataManager().partialCompletions;

            } else if (args.length > 1 && args[0].equalsIgnoreCase("questPoints")) {
                final @Nullable List<String> completions = questPointsAdminCommand.handleCompletions(sender, args);
                if (completions != null) {
                    StringUtil.copyPartialMatches(args[args.length - 1], completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else {
                    return null;
                }

            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("create")) {
                    main.getDataManager().completions.add("<Enter new Quest Name>");
                    return main.getDataManager().completions;
                } else if (args[0].equalsIgnoreCase("delete")) {
                    for (Quest quest : main.getQuestManager().getAllQuests()) {
                        main.getDataManager().completions.add(quest.getQuestName());
                    }
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("resetAndRemoveQuestForAllPlayers")) {
                    for (Quest quest : main.getQuestManager().getAllQuests()) {
                        main.getDataManager().completions.add(quest.getQuestName());
                    }
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("resetAndFailQuestForAllPlayers")) {
                    for (Quest quest : main.getQuestManager().getAllQuests()) {
                        main.getDataManager().completions.add(quest.getQuestName());
                    }
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("edit")) {
                    for (Quest quest : main.getQuestManager().getAllQuests()) {
                        main.getDataManager().completions.add(quest.getQuestName());
                    }
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("actions")) {
                    main.getDataManager().completions.add("add");
                    main.getDataManager().completions.add("edit");
                    main.getDataManager().completions.add("list");
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("give")) {
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().standardPlayerCompletions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("activeQuests")) {
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().standardPlayerCompletions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("completedQuests")) {
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().standardPlayerCompletions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("progress")) {
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().standardPlayerCompletions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("forcegive")) {
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().standardPlayerCompletions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("triggerObjective")) {
                    for (Quest quest : main.getQuestManager().getAllQuests()) {
                        for (Objective objective : quest.getObjectives()) {
                            if (objective instanceof TriggerCommandObjective) {
                                main.getDataManager().completions.add(((TriggerCommandObjective) objective).getTriggerName());
                            }
                        }
                    }
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("failQuest")) {
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().standardPlayerCompletions, main.getDataManager().partialCompletions);
                    return main.getDataManager().standardPlayerCompletions;
                } else if (args[0].equalsIgnoreCase("completeQuest")) {
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().standardPlayerCompletions, main.getDataManager().partialCompletions);
                    return main.getDataManager().standardPlayerCompletions;
                }

            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("edit")) {
                    main.getDataManager().completions.add("objectives");
                    main.getDataManager().completions.add("rewards");
                    main.getDataManager().completions.add("requirements");
                    main.getDataManager().completions.add("npcs");
                    main.getDataManager().completions.add("triggers");
                    main.getDataManager().completions.add("maxAccepts");
                    main.getDataManager().completions.add("takeEnabled");
                    main.getDataManager().completions.add("acceptCooldown");
                    main.getDataManager().completions.add("description");
                    main.getDataManager().completions.add("displayName");
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("actions")) {
                    if (args[1].equalsIgnoreCase("add")) {
                        main.getDataManager().completions.add("<Enter new, unique Action name>");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[1].equalsIgnoreCase("edit")) {
                        for (final Action action : main.getQuestManager().getAllActions()) {
                            main.getDataManager().completions.add(action.getActionName());
                        }
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    }

                } else if (args[0].equalsIgnoreCase("give")) {
                    for (Quest quest : main.getQuestManager().getAllQuests()) {
                        main.getDataManager().completions.add(quest.getQuestName());
                    }
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("progress")) {
                    for (Quest quest : main.getQuestManager().getAllQuests()) {
                        main.getDataManager().completions.add(quest.getQuestName());
                    }
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("forcegive")) {
                    for (Quest quest : main.getQuestManager().getAllQuests()) {
                        main.getDataManager().completions.add(quest.getQuestName());
                    }
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("triggerObjective")) {
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().standardPlayerCompletions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else if (args[0].equalsIgnoreCase("failQuest")) {
                    final UUID uuid;
                    final Player player = Bukkit.getPlayer(args[1]);
                    if (player != null) {
                        uuid = player.getUniqueId();
                    } else {
                        uuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
                    }
                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(uuid);
                    if (questPlayer != null) {
                        for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            main.getDataManager().completions.add(activeQuest.getQuest().getQuestName());
                        }
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    }
                } else if (args[0].equalsIgnoreCase("completeQuest")) {
                    final UUID uuid;
                    final Player player = Bukkit.getPlayer(args[1]);
                    if (player != null) {
                        uuid = player.getUniqueId();
                    } else {
                        uuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
                    }
                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(uuid);
                    if (questPlayer != null) {
                        for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            main.getDataManager().completions.add(activeQuest.getQuest().getQuestName());
                        }
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    }
                }
            } else if (args.length >= 4 && ((args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("description")) || (args[0].equalsIgnoreCase("actions") && args[1].equalsIgnoreCase("add")))) {
                if (args[0].equalsIgnoreCase("actions")) {
                    if (args[1].equalsIgnoreCase("add")) {
                        main.getDataManager().completions.add("<Enter Console Command>");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    }
                } else if (args[0].equalsIgnoreCase("edit")) {
                    main.getDataManager().completions.add("<Enter new Quest description>");
                    StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                }
            } else if (args.length >= 4 && args[2].equalsIgnoreCase("displayName")) {
                main.getDataManager().completions.add("<Enter new Quest display name>");
                StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                return main.getDataManager().partialCompletions;
            } else if (args.length >= 4 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("objectives")) {
                final List<String> completions = objectivesAdminCommand.handleCompletions(sender, args);
                if (completions != null) {
                    StringUtil.copyPartialMatches(args[args.length - 1], completions, main.getDataManager().partialCompletions);
                    return main.getDataManager().partialCompletions;
                } else {
                    return null;
                }

            } else if (args.length == 4) {
                if (args[0].equalsIgnoreCase("edit")) {
                    if (args[2].equalsIgnoreCase("maxAccepts")) {
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().numberCompletions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[2].equalsIgnoreCase("acceptCooldown")) {
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().numberCompletions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[2].equalsIgnoreCase("takeEnabled")) {
                        main.getDataManager().completions.add("Yes");
                        main.getDataManager().completions.add("No");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[2].equalsIgnoreCase("requirements")) {
                        main.getDataManager().completions.add("add");
                        main.getDataManager().completions.add("list");
                        main.getDataManager().completions.add("clear");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[2].equalsIgnoreCase("rewards")) {
                        main.getDataManager().completions.add("add");
                        main.getDataManager().completions.add("list");
                        main.getDataManager().completions.add("clear");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[2].equalsIgnoreCase("npcs")) {
                        main.getDataManager().completions.add("add");
                        main.getDataManager().completions.add("list");
                        main.getDataManager().completions.add("clear");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[2].equalsIgnoreCase("triggers")) {
                        main.getDataManager().completions.add("add");
                        main.getDataManager().completions.add("remove");
                        main.getDataManager().completions.add("list");
                        main.getDataManager().completions.add("clear");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    }
                } else if (args[0].equalsIgnoreCase("actions")) {
                    if (args[1].equalsIgnoreCase("edit")) {
                        main.getDataManager().completions.add("setCommand");
                        main.getDataManager().completions.add("delete");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    }
                }
            } else if (args.length >= 5 && args[0].equalsIgnoreCase("actions") && args[1].equalsIgnoreCase("edit") && args[3].equalsIgnoreCase("setCommand")) {
                main.getDataManager().completions.add("<Enter new Console Command");
                StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                return main.getDataManager().partialCompletions;
            } else if (args.length == 5) {
                if (args[0].equalsIgnoreCase("edit")) {
                    if (args[2].equalsIgnoreCase("requirements") && args[3].equalsIgnoreCase("add")) {
                        main.getDataManager().completions.add("OtherQuest");
                        main.getDataManager().completions.add("QuestPoints");
                        main.getDataManager().completions.add("Money");
                        main.getDataManager().completions.add("Permission");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[2].equalsIgnoreCase("rewards") && args[3].equalsIgnoreCase("add")) {
                        main.getDataManager().completions.add("ConsoleCommand");
                        main.getDataManager().completions.add("QuestPoints");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[2].equalsIgnoreCase("npcs") && args[3].equalsIgnoreCase("add")) {
                        if(main.isCitizensEnabled()){
                            for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                                main.getDataManager().completions.add("" + npc.getId());
                            }
                        }

                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[2].equalsIgnoreCase("triggers") && args[3].equalsIgnoreCase("add")) {
                        for (final Action action : main.getQuestManager().getAllActions()) {
                            main.getDataManager().completions.add(action.getActionName());
                        }
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[2].equalsIgnoreCase("triggers") && args[3].equalsIgnoreCase("remove")) {
                        final Quest quest = main.getQuestManager().getQuest(args[1]);
                        if (quest != null) {
                            int i = 1;
                            for (final Trigger ignored : quest.getTriggers()) {
                                main.getDataManager().completions.add("" + i);
                                i++;
                            }
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        }


                    }
                }
            } else if (args.length >= 6 && args[2].equalsIgnoreCase("rewards") && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("ConsoleCommand")) {
                main.getDataManager().completions.add("<Enter Console Command>");
                StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                return main.getDataManager().partialCompletions;
            } else if (args.length == 6) {
                if (args[0].equalsIgnoreCase("edit")) {
                    if (args[2].equalsIgnoreCase("requirements") && args[3].equalsIgnoreCase("add")) {
                        if (args[4].equalsIgnoreCase("OtherQuest")) {
                            for (Quest quest : main.getQuestManager().getAllQuests()) {
                                main.getDataManager().completions.add(quest.getQuestName());
                            }
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        } else if (args[4].equalsIgnoreCase("QuestPoints")) {
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().numberPositiveCompletions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        } else if (args[4].equalsIgnoreCase("Money")) {
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().numberPositiveCompletions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        } else if (args[4].equalsIgnoreCase("Permission")) {
                            main.getDataManager().completions.add("<Enter required Permission node>");
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        }
                    } else if (args[2].equalsIgnoreCase("rewards") && args[3].equalsIgnoreCase("add")) {
                        if (args[4].equalsIgnoreCase("QuestPoints")) {
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().numberPositiveCompletions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        }
                    } else if (args[2].equalsIgnoreCase("npcs") && args[3].equalsIgnoreCase("add")) {
                        main.getDataManager().completions.add("Yes");
                        main.getDataManager().completions.add("No");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    } else if (args[2].equalsIgnoreCase("triggers") && args[3].equalsIgnoreCase("add")) {
                        main.getDataManager().completions.add("DEATH");
                        main.getDataManager().completions.add("NPCDEATH");
                        main.getDataManager().completions.add("FAIL");
                        main.getDataManager().completions.add("COMPLETE");
                        main.getDataManager().completions.add("BEGIN");
                        main.getDataManager().completions.add("DISCONNECT");
                        main.getDataManager().completions.add("WORLDENTER");
                        main.getDataManager().completions.add("WORLDLEAVE");
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    }
                }
            } else if (args.length == 7) {
                if (args[0].equalsIgnoreCase("edit")) {
                    if (args[2].equalsIgnoreCase("requirements") && args[3].equalsIgnoreCase("add")) {
                        if (args[4].equalsIgnoreCase("OtherQuest")) {
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().numberPositiveCompletions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        } else if (args[4].equalsIgnoreCase("QuestPoints")) {
                            main.getDataManager().completions.add("Yes");
                            main.getDataManager().completions.add("No");
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        } else if (args[4].equalsIgnoreCase("Money")) {
                            main.getDataManager().completions.add("Yes");
                            main.getDataManager().completions.add("No");
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        }
                    } else if (args[2].equalsIgnoreCase("triggers") && args[3].equalsIgnoreCase("add")) {
                        final Quest quest = main.getQuestManager().getQuest(args[1]);
                        main.getDataManager().completions.add("Quest");
                        if (quest != null) {
                            final int objectiveCount = quest.getObjectives().size();
                            for (int i = 1; i <= objectiveCount; i++) {
                                main.getDataManager().completions.add("O" + i);
                            }
                        }
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    }
                }
            } else if (args.length == 8) {
                if (args[0].equalsIgnoreCase("edit")) {
                    if (args[2].equalsIgnoreCase("triggers") && args[3].equalsIgnoreCase("add")) {
                        main.getDataManager().completions.add("ALL");

                        for (final World world : Bukkit.getWorlds()) {
                            main.getDataManager().completions.add(world.getName());
                        }
                        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                        return main.getDataManager().partialCompletions;
                    }
                }
            } else if (args.length == 9) {
                if (args[0].equalsIgnoreCase("edit")) {
                    if (args[2].equalsIgnoreCase("triggers") && args[3].equalsIgnoreCase("add")) {
                        if (args[5].equalsIgnoreCase("DEATH")) {
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().numberPositiveCompletions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        } else if (args[5].equalsIgnoreCase("NPCDEATH")) {
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().numberPositiveCompletions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        } else if (args[5].equalsIgnoreCase("WORLDENTER")) {
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().numberPositiveCompletions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        } else if (args[5].equalsIgnoreCase("WORLDLEAVE")) {
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().numberPositiveCompletions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;
                        }
                    }
                }
            } else if (args.length == 10) {
                if (args[0].equalsIgnoreCase("edit")) {
                    if (args[2].equalsIgnoreCase("triggers") && args[3].equalsIgnoreCase("add")) {
                        if (args[5].equalsIgnoreCase("NPCDEATH")) {

                            if(main.isCitizensEnabled()){
                                for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                                    main.getDataManager().completions.add("" + npc.getId());
                                }
                            }

                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;

                        } else if (args[5].equalsIgnoreCase("WORLDENTER")) {
                            main.getDataManager().completions.add("ALL");
                            for (final World world : Bukkit.getWorlds()) {
                                main.getDataManager().completions.add("" + world.getName());
                            }
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;

                        } else if (args[5].equalsIgnoreCase("WORLDLEAVE")) {
                            main.getDataManager().completions.add("ALL");
                            for (final World world : Bukkit.getWorlds()) {
                                main.getDataManager().completions.add("" + world.getName());
                            }
                            StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
                            return main.getDataManager().partialCompletions;

                        }
                    }
                }
            }

        }


        StringUtil.copyPartialMatches(args[args.length - 1], main.getDataManager().completions, main.getDataManager().partialCompletions);
        return main.getDataManager().partialCompletions; //returns the possibility's to the client


    }


    public void getProgress(CommandSender sender, String playerName, String questName) {

        final Player player = Bukkit.getPlayer(playerName);
        if (player != null) {


            QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {

                ActiveQuest requestedActiveQuest = null;

                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    if (activeQuest.getQuest().getQuestName().equalsIgnoreCase(questName)) {
                        requestedActiveQuest = activeQuest;
                    }
                }
                if (requestedActiveQuest != null) {

                    sender.sendMessage("§eCompleted Objectives for Quest §b" + requestedActiveQuest.getQuest().getQuestName() + " §eof player §b" + playerName + " §a(online)§e:");
                    main.getQuestManager().sendCompletedObjectivesAndProgress(sender, requestedActiveQuest);

                    sender.sendMessage("§eActive Objectives for Quest §b" + requestedActiveQuest.getQuest().getQuestName() + " §eof player §b" + playerName + " §a(online)§e:");
                    main.getQuestManager().sendActiveObjectivesAndProgress(sender, requestedActiveQuest);


                } else {
                    sender.sendMessage("§cQuest §b" + questName + " §cnot found or not active!");
                    sender.sendMessage("§eActive quests of player §b" + playerName + " §a(online)§e:");
                    int counter = 1;
                    for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        sender.sendMessage("§a" + counter + ". §e" + activeQuest.getQuest().getQuestName());
                        counter += 1;
                    }
                }

            } else {
                sender.sendMessage("§cSeems like the player §b" + playerName + " §a(online) §cdid not accept any active quests!");
            }


        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

            QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
            if (questPlayer != null) {

                ActiveQuest requestedActiveQuest = null;

                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    if (activeQuest.getQuest().getQuestName().equalsIgnoreCase(questName)) {
                        requestedActiveQuest = activeQuest;
                    }
                }
                if (requestedActiveQuest != null) {


                    sender.sendMessage("§eCompleted Objectives for Quest §b" + requestedActiveQuest.getQuest().getQuestName() + " §eof player §b" + playerName + " §c(offline)§e:");
                    main.getQuestManager().sendCompletedObjectivesAndProgress(sender, requestedActiveQuest);

                    sender.sendMessage("§eActive Objectives for Quest §b" + requestedActiveQuest.getQuest().getQuestName() + " §eof player §b" + playerName + " §c(offline)§e:");
                    main.getQuestManager().sendActiveObjectivesAndProgress(sender, requestedActiveQuest);


                } else {
                    sender.sendMessage("§cQuest §b" + questName + " §cnot found or not active!");
                    sender.sendMessage("§eActive quests of player §b" + playerName + " §c(offline)§e:");
                    int counter = 1;
                    for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        sender.sendMessage("§a" + counter + ". §e" + activeQuest.getQuest().getQuestName());
                        counter += 1;
                    }
                }
            } else {
                sender.sendMessage("§cSeems like the player §b" + playerName + " §c(offline) did not accept any active quests!");
            }
        }

    }

}
