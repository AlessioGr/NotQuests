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

package rocks.gravili.notquests.paper.commands.arguments;

import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EntityTypeParser<C> implements ArgumentParser<C, String> {
    private final NotQuests main;
    private final boolean mythicMobsFactions;
    protected EntityTypeParser(NotQuests main, boolean mythicMobsFactions) {
        this.main = main;
        this.mythicMobsFactions = mythicMobsFactions;
    }

    public static <C> @NonNull ParserDescriptor<C, String> entityTypeParser(final NotQuests main) {
        return ParserDescriptor.of(new EntityTypeParser<>(main, false), String.class);
    }

    public static <C> @NonNull ParserDescriptor<C, String> entityTypeParser(final NotQuests main, boolean mythicMobsFactions) {
        return ParserDescriptor.of(new EntityTypeParser<>(main, mythicMobsFactions), String.class);
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull String> parse(@NonNull CommandContext<@NonNull C> commandContext, @NonNull CommandInput commandInput) {
        if (commandInput.isEmpty()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Invalid Category: " + commandContext));
        }
        String rawInput = commandInput.readString();
        if (mythicMobsFactions && main.getIntegrationsManager().isMythicMobsEnabled() && main.getIntegrationsManager().getMythicMobsManager() != null) {
            if(main.getIntegrationsManager().getMythicMobsManager().getFactionNames("mmfaction:").contains(rawInput)) {
                return ArgumentParseResult.success(rawInput);
            }
        }

        if (!main.getDataManager().standardEntityTypeCompletions.contains(rawInput) && !rawInput.equalsIgnoreCase("ANY")) {
            return ArgumentParseResult.failure(
                    new IllegalArgumentException("Entity type '" + rawInput + "' does not exist!"));
        }

        return ArgumentParseResult.success(rawInput);
    }


    @Override
    public @NonNull SuggestionProvider<C> suggestionProvider() {
        return (context, input) -> {
            // NOTE: Stream.toList() is immutable; wrap in ArrayList so the .add()/.addAll() below work.
            List<Suggestion> completions = new ArrayList<>(main.getDataManager().standardEntityTypeCompletions.stream().map(Suggestion::suggestion).toList());
            completions.add(Suggestion.suggestion("any"));

            //Add extra Mythic Mobs completions, if enabled
            if (mythicMobsFactions && main.getIntegrationsManager().isMythicMobsEnabled() && main.getIntegrationsManager().getMythicMobsManager() != null) {
                completions.addAll(main.getIntegrationsManager().getMythicMobsManager().getFactionNames("mmfaction:").stream().map(Suggestion::suggestion).toList());
            }
            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), input.input().split(" "), "[Mob Name / 'ANY']", "[...]");
            return CompletableFuture.completedFuture(completions);
        };
    }
}