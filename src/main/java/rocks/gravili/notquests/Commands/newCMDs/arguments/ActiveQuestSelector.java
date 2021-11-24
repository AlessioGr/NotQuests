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

package rocks.gravili.notquests.Commands.newCMDs.arguments;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import io.leangen.geantyref.TypeToken;
import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;

import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class ActiveQuestSelector<C> extends CommandArgument<C, Quest> {

    public ActiveQuestSelector(
            boolean required,
            String name,
            NotQuests main,
            String playerContext
    ) {
        super(
                required,
                name,
                new ActiveQuestsParser<>(main, playerContext),
                "",
                new TypeToken<>() {
                },
                null
        );
    }


    public static final class ActiveQuestsParser<C> implements ArgumentParser<C, Quest> {

        private final NotQuests main;
        private final String playerContext;

        /**
         * Constructs a new PluginsParser.
         */
        public ActiveQuestsParser(
                NotQuests main,
                String playerContext
        ) {
            this.main = main;
            this.playerContext = playerContext;
        }


        @NotNull
        @Override
        public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
            List<String> questNames = new java.util.ArrayList<>();

            SinglePlayerSelector singlePlayerSelector = context.get(playerContext);
            UUID uuid;
            if (singlePlayerSelector.getPlayer() != null) {
                uuid = singlePlayerSelector.getPlayer().getUniqueId();
            } else {
                uuid = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector()).getUniqueId();
            }

            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(uuid);
            if (questPlayer != null) {
                for (Quest quest : main.getQuestManager().getAllQuests()) {
                    if (questPlayer.hasAcceptedQuest(quest)) {
                        questNames.add(quest.getQuestName());
                    }

                }
            }


            return questNames;
        }

        @Override
        public @NonNull ArgumentParseResult<Quest> parse(@NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
            if (inputQueue.isEmpty()) {
                return ArgumentParseResult.failure(new NoInputProvidedException(ActiveQuestsParser.class, context));
            }
            final String input = inputQueue.peek();

            SinglePlayerSelector singlePlayerSelector = context.get(playerContext);
            UUID uuid;
            if (singlePlayerSelector.getPlayer() != null) {
                uuid = singlePlayerSelector.getPlayer().getUniqueId();
            } else {
                uuid = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector()).getUniqueId();
            }

            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(uuid);
            if (questPlayer == null) {
                return ArgumentParseResult.failure(new IllegalArgumentException("Quest '" + input + "' is not active!"
                ));
            }


            final Quest foundQuest = main.getQuestManager().getQuest(input);
            if (foundQuest == null) {
                return ArgumentParseResult.failure(new IllegalArgumentException("Quest '" + input + "' does not exist!"
                ));
            }
            inputQueue.remove();

            if (questPlayer.hasAcceptedQuest(foundQuest)) {
                return ArgumentParseResult.success(foundQuest);
            } else {
                return ArgumentParseResult.failure(new IllegalArgumentException("Quest '" + input + "' is not active!"
                ));
            }


        }

        @Override
        public boolean isContextFree() {
            return true;
        }
    }
}