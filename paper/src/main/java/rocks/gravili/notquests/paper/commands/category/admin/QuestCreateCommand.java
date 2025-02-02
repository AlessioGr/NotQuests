package rocks.gravili.notquests.paper.commands.category.admin;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.managers.data.Category;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static rocks.gravili.notquests.paper.commands.arguments.QuestParser.questParser;

public class QuestCreateCommand extends BaseCommand {
    public QuestCreateCommand(NotQuests notQuests, Command.Builder<CommandSender> builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(CommandManager<CommandSender> commandManager) {
        commandManager.command(builder.literal("create", Description.of("Create a new quest."))
                .required("questName", questParser(notQuests), Description.of("Quest Name"), (context, input) -> {
                    notQuests.getUtilManager().sendFancyCommandCompletion(context.sender(), input.input().split(" "), "[New Quest Name]", "");

                    return CompletableFuture.completedFuture(List.of(Suggestion.suggestion("<Enter new Quest Name>")));
                })
                .flag(notQuests.getCommandManager().categoryFlag)
                .handler((context) -> {
                    if (context.flags().contains(notQuests.getCommandManager().categoryFlag)) {
                        final Category category = context.flags().getValue(notQuests.getCommandManager().categoryFlag, notQuests.getDataManager().getDefaultCategory());
                        context.sender().sendMessage(notQuests.parse(notQuests.getQuestManager().createQuest(context.get("questName"), category)));
                    } else {
                        context.sender().sendMessage(notQuests.parse(notQuests.getQuestManager().createQuest(context.get("questName"))));
                    }
                }));
    }
}
