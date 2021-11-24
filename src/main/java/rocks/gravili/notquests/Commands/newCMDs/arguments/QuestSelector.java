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
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

import java.util.List;
import java.util.Queue;

public class QuestSelector<C, ArrayList> extends CommandArgument<C, List<Quest>> {

    public QuestSelector(
            boolean required,
            String name,
            NotQuests main
    ) {
        super(
                required,
                name,
                new QuestsParser<>(main),
                "",
                new TypeToken<List<Quest>>() {
                },
                null
        );
    }


    public static final class QuestsParser<C, ArrayList> implements ArgumentParser<C, List<Quest>> {

        private final NotQuests main;


        /**
         * Constructs a new PluginsParser.
         */
        public QuestsParser(
                NotQuests main
        ) {
            this.main = main;
        }


        @Override
        public List<String> suggestions(CommandContext<C> context, String input) {
            List<String> questNames = new java.util.ArrayList<>();
            for (Quest quest : main.getQuestManager().getAllQuests()) {
                questNames.add(quest.getQuestName());
            }
            return questNames;
        }

        @Override
        public @NonNull ArgumentParseResult<List<Quest>> parse(@NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
            if (inputQueue.isEmpty()) {
                return ArgumentParseResult.failure(new NoInputProvidedException(QuestsParser.class, context));
            }

            return ArgumentParseResult.success(main.getQuestManager().getAllQuests());

        }

        @Override
        public boolean isContextFree() {
            return true;
        }
    }
}