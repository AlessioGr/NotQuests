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

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.*;
import rocks.gravili.notquests.paper.commands.category.item.AdminItemsCommand;
import rocks.gravili.notquests.paper.commands.category.tag.AdminTagCommands;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQCommands;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import static rocks.gravili.notquests.paper.commands.arguments.ActionArgument.actionArgument;
import static rocks.gravili.notquests.paper.commands.arguments.CategoryArgument.categoryArgument;
import static rocks.gravili.notquests.paper.commands.arguments.ObjectiveArgument.objectiveArgument;
import static rocks.gravili.notquests.paper.commands.arguments.QuestArgument.questArgument;
import rocks.gravili.notquests.paper.conversation.ConversationManager;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class CommandManager {
    private final NotQuests main;
    // Re-usable value flags
    public NQFlag nametag_containsany;
    public NQFlag nametag_equals;
    public NQFlag taskDescription;
    public NQFlag maxDistance;
    public NQFlag categoryFlag;
    public NQFlag delayFlag;

    public NQFlag speakerColor;
    public NQFlag applyOn; // 0 = Quest
    public NQFlag world;
    public NQFlag locationX;
    public NQFlag locationY;
    public NQFlag locationZ;

    public NQFlag triggerWorldString;
    public NQFlag minimumTimeAfterCompletion;
    // NotQuests' own native-Brigadier command framework (migration target off Cloud).
    private NQCommands nqCommands;
    private NQCommandManager nqCommandManager;

    // SOLE remaining Cloud shim. The command tree itself no longer uses Cloud, but six not-yet-migrated
    // variable classes (EnderChestVariable, InventoryVariable, ContainerInventoryVariable,
    // QuestPointsVariable, PlaceholderAPINumberVariable and the Boolean/Number*VariableArgument
    // suggestion bridges) still build Cloud CommandFlags / construct a Cloud CommandContext via this
    // manager. It is kept only so those files compile until they are migrated. Fully-qualified on
    // purpose so the only Cloud reference in this file is this single, clearly-marked accessor.

    // Builders
    private NQCommandBuilder adminCommandBuilder;
    private NQCommandBuilder adminEditCommandBuilder;
    private NQCommandBuilder adminTagCommandBuilder;
    private NQCommandBuilder adminItemsCommandBuilder;
    private NQCommandBuilder adminConversationCommandBuilder;
    private NQCommandBuilder adminEditAddObjectiveCommandBuilder;
    private NQCommandBuilder adminEditAddRequirementCommandBuilder;
    private NQCommandBuilder adminEditAddRewardCommandBuilder;
    private NQCommandBuilder adminEditAddTriggerCommandBuilder;
    private NQCommandBuilder adminEditObjectiveAddUnlockConditionCommandBuilder;
    private NQCommandBuilder adminEditObjectiveAddProgressConditionCommandBuilder;
    private NQCommandBuilder adminEditObjectiveAddCompleteConditionCommandBuilder;

    private NQCommandBuilder adminEditObjectiveAddRewardCommandBuilder;
    private NQCommandBuilder adminAddActionCommandBuilder;
    private NQCommandBuilder adminExecuteActionCommandBuilder;

    private NQCommandBuilder adminActionsCommandBuilder;
    private NQCommandBuilder adminActionsEditCommandBuilder;
    private NQCommandBuilder adminActionsAddConditionCommandBuilder;
    private NQCommandBuilder adminAddConditionCommandBuilder;
    private NQCommandBuilder adminConditionCheckCommandBuilder;

    private AdminCommands adminCommands;
    private AdminEditCommands adminEditCommands;
    private AdminTagCommands adminTagCommands;
    private AdminItemsCommand adminItemsCommands;
    private AdminConversationCommands adminConversationCommands;
    // User
    private NQCommandBuilder userCommandBuilder;
    private UserCommands userCommands;
    // Admin
    private NQCommandBuilder adminEditObjectivesBuilder;

    private CommandMap commandMap;

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
        nametag_containsany = NQFlag.builder("nametag_containsany")
                .withArgument(NQArguments.stringArrayArgument())
                .withDescription(NQDescription.of("This word or every word seperated by a space needs to be part of the nametag"))
                .build();

        nametag_equals = NQFlag.builder("nametag_equals")
                .withArgument(NQArguments.stringArrayArgument())
                .withDescription(NQDescription.of("What the nametag has to be equal"))
                .build();

        taskDescription = NQFlag.builder("taskDescription")
                .withArgument(NQArguments.componentArgument(main))
                .withDescription(NQDescription.of("Custom description of the task"))
                .build();

        speakerColor = NQFlag.builder("speakerColor")
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> {
                    final ArrayList<String> completions = new ArrayList<>();
                    for (final NamedTextColor namedTextColor : NamedTextColor.NAMES.values()) {
                        completions.add("<" + namedTextColor + ">");
                    }
                    return completions;
                })
                .withDescription(NQDescription.of("Color of the speaker name"))
                .build();

        maxDistance = NQFlag.builder("maxDistance")
                .withArgument(NQArguments.integerArgument())
                .withDescription(NQDescription.of("Enter maximum distance of two locations"))
                .build();

        world = NQFlag.builder("world")
                .withArgument(NQArguments.worldArgument())
                .withDescription(NQDescription.of("World Name"))
                .build();

        applyOn = NQFlag.builder("applyOn")
                .withArgument(NQArguments.integerArgument())
                .withSuggestions((context, input) -> java.util.List.of("0", "1", "2"))
                .withDescription(NQDescription.of("To which part of the Quest it should apply (Examples: 'Quest', 'O1', 'O2. (O1 = Objective 1)."))
                .build();

        triggerWorldString = NQFlag.builder("world_name")
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> {
                    final ArrayList<String> completions = new ArrayList<>();
                    completions.add("ALL");
                    for (final World world : Bukkit.getWorlds()) {
                        completions.add(world.getName());
                    }
                    return completions;
                })
                .withDescription(NQDescription.of("World where the Trigger applies (Examples: 'world_the_end', 'farmworld', 'world', 'ALL')."))
                .build();

        minimumTimeAfterCompletion = NQFlag.builder("waitTimeAfterCompletion")
                .withArgument(NQArguments.longArgument())
                .withDescription(NQDescription.of("Enter minimum time you have to wait after completion."))
                .build();

        categoryFlag = NQFlag.builder("category")
                .withArgument(categoryArgument(main))
                .withDescription(NQDescription.of("Category name"))
                .build();

        delayFlag = NQFlag.builder("delay")
                .withArgument(NQArguments.durationArgument())
                .withDescription(NQDescription.of("Delay in milliseconds"))
                .build();

        locationX = NQFlag.builder("locationX")
                .withArgument(NQArguments.doubleArgument())
                .withDescription(NQDescription.of("Enter x coordinate location"))
                .build();

        locationY = NQFlag.builder("locationY")
                .withArgument(NQArguments.doubleArgument())
                .withDescription(NQDescription.of("Enter y coordinate location"))
                .build();

        locationZ = NQFlag.builder("locationZ")
                .withArgument(NQArguments.doubleArgument())
                .withDescription(NQDescription.of("Enter z coordinate location"))
                .build();
    }

    public final CommandMap getCommandMap() {
        return commandMap;
    }

    public void preSetupCommands() {
        // NotQuests' own native-Brigadier command framework (commands.framework package).
        try {
            nqCommands = new NQCommands(main);
            nqCommands.hook();
            nqCommandManager = new NQCommandManager(main, nqCommands);
        } catch (final Throwable t) {
            main.getLogManager().warn("Could not initialize the native command framework: " + t.getMessage());
        }

        preSetupGeneralCommands();
        preSetupUserCommands();
        preSetupAdminCommands();
    }

    public void preSetupGeneralCommands() {
    }

    public void preSetupUserCommands() {
        userCommandBuilder = nqCommandManager.commandBuilder(
                        "nq",
                        NQDescription.of("Player commands for NotQuests"),
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
        adminCommandBuilder = nqCommandManager.commandBuilder(
                        "nqa",
                        NQDescription.of("Admin commands for NotQuests"),
                        "nquestsadmin",
                        "nquestadmin",
                        "notquestadmin",
                        "qadmin",
                        "questadmin",
                        "qa",
                        "qag",
                        "notquestsadmin")
                .permission("notquests.admin");

        adminEditCommandBuilder = adminCommandBuilder.literal("edit", "e").required("quest", questArgument(main), NQDescription.of("Quest Name"));
        adminTagCommandBuilder = adminCommandBuilder.literal("tags", "t");
        adminItemsCommandBuilder = adminCommandBuilder.literal("items", "item", "i");
        adminConversationCommandBuilder = adminCommandBuilder.literal("conversations", "c");
        adminEditAddObjectiveCommandBuilder = adminEditCommandBuilder.literal("objectives", "o").literal("add");
        adminEditAddRequirementCommandBuilder = adminEditCommandBuilder.literal("requirements", "req").literal("add");
        adminEditAddRewardCommandBuilder = adminEditCommandBuilder.literal("rewards", "rew").literal("add");
        adminEditAddTriggerCommandBuilder = adminEditCommandBuilder.literal("triggers", "t")
                .literal("add").required("action", actionArgument(main), NQDescription.of("Action which will be executed when the Trigger triggers."));

        adminEditObjectivesBuilder = adminEditCommandBuilder.literal("objectives").literal("edit").required("objectiveId", objectiveArgument(main, 0), NQDescription.of("Objective-ID"));
        adminEditObjectiveAddUnlockConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions").literal("unlock").literal("add");
        adminEditObjectiveAddProgressConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions").literal("progress").literal("add");
        adminEditObjectiveAddCompleteConditionCommandBuilder = adminEditObjectivesBuilder.literal("conditions").literal("complete").literal("add");
        adminActionsCommandBuilder = adminCommandBuilder.literal("actions");
        adminActionsEditCommandBuilder = adminActionsCommandBuilder.literal("edit").required("action", actionArgument(main), NQDescription.of("Action Name"));

        adminActionsAddConditionCommandBuilder =
                adminActionsEditCommandBuilder.literal("conditions").literal("add");

        adminEditObjectiveAddRewardCommandBuilder =
                adminEditObjectivesBuilder.literal("rewards", "rew").literal("add");

        adminAddConditionCommandBuilder = adminCommandBuilder
                .literal("conditions")
                .literal("add")
                .required("Condition Identifier", NQArguments.stringArgument(), NQDescription.of("Condition Identifier"),
                        (context, input) -> java.util.List.of("[Enter new, unique Condition Identifier]"));


        adminConditionCheckCommandBuilder = adminCommandBuilder
                .literal("conditions")
                .literal("check");

        adminAddActionCommandBuilder = adminCommandBuilder
                .literal("actions")
                .literal("add")
                .required("Action Identifier", NQArguments.stringArgument(), NQDescription.of("Action Identifier"),
                        (context, input) -> java.util.List.of("[Enter new, unique Action Identifier]"));

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

        // User Stuff
        // Help menu
        nqCommandManager.command(
                userCommandBuilder
                        .literal("help")
                        .required("query", NQArguments.greedyStringArgument())
                        .handler(context -> {
                            main.sendMessage(context.sender(), "<main>NotQuests <unimportant>— available commands:");
                            for (final String usageLine : nqCommandManager.rootUsage("nq")) {
                                main.sendMessage(context.sender(), "<unimportant>" + usageLine);
                            }
                        }));

        userCommands = new UserCommands(main, nqCommandManager, userCommandBuilder);

        // Admin Stuff
        // Help Menu
        nqCommandManager.command(adminCommandBuilder.commandDescription(NQDescription.of("Opens the help menu"))
                .handler((context) -> {
                    main.sendMessage(context.sender(), "<main>NotQuests <unimportant>— available admin commands:");
                    for (final String usageLine : nqCommandManager.rootUsage("nqa")) {
                        main.sendMessage(context.sender(), "<unimportant>" + usageLine);
                    }
                    main.getUtilManager().sendFancyCommandCompletion(context.sender(), context.rawInput().input().split(" "), "[What would you like to do?]", "[...]");
                }));
        nqCommandManager.command(adminCommandBuilder
                .literal("help")
                .optional("query", NQArguments.greedyStringArgument())
                .handler(context -> {
                    main.sendMessage(context.sender(), "<main>NotQuests <unimportant>— available admin commands:");
                    for (final String usageLine : nqCommandManager.rootUsage("nqa")) {
                        main.sendMessage(context.sender(), "<unimportant>" + usageLine);
                    }
                }));

        adminCommands = new AdminCommands(main, nqCommandManager, adminCommandBuilder);

        adminEditCommands = new AdminEditCommands(main, nqCommandManager, adminEditCommandBuilder);

        adminTagCommands = new AdminTagCommands(main, nqCommandManager, adminTagCommandBuilder);

        adminItemsCommands = new AdminItemsCommand(main, nqCommandManager, adminItemsCommandBuilder);
    }

    public void setupAdminConversationCommands(
            final ConversationManager
                    conversationManager) { // Has to be done after ConversationManager is initialized
        adminConversationCommands =
                new AdminConversationCommands(
                        main, nqCommandManager, adminConversationCommandBuilder, conversationManager);
    }

    public final NQCommands getNQCommands() {
        return nqCommands;
    }

    public final NQCommandManager getNQCommandManager() {
        return nqCommandManager;
    }

    /**
     * Sole remaining Cloud accessor — kept only for the six not-yet-migrated variable classes that
     * still construct Cloud CommandFlags / a Cloud CommandContext from this manager. Do not add new
     * callers; migrate them to the native framework instead.
     */

    public final NQCommandBuilder getAdminCommandBuilder() {
        return adminCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditCommandBuilder() {
        return adminEditCommandBuilder;
    }

    public final NQCommandBuilder getAdminItemsCommandBuilder() {
        return adminItemsCommandBuilder;
    }

    public final NQCommandBuilder getAdminTagCommandBuilder() {
        return adminTagCommandBuilder;
    }

    public final NQCommandBuilder getAdminConversationCommandBuilder() {
        return adminConversationCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditAddObjectiveCommandBuilder() {
        return adminEditAddObjectiveCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditAddRequirementCommandBuilder() {
        return adminEditAddRequirementCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditObjectiveAddUnlockConditionCommandBuilder() {
        return adminEditObjectiveAddUnlockConditionCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditObjectiveAddProgressConditionCommandBuilder() {
        return adminEditObjectiveAddProgressConditionCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditObjectiveAddCompleteConditionCommandBuilder() {
        return adminEditObjectiveAddCompleteConditionCommandBuilder;
    }

    public final NQCommandBuilder getAdminActionsAddConditionCommandBuilder() {
        return adminActionsAddConditionCommandBuilder;
    }

    public final NQCommandBuilder getAdminActionsCommandBuilder() {
        return adminActionsCommandBuilder;
    }

    public final NQCommandBuilder getAdminActionsEdituilder() {
        return adminActionsEditCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditObjectiveAddRewardCommandBuilder() {
        return adminEditObjectiveAddRewardCommandBuilder;
    }

    public final NQCommandBuilder getAdminAddActionCommandBuilder() {
        return adminAddActionCommandBuilder;
    }

    public final NQCommandBuilder getAdminExecuteActionCommandBuilder() {
        return adminExecuteActionCommandBuilder;
    }

    public final NQCommandBuilder getAdminAddConditionCommandBuilder() {
        return adminAddConditionCommandBuilder;
    }

    public final NQCommandBuilder getAdminConditionCheckCommandBuilder() {
        return adminConditionCheckCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditObjectivesBuilder() {
        return adminEditObjectivesBuilder;
    }

    public final NQCommandBuilder getAdminEditAddRewardCommandBuilder() {
        return adminEditAddRewardCommandBuilder;
    }

    public final NQCommandBuilder getAdminEditAddTriggerCommandBuilder() {
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

    public final NQCommandBuilder getUserCommandBuilder() {
        return userCommandBuilder;
    }

    public final ObjectiveHolder getObjectiveHolderFromContextAndLevel(final NQCommandContext context, final int level) {
        return rocks.gravili.notquests.paper.commands.arguments.ObjectiveArgument.resolveHolder(context.brigadier(), level);
    }

    public final Objective getObjectiveFromContextAndLevel(final NQCommandContext context, final int level) {
        final ObjectiveHolder holder = rocks.gravili.notquests.paper.commands.arguments.ObjectiveArgument.resolveHolder(context.brigadier(), level + 1);
        return holder instanceof Objective ? (Objective) holder : null;
    }
}
