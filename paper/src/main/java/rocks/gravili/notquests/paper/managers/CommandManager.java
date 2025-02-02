/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.managers;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.component.TypedCommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.extras.AudienceProvider;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.*;
import rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionParser;
import rocks.gravili.notquests.paper.commands.arguments.MultiActionsParser;
import rocks.gravili.notquests.paper.commands.arguments.NQNPCParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.commands.category.item.AdminItemsCommand;
import rocks.gravili.notquests.paper.commands.category.tag.AdminTagCommands;
import rocks.gravili.notquests.paper.conversation.ConversationManager;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.bukkit.parser.WorldParser.worldParser;
import static org.incendo.cloud.minecraft.extras.parser.ComponentParser.miniMessageParser;
import static org.incendo.cloud.parser.standard.DoubleParser.doubleParser;
import static org.incendo.cloud.parser.standard.DurationParser.durationParser;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static org.incendo.cloud.parser.standard.LongParser.longParser;
import static org.incendo.cloud.parser.standard.StringArrayParser.stringArrayParser;
import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static rocks.gravili.notquests.paper.commands.arguments.ActionParser.actionParser;
import static rocks.gravili.notquests.paper.commands.arguments.ApplyOnParser.applyOnParser;
import static rocks.gravili.notquests.paper.commands.arguments.CategoryParser.categoryParser;
import static rocks.gravili.notquests.paper.commands.arguments.ObjectiveParser.objectiveParser;
import static rocks.gravili.notquests.paper.commands.arguments.QuestParser.questParser;

public class CommandManager {
    private final NotQuests main;
    // Re-usable value flags
    public CommandFlag<String[]> nametag_containsany;
    public CommandFlag<String[]> nametag_equals;
    public CommandFlag<Component> taskDescription;
    public CommandFlag<Integer> maxDistance;
    public CommandFlag<Category> categoryFlag;
    public CommandFlag<Duration> delayFlag;

    public CommandFlag<String> speakerColor;
    public CommandFlag<Integer> applyOn; // 0 = Quest
    public CommandFlag<World> world;
    public CommandFlag<Double> locationX;
    public CommandFlag<Double> locationY;
    public CommandFlag<Double> locationZ;

    public CommandFlag<String> triggerWorldString;
    public CommandFlag<Long> minimumTimeAfterCompletion;
    public CommandFlag<String> withProjectKorraAbilityFlag;
    private LegacyPaperCommandManager<CommandSender> commandManager;
    // Builders
    private Command.Builder<CommandSender> adminCommandBuilder;
    private Command.Builder<CommandSender> adminEditCommandBuilder;
    private Command.Builder<CommandSender> adminTagCommandBuilder;
    private Command.Builder<CommandSender> adminItemsCommandBuilder;
    private Command.Builder<CommandSender> adminConversationCommandBuilder;
    private Command.Builder<CommandSender> adminEditAddObjectiveCommandBuilder;
    private Command.Builder<CommandSender> adminEditAddRequirementCommandBuilder;
    private Command.Builder<CommandSender> adminEditAddRewardCommandBuilder;
    private Command.Builder<CommandSender> adminEditAddTriggerCommandBuilder;
    private Command.Builder<CommandSender> adminEditObjectiveAddUnlockConditionCommandBuilder;
    private Command.Builder<CommandSender> adminEditObjectiveAddProgressConditionCommandBuilder;
    private Command.Builder<CommandSender> adminEditObjectiveAddCompleteConditionCommandBuilder;

    private Command.Builder<CommandSender> adminEditObjectiveAddRewardCommandBuilder;
    private Command.Builder<CommandSender> adminAddActionCommandBuilder;
    private Command.Builder<CommandSender> adminExecuteActionCommandBuilder;

    private Command.Builder<CommandSender> adminActionsCommandBuilder;
    private Command.Builder<CommandSender> adminActionsEditCommandBuilder;
    private Command.Builder<CommandSender> adminActionsAddConditionCommandBuilder;
    private Command.Builder<CommandSender> adminAddConditionCommandBuilder;
    private Command.Builder<CommandSender> adminConditionCheckCommandBuilder;

    private AdminCommands adminCommands;
    private AdminEditCommands adminEditCommands;
    private AdminTagCommands adminTagCommands;
    private AdminItemsCommand adminItemsCommands;
    private AdminConversationCommands adminConversationCommands;
    // User
    private MinecraftHelp<CommandSender> minecraftUserHelp;
    private Command.Builder<CommandSender> userCommandBuilder;
    private UserCommands userCommands;
    // Admin
    private MinecraftHelp<CommandSender> minecraftAdminHelp;
    private Command.Builder<CommandSender> adminEditObjectivesBuilder;

    private CommandMap commandMap;

    private CommandPostProcessor<CommandSender> commandPostProcessor;

    public CommandManager(final NotQuests main) {
        this.main = main;

        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception ignored) {
            commandMap = null;
        }

        createCommandFlags();
    }

    public void createCommandFlags() {
        nametag_containsany = CommandFlag.builder("nametag_containsany")
                .withComponent(TypedCommandComponent.builder("nametag_containsany", stringArrayParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "<Enter nametag_containsany flag value>", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("<nametag_containsany flag value>"));
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("This word or every word seperated by a space needs to be part of the nametag"))
                .build();


        nametag_equals = CommandFlag.builder("nametag_equals")
                .withComponent(TypedCommandComponent.builder("nametag_equals", stringArrayParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "<Enter nametag_equals flag value>", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("<Enter nametag_equals flag value>"));
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("What the nametag has to be equal"))
                .build();

        taskDescription = CommandFlag.builder("taskDescription")
                .withComponent(TypedCommandComponent.builder("taskDescription", miniMessageParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Enter task description]", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("[Enter task description]"));
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Custom description of the task"))
                .build();

        speakerColor = CommandFlag.builder("speakerColor")
                .withComponent(TypedCommandComponent.builder("speakerColor", stringParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Enter speaker color (default: <WHITE>)]", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            for (NamedTextColor namedTextColor : NamedTextColor.NAMES.values()) {
                                completions.add(Suggestion.suggestion("<" + namedTextColor.toString() + ">"));
                            }
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Color of the speaker name"))
                .build();

        maxDistance = CommandFlag.builder("maxDistance")
                .withComponent(TypedCommandComponent.builder("maxDistance", integerParser(0))
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Enter maximum distance of two locations]", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("[Enter maximum distance of two locations]"));
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Enter maximum distance of two locations"))
                .build();

        world = CommandFlag.builder("world")
                .withComponent(TypedCommandComponent.builder("world", worldParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[World Name]", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            for (final World world : Bukkit.getWorlds()) {
                                completions.add(Suggestion.suggestion(world.getName()));
                            }
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("World Name"))
                .build();

        applyOn = CommandFlag.builder("applyOn")
                .withComponent(TypedCommandComponent.builder("applyOn", applyOnParser(main, "quest"))
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[0 = Quest, 1 = Objective 1, 2 = Objective 2, ...]", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("0"));
                            completions.add(Suggestion.suggestion("1"));
                            completions.add(Suggestion.suggestion("2"));
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("To which part of the Quest it should apply (Examples: 'Quest', 'O1', 'O2. (O1 = Objective 1)."))
                .build();

        triggerWorldString = CommandFlag.builder("world_name")
                .withComponent(TypedCommandComponent.builder("world_name", stringParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[World Name / 'ALL']", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("ALL"));
                            for (final World world : Bukkit.getWorlds()) {
                                completions.add(Suggestion.suggestion(world.getName()));
                            }
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("World where the Trigger applies (Examples: 'world_the_end', 'farmworld', 'world', 'ALL')."))
                .build();

        minimumTimeAfterCompletion = CommandFlag.builder("waitTimeAfterCompletion")
                .withComponent(TypedCommandComponent.builder("waitTimeAfterCompletion", longParser(0))
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Enter minimum time you have to wait after completion.]", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("0"));
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Enter minimum time you have to wait after completion."))
                .build(); // 0 = Quest

        if (main.getIntegrationsManager().isProjectKorraEnabled()) {
            withProjectKorraAbilityFlag = CommandFlag.builder("withProjectKorraAbility")
                    .withComponent(TypedCommandComponent.builder("withProjectKorraAbility", stringParser())
                            .suggestionProvider((context, lastString) -> {
                                main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Ability Name / 'ALL']", "");
                                ArrayList<Suggestion> completions = new ArrayList<>();
                                completions.add(Suggestion.suggestion("ALL"));
                                completions.addAll(main.getIntegrationsManager().getProjectKorraManager().getAbilityCompletions().stream().map(Suggestion::suggestion).toList());
                                return CompletableFuture.completedFuture(completions);
                            }))
                    .withDescription(Description.of("Project Korra Ability"))
                    .build();
        }

        categoryFlag = CommandFlag.builder("category")
                .withComponent(TypedCommandComponent.builder("category", categoryParser(main))
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Category Name]", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("[Enter Category Name]"));
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Category name"))
                .build();

        delayFlag = CommandFlag.builder("delay")
                .withComponent(TypedCommandComponent.builder("delay", durationParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Enter delay in milliseconds]", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("[Enter delay in milliseconds]"));
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Delay in milliseconds"))
                .build();

        locationX = CommandFlag.builder("locationX")
                .withComponent(TypedCommandComponent.builder("locationX", doubleParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Enter x coordinate location]", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("[Enter x coordinate location]"));
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Enter x coordinate location"))
                .build();

        locationY = CommandFlag.builder("locationY")
                .withComponent(TypedCommandComponent.builder("locationY", doubleParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Enter y coordinate location]", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("[Enter y coordinate location]"));
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Enter y coordinate location"))
                .build();

        locationZ = CommandFlag.builder("locationZ")
                .withComponent(TypedCommandComponent.builder("locationZ", doubleParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Enter z coordinate location]", "");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("[Enter z coordinate location]"));
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Enter z coordinate location"))
                .build();
    }

    public final CommandMap getCommandMap() {
        return commandMap;
    }

    public void preSetupCommands() {
        // Cloud command framework
        try {
            commandManager = new LegacyPaperCommandManager<>(main.getMain(), ExecutionCoordinator.simpleCoordinator(), SenderMapper.identity());
        } catch (final Exception e) {
            main.getLogManager().severe("There was an error setting up the commands.");
            return;
        }

        preSetupGeneralCommands();
        preSetupUserCommands();
        preSetupAdminCommands();
    }

    public void preSetupGeneralCommands() {
        // asynchronous completions
        if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions();
        }


        // brigadier
        try {
            commandManager.registerLegacyPaperBrigadier();
            CloudBrigadierManager<CommandSender, ?> cloudBrigadierManager = commandManager.brigadierManager();
            cloudBrigadierManager.setNativeNumberSuggestions(false);

            cloudBrigadierManager.registerMapping(
                    new TypeToken<StringVariableValueParser<CommandSender>>() {
                    }, builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));

            // Greedy string to prevent false, red brigardier color when entering special symbols like a
            // comma
            cloudBrigadierManager.registerMapping(
                    new TypeToken<NumberVariableValueParser<CommandSender>>() {
                    },
                    builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
            cloudBrigadierManager.registerMapping(
                    new TypeToken<BooleanVariableValueParser<CommandSender>>() {
                    },
                    builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
            cloudBrigadierManager.registerMapping(
                    new TypeToken<MultiActionsParser<CommandSender>>() {
                    },
                    builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
            cloudBrigadierManager.registerMapping(
                    new TypeToken<ItemStackSelectionParser<CommandSender>>() {
                    },
                    builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));

            cloudBrigadierManager.registerMapping(
                    new TypeToken<NQNPCParser<CommandSender>>() {
                    },
                    builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
        } catch (final Exception e) {
            main.getLogManager().warn("Failed to initialize Brigadier support: <highlight>" + e.getMessage());
        }

        commandPostProcessor = new CommandPostProcessor<>(main);
        commandManager.registerCommandPostProcessor(commandPostProcessor);
    }

    public void preSetupUserCommands() {
        minecraftUserHelp = MinecraftHelp.create("/nq help", commandManager, AudienceProvider.nativeAudience());

        minecraftUserHelp.colors().primary().styleApply(Style.style(NotQuestColors.main).toBuilder());
        minecraftUserHelp.colors().highlight().styleApply(Style.style(NamedTextColor.WHITE).toBuilder());
        minecraftUserHelp.colors().alternateHighlight().styleApply(Style.style(NotQuestColors.highlight).toBuilder());
        minecraftUserHelp.colors().text().styleApply(Style.style(NamedTextColor.GRAY).toBuilder());
        minecraftUserHelp.colors().accent().styleApply(Style.style(NamedTextColor.DARK_GRAY).toBuilder());

        userCommandBuilder = commandManager.commandBuilder(
                        "nq",
                        Description.of("Player commands for NotQuests"),
                        "notquests",
                        "nquests",
                        "nquest",
                        "notquest",
                        "quest",
                        "quests",
                        "q",
                        "qg")
                .permission("notquests.use");
    }

    public void preSetupAdminCommands() {

        minecraftAdminHelp = MinecraftHelp.create("/qa help", commandManager, AudienceProvider.nativeAudience());

        minecraftAdminHelp.colors().primary().styleApply(Style.style(NotQuestColors.main).toBuilder());
        minecraftAdminHelp.colors().highlight().styleApply(Style.style(NamedTextColor.WHITE).toBuilder());
        minecraftAdminHelp.colors().alternateHighlight().styleApply(Style.style(NotQuestColors.highlight).toBuilder());
        minecraftAdminHelp.colors().text().styleApply(Style.style(NamedTextColor.GRAY).toBuilder());
        minecraftAdminHelp.colors().accent().styleApply(Style.style(NamedTextColor.DARK_GRAY).toBuilder());

        adminCommandBuilder = commandManager.commandBuilder(
                        "nqa",
                        Description.of("Admin commands for NotQuests"),
                        "nquestsadmin",
                        "nquestadmin",
                        "notquestadmin",
                        "qadmin",
                        "questadmin",
                        "qa",
                        "qag",
                        "notquestsadmin")
                .permission("notquests.admin");

        adminEditCommandBuilder = adminCommandBuilder.literal("edit", "e").required("quest", questParser(main), Description.of("Quest Name"));
        adminTagCommandBuilder = adminCommandBuilder.literal("tags", "t");
        adminItemsCommandBuilder = adminCommandBuilder.literal("items", "item", "i");
        adminConversationCommandBuilder = adminCommandBuilder.literal("conversations", "c");
        adminEditAddObjectiveCommandBuilder = adminEditCommandBuilder.literal("objectives", "o").literal("add");
        adminEditAddRequirementCommandBuilder = adminEditCommandBuilder.literal("requirements", "req").literal("add");
        adminEditAddRewardCommandBuilder = adminEditCommandBuilder.literal("rewards", "rew").literal("add");
        adminEditAddTriggerCommandBuilder = adminEditCommandBuilder.literal("triggers", "t")
                .literal("add").required("action", actionParser(main), Description.of("Action which will be executed when the Trigger triggers."));

        adminEditObjectivesBuilder = adminEditCommandBuilder.literal("objectives").literal("edit").required("objectiveId", objectiveParser(main, 0), Description.of("Objective-ID"));
        adminEditObjectiveAddUnlockConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions").literal("unlock").literal("add");
        adminEditObjectiveAddProgressConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions").literal("progress").literal("add");
        adminEditObjectiveAddCompleteConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions").literal("complete").literal("add");
        adminActionsCommandBuilder = adminCommandBuilder.literal("actions");
        adminActionsEditCommandBuilder = adminActionsCommandBuilder.literal("edit").required("action", actionParser(main), Description.of("Action Name"));

        adminActionsAddConditionCommandBuilder =
                adminActionsEditCommandBuilder.literal("conditions").literal("add");

        adminEditObjectiveAddRewardCommandBuilder =
                adminEditObjectivesBuilder.literal("rewards", "rew").literal("add");

        adminAddConditionCommandBuilder = adminCommandBuilder
                .literal("conditions")
                .literal("add")
                .required("Condition Identifier", stringParser(), Description.of("Condition Identifier"), (context, lastString) -> {
                    main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "[New, unique Condition Identifier]", "...");
                    ArrayList<Suggestion> completions = new ArrayList<>();
                    completions.add(Suggestion.suggestion("[Enter new, unique Condition Identifier]"));
                    return CompletableFuture.completedFuture(completions);
                });


        adminConditionCheckCommandBuilder = adminCommandBuilder
                .literal("conditions")
                .literal("check");

        adminAddActionCommandBuilder = adminCommandBuilder
                .literal("actions")
                .literal("add")
                .required("Action Identifier", stringParser(), Description.of("Action Identifier"), (context, lastString) -> {
                    main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "[New, unique Action Identifier]", "...");
                    ArrayList<Suggestion> completions = new ArrayList<>();

                    completions.add(Suggestion.suggestion("[Enter new, unique Action Identifier]"));
                    return CompletableFuture.completedFuture(completions);
                });

        adminExecuteActionCommandBuilder = adminCommandBuilder
                .literal("actions")
                .literal("execute");
    }

    public void setupCommands() {

    /* PluginCommand notQuestsAdminCommand = main.getCommand("notquestsadminold");
    if (notQuestsAdminCommand != null) {
        final CommandNotQuestsAdmin commandNotQuestsAdmin = new CommandNotQuestsAdmin(main);
        notQuestsAdminCommand.setTabCompleter(commandNotQuestsAdmin);
        notQuestsAdminCommand.setExecutor(commandNotQuestsAdmin);


        registerCommodoreCompletions(commodore, notQuestsAdminCommand);
    }*/
        // Register the notquests command & tab completer. This command will be used by Players
    /*final PluginCommand notQuestsCommand = main.getCommand("notquests");
    if (notQuestsCommand != null) {
        final CommandNotQuests commandNotQuests = new CommandNotQuests(main);
        notQuestsCommand.setExecutor(commandNotQuests);
        notQuestsCommand.setTabCompleter(commandNotQuests);


    }*/

        constructCommands();
    }

    public void constructCommands() {

        // General Stuff
        MinecraftExceptionHandler<Audience> exceptionHandler = MinecraftExceptionHandler.create(AudienceProvider.nativeAudience())
                .decorator(message -> main.parse("<main>NotQuests > ").append(main.parse(main.getMiniMessage().serialize(message))))
                .defaultCommandExecutionHandler((context) -> {
                    main.getLogManager().debug("Command1: " + context.exception().getMessage());
                    if (main.getConfiguration().debug) {
                        context.exception().printStackTrace();
                    }
                    if (main.getConfiguration().debug) {
                        context.exception().printStackTrace();
                    }
                    main.getLogManager().debug("<error>" + context.exception().getCause().getMessage());
                }).defaultHandlers();

        /*
        TODO fix up the exception handler
        new MinecraftExceptionHandler<CommandSender>()
                .withArgumentParsingHandler()
                .withInvalidSenderHandler()
                .withInvalidSyntaxHandler()
                .withNoPermissionHandler()
                .withCommandExecutionHandler()
                .withDecorator(
                        message -> {
                            return main.parse("<main>NotQuests > ")
                                    .append(main.parse(main.getMiniMessage().serialize(message)));
                        })
                .withHandler(
                        MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX,
                        (sender, e) -> {
                            main.getLogManager().debug("Command0: " + e.toString());
                            if (main.getConfiguration().debug) {
                                e.printStackTrace();
                            }
                            final String[] split = e.getMessage().split("syntax is: ");
                            minecraftAdminHelp.queryCommands(split[1], sender);
                            return main.parse("<error>" + split[0] + "syntax is: <main>" + split[1]);
                        })
                .withHandler(
                        ExceptionType.COMMAND_EXECUTION,
                        (sender, e) -> {
                            main.getLogManager().debug("Command1: " + e.toString());
                            if (main.getConfiguration().debug) {
                                e.printStackTrace();
                            }
                            if (main.getConfiguration().debug) {
                                e.printStackTrace();
                            }
                            return main.parse("<error>" + e.getCause().getMessage());
                        })
                .withHandler(
                        ExceptionType.ARGUMENT_PARSING,
                        (sender, e) -> {
                            main.getLogManager().debug("Command2: " + e.toString());
                            if (main.getConfiguration().debug) {
                                e.printStackTrace();
                            }
                            return main.parse("<error>" + e.getCause().getMessage());
                        });

        exceptionHandler. (commandManager, AudienceProvider.nativeAudience());
        */
        // User Stuff
        // Help menu

        commandManager.command(
                userCommandBuilder
                        .literal("help")
                        .required("query", greedyStringParser())
                        .handler(context -> minecraftUserHelp.queryCommands(context.getOrDefault("query", "nq *"), context.sender())));

        userCommands = new UserCommands(main, commandManager, userCommandBuilder);

        // Admin Stuff
        // Help Menu
        commandManager.command(adminCommandBuilder.commandDescription(Description.of("Opens the help menu"))
                .handler((context) -> {
                    minecraftAdminHelp.queryCommands("qa *", context.sender());
                    main.getUtilManager().sendFancyCommandCompletion(context.sender(), context.rawInput().input().split(" "), "[What would you like to do?]", "[...]");
                }));
        commandManager.command(adminCommandBuilder
                .literal("help")
                .optional("query", greedyStringParser())
                .handler(context -> minecraftAdminHelp.queryCommands(context.getOrDefault("query", "qa *"), context.sender())));

        adminCommands = new AdminCommands(main, commandManager, adminCommandBuilder);

        adminEditCommands = new AdminEditCommands(main, commandManager, adminEditCommandBuilder);

        adminTagCommands = new AdminTagCommands(main, commandManager, adminTagCommandBuilder);

        adminItemsCommands = new AdminItemsCommand(main, commandManager, adminItemsCommandBuilder);
    }

    public void setupAdminConversationCommands(
            final ConversationManager
                    conversationManager) { // Has to be done after ConversationManager is initialized
        adminConversationCommands =
                new AdminConversationCommands(
                        main, commandManager, adminConversationCommandBuilder, conversationManager);
    }

    public final LegacyPaperCommandManager<CommandSender> getPaperCommandManager() {
        return commandManager;
    }

    public final Command.Builder<CommandSender> getAdminCommandBuilder() {
        return adminCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditCommandBuilder() {
        return adminEditCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminItemsCommandBuilder() {
        return adminItemsCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminTagCommandBuilder() {
        return adminTagCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminConversationCommandBuilder() {
        return adminConversationCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditAddObjectiveCommandBuilder() {
        return adminEditAddObjectiveCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditAddRequirementCommandBuilder() {
        return adminEditAddRequirementCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditObjectiveAddUnlockConditionCommandBuilder() {
        return adminEditObjectiveAddUnlockConditionCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditObjectiveAddProgressConditionCommandBuilder() {
        return adminEditObjectiveAddProgressConditionCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditObjectiveAddCompleteConditionCommandBuilder() {
        return adminEditObjectiveAddCompleteConditionCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminActionsAddConditionCommandBuilder() {
        return adminActionsAddConditionCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminActionsCommandBuilder() {
        return adminActionsCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminActionsEdituilder() {
        return adminActionsEditCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditObjectiveAddRewardCommandBuilder() {
        return adminEditObjectiveAddRewardCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminAddActionCommandBuilder() {
        return adminAddActionCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminExecuteActionCommandBuilder() {
        return adminExecuteActionCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminAddConditionCommandBuilder() {
        return adminAddConditionCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminConditionCheckCommandBuilder() {
        return adminConditionCheckCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditObjectivesBuilder() {
        return adminEditObjectivesBuilder;
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

    public final AdminTagCommands getAdminTagCommands() {
        return adminTagCommands;
    }

    public final AdminItemsCommand getAdminItemsCommands() {
        return adminItemsCommands;
    }

    public final AdminConversationCommands getAdminConversationCommands() {
        return adminConversationCommands;
    }

    // Player Stuff
    public final UserCommands getUserCommands() {
        return userCommands;
    }

    public final Command.Builder<CommandSender> getUserCommandBuilder() {
        return userCommandBuilder;
    }

    public final ObjectiveHolder getObjectiveHolderFromContextAndLevel(final CommandContext<CommandSender> context, final int level) {
        final ObjectiveHolder objectiveHolder;
        if (level == 0) {
            objectiveHolder = context.get("quest");
        } else if (level == 1) {
            objectiveHolder = context.get("objectiveId");
        } else {
            objectiveHolder = context.get("objectiveId" + level);
        }
        return objectiveHolder;
    }

    public final Objective getObjectiveFromContextAndLevel(final CommandContext<CommandSender> context, final int level) {
        final Objective objective;
        main.getLogManager().debug(context.get("objectiveId"));
        main.getLogManager().debug(context.get("objectiveId" + (level + 1)));
        if (level == 0) {
            objective = context.get("objectiveId");
        } else {
            objective = context.get("objectiveId" + (level + 1));
        }
        return objective;
    }
}
