package com.notquests.neoforge;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import com.notquests.core.NotQuestsPlugin;
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

import java.util.List;
import java.util.function.Supplier;

final class NeoForgeCoreCommandCompiler {
    private static final String FLAG_ARG = "__flags";

    private final NotQuestsPlugin plugin;
    private final NotQuestsCommands commands;
    private final NeoForgeText text;
    private final Supplier<String> version;

    NeoForgeCoreCommandCompiler(
            final NotQuestsPlugin plugin,
            final NotQuestsCommands commands,
            final NeoForgeText text,
            final Supplier<String> version) {
        this.plugin = plugin;
        this.commands = commands;
        this.text = text;
        this.version = version;
    }

    LiteralArgumentBuilder<CommandSourceStack> compile(
            final List<NQCommandRegistration<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>>
                    registrations,
            final String rootName) {
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();
        registrations.forEach(tree::register);
        final var root = tree.root(rootName);
        if (root == null || root.kind() != NQCommandKind.LITERAL) {
            throw new IllegalArgumentException("Core command root was not registered: " + rootName);
        }
        return literal(root, List.of(root));
    }

    private LiteralArgumentBuilder<CommandSourceStack> literal(
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node,
            final List<Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path) {
        final LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(node.name());
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
            builder.requires(source -> hasPermission(source, node.permission()));
        }
        boolean optionalHandlerAttached = false;
        for (final var child : node.childNodes()) {
            final var childPath = child.appendTo(path);
            if (child.kind() == NQCommandKind.LITERAL) {
                builder.then(literal(child, childPath));
            } else if (child.kind() == NQCommandKind.OPTIONAL) {
                optionalHandlerAttached |= attachOptionalOmitted(builder, child, childPath);
                builder.then(argument(child, childPath));
            } else {
                builder.then(argument(child, childPath));
            }
        }
        if (node.handler() != null) {
            builder.executes(context -> execute(node, context, ""));
            if (!node.commandFlags().isEmpty()) {
                final RequiredArgumentBuilder<CommandSourceStack, String> flags =
                        Commands.argument(FLAG_ARG, StringArgumentType.greedyString());
                flags.suggests(flagSuggestions(node));
                flags.executes(context -> execute(node, context, argument(context, FLAG_ARG)));
                builder.then(flags);
            }
        } else if (!optionalHandlerAttached && !node.childNodes().isEmpty()) {
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
            flags.executes(context -> execute(node, context, argument(context, FLAG_ARG)));
            builder.then(flags);
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
            case ITEM_SELECTION, ACTION_LIST -> NeoForgeArguments.commaToken();
            case INTEGER -> IntegerArgumentType.integer();
            case DOUBLE -> DoubleArgumentType.doubleArg();
            case BOOLEAN -> StringArgumentType.word();
            default -> StringArgumentType.word();
        };
    }

    private com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> argumentSuggestions(
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

    private com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> flagSuggestions(
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
        return commands.execute(
                        node,
                        new Context(context, flags, node.commandFlags()),
                        node.permission() == null || hasPermission(context.getSource(), node.permission()),
                        senderMatches(context, node.senderType()),
                        message -> send(context.getSource(), message))
                ? Command.SINGLE_SUCCESS
                : 0;
    }

    private int showHelp(
            final Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node,
            final List<Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path,
            final CommandContext<CommandSourceStack> context) {
        CommandBranchHelp.send(
                node,
                path,
                message -> send(context.getSource(), message),
                line -> context.getSource().sendSuccess(() -> usageLineComponent(line), false));
        return Command.SINGLE_SUCCESS;
    }

    private boolean hasPermission(final CommandSourceStack source, final String permission) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        try {
            return NeoForgePermissions.hasPermission(source.getPlayerOrException(), permission);
        } catch (final CommandSyntaxException exception) {
            return true;
        }
    }

    private boolean senderMatches(final CommandContext<CommandSourceStack> context, final Class<?> senderType) {
        if (senderType == null) {
            return true;
        }
        if (PlatformPlayer.class.isAssignableFrom(senderType)) {
            return questPlayer(context) != null;
        }
        return false;
    }

    private void send(final CommandSourceStack source, final CommandMessage message) {
        if (message.renderEmptyLine()) {
            source.sendSuccess(() -> text.component(""), false);
        } else if (message.message() != null && !message.message().isBlank()) {
            if (message.success()) {
                source.sendSuccess(() -> text.component(message.formattedMessage()), false);
            } else {
                source.sendFailure(text.component(message.formattedMessage()));
            }
        }
    }

    private Component usageLineComponent(final CommandBranchHelp.RichUsageLine usageLine) {
        MutableComponent component = Component.empty();
        for (final CommandBranchHelp.RichPart part : usageLine.parts()) {
            if (part.leadingSpace()) {
                component = component.append(Component.literal(" "));
            }
            component = component.append(partComponent(part));
        }
        return component;
    }

    private Component partComponent(final CommandBranchHelp.RichPart part) {
        MutableComponent component = Component.literal(part.text()).withStyle(color(part.style()));
        if (part.clickSyntax() != null && !part.clickSyntax().isBlank()) {
            component = component.withStyle(style ->
                    style.withClickEvent(new ClickEvent.SuggestCommand(part.clickSyntax())));
        }
        if (part.hover() != null && !part.hover().lines().isEmpty()) {
            component = component.withStyle(style ->
                    style.withHoverEvent(new HoverEvent.ShowText(hoverComponent(part.hover()))));
        }
        return component;
    }

    private Component hoverComponent(final CommandBranchHelp.RichHover hover) {
        MutableComponent component = Component.empty();
        boolean first = true;
        for (final CommandBranchHelp.RichHoverLine line : hover.lines()) {
            if (!first) {
                component = component.append(Component.literal("\n"));
            }
            component = component.append(Component.literal(line.text()).withStyle(color(line.style())));
            first = false;
        }
        return component;
    }

    private ChatFormatting color(final CommandBranchHelp.RichStyle style) {
        return switch (style) {
            case LITERAL -> ChatFormatting.YELLOW;
            case ARGUMENT -> ChatFormatting.AQUA;
            case FLAG -> ChatFormatting.GREEN;
            case DESCRIPTION -> ChatFormatting.GRAY;
            case MUTED -> ChatFormatting.DARK_GRAY;
        };
    }

    private String argument(final CommandContext<CommandSourceStack> context, final String name) {
        try {
            final Object value = context.getArgument(name, Object.class);
            return value == null ? "" : String.valueOf(value);
        } catch (final IllegalArgumentException exception) {
            return "";
        }
    }

    private PlatformPlayer questPlayer(final CommandContext<CommandSourceStack> context) {
        try {
            final ServerPlayer player = context.getSource().getPlayerOrException();
            return plugin.getOrCreatePlatformPlayer(player.getUUID().toString());
        } catch (final CommandSyntaxException exception) {
            return null;
        }
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
            return NeoForgeCoreCommandCompiler.this.argument(context, name);
        }

        @Override
        public Object rawArgument(final String name) {
            return argument(name);
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
            return NeoForgeCoreCommandCompiler.this.questPlayer(context);
        }

        @Override
        public Object platformSender() {
            return context.getSource();
        }

        @Override
        public String rawInput() {
            return context.getInput();
        }

        @Override
        public String platformVersion() {
            return version.get();
        }
    }
}
