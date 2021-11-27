/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquests.Managers;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import me.lucko.commodore.Commodore;
import me.lucko.commodore.CommodoreProvider;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import rocks.gravili.notquests.Commands.CommandNotQuests;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.Commands.newCMDs.AdminCommands;
import rocks.gravili.notquests.Commands.newCMDs.AdminEditCommands;
import rocks.gravili.notquests.Commands.newCMDs.arguments.ActionSelector;
import rocks.gravili.notquests.Commands.newCMDs.arguments.ApplyOnSelector;
import rocks.gravili.notquests.Commands.newCMDs.arguments.QuestSelector;
import rocks.gravili.notquests.Commands.old.CommandNotQuestsAdmin;
import rocks.gravili.notquests.NotQuests;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CommandManager {
    private final NotQuests main;
    private final boolean useNewCommands = true;
    private PaperCommandManager<CommandSender> commandManager;
    private MinecraftHelp<CommandSender> minecraftHelp;
    private Command.Builder<CommandSender> adminCommandBuilder;
    private Command.Builder<CommandSender> adminEditCommandBuilder;

    private Command.Builder<CommandSender> adminEditAddObjectiveCommandBuilder;
    private Command.Builder<CommandSender> adminEditAddRequirementCommandBuilder;
    private Command.Builder<CommandSender> adminEditAddRewardCommandBuilder;
    private Command.Builder<CommandSender> adminEditAddTriggerCommandBuilder;


    private final Commodore commodore;

    private AdminCommands adminCommands;
    private AdminEditCommands adminEditCommands;


    //Re-usable value flags
    public final CommandFlag<String[]> nametag_containsany;
    public final CommandFlag<String[]> nametag_equals;

    public final CommandFlag<Integer> applyOn; //0 = Quest
    public final CommandFlag<World> world;
    public final CommandFlag<String> triggerWorldString;

    public CommandManager(final NotQuests main) {
        this.main = main;
        if (CommodoreProvider.isSupported()) {
            // get a commodore instance
            commodore = CommodoreProvider.getCommodore(main);
        } else {
            commodore = null;
        }

        nametag_containsany = CommandFlag
                .newBuilder("nametag_containsany")
                .withArgument(StringArrayArgument.of("nametag_containsany",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender((CommandSender) context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Enter nametag_containsany flag value>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            completions.add("<nametag_containsany flag value>");
                            return completions;
                        }
                ))
                .withDescription(ArgumentDescription.of("This word or every word seperated by a space needs to be part of the nametag"))
                .build();

        nametag_equals = CommandFlag
                .newBuilder("nametag_equals")
                .withArgument(StringArrayArgument.of("nametag_equals",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender((CommandSender) context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Enter nametag_equals flag value>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            completions.add("<nametag_equals flag value>");
                            return completions;
                        }
                ))
                .withDescription(ArgumentDescription.of("What the nametag has to be equal"))
                .build();

        world = CommandFlag
                .newBuilder("world")
                .withArgument(WorldArgument.of("world"))
                .withDescription(ArgumentDescription.of("World Name"))
                .build();


        applyOn = CommandFlag
                .newBuilder("applyOn")
                .withArgument(ApplyOnSelector.of("applyOn", main, "quest"))
                .withDescription(ArgumentDescription.of("To which part of the Quest it should apply (Examples: 'Quest', 'O1', 'O2. (O1 = Objective 1)."))
                .build(); //0 = Quest

        triggerWorldString = CommandFlag
                .newBuilder("world_name")
                .withArgument(StringArgument.<CommandSender>newBuilder("world_name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[World Name / 'ALL']", "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("ALL");

                            for (final World world : Bukkit.getWorlds()) {
                                completions.add(world.getName());
                            }

                            return completions;
                        }
                ).single().build())
                .withDescription(ArgumentDescription.of("World where the Trigger applies (Examples: 'world_the_end', 'farmworld', 'world', 'ALL')."))
                .build();
    }


    private void registerCommodoreCompletions(Commodore commodore, PluginCommand command) {
        if (CommodoreProvider.isSupported()) {

            /*LiteralCommandNode<?> timeCommand = LiteralArgumentBuilder.literal("notquestsadmin")
                    .then(LiteralArgumentBuilder.literal("create")
                            .then(LiteralArgumentBuilder.literal("day"))
                            .then(LiteralArgumentBuilder.literal("noon"))
                            .then(LiteralArgumentBuilder.literal("night"))
                            .then(LiteralArgumentBuilder.literal("midnight"))
                            .then(RequiredArgumentBuilder.argument("time", IntegerArgumentType.integer())))
                    .then(LiteralArgumentBuilder.literal("delete")
                            .then(RequiredArgumentBuilder.argument("time", IntegerArgumentType.integer())))
                    .then(LiteralArgumentBuilder.literal("query")
                            .then(LiteralArgumentBuilder.literal("daytime"))
                            .then(LiteralArgumentBuilder.literal("gametime"))
                            .then(LiteralArgumentBuilder.literal("day"))
                    ).build();

            commodore.register(command, timeCommand);*/
        }

    }

    public void preSetupCommands() {
        if (useNewCommands) {
            //Cloud command framework
            try {
                commandManager = new PaperCommandManager<>(
                        /* Owning plugin */ main,
                        /* Coordinator function */ CommandExecutionCoordinator.simpleCoordinator(),
                        /* Command Sender -> C */ Function.identity(),
                        /* C -> Command Sender */ Function.identity()
                );
            } catch (final Exception e) {
                main.getLogManager().severe("There was an error setting up the commands.");
                return;
            }


            adminCommandBuilder = commandManager.commandBuilder("qa2", ArgumentDescription.of("Admin commands for NotQuests"), "notquestsadmin2, nqa2")
                    .permission("notquests.admin");

            adminEditCommandBuilder = adminCommandBuilder
                    .literal("edit")
                    .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Quest Name"));

            adminEditAddObjectiveCommandBuilder = adminEditCommandBuilder
                    .literal("objectives")
                    .literal("add");
            adminEditAddRequirementCommandBuilder = adminEditCommandBuilder
                    .literal("requirements")
                    .literal("add");
            adminEditAddRewardCommandBuilder = adminEditCommandBuilder
                    .literal("rewards")
                    .literal("add");
            adminEditAddTriggerCommandBuilder = adminEditCommandBuilder
                    .literal("triggers")
                    .literal("add")
                    .argument(ActionSelector.of("action", main), ArgumentDescription.of("Action which will be executed when the Trigger triggers."));

            //asynchronous completions
            if (commandManager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                commandManager.registerAsynchronousCompletions();
            }

            //brigadier/commodore
            try {
                commandManager.registerBrigadier();
                commandManager.brigadierManager().setNativeNumberSuggestions(false);
            } catch (final Exception e) {
                main.getLogger().warning("Failed to initialize Brigadier support: " + e.getMessage());
            }

            minecraftHelp = new MinecraftHelp<>(
                    "/qa2 help",
                    main.adventure()::sender,
                    commandManager
            );

            minecraftHelp.setHelpColors(MinecraftHelp.HelpColors.of(
                    NotQuestColors.main,
                    NamedTextColor.WHITE,
                    NotQuestColors.highlight,
                    NamedTextColor.GRAY,
                    NamedTextColor.DARK_GRAY
            ));
        }

    }

    public void setupCommands() {

        PluginCommand notQuestsAdminCommand = main.getCommand("notquestsadmin");
        if (notQuestsAdminCommand != null) {
            final CommandNotQuestsAdmin commandNotQuestsAdmin = new CommandNotQuestsAdmin(main);
            notQuestsAdminCommand.setTabCompleter(commandNotQuestsAdmin);
            notQuestsAdminCommand.setExecutor(commandNotQuestsAdmin);


            registerCommodoreCompletions(commodore, notQuestsAdminCommand);
        }
        //Register the notquests command & tab completer. This command will be used by Players
        final PluginCommand notQuestsCommand = main.getCommand("notquests");
        if (notQuestsCommand != null) {
            final CommandNotQuests commandNotQuests = new CommandNotQuests(main);
            notQuestsCommand.setExecutor(commandNotQuests);
            notQuestsCommand.setTabCompleter(commandNotQuests);


        }


        if (!useNewCommands) {
            /*final PluginCommand notQuestsAdminCommand = main.getCommand("notquestsadmin");
            if (notQuestsAdminCommand != null) {
                final CommandNotQuestsAdmin commandNotQuestsAdmin = new CommandNotQuestsAdmin(main);
                notQuestsAdminCommand.setTabCompleter(commandNotQuestsAdmin);
                notQuestsAdminCommand.setExecutor(commandNotQuestsAdmin);
            }
            //Register the notquests command & tab completer. This command will be used by Players
            final PluginCommand notQuestsCommand = main.getCommand("notquests");
            if (notQuestsCommand != null) {
                final CommandNotQuests commandNotQuests = new CommandNotQuests(main);
                notQuestsCommand.setExecutor(commandNotQuests);
                notQuestsCommand.setTabCompleter(commandNotQuests);
            }*/
        } else {





            constructCommands();
        }


    }


    public void constructCommands() {


       /* final Command.Builder<CommandSender> helpBuilder = commandManager.commandBuilder("notquestsadmin", "qa");
        commandManager.command(helpBuilder.meta(CommandMeta.DESCRIPTION, "fwefwe")
                .senderType(Player.class)
                .handler(commandContext -> {
                    minecraftHelp.queryCommands(commandContext.getOrDefault("", ""), commandContext.getSender());
                }));

        helpBuilder.literal("notquestsadmin",
                ArgumentDescription.of("Your Description"));*/


       /* commandManager.command(
                builder.literal("help", new String[0])
                .argument(com.magmaguy.shaded.cloud.arguments.standard.StringArgument.optional("query", com.magmaguy.shaded.cloud.arguments.standard.StringArgument.StringMode.GREEDY)).handler((context) -> {
                    this.minecraftHelp.queryCommands((String)context.getOrDefault("query", ""), context.getSender());
                })
        );*/


        //Help menu
        commandManager.command(adminCommandBuilder.meta(CommandMeta.DESCRIPTION, "Opens the help menu").handler((context) -> {
            minecraftHelp.queryCommands("qa2 *", context.getSender());
            final Audience audience = main.adventure().sender(context.getSender());
            final List<String> allArgs = context.getRawInput();
            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[What would you like to do?]", "[...]");

        }));
        commandManager.command(
                adminCommandBuilder.literal("help")
                        .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))

                        .handler(context -> {
                            minecraftHelp.queryCommands(context.getOrDefault("query", "qa2 *"), context.getSender());
                        })
        );


        MinecraftExceptionHandler<CommandSender> exceptionHandler = new MinecraftExceptionHandler<CommandSender>()
                .withArgumentParsingHandler()
                .withInvalidSenderHandler()
                .withInvalidSyntaxHandler()
                .withNoPermissionHandler()
                .withCommandExecutionHandler()
                .withDecorator(message -> {

                            return Component.text("NotQuests > ").color(NotQuestColors.main).append(Component.space()).append(message);
                        }
                )
                .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX, (sender, e) -> {
                    minecraftHelp.queryCommands(e.getMessage().split("syntax is: ")[1], sender);

                    return Component.text(e.getMessage(), NamedTextColor.RED);
                });

        exceptionHandler.apply(commandManager, main.adventure()::sender);

        adminCommands = new AdminCommands(main, commandManager, adminCommandBuilder);

        adminEditCommands = new AdminEditCommands(main, commandManager, adminEditCommandBuilder);


    }

    public final PaperCommandManager<CommandSender> getPaperCommandManager() {
        return commandManager;
    }

    public final Command.Builder<CommandSender> getAdminEditCommandBuilder() {
        return adminEditCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditAddObjectiveCommandBuilder() {
        return adminEditAddObjectiveCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditAddRequirementCommandBuilder() {
        return adminEditAddRequirementCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditAddRewardCommandBuilder() {
        return adminEditAddRewardCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditAddTriggerCommandBuilder() {
        return adminEditAddTriggerCommandBuilder;
    }


    public final AdminCommands getAdminCommands() {
        return adminCommands;
    }

    public final AdminEditCommands getAdminEditCommands() {
        return adminEditCommands;
    }
}
