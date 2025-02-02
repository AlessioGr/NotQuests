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

package rocks.gravili.notquests.paper.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.category.admin.*;
import rocks.gravili.notquests.paper.commands.category.admin.category.CategoryCreateCommand;
import rocks.gravili.notquests.paper.commands.category.admin.category.CategoryEditCommand;
import rocks.gravili.notquests.paper.commands.category.admin.category.CategoryListCommand;
import rocks.gravili.notquests.paper.commands.category.admin.structs.ObjectiveTriggerCommand;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.conditions.Condition.ConditionResult;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.bukkit.parser.PlayerParser.playerParser;
import static org.incendo.cloud.minecraft.extras.parser.ComponentParser.miniMessageParser;
import static org.incendo.cloud.parser.standard.BooleanParser.booleanParser;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static rocks.gravili.notquests.paper.commands.arguments.CategoryParser.categoryParser;
import static rocks.gravili.notquests.paper.commands.arguments.ConditionParser.conditionParser;

public class AdminCommands {
    public final ArrayList<String> placeholders;
    private final NotQuests notQuests;
    private final LegacyPaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> builder;
    private final Date resultDate;


    public AdminCommands(final NotQuests notQuests, LegacyPaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        this.notQuests = notQuests;
        this.manager = manager;
        this.builder = builder;


        placeholders = new ArrayList<>();
        placeholders.add("{PLAYER}");
        placeholders.add("{PLAYERUUID}");
        placeholders.add("{PLAYERX}");
        placeholders.add("{PLAYERY}");
        placeholders.add("{PLAYERZ}");
        placeholders.add("{WORLD}");
        placeholders.add("{QUEST}");
        placeholders.add("{{expression}}");

        resultDate = new Date();

        handleConditions();
        handleActions();
        handleQuestPoints();

        new DebugCommand(notQuests, builder).apply(manager);
        new EditorCommand(notQuests, builder).apply(manager);
        new ListCommand(notQuests, builder).apply(manager);
        new QuestCompleteCommand(notQuests, builder).apply(manager);
        new QuestCreateCommand(notQuests, builder).apply(manager);
        new QuestDeleteCommand(notQuests, builder).apply(manager);
        new QuestFailCommand(notQuests, builder).apply(manager);
        new QuestGiveCommand(notQuests, builder).apply(manager);
        new QuestProgressCommand(notQuests, builder).apply(manager);
        new QuestResetAndFailCommand(notQuests, builder).apply(manager);
        new QuestResetAndRemoveCommand(notQuests, builder).apply(manager);
        new QuestShowActiveCommand(notQuests, builder).apply(manager);
        new QuestShowCompletedCommand(notQuests, builder).apply(manager);
        new ReloadCommand(notQuests, builder).apply(manager);
        new SaveCommand(notQuests, builder).apply(manager);
        new VersionCommand(notQuests, builder).apply(manager);

        new ObjectiveTriggerCommand(notQuests, builder).apply(manager);

        new CategoryCreateCommand(notQuests, builder).apply(manager);
        new CategoryEditCommand(notQuests, builder).apply(manager);
        new CategoryListCommand(notQuests, builder).apply(manager);
    }


    public void handleDebugCommands() {


       /* manager.command(builder.literal("debug")
                .literal("beaconBeamAdvanced")
                .argument(WorldArgument.of("world"), Description.of("World name"))
                .argument(IntegerArgument.newBuilder("x"), Description.of("X coordinate"))
                .argument(IntegerArgument.newBuilder("y"), Description.of("Y coordinate"))
                .argument(IntegerArgument.newBuilder("z"), Description.of("Z coordinate"))

                .meta(CommandMeta.DESCRIPTION, "Spawns a beacon beam")
                .senderType(Player.class)
                .handler((context) -> {

                    World world = context.get("world");
                    final Vector coordinates = new Vector(context.get("x"), context.get("y"), context.get("z"));
                    Location location = coordinates.toLocation(world);





                    Player player = (Player) context.sender();



                    //Prepare Data
                    Connection connection = notQuests.getPacketManager().getModernPacketInjector().getConnection(notQuests.getPacketManager().getModernPacketInjector().getServerPlayer(player).connection);
                    location = location.clone();
                    BlockPos blockPos = new BlockPos(location.getX(), location.getY(), location.getZ());

                    Chunk chunk = location.getChunk();
                    CraftChunk craftChunk = (CraftChunk)chunk;
                    LevelChunk levelChunk = craftChunk.getHandle();

                    CraftWorld craftWorld = (CraftWorld)world;
                    ServerLevel serverLevel = craftWorld.getHandle();
                    //

                    BlockState beaconBlockState = location.getBlock().getState();
                    beaconBlockState.setType(Material.BEACON);

                    CraftBlockState craftBlockState = (CraftBlockState)beaconBlockState ;
                    net.minecraft.world.level.block.state.BlockState minecraftBlockState = craftBlockState.getHandle();


                    BlockState ironBlockState = location.getBlock().getState();
                    ironBlockState.setType(Material.IRON_BLOCK);


                    /*BlockPos blockPos3 = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    SectionPos sectionPos = SectionPos.of(blockPos3);

                    //SectionPos.sectionRelativePos()


                    notQuests.sendMessage(player, "<highlight>Section Pos: <notQuests>" + sectionPos.asLong()
                    );
                    notQuests.sendMessage(player, "<highlight>Section Pos Chunk x: <notQuests>" + sectionPos.chunk().x
                    );
                    notQuests.sendMessage(player, "<highlight>Section Pos blocks inside size: <notQuests>" + sectionPos.blocksInside().toArray().length);

                    for(Object blockPos1 : sectionPos.blocksInside().toArray()){
                        BlockPos blockPos2 = (BlockPos) blockPos1;
                        BlockEntity blockEntity =  serverLevel.getBlockEntity(blockPos2);
                        if(blockEntity != null){
                            notQuests.sendMessage(player, "<highlight>Section Pos blocks inside: <notQuests>" + blockEntity.getBlockState().getClass().toString()
                            );
                        }

                    }
                    notQuests.sendMessage(player, "<highlight>Section Pos short: <notQuests>" + sectionPos.toShortString()
                    );


                    //PalettedContainer<BlockState> pcB = new PalettedContainer<>();


                    //net.minecraft.world.level.block.state.BlockState[] presetBlockStates = serverLevel.chunkPacketBlockController.getPresetBlockStates(world, chunkPos, b0 << 4);

                    //PalettedContainer<BlockState> datapaletteblock = new PalettedContainer<>(net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES, presetBlockStates);



                   int sectionID = (int) (64/16)+ ( (383/16) / ((location.getBlockY())/16) );

                    notQuests.sendMessage(player, "<highlight>LevelChunkSection Section ID: <notQuests>" + sectionID);


                    LevelChunkSection section = levelChunk.getSection(sectionID);





                    notQuests.sendMessage(player, "<highlight>LevelChunkSection Section Count: <notQuests>" + levelChunk.getSectionsCount()
                    );
                    notQuests.sendMessage(player, "<highlight>LevelChunkSection Sections length: <notQuests>" + levelChunk.getSections().length
                    );

                    /*Iterator<net.minecraft.world.level.block.state.BlockState> it = section.getStates().registry.iterator();

                    ArrayList<String> names = new ArrayList<>();

                    while(it.hasNext()){
                        net.minecraft.world.level.block.state.BlockState blockState1 = it.next();
                        names.add(blockState1.getBlock().getClass().toString());

                    }
                    notQuests.sendMessage(player, "<notQuests>" + names.toString());*/
                    /*

                    ShortSet positions = ShortSet.of()


                    short count = 0;
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = 0; y < 16; y++) {
                                section.setBlockState(x, y, z, minecraftBlockState);
                                count++;
                            }
                        }
                    }

                    notQuests.sendMessage(player, "<highlight>Index 0 state: <notQuests>" + section.states.get(0).getBlock().getClass().getName()
                    );




                    notQuests.sendMessage(player, "<highlight>Positions: <notQuests>" + positions.toString()
                    );




                    ClientboundSectionBlocksUpdatePacket clientboundSectionBlocksUpdatePacket = new ClientboundSectionBlocksUpdatePacket(
                            sectionPos,
                            positions,
                            section,
                            false);

                    notQuests.sendMessage(player, "<notQuests>Sending packet...");

                    connection.send(clientboundSectionBlocksUpdatePacket);

                    notQuests.sendMessage(player, "<success>Packet sent!");*//*

                    player.sendBlockChange(location, beaconBlockState.getBlockData());

                    player.sendBlockChange(location.clone().add(0,-1,0), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(-1,-1,0), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(-1,-1,-1), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(-1,-1,1), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(1,-1,0), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(1,-1,1), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(1,-1,-1), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(0,-1,1), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(0,-1,-1), ironBlockState.getBlockData());

                }));*/
    }

    public void handleQuestPoints() {
        manager.command(builder.literal("questpoints")
                .required("player", playerParser(), Description.of("Player whose questpoints you want to see."))
                .literal("show", "view")
                .commandDescription(Description.of("Shows questpoints of a player"))
                .handler((context) -> {
                    final Player playerSelector = context.get("player");

                    if (playerSelector.isOnline()) {
                        final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(playerSelector.getUniqueId());
                        if (questPlayer != null) {
                            context.sender().sendMessage(notQuests.parse("<notQuests>Quest points for player <highlight>" + playerSelector.getName() + "</highlight> <green>(online)</green>: <highlight2>" + questPlayer.getQuestPoints()));
                        } else {
                            context.sender().sendMessage(notQuests.parse("<error>Seems like the player <highlight>" + playerSelector.getName() + "</highlight> <green>(online)</green> does not have any quest points!"));
                        }
                    } else {

                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerSelector.getUniqueId());

                        final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            context.sender().sendMessage(notQuests.parse("<notQuests>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>: <highlight2>" + questPlayer.getQuestPoints()));
                        } else {
                            context.sender().sendMessage(notQuests.parse("<error>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> does not have any quest points!"));
                        }
                    }


                }));


        manager.command(builder.literal("questpoints")
                .required("player", playerParser(), Description.of("Player to whom you want to add questpoints to."))
                .literal("add")
                .required("amount", integerParser(1), Description.of("Amount of questpoints to add"))
                .commandDescription(Description.of("Add questpoints to a player"))
                .handler((context) -> {
                    final Player playerSelector = context.get("player");
                    int questPointsToAdd = context.get("amount");

                    if (playerSelector.isOnline()) {
                        final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getOrCreateQuestPlayer(playerSelector.getUniqueId());
                        long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.addQuestPoints(questPointsToAdd, false);
                        context.sender().sendMessage(notQuests.parse(MessageFormat.format("<notQuests>Quest points for player <highlight>{0}</highlight> <green>(online)</green> have been set from <unimportant>{1}</unimportant> to <highlight2>{2}</highlight2>.", playerSelector.getName(), oldQuestPoints, oldQuestPoints + questPointsToAdd)));
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerSelector.getUniqueId());
                        final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getOrCreateQuestPlayerFromDatabase(offlinePlayer.getUniqueId());
                        final long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.addQuestPoints(questPointsToAdd, false);
                        context.sender().sendMessage(notQuests.parse("<notQuests>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints
                                + "</unimportant> to <highlight2>" + (oldQuestPoints + questPointsToAdd) + "</highlight2>."));

                        context.sender().sendMessage(notQuests.parse("<notQuests>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>: <highlight2>" + questPlayer.getQuestPoints()));
                    }
                }));

        manager.command(builder.literal("questpoints")
                .required("player", playerParser(), Description.of("Player of whom you want to remove questpoints from."))
                .literal("remove", "deduct")
                .required("amount", integerParser(1), Description.of("Amount of questpoints to remove"))
                .commandDescription(Description.of("Remove questpoints from a player"))
                .handler((context) -> {
                    final Player playerSelector = context.get("player");
                    int questPointsToRemove = context.get("amount");

                    if (playerSelector.isOnline()) {
                        final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getOrCreateQuestPlayer(playerSelector.getUniqueId());
                        long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.removeQuestPoints(questPointsToRemove, false);
                        context.sender().sendMessage(notQuests.parse("<notQuests>Quest points for player <highlight>" + playerSelector.getName() + "</highlight> <green>(online)</green> have been set from <unimportant>" + oldQuestPoints
                                + "</unimportant> to <highlight2>" + (oldQuestPoints - questPointsToRemove) + "</highlight2>."));
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerSelector.getUniqueId());
                        final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getOrCreateQuestPlayerFromDatabase(offlinePlayer.getUniqueId());
                        final long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.removeQuestPoints(questPointsToRemove, false);
                        context.sender().sendMessage(notQuests.parse("<notQuests>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints
                                + "</unimportant> to <highlight2>" + (oldQuestPoints - questPointsToRemove) + "</highlight2>."));

                        context.sender().sendMessage(notQuests.parse("<notQuests>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>: <highlight2>" + questPlayer.getQuestPoints()));
                    }
                }));


        manager.command(builder.literal("questpoints")
                .required("player", playerParser(), Description.of("Player whose questpoints amount you want to change."))
                .literal("set")
                .required("amount", integerParser(1), Description.of("New questpoints amount"))
                .commandDescription(Description.of("Set questpoints for a player"))
                .handler((context) -> {
                    final Player playerSelector = context.get("player");
                    int newQuestPointsAmount = context.get("amount");

                    if (playerSelector.isOnline()) {
                        final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getOrCreateQuestPlayer(playerSelector.getUniqueId());
                        long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.setQuestPoints(newQuestPointsAmount, false);
                        context.sender().sendMessage(notQuests.parse("<notQuests>Quest points for player <highlight>" + playerSelector.getName() + "</highlight> <green>(online)</green> have been set from <unimportant>" + oldQuestPoints
                                + "</unimportant> to <highlight2>" + (newQuestPointsAmount) + "</highlight2>."));
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerSelector.getUniqueId());
                        final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getOrCreateQuestPlayerFromDatabase(offlinePlayer.getUniqueId());
                        final long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.setQuestPoints(newQuestPointsAmount, false);
                        context.sender().sendMessage(notQuests.parse("<notQuests>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight2> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints
                                + "</unimportant> to <highlight2>" + (newQuestPointsAmount) + "</highlight2>."));
                        context.sender().sendMessage(notQuests.parse("<notQuests>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>: <highlight2>" + questPlayer.getQuestPoints()));
                    }
                }));


    }


    public void handleConditions() {

        final Command.Builder<CommandSender> conditionsBuilder = builder.literal("conditions");

        final Command.Builder<CommandSender> conditionsEditBuilder = conditionsBuilder
                .literal("edit")
                .required("condition", conditionParser(notQuests), Description.of("Condition Name"));


        manager.command(conditionsEditBuilder.commandDescription(Description.of("Removes a condition"))
                .literal("delete", "remove")
                .handler((context) -> {

                    final Condition condition = context.get("condition");

                    notQuests.getConditionsYMLManager().removeCondition(condition);
                    context.sender().sendMessage(notQuests.parse("<success>Condition with the name <highlight>" + condition.getConditionName() + "</highlight> has been deleted."));
                }));

        manager.command(conditionsEditBuilder.commandDescription(Description.of("Checks a condition"))
                .literal("check")
                .optional("player", playerParser(), Description.of("Player for which the condition will be checked"))
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    Player player = null;
                    if (context.contains("player")) {
                        player = context.get("player");
                    } else if (context.sender() instanceof Player senderPlayer) {
                        player = senderPlayer;
                    } else {
                        context.sender().sendMessage(notQuests.parse("<error>Execute in game or specify player"));
                        return;
                    }


                    final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
                    final ConditionResult result = condition.check(questPlayer);
                    final String resultMessage = result.fulfilled() ? "<success>Condition fulfilled!" : result.message();
                    context.sender().sendMessage(notQuests.parse("<success>Condition with the name <highlight>" + condition.getConditionName() + "</highlight> has been checked! Result:</success>\n" + resultMessage));
                }));

        manager.command(conditionsBuilder.commandDescription(Description.of("Shows all existing conditions."))
                .literal("list")
                .handler((context) -> {
                    int counter = 1;
                    context.sender().sendMessage(notQuests.parse("<highlight>All Conditions:"));
                    for (final String conditionIdentifier : notQuests.getConditionsYMLManager().getConditionsAndIdentifiers().keySet()) {
                        final Condition condition = notQuests.getConditionsYMLManager().getCondition(conditionIdentifier);
                        context.sender().sendMessage(notQuests.parse("<highlight>" + counter + ".</highlight> <notQuests>" + conditionIdentifier));
                        context.sender().sendMessage(notQuests.parse("  <veryUnimportant>└─</veryUnimportant> <unimportant>Type: <highlight2>" + condition.getConditionType()));
                        counter += 1;
                    }
                }));

        manager.command(conditionsEditBuilder.commandDescription(Description.of("Shows the current category of this Condition."))
                .literal("category")
                .literal("show")
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    context.sender().sendMessage(notQuests.parse(
                            "<notQuests>Category for condition <highlight>" + condition.getConditionName() + "</highlight>: <highlight2>"
                                    + condition.getCategory().getCategoryFullName() + "</highlight2>."
                    ));
                }));

        manager.command(conditionsEditBuilder.commandDescription(Description.of("Changes the current category of this Condition."))
                .literal("category")
                .literal("set")
                .required("category", categoryParser(notQuests), Description.of("New category for this Condition."))
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    final Category category = context.get("category");
                    if (condition.getCategory().getCategoryFullName().equalsIgnoreCase(category.getCategoryFullName())) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error> Error: The condition <highlight>" + condition.getConditionName() + "</highlight> already has the category <highlight2>" + condition.getCategory().getCategoryFullName() + "</highlight2>."
                        ));
                        return;
                    }


                    context.sender().sendMessage(notQuests.parse(
                            "<success>Category for condition <highlight>" + condition.getConditionName() + "</highlight> has successfully been changed from <highlight2>"
                                    + condition.getCategory().getCategoryFullName() + "</highlight2> to <highlight2>" + category.getCategoryFullName() + "</highlight2>!"
                    ));

                    condition.switchCategory(category);

                }));


        manager.command(conditionsEditBuilder.literal("description", Description.of("Sets the new description of the condition."))
                .literal("set")
                .required("description", miniMessageParser(), Description.of("Condition description"))
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    final String description = String.join(" ", (String[]) context.get("description"));

                    condition.setDescription(description);

                    condition.getCategory().getConditionsConfig().set("conditions." + condition.getConditionName() + ".description", description);
                    condition.getCategory().saveConditionsConfig();


                    context.sender().sendMessage(notQuests.parse("<success>Description successfully added to condition  <highlight>" + condition.getConditionName() + "</highlight>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(conditionsEditBuilder.literal("hidden", Description.of("Sets the new hidden status of the condition."))
                .literal("set")
                .required("hiddenStatusExpression", booleanParser(), Description.of("Expression"))
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    final String hiddenStatusExpression = context.get("hiddenStatusExpression");
                    final NumberExpression hiddenExpression = new NumberExpression(notQuests, hiddenStatusExpression);


                    condition.setHidden(hiddenExpression);

                    condition.getCategory().getConditionsConfig().set("conditions." + condition.getConditionName() + ".hiddenStatusExpression", hiddenStatusExpression);
                    condition.getCategory().saveConditionsConfig();


                    context.sender().sendMessage(notQuests.parse("<success>Hidden status successfully added to condition  <highlight>" + condition.getConditionName() + "</highlight>! New hidden status: <highlight2>"
                            + condition.getHiddenExpression()
                    ));
                }));

        manager.command(conditionsEditBuilder.literal("description", Description.of("Removes the description of the condition."))
                .literal("remove", "delete")
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    condition.removeDescription();

                    condition.getCategory().getConditionsConfig().set("conditions." + condition.getConditionName() + ".description", "");
                    condition.getCategory().saveConditionsConfig();

                    context.sender().sendMessage(notQuests.parse("<success>Description successfully removed from condition <highlight>" + condition.getConditionName() + "</highlight>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(conditionsEditBuilder.literal("description", Description.of("Shows the description of the condition."))
                .literal("show", "check")
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    context.sender().sendMessage(notQuests.parse("<notQuests>Description of condition <highlight>" + condition.getConditionName() + "</highlight>:\n"
                            + condition.getDescription()
                    ));
                }));
    }

    public void handleActions() {

        final Command.Builder<CommandSender> actionsBuilder = notQuests.getCommandManager().getAdminActionsCommandBuilder();

        final Command.Builder<CommandSender> actionsEditBuilder = notQuests.getCommandManager().getAdminActionsEdituilder();


        manager.command(actionsEditBuilder.commandDescription(Description.of("Removes an action"))
                .literal("delete", "remove")
                .handler((context) -> {
                    final Action action = context.get("action");

                    notQuests.getActionsYMLManager().removeAction(action);
                    context.sender().sendMessage(notQuests.parse("<success>Action with the name <highlight2>" + action.getActionName() + "</highlight2> has been deleted."));
                }));

        manager.command(actionsEditBuilder.commandDescription(Description.of("Executes an action"))
                .literal("execute", "run")
                .optional("player", playerParser(), Description.of("Player for which the action will be executed"))
                .flag(manager.flagBuilder("ignoreConditions").withDescription(Description.of("Ignores action conditions")))
                .flag(manager.flagBuilder("silent").withDescription(Description.of("Doesn't show the action executed message")))
                .handler((context) -> {
                    final Action action = context.get("action");

                    Player player = null;
                    if (context.contains("player")) {
                        player = context.get("player");
                    } else if (context.sender() instanceof Player senderPlayer) {
                        player = senderPlayer;
                    } else {
                        context.sender().sendMessage(notQuests.parse("<error>Execute in game or specify a player"));
                        return;
                    }

                    if (context.flags().contains("ignoreConditions")) {
                        action.execute(notQuests.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()));
                        context.sender().sendMessage(notQuests.parse("<success>Action with the name <highlight>" + action.getActionName() + "</highlight> has been executed!"));
                    } else {
                        notQuests.getActionManager().executeActionWithConditions(action, notQuests.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()), context.sender(), context.flags().contains("silent"));
                    }

                }));

        manager.command(actionsBuilder.commandDescription(Description.of("Shows all existing actions."))
                .literal("list")
                .handler((context) -> {
                    int counter = 1;
                    context.sender().sendMessage(notQuests.parse("<highlight>All Actions:"));
                    for (final String actionIdentifier : notQuests.getActionsYMLManager().getActionsAndIdentifiers().keySet()) {
                        final Action action = notQuests.getActionsYMLManager().getAction(actionIdentifier);
                        context.sender().sendMessage(notQuests.parse("<highlight>" + counter + ".</highlight> <notQuests>" + actionIdentifier));
                        context.sender().sendMessage(notQuests.parse("  <veryUnimportant>└─</veryUnimportant> <unimportant>Type:</unimportant> <highlight2>" + action.getActionType()));
                        counter += 1;
                    }
                }));

        manager.command(actionsEditBuilder.commandDescription(Description.of("Removes all conditions from this objective."))
                .literal("conditions")
                .literal("clear")
                .handler((context) -> {
                    final Action action = context.get("action");

                    action.clearConditions(action.getCategory().getActionsConfig(), "actions." + action.getActionName());
                    context.sender().sendMessage(notQuests.parse(
                            "<success>All conditions of action with identifier <highlight>" + action
                                    + "</highlight> have been removed!"
                    ));
                }));

        manager.command(actionsEditBuilder.commandDescription(Description.of("Lists all conditions of this objective."))
                .literal("conditions")
                .literal("list", "show")
                .handler((context) -> {
                    final Action action = context.get("action");

                    context.sender().sendMessage(notQuests.parse(
                            "<highlight>Conditions of action with identifier <highlight2>" + action.getActionName()
                                    + "</highlight2>:"
                    ));
                    int counter = 1;
                    for (Condition condition : action.getConditions()) {
                        context.sender().sendMessage(notQuests.parse("<highlight>" + counter + ".</highlight> <notQuests>" + condition.getConditionType()));
                        if (context.sender() instanceof final Player player) {
                            context.sender().sendMessage(notQuests.parse("<notQuests>" + condition.getConditionDescription(notQuests.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        } else {
                            context.sender().sendMessage(notQuests.parse("<notQuests>" + condition.getConditionDescription(null)));
                        }
                        counter += 1;
                    }

                    if (counter == 1) {
                        context.sender().sendMessage(notQuests.parse("<warn>This action has no conditions!"));
                    }


                }));


        final Command.Builder<CommandSender> editActionConditionsBuilder = actionsEditBuilder
                .literal("conditions")
                .literal("edit")
                .required("condition-id", integerParser(1), (context, input) -> {
                            notQuests.getUtilManager().sendFancyCommandCompletion(context.sender(), context.rawInput().input().split(" "), "[condition-id]", "[...]");
                            ArrayList<Suggestion> completions = new ArrayList<>();
                            final Action action = context.get("action");

                            for (final Condition condition : action.getConditions()) {
                                completions.add(Suggestion.suggestion("" + (action.getConditions().indexOf(condition) + 1)));
                            }

                            return CompletableFuture.completedFuture(completions);
                        }
                );

        manager.command(editActionConditionsBuilder.commandDescription(Description.of("Removes a condition from this Action."))
                .literal("delete", "remove")
                .handler((context) -> {
                    final Action action = context.get("action");

                    int conditionID = context.get("condition-id");
                    Condition condition = action.getConditions().get(conditionID - 1);

                    if (condition == null) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    action.removeCondition(condition, true, action.getCategory().getActionsConfig(), "actions." + action.getActionName());

                    context.sender().sendMessage(notQuests.parse("<notQuests>The condition with the ID <highlight>" + conditionID + "</highlight> of Action <highlight2>" + action.getActionName() + "</highlight2> has been removed!"));
                }));


        manager.command(editActionConditionsBuilder.commandDescription(Description.of("Sets the new description of the Action condition."))
                .literal("description")
                .literal("set")
                .required("description", miniMessageParser(), Description.of("Action condition description"))
                .handler((context) -> {
                    final Action action = context.get("action");

                    int conditionID = context.get("condition-id");
                    Condition condition = action.getConditions().get(conditionID - 1);

                    if (condition == null) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String description = String.join(" ", (String[]) context.get("description"));

                    condition.setDescription(description);

                    action.getCategory().getActionsConfig().set("actions." + action.getActionName() + ".conditions." + (action.getConditions().indexOf(condition) + 1) + ".description", condition.getDescription());
                    action.getCategory().saveActionsConfig();

                    context.sender().sendMessage(notQuests.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of action <highlight2>"
                            + action.getActionName() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editActionConditionsBuilder.commandDescription(Description.of("Removes the description of the Action condition."))
                .literal("description")
                .literal("remove", "delete")
                .handler((context) -> {
                    final Action action = context.get("action");

                    int conditionID = context.get("condition-id");
                    Condition condition = action.getConditions().get(conditionID - 1);

                    if (condition == null) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    condition.removeDescription();

                    action.getCategory().getActionsConfig().set("actions." + action.getActionName() + ".conditions." + (action.getConditions().indexOf(condition) + 1) + ".description", "");
                    action.getCategory().saveActionsConfig();


                    context.sender().sendMessage(notQuests.parse("<success>Description successfully removed from condition with ID <highlight>" + conditionID + "</highlight> of action <highlight2>"
                            + action.getActionName() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editActionConditionsBuilder.commandDescription(Description.of("Shows the description of the Action condition."))
                .literal("description")
                .literal("show", "check")
                .handler((context) -> {
                    final Action action = context.get("action");

                    int conditionID = context.get("condition-id");
                    Condition condition = action.getConditions().get(conditionID - 1);

                    if (condition == null) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    context.sender().sendMessage(notQuests.parse("<notQuests>Description of condition with ID <highlight>" + conditionID + "</highlight> of action <highlight2>"
                            + action.getActionName() + "</highlight2>:\n"
                            + condition.getDescription()
                    ));
                }));


        manager.command(editActionConditionsBuilder.literal("hidden", Description.of("Sets the new hidden status of the Action condition."))
                .literal("set")
                .required("hiddenStatusExpression", booleanParser(), Description.of("Expression"))
                .handler((context) -> {
                    final Action action = context.get("action");

                    int conditionID = context.get("condition-id");
                    Condition condition = action.getConditions().get(conditionID - 1);

                    if (condition == null) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String hiddenStatusExpression = context.get("hiddenStatusExpression");
                    final NumberExpression hiddenExpression = new NumberExpression(notQuests, hiddenStatusExpression);

                    condition.setHidden(hiddenExpression);

                    action.getCategory().getActionsConfig().set("actions." + action.getActionName() + ".conditions." + (action.getConditions().indexOf(condition) + 1) + ".hiddenStatusExpression", hiddenStatusExpression);
                    action.getCategory().saveActionsConfig();

                    context.sender().sendMessage(notQuests.parse("<success>Hidden status successfully added to condition with ID <highlight>" + conditionID + "</highlight> of action <highlight2>"
                            + action.getActionName() + "</highlight2>! New hidden status: <highlight2>"
                            + condition.getHiddenExpression()
                    ));
                }));


        manager.command(actionsEditBuilder
                .literal("category")
                .literal("show")
                .commandDescription(Description.of("Shows the current category of this Action."))
                .handler((context) -> {
                    final Action action = context.get("action");

                    context.sender().sendMessage(notQuests.parse(
                            "<notQuests>Category for action <highlight>" + action.getActionName() + "</highlight>: <highlight2>"
                                    + action.getCategory().getCategoryFullName() + "</highlight2>."
                    ));
                }));

        manager.command(actionsEditBuilder
                .literal("category", Description.of("Changes the current category of this Action."))
                .literal("set")
                .required("category", categoryParser(notQuests), Description.of("New category for this Action."))
                .handler((context) -> {
                    final Action action = context.get("action");
                    final Category category = context.get("category");
                    if (action.getCategory().getCategoryFullName().equalsIgnoreCase(category.getCategoryFullName())) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error> Error: The action <highlight>" + action.getActionName() + "</highlight> already has the category <highlight2>" + action.getCategory().getCategoryFullName() + "</highlight2>."
                        ));
                        return;
                    }

                    context.sender().sendMessage(notQuests.parse(
                            "<success>Category for action <highlight>" + action.getActionName() + "</highlight> has successfully been changed from <highlight2>"
                                    + action.getCategory().getCategoryFullName() + "</highlight2> to <highlight2>" + category.getCategoryFullName() + "</highlight2>!"
                    ));
                    action.switchCategory(category);

                }));
    }
}
