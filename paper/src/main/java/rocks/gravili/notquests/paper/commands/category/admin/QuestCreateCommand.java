package rocks.gravili.notquests.paper.commands.category.admin;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.managers.data.Category;

import java.util.List;

public class QuestCreateCommand extends BaseCommand {
    public QuestCreateCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.literal("create", NQDescription.of("Create a new quest."))
                .required("questName", NQArguments.stringArgument(), NQDescription.of("Quest Name"), (context, input) ->
                        List.of("<Enter new Quest Name>"))
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
