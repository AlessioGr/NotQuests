package rocks.gravili.notquests.paper.commands.category.admin;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import static rocks.gravili.notquests.paper.commands.arguments.ActiveQuestArgument.activeQuestArgument;

public class QuestCompleteCommand extends BaseCommand {

    public QuestCompleteCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.commandDescription(NQDescription.of("Completes an active quest for a player"))
                .literal("completeQuest")
                .required("player", NQArguments.playerArgument(), NQDescription.of("Player name whose quest should be completed."))
                .required("activeQuest", activeQuestArgument(notQuests), NQDescription.of("Active quest which should be completed."))
                .handler((context) -> {
                    final Player player = context.get("player");
                    final ActiveQuest activeQuest = context.get("activeQuest");
                    final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                    if (questPlayer != null) {
                        questPlayer.forceActiveQuestCompleted(activeQuest);
                        context.sender().sendMessage(notQuests.parse(
                                "<success>The active quest <highlight>" + activeQuest.getQuest().getIdentifier() + "</highlight> has been completed for player <highlight2>" + player.getName() + "</highlight2>!"
                        ));

                    } else {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Player <highlight>" + player.getName() + "</highlight> seems to not have accepted any quests!"
                        ));
                    }
                }));
    }
}
