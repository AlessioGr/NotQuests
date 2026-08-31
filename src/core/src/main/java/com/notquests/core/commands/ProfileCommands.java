package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ProfileCommands {
    private ProfileCommands() {}

    static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            userCommands(
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
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                profiles = root.literal(
                                "profiles",
                                NQDescription.of("Manages player quest profiles."))
                        .permission("notquests.user.profiles");
        commands.add(profiles.literal(
                        "show",
                        NQDescription.of("Shows your current quest profile and the other profiles you can switch to."))
                .commandDescription(NQDescription.of("Shows current profile and lists other profiles."))
                .handler(context -> profileMessages(plugin, context.questPlayer()))
                .registration());
        commands.add(profiles.literal(
                        "change",
                        NQDescription.of("Switches the active player profile."))
                .required(
                        "profile-name",
                        NQArgumentType.profileName(),
                        NQDescription.of("Quest profile name. Profile names cannot contain spaces."))
                .commandDescription(NQDescription.of("Shows current profile and lists other profiles."))
                .handler(context -> List.of(changeProfile(
                        plugin,
                        context.questPlayer(),
                        context.argument("profile-name"))))
                .registration());
        commands.add(profiles.literal(
                        "create",
                        NQDescription.of("Creates a new quest profile for the current player."))
                .required(
                        "profile-name",
                        NQArgumentType.word("profile name"),
                        NQDescription.of("Quest profile name. Profile names cannot contain spaces."),
                        (context, input) -> List.of("<enter new profile-name (no spaces!)"))
                .commandDescription(NQDescription.of("Creates a new profile."))
                .handler(context -> List.of(createProfile(
                        plugin,
                        context.questPlayer(),
                        context.argument("profile-name"))))
                .registration());
        return List.copyOf(commands);
    }

    static List<String> profileNames(final NotQuestsPlugin plugin, final PlatformPlayer questPlayer) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return List.of();
        }
        plugin.activeQuestPlayer(questPlayer.playerIdentifier());
        final String currentProfile = plugin.activeProfile(questPlayer.playerIdentifier());
        return plugin.playerProfileNames(questPlayer.playerIdentifier()).stream()
                .filter(profile -> !profile.equalsIgnoreCase(currentProfile))
                .toList();
    }

    static List<CommandMessage> profileMessages(
            final NotQuestsPlugin plugin,
            final PlatformPlayer questPlayer) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return List.of(CommandMessage.error("This command can only be used by a player."));
        }
        final QuestPlayer current = plugin.activeQuestPlayer(questPlayer.playerIdentifier());
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success(plugin.translate(questPlayer,
                "chat.profiles.list-current",
                Map.of(
                        "%PROFILENAME%", current.getProfile(),
                        "%QUESTPOINTS%", String.valueOf(current.getQuestPoints())),
                "<highlight>Current profile: <main>%PROFILENAME% (%QUESTPOINTS% quest points)")));
        messages.add(CommandMessage.success(plugin.translate(questPlayer,
                "chat.profiles.list-other-heading",
                Map.of(),
                "<highlight>Other profiles:")));
        final List<String> otherProfiles = profileNames(plugin, questPlayer);
        if (otherProfiles.isEmpty()) {
            messages.add(CommandMessage.success(plugin.translate(questPlayer,
                    "chat.profiles.list-other-none",
                    Map.of(),
                    "  <main>You have no more profiles.")));
        } else {
            for (final String profile : otherProfiles) {
                final QuestPlayer profileData = plugin.questPlayer(questPlayer.playerIdentifier(), profile);
                messages.add(CommandMessage.success(plugin.translate(questPlayer,
                        "chat.profiles.list-other",
                        Map.of(
                                "%PROFILENAME%", profileData.getProfile(),
                                "%QUESTPOINTS%", String.valueOf(profileData.getQuestPoints())),
                        "  <highlight>- <main>%PROFILENAME% (%QUESTPOINTS% quest points)")));
            }
        }
        return List.copyOf(messages);
    }

    static CommandMessage changeProfile(
            final NotQuestsPlugin plugin,
            final PlatformPlayer questPlayer,
            final String newProfile) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return CommandMessage.error("This command can only be used by a player.");
        }
        final String currentProfile = plugin.activeProfile(questPlayer.playerIdentifier());
        if (currentProfile.equalsIgnoreCase(newProfile)) {
            return CommandMessage.error(plugin.translate(questPlayer,
                    "chat.profiles.change-same-profile",
                    Map.of(),
                    "<error>You are already using that profile!"));
        }
        final String storedNewProfile = plugin.playerProfileNames(questPlayer.playerIdentifier()).stream()
                .filter(profile -> profile.equalsIgnoreCase(newProfile))
                .findFirst()
                .orElse(newProfile);
        if (!plugin.changePlayerProfile(questPlayer.playerIdentifier(), storedNewProfile)) {
            return CommandMessage.error(plugin.translate(questPlayer,
                    "chat.profiles.change-profile-doesnt-exist",
                    Map.of(),
                    "<error>That profile does not exist."));
        }
        plugin.saveData();
        return CommandMessage.success(plugin.translate(questPlayer,
                "chat.profiles.changed-successfully",
                Map.of(
                        "%OLDPROFILENAME%", currentProfile,
                        "%NEWPROFILENAME%", storedNewProfile),
                "<success>You have successfully changed your profile from %OLDPROFILENAME% to <highlight>%NEWPROFILENAME%</highlight>!"));
    }

    static CommandMessage createProfile(
            final NotQuestsPlugin plugin,
            final PlatformPlayer questPlayer,
            final String newProfile) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return CommandMessage.error("This command can only be used by a player.");
        }
        plugin.activeQuestPlayer(questPlayer.playerIdentifier());
        final boolean valid = newProfile != null && newProfile.matches("[0-9A-Za-z._-]+");
        if (!valid) {
            return CommandMessage.error(plugin.translate(questPlayer,
                    "chat.profiles.create-invalid-characters",
                    Map.of(),
                    "<error>The profile name you provided contains invalid characters."));
        }
        final String existing = plugin.playerProfileNames(questPlayer.playerIdentifier()).stream()
                .filter(profile -> profile.equalsIgnoreCase(newProfile))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return CommandMessage.error(plugin.translate(questPlayer,
                    "chat.profiles.create-already-exists",
                    Map.of("%NEWPROFILENAME%", existing),
                    "<error>A profile with the name <highlight>%NEWPROFILENAME%</highlight> already exists."));
        }
        plugin.createPlayerProfile(questPlayer.playerIdentifier(), newProfile);
        plugin.saveData();
        return CommandMessage.success(plugin.translate(questPlayer,
                "chat.profiles.created-successfully",
                Map.of("%NEWPROFILENAME%", newProfile),
                "<success>You have successfully created the profile <highlight>%NEWPROFILENAME%</highlight>! To switch to it, use <highlight2>/nq profiles change %NEWPROFILENAME%</highlight2>"));
    }
}
