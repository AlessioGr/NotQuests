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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.NQNPCResult;
import rocks.gravili.notquests.paper.managers.npc.NPCManager;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.managers.npc.NQNPCID;

public class NQNPCSelector<C> extends CommandArgument<C, NQNPCResult> { //TODO: allowRightClick selector which just returns null but adds it to the suggestions. Will have to be implemented by the command execution thingy then

  protected NQNPCSelector(
      final boolean required,
      final @NonNull String name,
      final @NonNull String defaultValue,
      final @Nullable
      BiFunction<@NonNull CommandContext<C>, @NonNull String, @NonNull List<@NonNull String>>
          suggestionsProvider,
      final @NonNull ArgumentDescription defaultDescription,
      final NotQuests main,
      final boolean allowNone,
      final boolean allowRightClickSelect) {
    super(
        required, name, new NQNPCsParser<>(main, allowNone, allowRightClickSelect), defaultValue, NQNPCResult.class, suggestionsProvider);
  }

  public static <C> NQNPCSelector.@NonNull Builder<C> newBuilder(
      final @NonNull String name, final NotQuests main, final boolean allowNone, final boolean allowRightClickSelect) {
    return new NQNPCSelector.Builder<>(name, main, allowNone, allowRightClickSelect);
  }

  public static <C> @NonNull CommandArgument<C, NQNPCResult> of(
      final @NonNull String name, final NotQuests main, final boolean allowNone, final boolean allowRightClickSelect) {
    return NQNPCSelector.<C>newBuilder(name, main, allowNone, allowRightClickSelect).asRequired().build();
  }

  public static <C> @NonNull CommandArgument<C, NQNPCResult> optional(
      final @NonNull String name, final NotQuests main, final boolean allowNone, final boolean allowRightClickSelect) {
    return NQNPCSelector.<C>newBuilder(name, main, allowNone, allowRightClickSelect).asOptional().build();
  }

  public static <C> @NonNull CommandArgument<C, NQNPCResult> optional(
      final @NonNull String name, final @NonNull NQNPCResult NQNPC, final NotQuests main, final boolean allowNone, final boolean allowRightClickSelect) {
    return NQNPCSelector.<C>newBuilder(name, main, allowNone, allowRightClickSelect)
        .asOptionalWithDefault(""+NQNPC.getNQNPC().getID().toString())
        .build();
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, NQNPCResult> {
    private final NotQuests main;
    private final boolean allowNone;

    private final boolean allowRightClickSelect;

    private Builder(final @NonNull String name, NotQuests main, final boolean allowNone, final boolean allowRightClickSelect) {
      super(NQNPCResult.class, name);
      this.main = main;
      this.allowNone = allowNone;
      this.allowRightClickSelect = allowRightClickSelect;
    }

    @Override
    public @NonNull CommandArgument<C, NQNPCResult> build() {
      return new NQNPCSelector<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          this.main,
          this.allowNone,
          this.allowRightClickSelect);
    }
  }

  public static final class NQNPCsParser<C> implements ArgumentParser<C, NQNPCResult> {

    private final NotQuests main;
    private final boolean allowNone;

    private final boolean allowRightClickSelect;

    /** Constructs a new PluginsParser. */
    public NQNPCsParser(final NotQuests main, final boolean allowNone, final boolean allowRightClickSelect) {
      this.main = main;
      this.allowNone = allowNone;
      this.allowRightClickSelect = allowRightClickSelect;
    }

    @NotNull
    @Override
    public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
      final NPCManager npcManager = main.getNPCManager();

      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              "[NQNPC Name]",
              "[...]");

      final List<String> allNPCs = new ArrayList<>(npcManager.getAllNPCsString());

      if(allowNone){
        allNPCs.add("none");
      }
      if(allowRightClickSelect){
        allNPCs.add("rightClickSelect");
      }

      return allNPCs;

    }

    @Override
    public @NonNull ArgumentParseResult<NQNPCResult> parse(
        @NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
      if (inputQueue.isEmpty()) {
        return ArgumentParseResult.failure(
            new NoInputProvidedException(NQNPCsParser.class, context));
      }
      final String input = inputQueue.peek();

      if(allowNone && input.equalsIgnoreCase("none")){
        inputQueue.remove();
        return ArgumentParseResult.success(new NQNPCResult(null, true, false));
      }

      if(allowRightClickSelect && input.equalsIgnoreCase("rightClickSelect")){
        inputQueue.remove();
        return ArgumentParseResult.success(new NQNPCResult(null, false, true));
      }

      if(!input.contains(":") || input.split(":").length != 2){
        return ArgumentParseResult.failure(
            new IllegalArgumentException("Wrong input. Format needs to be [NPC Plugin Name]:[NPC ID]. Please follow the command suggestions."));
      }

      final String pluginName = input.split(":")[0];
      final String npcIDString = input.split(":")[1];
      final int npcID;
      try{
        npcID = Integer.parseInt(npcIDString);
      }catch (NumberFormatException e){
        return ArgumentParseResult.failure(
            new IllegalArgumentException("NPC ID '" + npcIDString + "' is not a number."));
      }

      final NQNPC nqnpc = main.getNPCManager().getOrCreateNQNpc(pluginName, NQNPCID.fromInteger(npcID));
      if(nqnpc == null){
        return ArgumentParseResult.failure(
            new IllegalArgumentException("An NQNPC with the ID '" + npcID + "' from plugin '" + pluginName + "' does not exist."));
      }
      inputQueue.remove();

      return ArgumentParseResult.success(new NQNPCResult(nqnpc, false, false));
    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}
