package rocks.gravili.notquests.paper.commands.category.admin.category;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.managers.data.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryCreateCommand extends BaseCommand {
    public CategoryCreateCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        builder = builder.literal("categories").literal("create");

        commandManager.command(builder.required("categoryName", NQArguments.stringArgument(), NQDescription.of("Name of your new category"), (context, input) -> {
                            final List<String> suggestions = new ArrayList<>();
                            suggestions.add("<Enter new category name>");
                            suggestions.addAll(notQuests.getDataManager().getCategories().stream().map(Category::getDisplayName).toList());

                            return suggestions;
                        }
                )
                .handler((context) -> {
                    String fullNewCategoryIdentifier = context.get("categoryName");
                    fullNewCategoryIdentifier = fullNewCategoryIdentifier.replaceAll("[^0-9a-zA-Z-._]", "_");

                    if (notQuests.getDataManager().getCategory(fullNewCategoryIdentifier) != null) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Error: The category <highlight>" + fullNewCategoryIdentifier + "</highlight> already exists!"
                        ));
                        return;
                    }
                    if (fullNewCategoryIdentifier.endsWith(".") || fullNewCategoryIdentifier.startsWith(".")) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Error: The category <highlight>" + fullNewCategoryIdentifier + "</highlight> is invalid. It cannot contain a dot at the beginning or the end of the category. Dots are used to create a sub-category of an already existing category."
                        ));
                        return;
                    }

                    if (!fullNewCategoryIdentifier.contains(".")) {
                        notQuests.getDataManager().addCategory(
                                notQuests.getDataManager().createCategory(fullNewCategoryIdentifier, null)
                        );
                        context.sender().sendMessage(notQuests.parse("<success>Category <highlight>" + fullNewCategoryIdentifier + "</highlight> has successfully been created!"));
                    } else {
                        final String parentCategoryFullIdentifier = fullNewCategoryIdentifier.substring(0, fullNewCategoryIdentifier.lastIndexOf("."));
                        final Category foundParentCategory = notQuests.getDataManager().getCategory(parentCategoryFullIdentifier);
                        if (foundParentCategory == null) {
                            context.sender().sendMessage(notQuests.parse(
                                    "<error>Error: The parent company <highlight>" + parentCategoryFullIdentifier + "</highlight> does not exist."
                            ));
                            return;
                        }
                        notQuests.getDataManager().addCategory(
                                notQuests.getDataManager().createCategory(fullNewCategoryIdentifier.substring(fullNewCategoryIdentifier.lastIndexOf(".") + 1), foundParentCategory)
                        );
                        context.sender().sendMessage(notQuests.parse("<success>Category <highlight>" + fullNewCategoryIdentifier + "</highlight> has successfully been created!"));
                    }
                }));
    }
}
