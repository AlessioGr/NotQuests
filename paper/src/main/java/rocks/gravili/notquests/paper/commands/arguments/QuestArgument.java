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
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code QuestParser}: resolves a {@link Quest} by identifier. Reference
 * implementation for porting the remaining Cloud {@code ArgumentParser}s to {@link NQArgumentType}.
 */
public final class QuestArgument extends NQArgumentType<Quest> {
    private final NotQuests main;
    private final boolean takeEnabledOnly;

    public QuestArgument(final NotQuests main, final boolean takeEnabledOnly) {
        this.main = main;
        this.takeEnabledOnly = takeEnabledOnly;
    }

    public static QuestArgument questArgument(final NotQuests main, final boolean takeEnabledOnly) {
        return new QuestArgument(main, takeEnabledOnly);
    }

    public static QuestArgument questArgument(final NotQuests main) {
        return new QuestArgument(main, false);
    }

    @Override
    public Quest convert(final String input) throws CommandSyntaxException {
        final Quest foundQuest = main.getQuestManager().getQuest(input);
        if (foundQuest == null) {
            throw fail(
                    main.getLanguageManager()
                            .getString("chat.quest-does-not-exist", (QuestPlayer) null)
                            .replace("%QUESTNAME%", input));
        }
        if (takeEnabledOnly && !foundQuest.isTakeEnabled()) {
            throw fail(main.getLanguageManager().getString("chat.take-disabled", (QuestPlayer) null, foundQuest));
        }
        return foundQuest;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> names = new ArrayList<>();
        for (final Quest quest : main.getQuestManager().getAllQuests()) {
            if (!takeEnabledOnly || quest.isTakeEnabled()) {
                names.add(quest.getIdentifier());
            }
        }
        return names;
    }
}
