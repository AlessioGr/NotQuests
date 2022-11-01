package rocks.gravili.notquests.paper.commands.arguments;


import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.captions.CaptionVariable;
import cloud.commandframework.captions.StandardCaptionKeys;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.exceptions.parsing.ParserException;
import cloud.commandframework.util.StringUtils;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apiguardian.api.API;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import rocks.gravili.notquests.paper.NotQuests;

@SuppressWarnings("unused")
@API(status = API.Status.STABLE)
public final class MiniMessageStringSelector<C> extends CommandArgument<C, String> {

  private static final Pattern QUOTED_DOUBLE = Pattern.compile("\"(?<inner>(?:[^\"\\\\]|\\\\.)*)\"");
  private static final Pattern QUOTED_SINGLE = Pattern.compile("'(?<inner>(?:[^'\\\\]|\\\\.)*)'");

  private final StringMode stringMode;
  private boolean withPlaceholders = false;

  private MiniMessageStringSelector(
      final boolean required,
      final @NonNull String name,
      final @NonNull StringMode stringMode,
      final @NonNull String defaultValue,
      final @NonNull BiFunction<@NonNull CommandContext<C>, @NonNull String,
          @NonNull List<@NonNull String>> suggestionsProvider,
      final @NonNull ArgumentDescription defaultDescription,
      NotQuests main,
      boolean withPlaceholders
  ) {
    super(required, name, new MiniMessageStringParser<>(stringMode, suggestionsProvider, main, withPlaceholders),
        defaultValue, String.class, null, defaultDescription
    );
    this.stringMode = stringMode;
    this.withPlaceholders = withPlaceholders;
  }

  /**
   * Create a new builder
   *
   * @param name Name of the argument
   * @param <C>  Command sender type
   * @return Created builder
   */
  public static <C> MiniMessageStringSelector.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main) {
    return new MiniMessageStringSelector.Builder<>(name, main);
  }

  /**
   * Create a new required single string command argument
   *
   * @param name Argument name
   * @param <C>  Command sender type
   * @return Created argument
   */
  public static <C> @NonNull CommandArgument<C, String> of(final @NonNull String name, final NotQuests main) {
    return MiniMessageStringSelector.<C>newBuilder(name, main).single().asRequired().build();
  }

  /**
   * Create a new required command argument
   *
   * @param name       Argument name
   * @param stringMode String mode
   * @param <C>        Command sender type
   * @return Created argument
   */
  public static <C> @NonNull CommandArgument<C, String> of(
      final @NonNull String name,
      final @NonNull StringMode stringMode, final NotQuests main
  ) {
    return MiniMessageStringSelector.<C>newBuilder(name, main).withMode(stringMode).asRequired().build();
  }

  /**
   * Create a new optional single string command argument
   *
   * @param name Argument name
   * @param <C>  Command sender type
   * @return Created argument
   */
  public static <C> @NonNull CommandArgument<C, String> optional(final @NonNull String name, final NotQuests main) {
    return MiniMessageStringSelector.<C>newBuilder(name, main).single().asOptional().build();
  }

  /**
   * Create a new optional command argument
   *
   * @param name       Argument name
   * @param stringMode String mode
   * @param <C>        Command sender type
   * @return Created argument
   */
  public static <C> @NonNull CommandArgument<C, String> optional(
      final @NonNull String name,
      final @NonNull StringMode stringMode, final NotQuests main
  ) {
    return MiniMessageStringSelector.<C>newBuilder(name, main).withMode(stringMode).asOptional().build();
  }

  /**
   * Create a new required command argument with a default value
   *
   * @param name          Argument name
   * @param defaultString Default string
   * @param <C>           Command sender type
   * @return Created argument
   */
  public static <C> @NonNull CommandArgument<C, String> optional(
      final @NonNull String name,
      final @NonNull String defaultString, final NotQuests main
  ) {
    return MiniMessageStringSelector.<C>newBuilder(name, main).asOptionalWithDefault(defaultString).build();
  }

  /**
   * Create a new required command argument with the 'single' parsing mode
   *
   * @param name Argument name
   * @param <C>  Command sender type
   * @return Created argument
   */
  public static <C> @NonNull CommandArgument<C, String> single(final @NonNull String name, final NotQuests main) {
    return of(name, StringMode.SINGLE, main);
  }

  /**
   * Create a new required command argument with the 'greedy' parsing mode
   *
   * @param name Argument name
   * @param <C>  Command sender type
   * @return Created argument
   */
  public static <C> @NonNull CommandArgument<C, String> greedy(final @NonNull String name, final NotQuests main) {
    return of(name, StringMode.GREEDY, main);
  }

  /**
   * Create a new required command argument with the 'greedy flag yielding' parsing mode
   *
   * @param name Argument name
   * @param <C>  Command sender type
   * @return Created argument
   * @since 1.7.0
   */
  @API(status = API.Status.STABLE, since = "1.7.0")
  public static <C> @NonNull CommandArgument<C, String> greedyFlagYielding(final @NonNull String name, final NotQuests main) {
    return of(name, StringMode.GREEDY_FLAG_YIELDING, main);
  }

  /**
   * Create a new required command argument with the 'quoted' parsing mode
   *
   * @param name Argument name
   * @param <C>  Command sender type
   * @return Created argument
   */
  public static <C> @NonNull CommandArgument<C, String> quoted(final @NonNull String name, final NotQuests main) {
    return of(name, StringMode.QUOTED, main);
  }

  /**
   * Get the string mode
   *
   * @return String mode
   */
  public @NonNull StringMode getStringMode() {
    return this.stringMode;
  }


  @API(status = API.Status.STABLE)
  public enum StringMode {
    SINGLE,
    QUOTED,
    GREEDY,
    /**
     * Greedy string that will consume the input until a flag is present.
     *
     * @since 1.7.0
     */
    @API(status = API.Status.STABLE, since = "1.7.0")
    GREEDY_FLAG_YIELDING
  }


  @API(status = API.Status.STABLE)
  public static final class Builder<C> extends CommandArgument.Builder<C, String> {
    private final NotQuests main;
    private boolean withPlaceholders = false;
    private StringMode stringMode = StringMode.SINGLE;
    private BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider = (v1, v2) -> Collections.emptyList();

    private Builder(final @NonNull String name, NotQuests main) {
      super(String.class, name);
      this.main = main;
    }

    public MiniMessageStringSelector.Builder<C> withPlaceholders() {
      this.withPlaceholders = true;
      return this;
    }

    /**
     * Set the String mode
     *
     * @param stringMode String mode to parse with
     * @return Builder instance
     */
    private MiniMessageStringSelector.Builder<C> withMode(final @NonNull StringMode stringMode) {
      this.stringMode = stringMode;
      return this;
    }

    /**
     * Set the string mode to greedy
     *
     * @return Builder instance
     */
    public MiniMessageStringSelector.Builder<C> greedy() {
      this.stringMode = StringMode.GREEDY;
      return this;
    }

    /**
     * Greedy string that will consume the input until a flag is present.
     *
     * @return Builder instance
     * @since 1.7.0
     */
    @API(status = API.Status.STABLE, since = "1.7.0")
    public MiniMessageStringSelector.Builder<C> greedyFlagYielding() {
      this.stringMode = StringMode.GREEDY_FLAG_YIELDING;
      return this;
    }

    /**
     * Set the string mode to single
     *
     * @return Builder instance
     */
    public MiniMessageStringSelector.Builder<C> single() {
      this.stringMode = StringMode.SINGLE;
      return this;
    }

    /**
     * Set the string mode to greedy
     *
     * @return Builder instance
     */
    public MiniMessageStringSelector.Builder<C> quoted() {
      this.stringMode = StringMode.QUOTED;
      return this;
    }

    /**
     * Set the suggestions provider
     *
     * @param suggestionsProvider Suggestions provider
     * @return Builder instance
     */
    @Override
    public MiniMessageStringSelector.Builder<C> withSuggestionsProvider(
        final @NonNull BiFunction<@NonNull CommandContext<C>,
            @NonNull String, @NonNull List<@NonNull String>> suggestionsProvider
    ) {
      this.suggestionsProvider = suggestionsProvider;
      return this;
    }

    /**
     * Builder a new string argument
     *
     * @return Constructed argument
     */
    @Override
    public @NonNull MiniMessageStringSelector<C> build() {
      return new MiniMessageStringSelector<>(this.isRequired(), this.getName(), this.stringMode,
          this.getDefaultValue(), this.suggestionsProvider, this.getDefaultDescription(),
      this.main,
          this.withPlaceholders
      );
    }
  }


  @SuppressWarnings("UnnecessaryLambda")
  @API(status = API.Status.STABLE)
  public static final class MiniMessageStringParser<C> implements ArgumentParser<C, String> {
    private final NotQuests main;
    private final boolean withPlaceholders;
    private static final Pattern FLAG_PATTERN = Pattern.compile("(-[A-Za-z_\\-0-9])|(--[A-Za-z_\\-0-9]*)");

    private final StringMode stringMode;
    private final BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider;

    /**
     * Construct a new string parser
     *
     * @param stringMode          String parsing mode
     * @param suggestionsProvider Suggestions provider
     */
    public MiniMessageStringParser(
        final @NonNull StringMode stringMode,
        final @NonNull BiFunction<@NonNull CommandContext<C>, @NonNull String,
            @NonNull List<@NonNull String>> suggestionsProvider, NotQuests main, boolean withPlaceholders
    ) {
      this.stringMode = stringMode;
      this.suggestionsProvider = suggestionsProvider;
      this.main = main;
      this.withPlaceholders = withPlaceholders;
    }

    @Override
    public @NonNull ArgumentParseResult<String> parse(
        final @NonNull CommandContext<C> commandContext,
        final @NonNull Queue<@NonNull String> inputQueue
    ) {
      final String input = inputQueue.peek();
      if (input == null) {
        return ArgumentParseResult.failure(new NoInputProvidedException(
            MiniMessageStringParser.class,
            commandContext
        ));
      }

      if (this.stringMode == StringMode.SINGLE) {
        inputQueue.remove();
        return ArgumentParseResult.success(input);
      } else if (this.stringMode == StringMode.QUOTED) {
        return this.parseQuoted(commandContext, inputQueue);
      } else {
        return this.parseGreedy(commandContext, inputQueue);
      }
    }

    private @NonNull ArgumentParseResult<String> parseQuoted(
        final @NonNull CommandContext<C> commandContext,
        final @NonNull Queue<@NonNull String> inputQueue
    ) {
      final String peek = inputQueue.peek();
      if (peek != null && !peek.startsWith("'") && !peek.startsWith("\"")) {
        inputQueue.remove();
        return ArgumentParseResult.success(peek);
      }

      final StringJoiner sj = new StringJoiner(" ");
      for (final String string : inputQueue) {
        sj.add(string);
      }
      final String string = sj.toString();

      final Matcher doubleMatcher = QUOTED_DOUBLE.matcher(string);
      String doubleMatch = null;
      if (doubleMatcher.find()) {
        doubleMatch = doubleMatcher.group("inner");
      }
      final Matcher singleMatcher = QUOTED_SINGLE.matcher(string);
      String singleMatch = null;
      if (singleMatcher.find()) {
        singleMatch = singleMatcher.group("inner");
      }

      String inner = null;
      if (singleMatch != null && doubleMatch != null) {
        final int singleIndex = string.indexOf(singleMatch);
        final int doubleIndex = string.indexOf(doubleMatch);
        inner = doubleIndex < singleIndex ? doubleMatch : singleMatch;
      } else if (singleMatch == null && doubleMatch != null) {
        inner = doubleMatch;
      } else if (singleMatch != null) {
        inner = singleMatch;
      }

      if (inner != null) {
        final int numSpaces = StringUtils.countCharOccurrences(inner, ' ');
        for (int i = 0; i <= numSpaces; i++) {
          inputQueue.remove();
        }
      } else {
        inner = inputQueue.peek();
        if (inner.startsWith("\"") || inner.startsWith("'")) {
          return ArgumentParseResult.failure(new MiniMessageStringSelector.StringParseException(sj.toString(),
              StringMode.QUOTED, commandContext
          ));
        } else {
          inputQueue.remove();
        }
      }

      inner = inner.replace("\\\"", "\"").replace("\\'", "'");

      return ArgumentParseResult.success(inner);
    }

    private @NonNull ArgumentParseResult<String> parseGreedy(
        final @NonNull CommandContext<C> commandContext,
        final @NonNull Queue<@NonNull String> inputQueue
    ) {
      final StringJoiner sj = new StringJoiner(" ");
      final int size = inputQueue.size();

      for (int i = 0; i < size; i++) {
        final String string = inputQueue.peek();

        if (string == null) {
          break;
        }

        if (this.stringMode == StringMode.GREEDY_FLAG_YIELDING) {
          // The pattern requires a leading space.
          if (FLAG_PATTERN.matcher(string).matches()) {
            break;
          }
        }

        sj.add(string);
        inputQueue.remove();
      }

      return ArgumentParseResult.success(sj.toString());
    }

    @Override
    public @NonNull List<@NonNull String> suggestions(
        final @NonNull CommandContext<C> context,
        @NonNull String input
    ) {

      final List<String> completions = new java.util.ArrayList<>(this.suggestionsProvider.apply(context, input));

      boolean suggestWithStringAtStart = false;
      if(input.startsWith("\"")){
        suggestWithStringAtStart = true;
        input = input.substring(1);
      }

      final String prefix = suggestWithStringAtStart ? "\"" : "";

      final String rawInput = context.getRawInputJoined();
      if (input.startsWith("{") && withPlaceholders) {
        for(final String placeholder : main.getCommandManager().getAdminCommands().placeholders){
          completions.add(prefix + placeholder);
        }
      } else {
        if (input.startsWith("<")) {
          for (String color : main.getUtilManager().getMiniMessageTokens()) {
            completions.add(prefix+"<" + color + ">");
            // Now the closings. First we search IF it contains an opening and IF it doesnt contain
            // more closings than the opening
            if (rawInput.contains(prefix+"<" + color + ">")) {
              if (org.apache.commons.lang.StringUtils.countMatches(rawInput, "<" + color + ">")
                  > org.apache.commons.lang.StringUtils.countMatches(rawInput, "</" + color + ">")) {
                completions.add(prefix+"</" + color + ">");
              }
            }
          }
        } else {
          completions.add(prefix+"<Enter Message (put in \" \" quotes if you need spaces)>");
        }
      }

      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              prefix+"<Enter Message>",
              "");

      return completions;
    }

    @Override
    public boolean isContextFree() {
      return true;
    }

    /**
     * Get the string mode
     *
     * @return String mode
     */
    public @NonNull StringMode getStringMode() {
      return this.stringMode;
    }
  }


  @API(status = API.Status.STABLE)
  public static final class StringParseException extends ParserException {

    private static final long serialVersionUID = -8903115465005472945L;
    private final String input;
    private final StringMode stringMode;

    /**
     * Construct a new string parse exception
     *
     * @param input      Input
     * @param stringMode String mode
     * @param context    Command context
     */
    public StringParseException(
        final @NonNull String input,
        final @NonNull StringMode stringMode,
        final @NonNull CommandContext<?> context
    ) {
      super(
          MiniMessageStringParser.class,
          context,
          StandardCaptionKeys.ARGUMENT_PARSE_FAILURE_STRING,
          CaptionVariable.of("input", input),
          CaptionVariable.of("stringMode", stringMode.name())
      );
      this.input = input;
      this.stringMode = stringMode;
    }


    /**
     * Get the input provided by the sender
     *
     * @return Input
     */
    public @NonNull String getInput() {
      return this.input;
    }

    /**
     * Get the string mode
     *
     * @return String mode
     */
    public @NonNull StringMode getStringMode() {
      return this.stringMode;
    }
  }
}
