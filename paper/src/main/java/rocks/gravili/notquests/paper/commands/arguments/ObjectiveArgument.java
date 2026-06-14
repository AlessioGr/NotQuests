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
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code ObjectiveParser}. Native Brigadier argument types parse in
 * isolation and cannot see prior positional arguments, so this argument parses only the raw objective
 * <b>id</b>. The actual {@link Objective} is resolved from the id chain (quest → objectiveId →
 * objectiveId2 → …) by {@link #resolveHolder} at the point of consumption (the command handler, via
 * {@code CommandManager#getObjectiveFromContextAndLevel}) and for tab-completion in {@link #suggest}.
 */
public final class ObjectiveArgument extends NQArgumentType<String> {
    private final NotQuests main;
    private final int level;

    public ObjectiveArgument(final NotQuests main, final int level) {
        this.main = main;
        this.level = level;
    }

    public static ObjectiveArgument objectiveArgument(final NotQuests main, final int level) {
        return new ObjectiveArgument(main, level);
    }

    @Override
    public String convert(final String input) {
        return input; // raw objective id; resolved against the prior args by resolveHolder()
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final ObjectiveHolder holder = resolveHolder(context, level);
        final List<String> ids = new ArrayList<>();
        if (holder != null) {
            for (final Objective objective : holder.getObjectives()) {
                ids.add(String.valueOf(objective.getObjectiveID()));
            }
        }
        return ids;
    }

    /**
     * Resolves the {@link ObjectiveHolder} at {@code level} (0 = the quest) by walking the prior
     * positional arguments: {@code quest}, then {@code objectiveId}, {@code objectiveId2}, … each of
     * which is now a raw id string.
     */
    public static ObjectiveHolder resolveHolder(final CommandContext<?> context, final int level) {
        ObjectiveHolder holder = arg(context, "quest");
        for (int l = 1; l <= level; l++) {
            final String key = (l == 1) ? "objectiveId" : ("objectiveId" + l);
            final String rawId = arg(context, key);
            if (rawId == null) {
                return holder;
            }
            holder = findObjective(holder, rawId);
            if (holder == null) {
                return null;
            }
        }
        return holder;
    }

    public static Objective findObjective(final ObjectiveHolder holder, final String rawId) {
        if (holder == null) {
            return null;
        }
        for (final Objective objective : holder.getObjectives()) {
            if (String.valueOf(objective.getObjectiveID()).equalsIgnoreCase(rawId)) {
                return objective;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T arg(final CommandContext<?> context, final String name) {
        try {
            return (T) context.getArgument(name, Object.class);
        } catch (final IllegalArgumentException notPresent) {
            return null;
        }
    }
}
