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

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Native-framework port of {@code ApplyOnParser}: resolves an "apply on" target as the integer
 * {@code 0} (the literal {@code Quest}) or an objective id of the {@link Quest} carried by a prior
 * positional argument named {@code questContext}.
 *
 * <p>NOTE: the Cloud parser read the quest from the command context inside {@code parse}. Brigadier
 * does not hand {@code convert} a context, so resolution is delegated to
 * {@link #convert(CommandContext, String)}.
 */
public final class ApplyOnArgument extends NQArgumentType<Integer> {
    private final NotQuests main;
    private final String questContext;

    public ApplyOnArgument(final NotQuests main, final String questContext) {
        this.main = main;
        this.questContext = questContext;
    }

    public static ApplyOnArgument applyOnArgument(final NotQuests main, final String questContext) {
        return new ApplyOnArgument(main, questContext);
    }

    @Override
    public Integer convert(final String input) throws CommandSyntaxException {
        // applyOn is a small integer: 0 = Quest, 1 = Objective 1, ... Accept "Quest", "O1"/"O2"
        // (objective shorthand) or a bare number. No prior-argument context is needed.
        if (input.equalsIgnoreCase("Quest")) {
            return 0;
        }
        try {
            return Integer.parseInt(input.toLowerCase(Locale.ROOT).replace("o", "").trim());
        } catch (final NumberFormatException e) {
            throw fail("ApplyOn '" + input + "' is not valid (use 'Quest', 'O1', 'O2', ... or a number)");
        }
    }

    public Integer convert(final CommandContext<?> context, final String input) throws CommandSyntaxException {
        if (input.equalsIgnoreCase("Quest")) {
            return 0;
        }
        final Quest quest = (Quest) context.getArgument(questContext, Object.class);
        try {
            final int objectiveID = Integer.parseInt(input.toLowerCase(Locale.ROOT).replace("o", ""));
            if (quest.getObjectiveFromID(objectiveID) != null) {
                return objectiveID;
            } else {
                throw fail("ApplyOn Objective '" + input + "' is not an objective of the Quest!");
            }
        } catch (final NumberFormatException e) {
            throw fail("ApplyOn Objective '" + input + "' is not a valid applyOn objective!");
        }
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> entries = new ArrayList<>();
        final Quest quest = (Quest) context.getArgument(questContext, Object.class);
        entries.add("Quest");
        for (final Objective objective : quest.getObjectives()) {
            entries.add("O" + objective.getObjectiveID());
        }
        return entries;
    }
}
