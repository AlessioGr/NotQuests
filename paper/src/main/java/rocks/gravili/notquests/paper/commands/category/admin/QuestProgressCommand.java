package rocks.gravili.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import static org.incendo.cloud.bukkit.parser.OfflinePlayerParser.offlinePlayerParser;
import static rocks.gravili.notquests.paper.commands.arguments.ActiveQuestParser.activeQuestParser;

public class QuestProgressCommand extends BaseCommand {

    public QuestProgressCommand(NotQuests notQuests, Command.Builder<CommandSender> builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(CommandManager<CommandSender> commandManager) {
        commandManager.command(builder.literal("progress", Description.of("Shows the progress for a quest of another player"))
                .required("player", offlinePlayerParser(), Description.of("Player progress you want to see"))
                .required("activeQuest", activeQuestParser(notQuests), Description.of("Quest name of the quest you wish to see the progress for."))
                .handler((context) -> {
                    final OfflinePlayer offlinePlayer = context.get("player");
                    getProgress(context.sender(), offlinePlayer, context.get("activeQuest"));
                }));
    }

    private void getProgress(CommandSender sender, OfflinePlayer offlinePlayer, ActiveQuest activeQuest) {
        sender.sendMessage(Component.empty());

        QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());
        if (questPlayer != null) {
            if (activeQuest != null) {
                sender.sendMessage(notQuests.parse(
                        "<main>Completed Objectives for Quest <highlight>" + activeQuest.getQuest().getIdentifier() + "</highlight> of player <highlight2>"
                                + offlinePlayer.getName() + "</highlight2> <green>(online)</green>:"
                ));
                notQuests.getQuestManager().sendCompletedObjectivesAndProgress(questPlayer, activeQuest);

                sender.sendMessage(notQuests.parse(
                        "<main>Active Objectives for Quest <highlight>" + activeQuest.getQuest().getIdentifier() + "</highlight> of player <highlight2>"
                                + offlinePlayer.getName() + "</highlight2>" + getOfflineOnline(offlinePlayer) + ":"
                ));
                notQuests.getQuestManager().sendActiveObjectivesAndProgress(questPlayer, activeQuest, 0);


            } else {
                sender.sendMessage(notQuests.parse(
                        "<error>Quest was not found or active!"
                ));
                sender.sendMessage(notQuests.parse("<main>Active quests of player <highlight>" + offlinePlayer.getName() + "</highlight> <green>(online)</green>:"));
                int counter = 1;
                for (ActiveQuest activeQuest1 : questPlayer.getActiveQuests()) {
                    sender.sendMessage(notQuests.parse("<highlight>" + counter + ".</highlight> <main>" + activeQuest1.getQuest().getIdentifier()));
                    counter += 1;
                }
                sender.sendMessage(notQuests.parse("<unimportant>Total active quests: <highlight2>" + (counter - 1) + "</highlight2>."));
            }
        } else {
            sender.sendMessage(notQuests.parse("<error>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight>" + getOfflineOnline(offlinePlayer) + "did not accept any active quests."));
        }
    }

    private String getOfflineOnline(OfflinePlayer offlinePlayer) {
        return ((offlinePlayer.isOnline() ? " <green>(online)</green>" : " <red>(offline)</red>"));
    }
}
