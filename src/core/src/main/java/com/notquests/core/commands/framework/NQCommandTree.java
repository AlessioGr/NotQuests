package com.notquests.core.commands.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Platform-neutral command tree builder. Platform adapters register command registry entries here, then
 * compile the merged tree to their native command system.
 */
public final class NQCommandTree<A extends NQCommandSchema.Argument, F extends NQCommandSchema.Flag, S, H> {
    private final Map<String, Node<A, F, S, H>> roots = new LinkedHashMap<>();
    private Consumer<Void> onChanged = ignored -> {};

    public void onChanged(final Consumer<Void> onChanged) {
        this.onChanged = onChanged == null ? ignored -> {} : onChanged;
    }

    public void register(final NQCommandRegistration<A, F, S, H> type) {
        final List<NQCommandStep<A, S>> steps = type.steps();
        if (steps.isEmpty()) {
            return;
        }
        final NQCommandStep<A, S> rootStep = steps.get(0);
        final Node<A, F, S, H> root =
                roots.computeIfAbsent(rootStep.name(), n -> new Node<>(rootStep.kind(), rootStep.name()));
        addAliases(root, rootStep.aliases());
        if (root.description.isEmpty() && !rootStep.description().isEmpty()) {
            root.description = rootStep.description();
        }
        Node<A, F, S, H> current = root;
        for (int i = 1; i < steps.size(); i++) {
            final NQCommandStep<A, S> step = steps.get(i);
            final Node<A, F, S, H> child =
                    current.children.computeIfAbsent(step.name(), n -> new Node<>(step.kind(), step.name()));
            if (step.argument() != null) {
                child.argument = step.argument();
            }
            if (!step.description().isEmpty()) {
                child.description = step.description();
            }
            if (step.suggestionOverride() != null) {
                child.suggestionOverride = step.suggestionOverride();
            }
            current = child;
        }
        current.handler = type.handler();
        current.permission = type.permission();
        current.senderType = type.senderType();
        current.flags = type.flags();
        if (!type.commandDescription().isEmpty()) {
            current.commandDescription = type.commandDescription();
        }
        onChanged.accept(null);
    }

    public Node<A, F, S, H> root(final String rootName) {
        return roots.get(rootName);
    }

    public Collection<Node<A, F, S, H>> roots() {
        return List.copyOf(roots.values());
    }

    public List<NQCommandSchema.CommandInfo> commandInfos() {
        return NQCommandSchema.commandInfos(roots.values());
    }

    private static void addAliases(final Node<?, ?, ?, ?> node, final List<String> aliases) {
        for (final String alias : aliases) {
            if (!node.aliases.contains(alias)) {
                node.aliases.add(alias);
            }
        }
    }

    public static final class Node<A extends NQCommandSchema.Argument, F extends NQCommandSchema.Flag, S, H>
            implements NQCommandSchema.Node {
        private final NQCommandKind kind;
        private final String name;
        private final List<String> aliases = new ArrayList<>();
        private final Map<String, Node<A, F, S, H>> children = new LinkedHashMap<>();
        private A argument;
        private NQDescription description = NQDescription.EMPTY;
        private S suggestionOverride;
        private NQDescription commandDescription = NQDescription.EMPTY;
        private H handler;
        private String permission;
        private Class<?> senderType;
        private List<F> flags = List.of();

        private Node(final NQCommandKind kind, final String name) {
            this.kind = kind;
            this.name = name;
        }

        public NQCommandKind kind() {
            return kind;
        }

        @Override
        public String kindName() {
            return kind.name();
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<String> aliases() {
            return List.copyOf(aliases);
        }

        public Collection<Node<A, F, S, H>> childNodes() {
            return List.copyOf(children.values());
        }

        @Override
        public List<? extends NQCommandSchema.Node> children() {
            return List.copyOf(children.values());
        }

        public A argument() {
            return argument;
        }

        @Override
        public NQCommandSchema.Argument argumentInfo() {
            return argument;
        }

        @Override
        public NQDescription description() {
            return description;
        }

        public S suggestionOverride() {
            return suggestionOverride;
        }

        @Override
        public NQDescription commandDescription() {
            return commandDescription;
        }

        public H handler() {
            return handler;
        }

        @Override
        public boolean executable() {
            return handler != null;
        }

        @Override
        public String permission() {
            return permission;
        }

        public Class<?> senderType() {
            return senderType;
        }

        @Override
        public String senderTypeName() {
            return senderType == null ? "any" : senderType.getSimpleName();
        }

        public List<F> commandFlags() {
            return flags;
        }

        public List<Node<A, F, S, H>> appendTo(final List<Node<A, F, S, H>> path) {
            final ArrayList<Node<A, F, S, H>> nodes = new ArrayList<>(path.size() + 1);
            nodes.addAll(path);
            nodes.add(this);
            return List.copyOf(nodes);
        }

        @Override
        public List<? extends NQCommandSchema.Flag> flags() {
            return flags;
        }
    }
}
