/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.commands.category.tag;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.tags.Tag;
import rocks.gravili.notquests.paper.managers.tags.TagType;


public class AdminTagCommands {
    private final NotQuests main;
    private final NQCommandManager manager;
    private final NQCommandBuilder editBuilder;

    public AdminTagCommands(final NotQuests main, NQCommandManager manager, NQCommandBuilder editBuilder) {
        this.main = main;
        this.manager = manager;
        this.editBuilder = editBuilder;

        manager.command(editBuilder.commandDescription(NQDescription.of("Creates a new tag of given type"))
                .literal("create")
                .required("type", rocks.gravili.notquests.paper.commands.framework.NQArguments.enumArgument(TagType.class))
                .required("name", NQArguments.stringArgument(), NQDescription.of("Tag Name"))
                .handler(commandContext -> {
                    var tagType = (TagType) commandContext.get("type");
                    var tagName = (String) commandContext.get("name");

                    if (main.getTagManager().getTag(tagName) != null) {
                        commandContext.sender().sendMessage(main.parse("<error>Error: The tag <highlight>" + tagName + "</highlight> already exists!"));
                        return;
                    }

                    var tag = new Tag(main, tagName, tagType);
                    if (commandContext.flags().isPresent(main.getCommandManager().categoryFlag)) {
                        final Category category = commandContext.flags().getValue(
                                main.getCommandManager().categoryFlag,
                                main.getDataManager().getDefaultCategory()
                        );
                        tag.setCategory(category);
                    }
                    main.getTagManager().addTag(tag);

                    commandContext.sender().sendMessage(main.parse(
                            "<success>The " + tagType.name().toLowerCase() + " tag <highlight>" + tagName
                                    + "</highlight> has been added successfully!")
                    );
                })

        );

        manager.command(editBuilder.commandDescription(NQDescription.of("Lists all tags"))
                .literal("list")
                .handler((context) -> {
                    context.sender().sendMessage(main.parse("<highlight>All tags:"));
                    int counter = 1;

                    for (final Tag tag : main.getTagManager().getTags()) {
                        context.sender().sendMessage(main.parse(
                                "<highlight>"
                                        + counter
                                        + ".</highlight> <main>"
                                        + tag.getTagName()
                                        + "</main> <highlight2>Type: <main>"
                                        + tag.getTagType().name())
                        );
                        counter++;
                    }
                }));

        manager.command(editBuilder.commandDescription(NQDescription.of("Deletes an existing tag."))
                .literal("delete", "remove")
                .required("tag-name", NQArguments.stringArgument(), NQDescription.of("Tag Name"), (context, input) ->
                        main.getTagManager().getTags().stream().map(Tag::getTagName).toList())
                .handler((context) -> {
                    final String tagName = context.get("tag-name");

                    var foundTag = main.getTagManager().getTag(tagName);
                    if (foundTag == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Error: The tag <highlight>"
                                        + tagName
                                        + "</highlight> doesn't exists!")
                        );
                        return;
                    }

                    main.getTagManager().deleteTag(foundTag);

                    context.sender().sendMessage(main.parse(
                            "<success>The tag <highlight>"
                                    + tagName
                                    + "</highlight> has been deleted successfully!")
                    );
                }));

        final NQFlag tagCheckPlayerFlag =
                NQFlag.builder("player").withArgument(rocks.gravili.notquests.paper.commands.framework.NQArguments.playerArgument()).build();

        manager.command(editBuilder.commandDescription(NQDescription.of("Shows a player's current value for a tag."))
                .literal("check")
                .required("tag-name", NQArguments.stringArgument(), NQDescription.of("Tag Name"), (context, input) ->
                        main.getTagManager().getTags().stream().map(Tag::getTagName).toList())
                .flag(tagCheckPlayerFlag)
                .handler((context) -> {
                    final String tagName = context.get("tag-name");

                    final Tag foundTag = main.getTagManager().getTag(tagName);
                    if (foundTag == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Error: The tag <highlight>"
                                        + tagName
                                        + "</highlight> doesn't exists!")
                        );
                        return;
                    }

                    final Player playerSelector = context.flags().getValue(tagCheckPlayerFlag, null);
                    final Player player;
                    if (playerSelector != null) {
                        player = playerSelector;
                    } else if (context.sender() instanceof final Player senderPlayer) {
                        player = senderPlayer;
                    } else {
                        context.sender().sendMessage(main.parse(
                                "<error>Error: Run this in-game, or specify a player with <highlight>--player</highlight> from console.")
                        );
                        return;
                    }

                    final Object tagValue = main.getQuestPlayerManager()
                            .getOrCreateQuestPlayer(player.getUniqueId())
                            .getTagValue(foundTag.getTagName());

                    context.sender().sendMessage(main.parse(
                            "<main>" + foundTag.getTagType().name().toLowerCase() + " tag <highlight>"
                                    + foundTag.getTagName() + "</highlight> for <highlight2>" + player.getName()
                                    + "</highlight2>:</main> <highlight>" + (tagValue != null ? tagValue : "not set"))
                    );
                }));
    }
}