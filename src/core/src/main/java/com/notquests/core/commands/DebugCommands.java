package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.managers.CommandManager;
import com.notquests.core.managers.DataManager.ReloadTarget;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

final class DebugCommands {
    private DebugCommands() {}

    static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            saveCommands(
                    final NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            root,
                    final NotQuestsPlugin plugin) {
        return List.of(root.literal("save", NQDescription.of("Saves NotQuests configuration and player data to disk."))
                .commandDescription(NQDescription.of("Saves all NotQuests data."))
                .handler(ignored -> List.of(saveData(plugin)))
                .registration());
    }

    static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            reloadCommands(
                    final NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            root,
                    final NotQuestsPlugin plugin) {
        final ArrayList<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();
        commands.add(root.literal("reload", NQDescription.of("Reloads selected NotQuests files without restarting the server."))
                .commandDescription(NQDescription.of("Reloads general configuration, language files, and conversations."))
                .handler(ignored -> List.of(reloadData(plugin, ReloadTarget.ALL)))
                .registration());
        commands.add(root.literal("reload", NQDescription.of("Reloads selected NotQuests files without restarting the server."))
                .literal("general.yml", NQDescription.of("Reloads the main general.yml configuration file."))
                .commandDescription(NQDescription.of("Reloads general.yml."))
                .handler(ignored -> List.of(reloadData(plugin, ReloadTarget.CONFIG)))
                .registration());
        commands.add(root.literal("reload", NQDescription.of("Reloads selected NotQuests files without restarting the server."))
                .literal("languages", NQDescription.of("Reloads NotQuests translation and language files."))
                .commandDescription(NQDescription.of("Reloads language files."))
                .handler(ignored -> List.of(reloadData(plugin, ReloadTarget.LANGUAGES)))
                .registration());
        commands.add(root.literal("reload", NQDescription.of("Reloads selected NotQuests files without restarting the server."))
                .literal("conversations", NQDescription.of("Reloads saved NotQuests conversations from disk."))
                .commandDescription(NQDescription.of("Reloads conversation files."))
                .handler(ignored -> List.of(reloadData(plugin, ReloadTarget.CONVERSATIONS)))
                .registration());
        return List.copyOf(commands);
    }

    static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            debugCommands(
                    final NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            root,
                    final NotQuestsPlugin plugin,
                    final NotQuestsAdapter adapter,
                    final CommandManager commandManager,
                    final Supplier<String> version,
                    final Supplier<String> minecraftVersion,
                    final Supplier<Path> dataFolder) {
        final ArrayList<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                debug = root.literal("debug", NQDescription.of("Opens NotQuests debug utilities."));
        commands.add(debug.handler(context -> List.of(toggleDebug(plugin, context.questPlayer())))
                .registration());
        commands.add(debug.literal("clearOwnChat", NQDescription.of("Clears the command sender's chat for debugging."))
                .commandDescription(NQDescription.of("Clears your own chat."))
                .handler(context -> List.of(clearOwnChat()))
                .registration());
        commands.add(debug.literal("worldInfo", NQDescription.of("Shows debug information about the current world."))
                .commandDescription(NQDescription.of("Shows information about the command sender's current world."))
                .handler(context -> worldInfo(context.questPlayer()))
                .registration());
        commands.add(debug.literal(
                        "loadDataUnsafe",
                        NQDescription.of("Unsafely reloads all NotQuests configuration and runtime data for debugging."))
                .commandDescription(NQDescription.of(
                        "Unsafely reloads quests, player runtime data, GUI layouts, and NPC attachments in addition to normal reload targets."))
                .handler(context -> reloadDataUnsafe(plugin))
                .registration());
        commands.add(debug.literal(
                        "disablePluginAndSaving",
                        NQDescription.of("Disables NotQuests saving and loading."))
                .required(
                        "reason",
                        NQArgumentType.word("reason"),
                        NQDescription.of("Reason recorded for enabling or disabling NotQuests."))
                .commandDescription(NQDescription.of("Disables NotQuests saving and loading."))
                .handler(context -> List.of(disableSavingAndLoading(plugin, context.argument("reason"))))
                .registration());
        commands.add(debug.literal(
                        "showErrorsAndWarnings",
                        NQDescription.of("Shows collected NotQuests errors and warnings."))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>presence(
                        NQFlags.PRINT_TO_CONSOLE.name(), NQFlags.PRINT_TO_CONSOLE.description()))
                .commandDescription(NQDescription.of("Shows the current errors and warnings NotQuests collected."))
                .handler(context -> errorsAndWarnings(
                        plugin,
                        adapter,
                        context.flagPresent(NQFlags.PRINT_TO_CONSOLE.name())))
                .registration());
        commands.add(debug.literal(
                        "exportCommandSchema",
                        NQDescription.of("Exports the command schema JSON generated from the live command tree."))
                .commandDescription(NQDescription.of("Exports the generated command schema as JSON."))
                .handler(context -> exportCommandSchema(commandManager, version.get(), dataFolder.get()))
                .registration());
        commands.add(debug.literal(
                        "exportMetadata",
                        NQDescription.of("Exports command schema plus runtime registry metadata JSON."))
                .commandDescription(NQDescription.of("Exports the generated runtime metadata bundle as JSON."))
                .handler(context -> exportMetadata(commandManager, version.get(), minecraftVersion.get(), dataFolder.get()))
                .registration());
        commands.add(debug.literal(
                        "enablePluginAndSaving",
                        NQDescription.of("Re-enables NotQuests saving and loading."))
                .required(
                        "reason",
                        NQArgumentType.word("reason"),
                        NQDescription.of("Reason recorded for enabling or disabling NotQuests."))
                .commandDescription(NQDescription.of("Enables NotQuests saving and loading."))
                .handler(context -> List.of(enableSavingAndLoading(plugin, context.argument("reason"))))
                .registration());
        commands.add(debug.literal("testcommand", NQDescription.of("Shows stored conversation chat history for debugging."))
                .commandDescription(NQDescription.of("Shows stored conversation chat history for debugging."))
                .handler(context -> debugConversationChatHistory(plugin, context.questPlayer(), false))
                .registration());
        commands.add(debug.literal("testcommand2", NQDescription.of("Shows indexed stored conversation chat history for debugging."))
                .commandDescription(NQDescription.of("Shows indexed stored conversation chat history for debugging."))
                .handler(context -> debugConversationChatHistory(plugin, context.questPlayer(), true))
                .registration());
        commands.add(debug.literal("beaconBeam", NQDescription.of("Spawns a debug beacon beam for one player."))
                .required(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player who should see the debug beacon beam."))
                .required(
                        "location-name",
                        NQArgumentType.word("location name"),
                        NQDescription.of("Temporary debug name used to identify this beacon beam location."))
                .required(
                        "location",
                        NQArgumentType.location(),
                        NQDescription.of("World and coordinates where the debug beacon beam should appear."))
                .commandDescription(NQDescription.of("Spawns a debug beacon beam."))
                .handler(context -> List.of(debugBeaconBeam(
                        plugin,
                        adapter,
                        CommandSupport.targetPlatformPlayer(adapter, context.argument("player"), context.questPlayer()),
                        context.argument("location-name"),
                        context.rawArgument("location"))))
                .registration());
        return List.copyOf(commands);
    }

    static CommandMessage saveData(final NotQuestsPlugin plugin) {
        return plugin.saveData()
                ? CommandMessage.success("<success>NotQuests configuration and player data has been saved")
                : CommandMessage.error("<error>Could not save NotQuests data.");
    }

    static CommandMessage reloadData(final NotQuestsPlugin plugin, final ReloadTarget target) {
        final ReloadTarget reloadTarget = target == null ? ReloadTarget.ALL : target;
        if (!plugin.reloadData(reloadTarget)) {
            return CommandMessage.error("Could not reload NotQuests "
                    + CommandSupport.highlight(reloadTarget.name().toLowerCase(Locale.ROOT)) + ".");
        }
        return CommandMessage.success(switch (reloadTarget) {
            case ALL -> "<success>NotQuests general.yml, language configuration and conversations have been reloaded. "
                    + "<unimportant>Quests and player database data were not reloaded.";
            case CONFIG -> "<success>General.yml has been reloaded.";
            case LANGUAGES -> "<success>Languages have been reloaded.";
            case CONVERSATIONS -> "<success>Conversations have been reloaded.";
        });
    }

    static List<CommandMessage> reloadDataUnsafe(final NotQuestsPlugin plugin) {
        if (!plugin.reloadAllDataUnsafe()) {
            return List.of(
                    CommandMessage.emptyLine(),
                    CommandMessage.success("<main>Reloading all NotQuests data through the unsafe debug operation..."),
                    CommandMessage.error("<error>Could not reload all NotQuests data."));
        }
        return List.of(
                CommandMessage.emptyLine(),
                CommandMessage.success("<main>Reloading all NotQuests data through the unsafe debug operation..."),
                CommandMessage.success("<success>All NotQuests configuration, quests, runtime data, GUI layouts, and NPC attachments have been reloaded."));
    }

    static CommandMessage toggleDebug(final NotQuestsPlugin plugin, final PlatformPlayer questPlayer) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return CommandMessage.error("This command can only be used by a player.");
        }
        return plugin.toggleDebugPlayer(questPlayer)
                ? CommandMessage.success("<success>Your debug mode has been enabled.")
                : CommandMessage.success("<success>Your debug mode has been disabled.");
    }

    static List<CommandMessage> worldInfo(final PlatformPlayer questPlayer) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return List.of(CommandMessage.error("This command can only be used by a player."));
        }
        return List.of(
                CommandMessage.emptyLine(),
                CommandMessage.success("<main>Current world name: <highlight>" + questPlayer.worldName()
                        + "\n<main>Current world UUD: <highlight>" + questPlayer.worldIdentifier()));
    }

    static CommandMessage clearOwnChat() {
        return CommandMessage.success("<white>" + "\n".repeat(30));
    }

    static CommandMessage disableSavingAndLoading(final NotQuestsPlugin plugin, final String reason) {
        if (plugin.pluginStatus().isDisabled()) {
            return CommandMessage.error("<error>Error: NotQuests is already disabled");
        }
        plugin.pluginStatus().disableSavingAndLoading(reason);
        return CommandMessage.success("<main>Disabling NotQuests...");
    }

    static CommandMessage enableSavingAndLoading(final NotQuestsPlugin plugin, final String reason) {
        if (!plugin.pluginStatus().isDisabled()) {
            return CommandMessage.error("<error>Error: NotQuests is already enabled");
        }
        plugin.pluginStatus().enableSavingAndLoading();
        return CommandMessage.success("<main>Enabling NotQuests...");
    }

    static List<CommandMessage> errorsAndWarnings(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final boolean printToConsole) {
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        final List<String> errors = plugin.logManager().errorLogs();
        final List<String> warnings = plugin.logManager().warningLogs();
        if (errors.isEmpty() && warnings.isEmpty()) {
            final CommandMessage empty = CommandMessage.success("<success>No NotQuests errors or warnings recorded.");
            if (printToConsole) {
                adapter.logInfo(empty.message());
                return List.of(CommandMessage.success("<success>Error and warnings have been printed to console successfully!"));
            }
            return List.of(empty);
        }
        if (!errors.isEmpty()) {
            messages.add(CommandMessage.error("<error>Errors:"));
            errors.forEach(error -> messages.add(CommandMessage.error(error)));
        }
        if (!warnings.isEmpty()) {
            messages.add(CommandMessage.success("<warn>Warnings:"));
            warnings.forEach(warning -> messages.add(CommandMessage.success(warning)));
        }
        if (printToConsole) {
            for (final CommandMessage message : messages) {
                adapter.logInfo(message.message());
            }
            return List.of(CommandMessage.success("<success>Error and warnings have been printed to console successfully!"));
        }
        return List.copyOf(messages);
    }

    static List<CommandMessage> debugConversationChatHistory(
            final NotQuestsPlugin plugin,
            final PlatformPlayer questPlayer,
            final boolean indexed) {
        final List<String> history = plugin.conversationChatHistory(questPlayer, indexed);
        if (history == null || history.isEmpty()) {
            return List.of(CommandMessage.emptyLine(), CommandMessage.error("<error>No chat history!"));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.emptyLine());
        for (final String line : history) {
            messages.add(CommandMessage.success(line));
        }
        return List.copyOf(messages);
    }

    static CommandMessage debugBeaconBeam(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final PlatformPlayer target,
            final String beamName,
            final Object rawLocation) {
        if (target == null || !target.hasPlayer()) {
            return CommandMessage.error("<error>Player is not online: " + beamName);
        }
        final NQLocation location = parseLocation(adapter, rawLocation);
        if (location == null) {
            return CommandMessage.error("<error>Location is missing.");
        }
        return plugin.showTemporaryObjectiveMarker(
                        target, beamName, location, Duration.ofSeconds(20))
                ? CommandMessage.success("<success>Beacon beam spawned successfully!")
                : CommandMessage.error("<error>Could not spawn beacon beam.");
    }

    private static NQLocation parseLocation(final NotQuestsAdapter adapter, final Object rawLocation) {
        if (rawLocation instanceof final NQLocation location) {
            return location;
        }
        if (!(rawLocation instanceof final String raw) || raw.isBlank()) {
            return null;
        }
        final String[] tokens = raw.trim().split("\\s+");
        if (tokens.length < 4) {
            return null;
        }
        try {
            return adapter.location(
                    tokens[0],
                    Double.parseDouble(tokens[1]),
                    Double.parseDouble(tokens[2]),
                    Double.parseDouble(tokens[3]));
        } catch (final NumberFormatException exception) {
            return null;
        }
    }

    private static List<CommandMessage> exportCommandSchema(
            final CommandManager commandManager,
            final String pluginVersion,
            final Path dataFolder) {
        try {
            final Path output = generatedDir(dataFolder).resolve("commands.json");
            Files.createDirectories(output.getParent());
            Files.writeString(output, commandManager.commandIndex(pluginVersion).toJson(), StandardCharsets.UTF_8);
            return List.of(CommandMessage.success(
                    "Exported NotQuests command schema to " + output + "."));
        } catch (final Exception exception) {
            return List.of(CommandMessage.error(
                    "Failed to export NotQuests command schema: " + exception.getMessage()));
        }
    }

    private static List<CommandMessage> exportMetadata(
            final CommandManager commandManager,
            final String pluginVersion,
            final String minecraftVersion,
            final Path dataFolder) {
        try {
            final Path output = generatedDir(dataFolder).resolve("metadata.json");
            Files.createDirectories(output.getParent());
            Files.writeString(
                    output,
                    commandManager.metadataIndex(pluginVersion, minecraftVersion).toJson(),
                    StandardCharsets.UTF_8);
            return List.of(CommandMessage.success(
                    "Exported NotQuests metadata to " + output + "."));
        } catch (final Exception exception) {
            return List.of(CommandMessage.error(
                    "Failed to export NotQuests metadata: " + exception.getMessage()));
        }
    }

    private static Path generatedDir(final Path dataFolder) {
        return dataFolder.resolve("generated");
    }
}
