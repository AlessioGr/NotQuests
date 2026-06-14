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
import rocks.gravili.notquests.paper.managers.data.Category;

import java.util.ArrayList;
import java.util.List;

/** Native-framework port of {@code CategoryParser}: resolves a {@link Category} by name. */
public final class CategoryArgument extends NQArgumentType<Category> {
    private final NotQuests main;

    public CategoryArgument(final NotQuests main) {
        this.main = main;
    }

    public static CategoryArgument categoryArgument(final NotQuests main) {
        return new CategoryArgument(main);
    }

    @Override
    public Category convert(final String input) throws CommandSyntaxException {
        for (final Category category : main.getDataManager().getCategories()) {
            if (category.getCategoryName().equalsIgnoreCase(input)) {
                return category;
            }
        }
        throw fail("No Category found: " + input);
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> entries = new ArrayList<>();
        for (final Category category : main.getDataManager().getCategories()) {
            entries.add(category.getCategoryName());
        }
        return entries;
    }
}
