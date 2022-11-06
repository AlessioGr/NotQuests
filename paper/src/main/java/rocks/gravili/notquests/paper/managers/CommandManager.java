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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.DoubleArgument;
import cloud.commandframework.arguments.standard.DurationArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.LongArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler.ExceptionType;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.leangen.geantyref.TypeToken;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.AdminCommands;
import rocks.gravili.notquests.paper.commands.AdminConversationCommands;
import rocks.gravili.notquests.paper.commands.AdminEditCommands;
import rocks.gravili.notquests.paper.commands.AdminItemsCommands;
import rocks.gravili.notquests.paper.commands.AdminTagCommands;
import rocks.gravili.notquests.paper.commands.NotQuestColors;
import rocks.gravili.notquests.paper.commands.UserCommands;
import rocks.gravili.notquests.paper.commands.arguments.ActionSelector;
import rocks.gravili.notquests.paper.commands.arguments.ApplyOnSelector;
import rocks.gravili.notquests.paper.commands.arguments.CategorySelector;
import rocks.gravili.notquests.paper.commands.arguments.CommandSelector;
import rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument;
import rocks.gravili.notquests.paper.commands.arguments.MiniMessageSelector;
import rocks.gravili.notquests.paper.commands.arguments.MiniMessageStringSelector;
import rocks.gravili.notquests.paper.commands.arguments.MultipleActionsSelector;
import rocks.gravili.notquests.paper.commands.arguments.NQNPCSelector;
import rocks.gravili.notquests.paper.commands.arguments.ObjectiveSelector;
import rocks.gravili.notquests.paper.commands.arguments.QuestSelector;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueArgument;
import rocks.gravili.notquests.paper.conversation.ConversationManager;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;

public class CommandManager {
  private final NotQuests main;
  // Re-usable value flags
  public CommandFlag<String[]> nametag_containsany;
  public CommandFlag<String[]> nametag_equals;
  public CommandFlag<String> taskDescription;
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
  private PaperCommandManager<CommandSender> commandManager;
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
  private AdminItemsCommands adminItemsCommands;
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
    nametag_containsany =
        CommandFlag.newBuilder("nametag_containsany")
            .withArgument(
                StringArrayArgument.of(
                    "nametag_containsany",
                    (context, lastString) -> {
                      final List<String> allArgs = context.getRawInput();
                      main.getUtilManager()
                          .sendFancyCommandCompletion(
                              (CommandSender) context.getSender(),
                              allArgs.toArray(new String[0]),
                              "<Enter nametag_containsany flag value>",
                              "");
                      ArrayList<String> completions = new ArrayList<>();
                      completions.add("<nametag_containsany flag value>");
                      return completions;
                    }))
            .withDescription(
                ArgumentDescription.of(
                    "This word or every word seperated by a space needs to be part of the nametag"))
            .build();

    nametag_equals =
        CommandFlag.newBuilder("nametag_equals")
            .withArgument(
                StringArrayArgument.of(
                    "nametag_equals",
                    (context, lastString) -> {
                      final List<String> allArgs = context.getRawInput();
                      main.getUtilManager()
                          .sendFancyCommandCompletion(
                              (CommandSender) context.getSender(),
                              allArgs.toArray(new String[0]),
                              "<Enter nametag_equals flag value>",
                              "");
                      ArrayList<String> completions = new ArrayList<>();
                      completions.add("<nametag_equals flag value>");
                      return completions;
                    }))
            .withDescription(ArgumentDescription.of("What the nametag has to be equal"))
            .build();

    taskDescription =
        CommandFlag.newBuilder("taskDescription")
            .withArgument(
                MiniMessageStringSelector.<CommandSender>newBuilder("Task Description", main)
                    .withPlaceholders()
                    .quoted().build())
            .withDescription(ArgumentDescription.of("Custom description of the task"))
            .build();

    speakerColor =
        CommandFlag.newBuilder("speakerColor")
            .withArgument(
                StringArgument.<CommandSender>newBuilder("Speaker Color")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  (CommandSender) context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[Enter speaker color (default: <WHITE>)]",
                                  "");

                          ArrayList<String> completions = new ArrayList<>();
                          for (NamedTextColor namedTextColor : NamedTextColor.NAMES.values()) {
                            completions.add("<" + namedTextColor.toString() + ">");
                          }
                          return completions;
                        })
                    .single())
            .withDescription(ArgumentDescription.of("Color of the speaker name"))
            .build();

    maxDistance =
        CommandFlag.newBuilder("maxDistance")
            .withArgument(IntegerArgument.of("maxDistance"))
            .withDescription(ArgumentDescription.of("Enter maximum distance of two locations."))
            .build(); // 0 = Quest

    world =
        CommandFlag.newBuilder("world")
            .withArgument(WorldArgument.of("world"))
            .withDescription(ArgumentDescription.of("World Name"))
            .build();

    applyOn =
        CommandFlag.newBuilder("applyOn")
            .withArgument(ApplyOnSelector.of("applyOn", main, "quest"))
            .withDescription(
                ArgumentDescription.of(
                    "To which part of the Quest it should apply (Examples: 'Quest', 'O1', 'O2. (O1 = Objective 1)."))
            .build(); // 0 = Quest

    triggerWorldString =
        CommandFlag.newBuilder("world_name")
            .withArgument(
                StringArgument.<CommandSender>newBuilder("world_name")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  (CommandSender) context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[World Name / 'ALL']",
                                  "");

                          ArrayList<String> completions = new ArrayList<>();

                          completions.add("ALL");

                          for (final World world : Bukkit.getWorlds()) {
                            completions.add(world.getName());
                          }

                          return completions;
                        })
                    .single()
                    .build())
            .withDescription(
                ArgumentDescription.of(
                    "World where the Trigger applies (Examples: 'world_the_end', 'farmworld', 'world', 'ALL')."))
            .build();

    minimumTimeAfterCompletion =
        CommandFlag.newBuilder("waitTimeAfterCompletion")
            .withArgument(LongArgument.of("waitTimeAfterCompletion"))
            .withDescription(
                ArgumentDescription.of("Enter minimum time you have to wait after completion."))
            .build(); // 0 = Quest

    if (main.getIntegrationsManager().isProjectKorraEnabled()) {
      withProjectKorraAbilityFlag =
          CommandFlag.newBuilder("withProjectKorraAbility")
              .withArgument(
                  StringArgument.<CommandSender>newBuilder("ability_name")
                      .withSuggestionsProvider(
                          (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager()
                                .sendFancyCommandCompletion(
                                    (CommandSender) context.getSender(),
                                    allArgs.toArray(new String[0]),
                                    "[Ability Name / 'ALL']",
                                    "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("any");

                            completions.addAll(
                                main.getIntegrationsManager()
                                    .getProjectKorraManager()
                                    .getAbilityCompletions());

                            return completions;
                          })
                      .single()
                      .build())
              .withDescription(ArgumentDescription.of("Project Korra Ability"))
              .build();
    }

    categoryFlag =
        CommandFlag.newBuilder("category")
            .withArgument(CategorySelector.of("category", main))
            .withDescription(ArgumentDescription.of("Category name"))
            .build();
    delayFlag =
        CommandFlag.newBuilder("delay")
            .withArgument(DurationArgument.of("delay"))
            .withDescription(ArgumentDescription.of("Delay in milliseconds"))
            .build();

    locationX =
        CommandFlag.newBuilder("locationX")
            .withArgument(DoubleArgument.of("locationX"))
            .withDescription(ArgumentDescription.of("Enter x coordinate location"))
            .build();

    locationY =
        CommandFlag.newBuilder("locationY")
            .withArgument(DoubleArgument.of("locationY"))
            .withDescription(ArgumentDescription.of("Enter y coordinate location"))
            .build();

    locationZ =
        CommandFlag.newBuilder("locationZ")
            .withArgument(DoubleArgument.of("locationZ"))
            .withDescription(ArgumentDescription.of("Enter z coordinate location"))
            .build();
  }

  public final CommandMap getCommandMap() {
    return commandMap;
  }

  public void preSetupCommands() {
    // Cloud command framework
    try {
      commandManager =
          new PaperCommandManager<>(
              /* Owning plugin */ main.getMain(),
              /* Coordinator function */ CommandExecutionCoordinator.simpleCoordinator(),
              /* Command Sender -> C */ Function.identity(),
              /* C -> Command Sender */ Function.identity());
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
      commandManager.registerBrigadier();
      CloudBrigadierManager<CommandSender, ?> cloudBrigadierManager =
          commandManager.brigadierManager();
      if (cloudBrigadierManager != null) {
        cloudBrigadierManager.setNativeNumberSuggestions(false);

        cloudBrigadierManager.registerMapping(
            new TypeToken<CommandSelector.CommandParser<CommandSender>>() {},
            builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));

        cloudBrigadierManager.registerMapping(
            new TypeToken<MiniMessageStringSelector.MiniMessageStringParser<CommandSender>>() {},
            builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));

        cloudBrigadierManager.registerMapping(
            new TypeToken<MiniMessageSelector.MiniMessageParser<CommandSender>>() {},
            builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));

        cloudBrigadierManager.registerMapping(
            new TypeToken<StringVariableValueArgument.StringParser<CommandSender>>() {},
            builder -> builder.cloudSuggestions().toConstant(StringArgumentType.string()));

        // Greedy string to prevent false, red brigardier color when entering special symbols like a
        // comma
        cloudBrigadierManager.registerMapping(
            new TypeToken<NumberVariableValueArgument.StringParser<CommandSender>>() {},
            builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
        cloudBrigadierManager.registerMapping(
            new TypeToken<BooleanVariableValueArgument.StringParser<CommandSender>>() {},
            builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
        cloudBrigadierManager.registerMapping(
            new TypeToken<MultipleActionsSelector.ListActionsParser<CommandSender>>() {},
            builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
        cloudBrigadierManager.registerMapping(
            new TypeToken<ItemStackSelectionArgument.MaterialParser<CommandSender>>() {},
            builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));

        cloudBrigadierManager.registerMapping(
            new TypeToken<NQNPCSelector.NQNPCsParser<CommandSender>>() {},
            builder -> builder.cloudSuggestions().toConstant(StringArgumentType.greedyString()));
      } else {
        main.getLogManager()
            .warn("Failed to initialize Brigadier support. Brigadier manager is null.");
      }
    } catch (final Exception e) {
      main.getLogManager()
          .warn("Failed to initialize Brigadier support: <highlight>" + e.getMessage());
    }

    commandPostProcessor = new CommandPostProcessor<>(main);
    commandManager.registerCommandPostProcessor(commandPostProcessor);
  }

  public void preSetupUserCommands() {
    minecraftUserHelp =
        new MinecraftHelp<>("/nq help", AudienceProvider.nativeAudience(), commandManager);

    minecraftUserHelp.setHelpColors(
        MinecraftHelp.HelpColors.of(
            NotQuestColors.main,
            NamedTextColor.WHITE,
            NotQuestColors.highlight,
            NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY));

    userCommandBuilder =
        commandManager
            .commandBuilder(
                "nq",
                ArgumentDescription.of("Player commands for NotQuests"),
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

    minecraftAdminHelp =
        new MinecraftHelp<>("/qa help", AudienceProvider.nativeAudience(), commandManager);

    minecraftAdminHelp.setHelpColors(
        MinecraftHelp.HelpColors.of(
            NotQuestColors.main,
            NamedTextColor.WHITE,
            NotQuestColors.highlight,
            NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY));

    adminCommandBuilder =
        commandManager
            .commandBuilder(
                "nqa",
                ArgumentDescription.of("Admin commands for NotQuests"),
                "nquestsadmin",
                "nquestadmin",
                "notquestadmin",
                "qadmin",
                "questadmin",
                "qa",
                "qag",
                "notquestsadmin")
            .permission("notquests.admin");

    adminEditCommandBuilder =
        adminCommandBuilder
            .literal("edit", "e")
            .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Quest Name"));

    adminTagCommandBuilder = adminCommandBuilder.literal("tags", "t");

    adminItemsCommandBuilder = adminCommandBuilder.literal("items", "item", "i");

    adminConversationCommandBuilder = adminCommandBuilder.literal("conversations", "c");

    adminEditAddObjectiveCommandBuilder =
        adminEditCommandBuilder.literal("objectives", "o").literal("add");
    adminEditAddRequirementCommandBuilder =
        adminEditCommandBuilder.literal("requirements", "req").literal("add");
    adminEditAddRewardCommandBuilder =
        adminEditCommandBuilder.literal("rewards", "rew").literal("add");
    adminEditAddTriggerCommandBuilder =
        adminEditCommandBuilder
            .literal("triggers", "t")
            .literal("add")
            .argument(
                ActionSelector.of("action", main),
                ArgumentDescription.of("Action which will be executed when the Trigger triggers."));

    adminEditObjectivesBuilder =
        adminEditCommandBuilder
            .literal("objectives")
            .literal("edit")
            .argument(
                ObjectiveSelector.<CommandSender>newBuilder("Objective ID", main, 0).build(),
                ArgumentDescription.of("Objective ID"));

    adminEditObjectiveAddUnlockConditionCommandBuilder =
        adminEditObjectivesBuilder.literal("conditions").literal("unlock").literal("add");

    adminEditObjectiveAddProgressConditionCommandBuilder =
        adminEditObjectivesBuilder.literal("conditions").literal("progress").literal("add");

    adminEditObjectiveAddCompleteConditionCommandBuilder =
        adminEditObjectivesBuilder.literal("conditions").literal("complete").literal("add");

    adminActionsCommandBuilder = adminCommandBuilder.literal("actions");

    adminActionsEditCommandBuilder =
        adminActionsCommandBuilder
            .literal("edit")
            .argument(ActionSelector.of("action", main), ArgumentDescription.of("Action Name"));

    adminActionsAddConditionCommandBuilder =
        adminActionsEditCommandBuilder.literal("conditions").literal("add");

    adminEditObjectiveAddRewardCommandBuilder =
        adminEditObjectivesBuilder.literal("rewards", "rew").literal("add");

    adminAddConditionCommandBuilder =
        adminCommandBuilder
            .literal("conditions")
            .literal("add")
            .argument(
                StringArgument.<CommandSender>newBuilder("Condition Identifier")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[New, unique Condition Identifier]",
                                  "...");

                          ArrayList<String> completions = new ArrayList<>();

                          completions.add("[Enter new, unique Condition Identifier]");
                          return completions;
                        }));

    adminConditionCheckCommandBuilder =
        adminCommandBuilder
            .literal("conditions")
            .literal("check");

    adminAddActionCommandBuilder =
        adminCommandBuilder
            .literal("actions")
            .literal("add")
            .argument(
                StringArgument.<CommandSender>newBuilder("Action Identifier")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[New, unique Action Identifier]",
                                  "...");

                          ArrayList<String> completions = new ArrayList<>();

                          completions.add("[Enter new, unique Action Identifier]");
                          return completions;
                        }));

    adminExecuteActionCommandBuilder =
        adminCommandBuilder
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
    MinecraftExceptionHandler<CommandSender> exceptionHandler =
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
                    if(main.getConfiguration().debug){
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
                    if(main.getConfiguration().debug){
                        e.printStackTrace();
                    }
                    if(main.getConfiguration().debug){
                        e.printStackTrace();
                    }
                  return main.parse("<error>" + e.getCause().getMessage());
                })
            .withHandler(
                ExceptionType.ARGUMENT_PARSING,
                (sender, e) -> {
                    main.getLogManager().debug("Command2: " + e.toString());
                    if(main.getConfiguration().debug){
                        e.printStackTrace();
                    }
                  return main.parse("<error>" + e.getCause().getMessage());
                })
        ;

    exceptionHandler.apply(commandManager, AudienceProvider.nativeAudience());

    // User Stuff
    // Help menu

    commandManager.command(
        userCommandBuilder
            .literal("help")
            .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
            .handler(
                context -> {
                  minecraftUserHelp.queryCommands(
                      context.getOrDefault("query", "nq *"), context.getSender());
                }));

    userCommands = new UserCommands(main, commandManager, userCommandBuilder);

    // Admin Stuff
    // Help Menu
    commandManager.command(
        adminCommandBuilder
            .meta(CommandMeta.DESCRIPTION, "Opens the help menu")
            .handler(
                (context) -> {
                  minecraftAdminHelp.queryCommands("qa *", context.getSender());
                  final List<String> allArgs = context.getRawInput();
                  main.getUtilManager()
                      .sendFancyCommandCompletion(
                          context.getSender(),
                          allArgs.toArray(new String[0]),
                          "[What would you like to do?]",
                          "[...]");
                }));
    commandManager.command(
        adminCommandBuilder
            .literal("help")
            .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
            .handler(
                context -> {
                  minecraftAdminHelp.queryCommands(
                      context.getOrDefault("query", "qa *"), context.getSender());
                }));

    adminCommands = new AdminCommands(main, commandManager, adminCommandBuilder);

    adminEditCommands = new AdminEditCommands(main, commandManager, adminEditCommandBuilder);

    adminTagCommands = new AdminTagCommands(main, commandManager, adminTagCommandBuilder);

    adminItemsCommands = new AdminItemsCommands(main, commandManager, adminItemsCommandBuilder);
  }

  public void setupAdminConversationCommands(
      final ConversationManager
          conversationManager) { // Has to be done after ConversationManager is initialized
    adminConversationCommands =
        new AdminConversationCommands(
            main, commandManager, adminConversationCommandBuilder, conversationManager);
  }

  public final PaperCommandManager<CommandSender> getPaperCommandManager() {
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

  public final AdminItemsCommands getAdminItemsCommands() {
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

  public final ObjectiveHolder getObjectiveHolderFromContextAndLevel(final CommandContext<CommandSender> context, final int level){
    final ObjectiveHolder objectiveHolder;
    if(level == 0){
      objectiveHolder = context.get("quest");
    }else if(level == 1){
      objectiveHolder = context.get("Objective ID");
    } else {
      objectiveHolder = context.get("Objective ID " + level);
    }
    return objectiveHolder;
  }

  public final Objective getObjectiveFromContextAndLevel(final CommandContext<CommandSender> context, final int level){
    final Objective objective;
    if(level == 0){
      objective = context.get("Objective ID");
    }else {
      objective = context.get("Objective ID " + (level+1));
    }
    return objective;
  }
}
