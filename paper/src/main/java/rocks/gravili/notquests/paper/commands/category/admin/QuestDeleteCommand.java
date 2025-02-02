package rocks.gravili.notquests.paper.commands.category.admin;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.structs.Quest;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static rocks.gravili.notquests.paper.commands.arguments.QuestParser.questParser;

public class QuestDeleteCommand extends BaseCommand {
    public QuestDeleteCommand(NotQuests notQuests, Command.Builder<CommandSender> builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(CommandManager<CommandSender> commandManager) {
        commandManager.command(builder.literal("delete", Description.of("Delete an existing Quest."))
                .required("questName", questParser(notQuests), Description.of("Quest Name"), (context, input) -> {
                    notQuests.getUtilManager().sendFancyCommandCompletion(context.sender(), input.input().split(" "), "[Name of the Quest you want to delete]", "");

                    ArrayList<Suggestion> completions = new ArrayList<>();

                    for (Quest quest : notQuests.getQuestManager().getAllQuests()) {
                        completions.add(Suggestion.suggestion(quest.getIdentifier()));
                    }
                    return CompletableFuture.completedFuture(completions);
                })
                .handler((context) -> context.sender().sendMessage(notQuests.parse(notQuests.getQuestManager().deleteQuest(context.get("questName"))))));
    }
}
