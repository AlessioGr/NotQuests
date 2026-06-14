package rocks.gravili.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.CompletedQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

import static rocks.gravili.notquests.paper.commands.arguments.QuestArgument.questArgument;

public class QuestResetAndRemoveCommand extends BaseCommand {
    public QuestResetAndRemoveCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {

        builder = builder.commandDescription(NQDescription.of("Removes the quest from a specific player players, removes it from completed quests, resets the accept cooldown and basically everything else."))
                .literal("resetAndRemoveQuest");
        commandManager.command(builder
                .required("player", NQArguments.playerArgument(), NQDescription.of("Player name"))
                .required("quest", questArgument(notQuests), NQDescription.of("Name of the Quest which should be reset and removed."))
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    final OfflinePlayer player = context.get("player");

                    removeQuest(player, context);
                    context.sender().sendMessage(notQuests.parse("<success>Operation done!"));
                }));

        commandManager.command(builder
                .literal("all")
                .required("quest", questArgument(notQuests), NQDescription.of("Name of the Quest which should be reset and removed."))
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());

                    notQuests.getQuestPlayerManager().getAllQuestPlayersForAllProfiles().forEach(questPlayer -> {
                        removeQuest(Bukkit.getOfflinePlayer(questPlayer.getUniqueId()), context);
                    });

                    context.sender().sendMessage(notQuests.parse("<success>Operation done!"));
                }));
    }

    private void removeQuest(OfflinePlayer offlinePlayer, NQCommandContext context) {
        final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());

        if (questPlayer == null) {
            context.sender().sendMessage(notQuests.parse(
                    "<error>Error: QuestPlayer of Player <highlight>" + offlinePlayer.getName()+ "</highlight> not found."
            ));
            return;
        }
        final Quest quest = context.get("quest");
        final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            if (activeQuest.getQuest().equals(quest)) {
                activeQuestsToRemove.add(activeQuest);
                context.sender().sendMessage(notQuests.parse("<success>Removed the quest as an active quest for the player with the UUID <highlight>"
                        + questPlayer.getUniqueId().toString() + "</highlight> and name <highlight2>"
                        + Bukkit.getOfflinePlayer(questPlayer.getUniqueId()).getName() + "</highlight2>."
                ));

            }
        }

        questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

        final ArrayList<CompletedQuest> completedQuestsToRemove = new ArrayList<>();

        for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
            if (completedQuest.getQuest().equals(quest)) {
                completedQuestsToRemove.add(completedQuest);
                context.sender().sendMessage(notQuests.parse("<success>Removed the quest as a completed quest for the player with the UUID <highlight>"
                        + questPlayer.getUniqueId().toString() + "</highlight> and name <highlight2>"
                        + Bukkit.getOfflinePlayer(questPlayer.getUniqueId()).getName() + "</highlight2>."
                ));
            }

        }

        questPlayer.getCompletedQuests().removeAll(completedQuestsToRemove);
    }
}
