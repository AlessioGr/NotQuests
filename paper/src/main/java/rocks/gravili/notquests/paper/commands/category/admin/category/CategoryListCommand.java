package rocks.gravili.notquests.paper.commands.category.admin.category;

import net.kyori.adventure.text.Component;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.managers.data.Category;

public class CategoryListCommand extends BaseCommand {
    public CategoryListCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.commandDescription(NQDescription.of("Lists all categories."))
                .literal("categories")
                .literal("list")
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse("<highlight>All categories:"));
                    int counter = 1;
                    for (final Category category : notQuests.getDataManager().getCategories()) {
                        context.sender().sendMessage(notQuests.parse("<highlight>" + counter + ".</highlight> <main>" + category.getCategoryFullName()));
                        counter++;
                    }
                }));
    }
}
