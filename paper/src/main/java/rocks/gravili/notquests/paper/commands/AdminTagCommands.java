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

package rocks.gravili.notquests.paper.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.tags.Tag;
import rocks.gravili.notquests.paper.managers.tags.TagType;

public class AdminTagCommands {
  private final NotQuests main;
  private final PaperCommandManager<CommandSender> manager;
  private final Command.Builder<CommandSender> editBuilder;

  public AdminTagCommands(
      final NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> editBuilder) {
    this.main = main;
    this.manager = manager;
    this.editBuilder = editBuilder;

    manager.command(
        editBuilder
            .literal("create")
            .literal("Boolean")
            .argument(StringArgument.of("name"), ArgumentDescription.of("Tag Name"))
            .flag(main.getCommandManager().categoryFlag)
            .meta(CommandMeta.DESCRIPTION, "Creates a new Boolean tag.")
            .handler(
                (context) -> {
                  final String tagName = context.get("name");

                  if (main.getTagManager().getTag(tagName) != null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The tag <highlight>"
                                    + tagName
                                    + "</highlight> already exists!"));
                    return;
                  }
                  final Tag tag = new Tag(main, tagName, TagType.BOOLEAN);
                  if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    final Category category =
                        context
                            .flags()
                            .getValue(
                                main.getCommandManager().categoryFlag,
                                main.getDataManager().getDefaultCategory());
                    tag.setCategory(category);
                  }
                  main.getTagManager().addTag(tag);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>The boolean tag <highlight>"
                                  + tagName
                                  + "</highlight> has been added successfully!"));
                }));

    manager.command(
        editBuilder
            .literal("create")
            .literal("Integer")
            .argument(StringArgument.of("name"), ArgumentDescription.of("Tag Name"))
            .flag(main.getCommandManager().categoryFlag)
            .meta(CommandMeta.DESCRIPTION, "Creates a new Integer tag.")
            .handler(
                (context) -> {
                  final String tagName = context.get("name");

                  if (main.getTagManager().getTag(tagName) != null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The tag <highlight>"
                                    + tagName
                                    + "</highlight> already exists!"));
                    return;
                  }

                  final Tag tag = new Tag(main, tagName, TagType.INTEGER);
                  if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    final Category category =
                        context
                            .flags()
                            .getValue(
                                main.getCommandManager().categoryFlag,
                                main.getDataManager().getDefaultCategory());
                    tag.setCategory(category);
                  }
                  main.getTagManager().addTag(tag);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>The integer tag <highlight>"
                                  + tagName
                                  + "</highlight> has been added successfully!"));
                }));

    manager.command(
        editBuilder
            .literal("create")
            .literal("Float")
            .argument(StringArgument.of("name"), ArgumentDescription.of("Tag Name"))
            .flag(main.getCommandManager().categoryFlag)
            .meta(CommandMeta.DESCRIPTION, "Creates a new Float tag.")
            .handler(
                (context) -> {
                  final String tagName = context.get("name");

                  if (main.getTagManager().getTag(tagName) != null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The tag <highlight>"
                                    + tagName
                                    + "</highlight> already exists!"));
                    return;
                  }

                  final Tag tag = new Tag(main, tagName, TagType.FLOAT);
                  if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    final Category category =
                        context
                            .flags()
                            .getValue(
                                main.getCommandManager().categoryFlag,
                                main.getDataManager().getDefaultCategory());
                    tag.setCategory(category);
                  }
                  main.getTagManager().addTag(tag);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>The float tag <highlight>"
                                  + tagName
                                  + "</highlight> has been added successfully!"));
                }));

    manager.command(
        editBuilder
            .literal("create")
            .literal("Double")
            .argument(StringArgument.of("name"), ArgumentDescription.of("Tag Name"))
            .flag(main.getCommandManager().categoryFlag)
            .meta(CommandMeta.DESCRIPTION, "Creates a new Double tag.")
            .handler(
                (context) -> {
                  final String tagName = context.get("name");

                  if (main.getTagManager().getTag(tagName) != null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The tag <highlight>"
                                    + tagName
                                    + "</highlight> already exists!"));
                    return;
                  }

                  final Tag tag = new Tag(main, tagName, TagType.DOUBLE);
                  if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    final Category category =
                        context
                            .flags()
                            .getValue(
                                main.getCommandManager().categoryFlag,
                                main.getDataManager().getDefaultCategory());
                    tag.setCategory(category);
                  }
                  main.getTagManager().addTag(tag);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>The double tag <highlight>"
                                  + tagName
                                  + "</highlight> has been added successfully!"));
                }));

    manager.command(
        editBuilder
            .literal("create")
            .literal("String")
            .argument(StringArgument.of("name"), ArgumentDescription.of("Tag Name"))
            .flag(main.getCommandManager().categoryFlag)
            .meta(CommandMeta.DESCRIPTION, "Creates a new String tag.")
            .handler(
                (context) -> {
                  final String tagName = context.get("name");

                  if (main.getTagManager().getTag(tagName) != null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The tag <highlight>"
                                    + tagName
                                    + "</highlight> already exists!"));
                    return;
                  }

                  final Tag tag = new Tag(main, tagName, TagType.STRING);
                  if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    final Category category =
                        context
                            .flags()
                            .getValue(
                                main.getCommandManager().categoryFlag,
                                main.getDataManager().getDefaultCategory());
                    tag.setCategory(category);
                  }
                  main.getTagManager().addTag(tag);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>The string tag <highlight>"
                                  + tagName
                                  + "</highlight> has been added successfully!"));
                }));

    manager.command(
        editBuilder
            .literal("list")
            .meta(CommandMeta.DESCRIPTION, "Lists all tags")
            .handler(
                (context) -> {
                  context.getSender().sendMessage(main.parse("<highlight>All tags:"));
                  int counter = 1;

                  for (final Tag tag : main.getTagManager().getTags()) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<highlight>"
                                    + counter
                                    + ".</highlight> <main>"
                                    + tag.getTagName()
                                    + "</main> <highlight2>Type: <main>"
                                    + tag.getTagType().name()));
                    counter++;
                  }
                }));

    manager.command(
        editBuilder
            .literal("delete", "remove")
            .argument(
                StringArgument.<CommandSender>newBuilder("Tag Name")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[Tag Name]",
                                  "");

                          final ArrayList<String> suggestions = new ArrayList<>();
                          for (final Tag tag : main.getTagManager().getTags()) {
                            suggestions.add("" + tag.getTagName());
                          }
                          return suggestions;
                        })
                    .single()
                    .build())
            .meta(CommandMeta.DESCRIPTION, "Deletes an existing tag.")
            .handler(
                (context) -> {
                  final String tagName = context.get("Tag Name");

                  final Tag foundTag = main.getTagManager().getTag(tagName);
                  if (foundTag == null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The tag <highlight>"
                                    + tagName
                                    + "</highlight> doesn't exists!"));
                    return;
                  }

                  main.getTagManager().deleteTag(foundTag);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>The tag <highlight>"
                                  + tagName
                                  + "</highlight> has been deleted successfully!"));
                }));
  }
}
