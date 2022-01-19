package rocks.gravili.notquests.paper.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.tags.Tag;
import rocks.gravili.notquests.paper.managers.tags.TagType;

public class AdminItemsCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> editBuilder;


    public AdminItemsCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> editBuilder) {
        this.main = main;
        this.manager = manager;
        this.editBuilder = editBuilder;

        /*manager.command(editBuilder.literal("create")
                .literal("Boolean")
                .argument(StringArgument.of("name"), ArgumentDescription.of("Tag Name"))
                .flag(main.getCommandManager().categoryFlag)
                .meta(CommandMeta.DESCRIPTION, "Creates a new Boolean tag.")
                .handler((context) -> {
                    final String tagName = context.get("name");



                    if(main.getTagManager().getTag(tagName) != null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Error: The tag <highlight>" + tagName + "</highlight> already exists!"
                        ));
                        return;
                    }
                    Tag tag = new Tag(main, tagName, TagType.BOOLEAN);
                    if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                        final Category category = context.flags().getValue(main.getCommandManager().categoryFlag, main.getDataManager().getDefaultCategory());
                        tag.setCategory(category);
                    }
                    main.getTagManager().addTag(tag);

                    context.getSender().sendMessage(main.parse(
                            "<success>The tag <highlight>" + tagName + "</highlight> has been added successfully!"
                    ));
                }));*/
    }
}
