package rocks.gravili.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import static org.incendo.cloud.bukkit.parser.PlayerParser.playerParser;

public class QuestShowActiveCommand extends BaseCommand {
    public QuestShowActiveCommand(NotQuests notQuests, Command.Builder<CommandSender> builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(CommandManager<CommandSender> commandManager) {
        commandManager.command(builder.commandDescription(Description.of("Shows the active quests of a player."))
                .literal("activeQuests")
                .required("player", playerParser(), Description.of("Player to display the completed quests of."))
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    final Player player = context.get("player");

                    QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                    String onlineMessagePart = questPlayer != null ? "<green>(online)</green>" : "<red>(offline)</red>";
                    if (questPlayer != null) {
                        context.sender().sendMessage(notQuests.parse("<main>Active quests of player <highlight>" + player.getName() + "</highlight>" + onlineMessagePart + ":"));
                        int counter = 1;
                        for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            context.sender().sendMessage(notQuests.parse("<highlight>" + counter + ".</highlight> <main>" + activeQuest.getQuest().getIdentifier()));
                            counter += 1;
                        }
                        context.sender().sendMessage(notQuests.parse("<unimportant>Total active quests: <highlight2>" + (counter - 1) + "</highlight2>."));
                    } else {
                        context.sender().sendMessage(notQuests.parse("<error>Seems like the player <highlight>" + player.getName() + "</highlight> <green>(online)</green> did not accept any active quests."));
                    }
                })
        );
    }
}
