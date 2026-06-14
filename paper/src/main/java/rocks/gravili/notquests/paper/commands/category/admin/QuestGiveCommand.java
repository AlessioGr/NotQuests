package rocks.gravili.notquests.paper.commands.category.admin;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.Quest;

import static rocks.gravili.notquests.paper.commands.arguments.QuestArgument.questArgument;

public class QuestGiveCommand extends BaseCommand {

    public QuestGiveCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.literal("give", NQDescription.of("Gives a player a quest without bypassing the Quest requirements."))
                .required("player", NQArguments.playerArgument(), NQDescription.of("Player who should start the quest."))
                .required("quest", questArgument(notQuests), NQDescription.of("Name of the Quest the player should start."))
                .flag(NQFlag.presence("force", NQDescription.of("Force the player to start the quest, bypassing requirements.")))
                .handler((context) -> {
                    final Player player = context.get("player");
                    final Quest quest = context.get("quest");
                    if (context.flags().hasFlag("force")) {
                        context.sender().sendMessage(notQuests.parse("<main>" + notQuests.getQuestPlayerManager().forceAcceptQuest(player.getUniqueId(), quest)));
                    } else {
                        context.sender().sendMessage(notQuests.parse("<main>" + notQuests.getQuestPlayerManager().acceptQuest(player, quest, true, true)));
                        
                    }
                }));
    }
}
