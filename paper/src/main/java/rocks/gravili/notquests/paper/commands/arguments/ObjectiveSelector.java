/*
 * NotQuests - A Objectiveing plugin for Minecraft Servers
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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;

public class ObjectiveSelector<C> extends CommandArgument<C, Objective> {
  private final int level;

  protected ObjectiveSelector(
      final boolean required,
      final @NonNull String name,
      final @NonNull String defaultValue,
      final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String,
          @NonNull List<@NonNull String>> suggestionsProvider,
      final @NonNull ArgumentDescription defaultDescription,
      final NotQuests main,
      final int level
  ) {
    super(required, name, new ObjectivesParser<>(main, level), defaultValue, Objective.class, suggestionsProvider);
    this.level = level;
  }


  public static <C> ObjectiveSelector.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main, int level) {
    return new ObjectiveSelector.Builder<>(name, main, level);
  }

  public static <C> @NonNull CommandArgument<C, Objective> of(final @NonNull String name, final NotQuests main, int level) {
    return ObjectiveSelector.<C>newBuilder(name, main, level).asRequired().build();
  }

  public static <C> @NonNull CommandArgument<C, Objective> optional(final @NonNull String name, final NotQuests main, int level) {
    return ObjectiveSelector.<C>newBuilder(name, main, level).asOptional().build();
  }

  public static <C> @NonNull CommandArgument<C, Objective> optional(
      final @NonNull String name,
      final @NonNull Objective objective,
      final NotQuests main,
      int level
  ) {
    return ObjectiveSelector.<C>newBuilder(name, main, level).asOptionalWithDefault(objective.getIdentifier()).build();
  }

  public int getLevel() {
    return this.level;
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, Objective> {
    private final NotQuests main;
    private int level = 0;

    private Builder(final @NonNull String name, NotQuests main, int level) {
      super(Objective.class, name);
      this.main = main;
      this.level = level;
    }

    @Override
    public @NonNull CommandArgument<C, Objective> build() {
      return new ObjectiveSelector<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          this.main,
          level
      );
    }
  }


  public static final class ObjectivesParser<C> implements ArgumentParser<C, Objective> {

    private final NotQuests main;
    private final int level;


    /**
     * Constructs a new PluginsParser.
     */
    public ObjectivesParser(
        NotQuests main,
        int level
    ) {
      this.main = main;
      this.level = level;
    }

    private ObjectiveHolder getObjectiveHolderForLevel(final @NotNull CommandContext<C> context, int level){
      final ObjectiveHolder objectiveHolder;
      if(level == 0){
        objectiveHolder = context.get("quest");
      }else if(level == 1){
        objectiveHolder = context.get("Objective ID");
        //objectiveHolder = getObjectiveHolderForLevel(context, 0).getObjectiveFromID(context.get("Objective ID"));
      }else{
        objectiveHolder = context.get("Objective ID " + level);

        //objectiveHolder = getObjectiveHolderForLevel(context, level-1).getObjectiveFromID(context.get("Objective ID " + level));
      }
      return objectiveHolder;
    }

    @NotNull
    @Override
    public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
      final List<String> objectiveNames = new java.util.ArrayList<>();
      final ObjectiveHolder objectiveHolder = getObjectiveHolderForLevel(context, level);

      for (final Objective objective : objectiveHolder.getObjectives()) {
        objectiveNames.add(""+objective.getObjectiveID());
      }

      final List<String> allArgs = context.getRawInput();

      main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.getSender(), allArgs.toArray(new String[0]), "[Objective ID]", "[...]");

      return objectiveNames;
    }

    @Override
    public @NonNull ArgumentParseResult<Objective> parse(@NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
      if (inputQueue.isEmpty()) {
        return ArgumentParseResult.failure(new NoInputProvidedException(ObjectivesParser.class, context));
      }
      final String input = inputQueue.peek();
      inputQueue.remove();

      int objectiveIDParsed;
      try{
        objectiveIDParsed = Integer.parseInt(input);
      }catch (NumberFormatException e){
        return ArgumentParseResult.failure(new NumberFormatException("The Objective ID must be a number!"));
      }

      final ObjectiveHolder objectiveHolder = getObjectiveHolderForLevel(context, level);
      final Objective foundObjective = objectiveHolder.getObjectiveFromID(objectiveIDParsed);
      if (foundObjective == null) {
        return ArgumentParseResult.failure(new IllegalArgumentException("The Objective with the ID " + objectiveIDParsed + " does not exist!"));
      }



      return ArgumentParseResult.success(foundObjective);

    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}