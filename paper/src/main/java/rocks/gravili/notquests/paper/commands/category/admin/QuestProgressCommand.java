package rocks.gravili.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import static rocks.gravili.notquests.paper.commands.arguments.ActiveQuestArgument.activeQuestArgument;

public class QuestProgressCommand extends BaseCommand {

    public QuestProgressCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.literal("progress", NQDescription.of("Shows the progress for a quest of another player"))
                .required("player", NQArguments.playerArgument(), NQDescription.of("Player progress you want to see"))
                .required("activeQuest", activeQuestArgument(notQuests), NQDescription.of("Quest name of the quest you wish to see the progress for."))
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
