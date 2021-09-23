package notquests.notquests.Commands.AdminCommands;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.QuestPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class QuestPointsAdminCommand {

    private final NotQuests main;

    public QuestPointsAdminCommand(final NotQuests main) {
        this.main = main;
    }


    public void handleQuestPointsAdminCommand(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            sender.sendMessage("§cPlease enter a player name! Command usage:");
            showUsage(sender, args);
        } else if (args.length == 2) {
            sender.sendMessage("§cPlease enter the 3. argument");
            showUsage(sender, args);
        } else if (args.length == 3) {
            if (args[2].equalsIgnoreCase("show") || args[2].equalsIgnoreCase("view")) {
                showQuestPoints(sender, args[1]);
            } else {
                if (args[2].equalsIgnoreCase("add")) {
                    sender.sendMessage("§cPlease enter the 4. argument");
                    showUsage(sender, args);
                } else if (args[2].equalsIgnoreCase("remove") || args[2].equalsIgnoreCase("deduct")) {
                    sender.sendMessage("§cPlease enter the 4. argument");
                    showUsage(sender, args);
                } else if (args[2].equalsIgnoreCase("set")) {
                    sender.sendMessage("§cPlease enter the 4. argument");
                    showUsage(sender, args);
                } else {
                    sender.sendMessage("§cWrong command usage!");
                    showUsage(sender, args);
                }
            }
        } else if (args.length == 4) {
            if (args[2].equalsIgnoreCase("add")) {
                addQuestPoints(sender, args[1], Long.parseLong(args[3]));
            } else if (args[2].equalsIgnoreCase("remove") || args[2].equalsIgnoreCase("deduct")) {
                removeQuestPoints(sender, args[1], Long.parseLong(args[3]));
            } else if (args[2].equalsIgnoreCase("set")) {
                setQuestPoints(sender, args[1], Long.parseLong(args[3]));
            } else {
                sender.sendMessage("§cWrong command usage!");
                showUsage(sender, args);
            }

        } else {
            sender.sendMessage("§cWrong command usage!");
            showUsage(sender, args);
        }
    }

    public List<String> handleCompletions(final CommandSender commandSender, final String[] args) {
        main.getDataManager().completions.clear();
        if (args.length == 2) {
            return main.getDataManager().standardPlayerCompletions;
        } else if (args.length == 3) {
            main.getDataManager().completions.add("show");
            main.getDataManager().completions.add("add");
            main.getDataManager().completions.add("remove");
            main.getDataManager().completions.add("set");
            return main.getDataManager().completions;
        } else if (args.length == 4) {
            if (args[2].equalsIgnoreCase("add")) {
                return main.getDataManager().numberPositiveCompletions;
            } else if (args[2].equalsIgnoreCase("remove")) {
                return main.getDataManager().numberPositiveCompletions;
            } else if (args[2].equalsIgnoreCase("set")) {
                return main.getDataManager().numberPositiveCompletions;
            }
        }
        return null;
    }


    private void showUsage(final CommandSender sender, final String[] args) {
        if (args.length == 2) {
            sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " §3show/view");
            sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " §3add [Amount to add]");
            sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " §3remove/deduct [Amount to remove]");
            sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " §3set [New amount]");
        } else if (args.length == 3) {
            if (args[2].equalsIgnoreCase("show") || args[2].equalsIgnoreCase("view") || args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove") || args[2].equalsIgnoreCase("deduct") || args[2].equalsIgnoreCase("set")) {
                if (args[2].equalsIgnoreCase("show") || args[2].equalsIgnoreCase("view")) {
                    sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " " + args[2]);
                } else if (args[2].equalsIgnoreCase("add")) {
                    sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " " + args[2] + " §3[Amount to add]");
                } else if (args[2].equalsIgnoreCase("remove") || args[2].equalsIgnoreCase("deduct")) {
                    sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " " + args[2] + " §3[Amount to remove]");
                } else if (args[2].equalsIgnoreCase("set")) {
                    sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " " + args[2] + " §3[New amount]");
                } else {
                    sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " " + args[2] + " §3[Amount to add]");
                    sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " " + args[2] + " §3[Amount to remove]");
                    sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " " + args[2] + " §3[New amount]");
                }


            } else {
                sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " §3show/view");
                sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " §3add [Amount to add]");
                sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " §3remove/deduct [Amount to remove]");
                sender.sendMessage("§e/nquestsadmin §6questpoints §2" + args[1] + " §3set [New amount]");
            }

        } else {
            sender.sendMessage("§e/nquestsadmin §6questpoints §3[Player Name] show/view");
            sender.sendMessage("§e/nquestsadmin §6questpoints §3[Player Name] add [Amount to add]");
            sender.sendMessage("§e/nquestsadmin §6questpoints §3[Player Name] remove/deduct [Amount to remove]");
            sender.sendMessage("§e/nquestsadmin §6questpoints §3[Player Name] set [New amount]");
        }

    }


    private void showQuestPoints(final CommandSender sender, final String playerName) {
        final Player player = Bukkit.getPlayer(playerName);
        if (player != null) {

            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                sender.sendMessage("§eQuest points for player §b" + playerName + " §a(online)§e: " + questPlayer.getQuestPoints());
            } else {
                sender.sendMessage("§cSeems like the player §b" + playerName + " §a(online) §cdoes not have any quest points!");
            }
        } else {
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
            if (questPlayer != null) {
                sender.sendMessage("§eQuest points for player §b" + playerName + " §c(offline)§e: " + questPlayer.getQuestPoints());

            } else {
                sender.sendMessage("§cSeems like the player §b" + playerName + " §c(offline) does not have any quest points!");
            }
        }
    }


    private void setQuestPoints(final CommandSender sender, final String playerName, final long questPointAmount) {
        final Player player = Bukkit.getPlayer(playerName);
        if (player != null) {

            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                long oldQuestPoints = questPlayer.getQuestPoints();
                questPlayer.setQuestPoints(questPointAmount, false);
                sender.sendMessage("§eQuest points for player §b" + playerName + " §a(online)§e have been set from §7" + oldQuestPoints + " §eto §f" + questPointAmount + "§e.");
            } else {
                sender.sendMessage("§eSeems like the player §b" + playerName + " §a(online) §enever accepted any quests! A new QuestPlayer has been created for him.");
                sender.sendMessage("§eQuest player creation status: " + main.getQuestPlayerManager().createQuestPlayer(player.getUniqueId()));
                final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                if (newQuestPlayer != null) {
                    long oldQuestPoints = newQuestPlayer.getQuestPoints();
                    newQuestPlayer.setQuestPoints(questPointAmount, false);
                    sender.sendMessage("§eQuest points for player §b" + playerName + " §a(online)§e have been set from §7" + oldQuestPoints + " §eto §f" + questPointAmount + "§e.");
                } else {
                    sender.sendMessage("§cSomething went wrong during the questPlayer creation!");
                }

            }
        } else {
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
            if (questPlayer != null) {
                final long oldQuestPoints = questPlayer.getQuestPoints();
                questPlayer.setQuestPoints(questPointAmount, false);
                sender.sendMessage("§eQuest points for player §b" + playerName + " §c(offline)§e have been set from §7" + oldQuestPoints + " §eto §f" + questPointAmount + "§e.");
            } else {
                sender.sendMessage("§eSeems like the player §b" + playerName + " §c(offline) §enever accepted any quests! A new QuestPlayer has been created for him.");
                sender.sendMessage("§eQuest player creation status: " + main.getQuestPlayerManager().createQuestPlayer(offlinePlayer.getUniqueId()));
                final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                if (newQuestPlayer != null) {
                    final long oldQuestPoints = newQuestPlayer.getQuestPoints();
                    newQuestPlayer.setQuestPoints(questPointAmount, false);
                    sender.sendMessage("§eQuest points for player §b" + playerName + " §c(offline)§e have been set from §7" + oldQuestPoints + " §eto §f" + questPointAmount + "§e.");
                } else {
                    sender.sendMessage("§cSomething went wrong during the questPlayer creation!");
                }

            }
        }
    }

    private void addQuestPoints(final CommandSender sender, final String playerName, final long questPointsToAdd) {
        final Player player = Bukkit.getPlayer(playerName);
        if (player != null) {

            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                long oldQuestPoints = questPlayer.getQuestPoints();
                questPlayer.addQuestPoints(questPointsToAdd, false);
                sender.sendMessage("§eQuest points for player §b" + playerName + " §a(online)§e have been set from §7" + oldQuestPoints + " §eto §f" + (oldQuestPoints + questPointsToAdd) + "§e.");
            } else {
                sender.sendMessage("§eSeems like the player §b" + playerName + " §a(online) §enever accepted any quests! A new QuestPlayer has been created for him.");
                sender.sendMessage("§eQuest player creation status: " + main.getQuestPlayerManager().createQuestPlayer(player.getUniqueId()));
                final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                if (newQuestPlayer != null) {
                    long oldQuestPoints = newQuestPlayer.getQuestPoints();
                    newQuestPlayer.addQuestPoints(questPointsToAdd, false);
                    sender.sendMessage("§eQuest points for player §b" + playerName + " §a(online)§e have been set from §7" + oldQuestPoints + " §eto §f" + (oldQuestPoints + questPointsToAdd) + "§e.");
                } else {
                    sender.sendMessage("§cSomething went wrong during the questPlayer creation!");
                }

            }
        } else {
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
            if (questPlayer != null) {
                final long oldQuestPoints = questPlayer.getQuestPoints();
                questPlayer.addQuestPoints(questPointsToAdd, false);
                sender.sendMessage("§eQuest points for player §b" + playerName + " §c(offline)§e have been set from §7" + oldQuestPoints + " §eto §f" + (oldQuestPoints + questPointsToAdd) + "§e.");
            } else {
                sender.sendMessage("§eSeems like the player §b" + playerName + " §c(offline) §enever accepted any quests! A new QuestPlayer has been created for him.");
                sender.sendMessage("§eQuest player creation status: " + main.getQuestPlayerManager().createQuestPlayer(offlinePlayer.getUniqueId()));
                final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                if (newQuestPlayer != null) {
                    final long oldQuestPoints = newQuestPlayer.getQuestPoints();
                    newQuestPlayer.addQuestPoints(questPointsToAdd, false);
                    sender.sendMessage("§eQuest points for player §b" + playerName + " §c(offline)§e have been set from §7" + oldQuestPoints + " §eto §f" + (oldQuestPoints + questPointsToAdd) + "§e.");
                } else {
                    sender.sendMessage("§cSomething went wrong during the questPlayer creation!");
                }

            }
        }
    }


    private void removeQuestPoints(final CommandSender sender, final String playerName, final long questPointsToRemove) {
        final Player player = Bukkit.getPlayer(playerName);
        if (player != null) {

            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                long oldQuestPoints = questPlayer.getQuestPoints();
                questPlayer.removeQuestPoints(questPointsToRemove, false);
                sender.sendMessage("§eQuest points for player §b" + playerName + " §a(online)§e have been set from §7" + oldQuestPoints + " §eto §f" + (oldQuestPoints - questPointsToRemove) + "§e.");
            } else {
                sender.sendMessage("§eSeems like the player §b" + playerName + " §a(online) §enever accepted any quests! A new QuestPlayer has been created for him.");
                sender.sendMessage("§eQuest player creation status: " + main.getQuestPlayerManager().createQuestPlayer(player.getUniqueId()));
                final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                if (newQuestPlayer != null) {
                    long oldQuestPoints = newQuestPlayer.getQuestPoints();
                    newQuestPlayer.removeQuestPoints(questPointsToRemove, false);
                    sender.sendMessage("§eQuest points for player §b" + playerName + " §a(online)§e have been set from §7" + oldQuestPoints + " §eto §f" + (oldQuestPoints - questPointsToRemove) + "§e.");
                } else {
                    sender.sendMessage("§cSomething went wrong during the questPlayer creation!");
                }

            }
        } else {
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
            if (questPlayer != null) {
                final long oldQuestPoints = questPlayer.getQuestPoints();
                questPlayer.removeQuestPoints(questPointsToRemove, false);
                sender.sendMessage("§eQuest points for player §b" + playerName + " §c(offline)§e have been set from §7" + oldQuestPoints + " §eto §f" + (oldQuestPoints - questPointsToRemove) + "§e.");
            } else {
                sender.sendMessage("§eSeems like the player §b" + playerName + " §c(offline) §enever accepted any quests! A new QuestPlayer has been created for him.");
                sender.sendMessage("§eQuest player creation status: " + main.getQuestPlayerManager().createQuestPlayer(offlinePlayer.getUniqueId()));
                final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                if (newQuestPlayer != null) {
                    final long oldQuestPoints = newQuestPlayer.getQuestPoints();
                    newQuestPlayer.removeQuestPoints(questPointsToRemove, false);
                    sender.sendMessage("§eQuest points for player §b" + playerName + " §c(offline)§e have been set from §7" + oldQuestPoints + " §eto §f" + (oldQuestPoints - questPointsToRemove) + "§e.");
                } else {
                    sender.sendMessage("§cSomething went wrong during the questPlayer creation!");
                }

            }
        }
    }

}
