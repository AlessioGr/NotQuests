/*
 * NotQuests - A Categorying plugin for Minecraft Servers
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
//
// MIT License
//
// Copyright (c) 2021 Alexander Söderberg & Contributors
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package rocks.gravili.notquests.paper.commands.arguments;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;

import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

public class CategorySelector<C> extends CommandArgument<C, Category> {

    protected CategorySelector(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String,
                    @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription,
            NotQuests main
    ) {
        super(required, name, new CategoryParser<>(main), defaultValue, Category.class, suggestionsProvider);
    }


    public static <C> CategorySelector.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main) {
        return new CategorySelector.Builder<>(name, main);
    }

    public static <C> @NonNull CommandArgument<C, Category> of(final @NonNull String name, final NotQuests main) {
        return CategorySelector.<C>newBuilder(name, main).asRequired().build();
    }

    public static <C> @NonNull CommandArgument<C, Category> optional(final @NonNull String name, final NotQuests main) {
        return CategorySelector.<C>newBuilder(name, main).asOptional().build();
    }

    public static <C> @NonNull CommandArgument<C, Category> optional(
            final @NonNull String name,
            final @NonNull Category Category,
            final NotQuests main
    ) {
        return CategorySelector.<C>newBuilder(name, main).asOptionalWithDefault(Category.getCategoryName()).build();
    }


    public static final class Builder<C> extends CommandArgument.Builder<C, Category> {
        private final NotQuests main;
        private boolean takeEnabledOnly = false;

        private Builder(final @NonNull String name, NotQuests main) {
            super(Category.class, name);
            this.main = main;
        }

        public CommandArgument.Builder<C, Category> takeEnabledOnly() {
            this.takeEnabledOnly = true;
            return this;
        }

        @Override
        public @NonNull CommandArgument<C, Category> build() {
            return new CategorySelector<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription(),
                    this.main
            );
        }
    }


    public static final class CategoryParser<C> implements ArgumentParser<C, Category> {

        private final NotQuests main;


        /**
         * Constructs a new PluginsParser.
         */
        public CategoryParser(
                NotQuests main
        ) {
            this.main = main;
        }


        @NotNull
        @Override
        public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
            final List<String> categoryNames = new java.util.ArrayList<>();
            for (Category category : main.getDataManager().getCategories()) {
                categoryNames.add(category.getCategoryFullName());
            }

            final List<String> allArgs = context.getRawInput();

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.getSender(), allArgs.toArray(new String[0]), "[Category Name]", "[...]");

            return categoryNames;
        }

        @Override
        public @NonNull ArgumentParseResult<Category> parse(@NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
            if (inputQueue.isEmpty()) {
                return ArgumentParseResult.failure(new NoInputProvidedException(CategoryParser.class, context));
            }
            final String input = inputQueue.peek();
            inputQueue.remove();
            final Category foundCategory = main.getDataManager().getCategory(input);
            if (foundCategory == null) {
                return ArgumentParseResult.failure(new IllegalArgumentException("Error: The category " + input + " does not exist."));
            }


            return ArgumentParseResult.success(foundCategory);

        }

        @Override
        public boolean isContextFree() {
            return true;
        }
    }
}