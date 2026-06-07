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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.NQNPCResult;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.managers.npc.NQNPCID;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NQNPCParser<C> implements ArgumentParser<C, NQNPCResult> {
    private final NotQuests main;

    private final boolean allowNone;
    private final boolean allowRightClickSelect;

    protected NQNPCParser(NotQuests main, boolean allowNone, boolean allowRightClickSelect) {
        this.main = main;
        this.allowNone = allowNone;
        this.allowRightClickSelect = allowRightClickSelect;
    }

    public static <C> @NonNull ParserDescriptor<C, NQNPCResult> nqNPCParser(final NotQuests main, boolean allowNone, boolean allowRightClickSelect) {
        return ParserDescriptor.of(new NQNPCParser<>(main, allowNone, allowRightClickSelect), NQNPCResult.class);
    }

    public static <C> @NonNull ParserDescriptor<C, NQNPCResult> nqNPCParser(final NotQuests main) {
        return ParserDescriptor.of(new NQNPCParser<>(main, false, false), NQNPCResult.class);
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull NQNPCResult> parse(@NonNull CommandContext<@NonNull C> commandContext, @NonNull CommandInput commandInput) {
        if (commandInput.isEmpty()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Invalid Conversation: " + commandContext));
        }
        String rawInput = commandInput.readString();

        if(allowNone && rawInput.equalsIgnoreCase("none")){
            // TODO: Missing context
            // inputQueue.remove();
            return ArgumentParseResult.success(new NQNPCResult(null, true, false));
        }

        if(allowRightClickSelect && rawInput.equalsIgnoreCase("rightClickSelect")){
            // TODO: Missing context
            // inputQueue.remove();
            return ArgumentParseResult.success(new NQNPCResult(null, false, true));
        }

        if(!rawInput.contains(":") || rawInput.split(":").length != 2){
            return ArgumentParseResult.failure(new IllegalArgumentException("Wrong input. Format needs to be [NPC Plugin Name]:[NPC ID]. Please follow the command suggestions."));
        }

        Integer foundInteger;
        for (String npcIdentifier : NotQuests.getInstance().getNPCManager().getAllNPCsString()) {
            if (npcIdentifier.equalsIgnoreCase(rawInput)) {
                String type = npcIdentifier.split(":")[0];
                String id = npcIdentifier.split(":")[1];
                // FancyNPCs uses String (UUID) ids; Citizens uses int ids. Parsing a FancyNPCs UUID as
                // an int threw NumberFormatException, so branch on the NPC backend.
                final NQNPCID nqnpcid;
                if (type.equalsIgnoreCase("fancynpcs")) {
                    nqnpcid = NQNPCID.fromString(id);
                } else {
                    try {
                        nqnpcid = NQNPCID.fromInteger(Integer.parseInt(id));
                    } catch (final NumberFormatException e) {
                        return ArgumentParseResult.failure(
                                new IllegalArgumentException("Invalid NPC id '" + id + "' for type '" + type + "'"));
                    }
                }
                NQNPC npc = main.getNPCManager().getOrCreateNQNpc(type, nqnpcid);
                if(npc == null){
                    return ArgumentParseResult.failure(new IllegalArgumentException("No NPC found: " + commandContext));
                }
                // A concrete NPC was resolved, so this is neither a "none" nor a "rightClickSelect"
                // request — passing the parser's allow* config here made every specified NPC behave
                // like rightClickSelect.
                return ArgumentParseResult.success(new NQNPCResult(npc, false, false));
            }
        }
        return ArgumentParseResult.failure(new IllegalArgumentException("No NPC found: " + commandContext));
    }


    @Override
    public @NonNull SuggestionProvider<C> suggestionProvider() {
        return (context, input) -> {
            List<Suggestion> entries = new java.util.ArrayList<>();
            if (allowRightClickSelect) {
                entries.add(Suggestion.suggestion("rightClickSelect"));
            }
            if (allowNone) {
                entries.add(Suggestion.suggestion("none"));
            }
            for (String npcIdentifier : main.getNPCManager().getAllNPCsString()) {
                entries.add(Suggestion.suggestion(npcIdentifier));
            }
            return CompletableFuture.completedFuture(entries);
        };
    }
}