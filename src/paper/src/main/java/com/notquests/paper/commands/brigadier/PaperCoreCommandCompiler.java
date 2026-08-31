package com.notquests.paper.commands.brigadier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.notquests.core.commands.NotQuestsCommands;
import com.notquests.core.commands.framework.CommandBranchHelp;
import com.notquests.core.commands.framework.CommandMessage;
import com.notquests.core.commands.framework.NQArgumentType;
import com.notquests.core.commands.framework.NQCommandContext;
import com.notquests.core.commands.framework.NQCommandHandler;
import com.notquests.core.commands.framework.NQCommandKind;
import com.notquests.core.commands.framework.NQCommandRegistration;
import com.notquests.core.commands.framework.NQCommandTree.Node;
import com.notquests.core.commands.framework.NQCommandTree;
import com.notquests.core.commands.framework.NQFlag;
import com.notquests.core.commands.framework.NQSuggestionProvider;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperNotQuestsAdapter;
import com.notquests.paper.PaperPlayer;
import com.notquests.paper.commands.arguments.ItemStackSelectionArgument;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class PaperCoreCommandCompiler {
    private static final String FLAG_ARG = "__flags";
    private static final TextColor LITERAL_COLOR = TextColor.color(0xE8F1FF);
    private static final TextColor ARGUMENT_COLOR = TextColor.color(0x00FFFB);
    private static final TextColor FLAG_COLOR = TextColor.color(0xF9D66B);
    private static final TextColor DESCRIPTION_COLOR = TextColor.color(0xB7C2D5);

    private final NotQuests main;
    private final NotQuestsCommands commands;

    public PaperCoreCommandCompiler(final NotQuests main, final NotQuestsCommands commands) {
        this.main = main;
        this.commands = commands;
    }

    public void register(
            final Commands paperCommands,
            final List<NQCommandRegistration<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>>
                    registrations) {
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();
        registrations.forEach(tree::register);
        for (final var root : tree.roots()) {
            final LiteralCommandNode<CommandSourceStack> node = literal(root, List.of(root)).build();
            paperCommands.register(node, root.description().textDescription(), root.aliases());
        }
    }

    private LiteralArgumentBuilder<CommandSourceStack> literal(
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node,
            final List<Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path) {
        return literal(node, path, node.name());
    }

    private LiteralArgumentBuilder<CommandSourceStack> literal(
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node,
            final List<Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path,
            final String literalName) {
        final LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(literalName);
        populate(builder, node, path);
        return builder;
    }

    private RequiredArgumentBuilder<CommandSourceStack, ?> argument(
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node,
            final List<Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path) {
        final RequiredArgumentBuilder<CommandSourceStack, ?> builder =
                Commands.argument(node.name(), nativeArgument(node.argument()));
        builder.suggests(argumentSuggestions(node));
        populate(builder, node, path);
        return builder;
    }

    private void populate(
            final ArgumentBuilder<CommandSourceStack, ?> builder,
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node,
            final List<Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path) {
        if (node.permission() != null) {
            builder.requires(source -> source.getSender().hasPermission(node.permission()));
        }
        boolean optionalHandlerAttached = false;
        for (final var child : node.childNodes()) {
            final var childPath = child.appendTo(path);
            if (child.kind() == NQCommandKind.LITERAL) {
                if (child.aliases().contains("")) {
                    attachEmptyLiteralAlias(builder, child, childPath);
                }
                builder.then(literal(child, childPath).build());
                for (final String alias : child.aliases()) {
                    if (alias != null && !alias.isBlank()) {
                        builder.then(hiddenAlias(literal(child, childPath, alias).build()));
                    }
                }
            } else if (child.kind() == NQCommandKind.OPTIONAL) {
                optionalHandlerAttached |= attachOptionalOmitted(builder, child, childPath);
                builder.then(argument(child, childPath).build());
            } else {
                builder.then(argument(child, childPath).build());
            }
        }
        if (node.handler() != null) {
            builder.executes(context -> execute(node, context, ""));
            if (!node.commandFlags().isEmpty()) {
                final RequiredArgumentBuilder<CommandSourceStack, String> flags =
                        Commands.argument(FLAG_ARG, StringArgumentType.greedyString());
                flags.suggests(flagSuggestions(node));
                flags.executes(context -> execute(
                        node,
                        context,
                        StringArgumentType.getString(context, FLAG_ARG)));
                builder.then(flags.build());
            }
        } else if (!optionalHandlerAttached && !node.childNodes().isEmpty()) {
            builder.executes(context -> showHelp(node, path, context));
        }
    }

    private static <S> LiteralCommandNode<S> hiddenAlias(final LiteralCommandNode<S> alias) {
        final LiteralCommandNode<S> hidden = new LiteralCommandNode<>(
                alias.getLiteral(),
                alias.getCommand(),
                alias.getRequirement(),
                alias.getRedirect(),
                alias.getRedirectModifier(),
                alias.isFork()) {
            @Override
            public CompletableFuture<Suggestions> listSuggestions(
                    final CommandContext<S> context,
                    final SuggestionsBuilder builder) {
                return Suggestions.empty();
            }
        };
        alias.getChildren().forEach(hidden::addChild);
        return hidden;
    }

    private void attachEmptyLiteralAlias(
            final ArgumentBuilder<CommandSourceStack, ?> builder,
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node,
            final List<Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path) {
        if (node.handler() != null) {
            builder.executes(context -> execute(node, context, ""));
            if (!node.commandFlags().isEmpty()) {
                final RequiredArgumentBuilder<CommandSourceStack, String> flags =
                        Commands.argument(FLAG_ARG, StringArgumentType.greedyString());
                flags.suggests(flagSuggestions(node));
                flags.executes(context -> execute(
                        node,
                        context,
                        StringArgumentType.getString(context, FLAG_ARG)));
                builder.then(flags.build());
            }
        } else if (!node.childNodes().isEmpty()) {
            builder.executes(context -> showHelp(node, path, context));
        }
    }

    private boolean attachOptionalOmitted(
            final ArgumentBuilder<CommandSourceStack, ?> builder,
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node,
            final List<Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path) {
        if (node.handler() == null) {
            return false;
        }
        builder.executes(context -> execute(node, context, ""));
        if (!node.commandFlags().isEmpty()) {
            final RequiredArgumentBuilder<CommandSourceStack, String> flags =
                    Commands.argument(FLAG_ARG, StringArgumentType.greedyString());
            flags.suggests(flagSuggestions(node));
            flags.executes(context -> execute(
                    node,
                    context,
                    StringArgumentType.getString(context, FLAG_ARG)));
            builder.then(flags.build());
        }
        return true;
    }

    private ArgumentType<?> nativeArgument(final NQArgumentType argument) {
        if (argument == null) {
            return StringArgumentType.word();
        }
        return switch (argument.kind()) {
            case GREEDY_STRING, LOCATION, NUMBER_EXPRESSION, BOOLEAN_EXPRESSION ->
                    StringArgumentType.greedyString();
            case ITEM_SELECTION, ACTION_LIST -> ItemStackSelectionArgument.itemStackSelectionArgument();
            case INTEGER -> IntegerArgumentType.integer();
            case DOUBLE -> DoubleArgumentType.doubleArg();
            case BOOLEAN -> StringArgumentType.word();
            default -> StringArgumentType.word();
        };
    }

    private SuggestionProvider<CommandSourceStack> argumentSuggestions(
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node) {
        return (context, builder) -> {
            final var suggestions = commands.suggestions(
                    node,
                    new Context(context, "", List.of()),
                    builder.getRemaining());
            for (final String value : suggestions.values()) {
                builder.suggest(value);
            }
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSourceStack> flagSuggestions(
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node) {
        return (context, builder) -> {
            final String input = builder.getRemaining();
            final var suggestions = commands.flagSuggestions(
                    node,
                    new Context(context, input, node.commandFlags()),
                    input);
            final var tokenBuilder = builder.createOffset(builder.getStart() + suggestions.tokenStart());
            for (final String value : suggestions.values()) {
                tokenBuilder.suggest(value);
            }
            return tokenBuilder.buildFuture();
        };
    }

    private int execute(
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node,
            final CommandContext<CommandSourceStack> context,
            final String flags) {
        final CommandSender sender = context.getSource().getSender();
        return commands.execute(
                        node,
                        new Context(context, flags, node.commandFlags()),
                        node.permission() == null || sender.hasPermission(node.permission()),
                        senderMatches(context, node.senderType()),
                        message -> send(sender, message))
                ? Command.SINGLE_SUCCESS
                : 0;
    }

    private boolean senderMatches(
            final CommandContext<CommandSourceStack> context,
            final Class<?> senderType) {
        if (senderType == null) {
            return true;
        }
        if (PlatformPlayer.class.isAssignableFrom(senderType)) {
            return questPlayer(context) != null;
        }
        return senderType.isInstance(context.getSource().getSender());
    }

    private int showHelp(
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node,
            final List<Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path,
            final CommandContext<CommandSourceStack> context) {
        final CommandSender sender = context.getSource().getSender();
        CommandBranchHelp.send(
                node,
                path,
                message -> send(sender, message),
                line -> main.sendMessage(sender, usageLineComponent(line)));
        return Command.SINGLE_SUCCESS;
    }

    private void send(final CommandSender sender, final CommandMessage message) {
        if (message.renderEmptyLine()) {
            sender.sendMessage(Component.empty());
        } else if (message.message() != null && !message.message().isBlank()) {
            sender.sendMessage(main.parse(message.formattedMessage()));
        }
    }

    private Component usageLineComponent(final CommandBranchHelp.RichUsageLine usageLine) {
        Component component = Component.empty();
        for (final CommandBranchHelp.RichPart part : usageLine.parts()) {
            if (part.leadingSpace()) {
                component = component.append(Component.space());
            }
            component = component.append(partComponent(part));
        }
        return component;
    }

    private Component partComponent(final CommandBranchHelp.RichPart part) {
        Component component = Component.text(part.text(), color(part.style()));
        if (part.clickSyntax() != null && !part.clickSyntax().isBlank()) {
            component = component.clickEvent(ClickEvent.suggestCommand(part.clickSyntax()));
        }
        if (part.hover() != null && !part.hover().lines().isEmpty()) {
            component = component.hoverEvent(HoverEvent.showText(hoverComponent(part.hover())));
        }
        return component;
    }

    private Component hoverComponent(final CommandBranchHelp.RichHover hover) {
        Component component = Component.empty();
        boolean first = true;
        for (final CommandBranchHelp.RichHoverLine line : hover.lines()) {
            if (!first) {
                component = component.append(Component.newline());
            }
            component = component.append(Component.text(line.text(), color(line.style())));
            first = false;
        }
        return component;
    }

    private TextColor color(final CommandBranchHelp.RichStyle style) {
        return switch (style) {
            case LITERAL -> LITERAL_COLOR;
            case ARGUMENT -> ARGUMENT_COLOR;
            case FLAG -> FLAG_COLOR;
            case DESCRIPTION -> DESCRIPTION_COLOR;
            case MUTED -> NamedTextColor.DARK_GRAY;
        };
    }

    private PaperPlayer questPlayer(final CommandContext<CommandSourceStack> context) {
        if (context == null
                || context.getSource() == null
                || !(context.getSource().getSender() instanceof final Player player)) {
            return null;
        }
        return PaperNotQuestsAdapter.asPaperPlayer(
                main.getCorePlugin().getOrCreatePlatformPlayer(player.getUniqueId().toString()));
    }

    private final class Context implements NQCommandContext {
        private final CommandContext<CommandSourceStack> context;
        private final NQCommandContext.Flags flags;

        private Context(
                final CommandContext<CommandSourceStack> context,
                final String rawFlags,
                final List<NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>> availableFlags) {
            this.context = context;
            this.flags = NQCommandContext.flags(rawFlags, availableFlags);
        }

        @Override
        public String argument(final String name) {
            final Object value = rawArgument(name);
            return value == null ? "" : String.valueOf(value);
        }

        @Override
        public Object rawArgument(final String name) {
            try {
                return context.getArgument(name, Object.class);
            } catch (final IllegalArgumentException exception) {
                return "";
            }
        }

        @Override
        public boolean flagPresent(final String name) {
            return flags.contains(name);
        }

        @Override
        public String flag(final String name) {
            return flags.value(name);
        }

        @Override
        public Object rawFlag(final String name) {
            return flags.rawValue(name);
        }

        @Override
        public PlatformPlayer questPlayer() {
            return PaperCoreCommandCompiler.this.questPlayer(context);
        }

        @Override
        public Object platformSender() {
            return context.getSource().getSender();
        }

        @Override
        public String rawInput() {
            return context.getInput();
        }

        @Override
        public String platformVersion() {
            return main.getMain().getDescription().getVersion();
        }
    }
}
