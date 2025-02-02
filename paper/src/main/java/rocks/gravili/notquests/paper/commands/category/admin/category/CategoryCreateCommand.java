package rocks.gravili.notquests.paper.commands.category.admin.category;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.managers.data.Category;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static rocks.gravili.notquests.paper.commands.arguments.CategoryParser.categoryParser;

public class CategoryCreateCommand extends BaseCommand {
    public CategoryCreateCommand(NotQuests notQuests, Command.Builder<CommandSender> builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(CommandManager<CommandSender> commandManager) {
        builder = builder.literal("creater");

        commandManager.command(builder.required("categoryName", categoryParser(notQuests), Description.of("Name of your new category"), (context, input) -> {
                            notQuests.getUtilManager().sendFancyCommandCompletion(context.sender(), input.input().split(" "), "[Name of your new category]", "");

                            final ArrayList<Suggestion> suggestions = new ArrayList<>();
                            suggestions.add(Suggestion.suggestion("<Enter new category name>"));
                            suggestions.addAll(notQuests.getDataManager().getCategories().stream().map(category -> Suggestion.suggestion(category.getDisplayName())).toList());

                            return CompletableFuture.completedFuture(suggestions);
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
