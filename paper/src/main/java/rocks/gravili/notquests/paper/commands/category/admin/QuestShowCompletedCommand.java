package rocks.gravili.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.structs.CompletedQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Date;

import static org.incendo.cloud.bukkit.parser.PlayerParser.playerParser;

public class QuestShowCompletedCommand extends BaseCommand {

    private final Date resultDate;

    public QuestShowCompletedCommand(NotQuests notQuests, Command.Builder<CommandSender> builder) {
        super(notQuests, builder);
        resultDate = new Date();
    }

    @Override
    public void apply(CommandManager<CommandSender> commandManager) {
        commandManager.command(builder.commandDescription(Description.of("Completes an active quest for a player"))
                .literal("completedQuests")
                .required("player", playerParser(), Description.of("Player to display the completed quests of."))
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    final OfflinePlayer player = context.get("player");
                    if (player != null) {
                        QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                        String onlineMessagePart = questPlayer != null ? "<green>(online)</green>" : "<red>(offline)</red>";
                        if (questPlayer != null) {
                            context.sender().sendMessage(notQuests.parse("<main>Completed quests of player <highlight>" + player.getName() + "</highlight> " + onlineMessagePart + ":"));
                            int counter = 1;
                            for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                                resultDate.setTime(completedQuest.getTimeCompleted());
                                context.sender().sendMessage(notQuests.parse("<highlight>" + counter + ".</highlight> <highlight2>" + completedQuest.getQuest().getIdentifier()
                                        + "</highlight2> <main>Completed: </main><highlight2>" + resultDate + "</highlight2>"
                                ));
                                counter += 1;
                            }

                            context.sender().sendMessage(notQuests.parse("<unimportant>Total completed quests: <highlight2>" + (counter - 1) + "</highlight2>."));
                        } else {
                            context.sender().sendMessage(notQuests.parse("<error>Seems like the player <highlight>" + player.getName() + "</highlight> <green>(online)</green> never completed any quests."));
                        }
                    }
                })
        );
    }
}
