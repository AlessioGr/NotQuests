package com.notquests.core.commands.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Structured representation of the command tree. It is generated from the same internal tree that is
 * compiled into Brigadier, so in-game help, exported docs, and tests can use one source of truth.
 */
public final class NQCommandSchema {
    private NQCommandSchema() {}

    public interface Argument {
        String argumentTypeName();

        String valueTypeName();
    }

    public interface Flag {
        String name();

        NQDescription description();

        boolean isPresence();

        Argument valueArgumentInfo();
    }

    public interface Node {
        String kindName();

        String name();

        List<String> aliases();

        List<? extends Node> children();

        Argument argumentInfo();

        NQDescription description();

        NQDescription commandDescription();

        boolean executable();

        String permission();

        String senderTypeName();

        List<? extends Flag> flags();
    }

    public record Root(String name, NQDescription description, List<String> aliases) {
        public Root {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Command root name cannot be blank.");
            }
            description = Objects.requireNonNull(description, "description");
            aliases = List.copyOf(aliases == null ? List.of() : aliases);
        }

        public List<String> names() {
            final ArrayList<String> names = new ArrayList<>(aliases.size() + 1);
            names.add(name);
            names.addAll(aliases);
            return List.copyOf(names);
        }

        public boolean matches(final String commandName) {
            return commandName != null
                    && !commandName.isBlank()
                    && names().stream().anyMatch(name -> name.equalsIgnoreCase(commandName));
        }
    }

    public static final Root USER = new Root(
            "nq",
            NQDescription.of("Player commands for NotQuests"),
            List.of("notquests", "nquests", "nquest", "notquest", "quest", "quests", "q", "qg"));

    public static final Root ADMIN = new Root(
            "nqa",
            NQDescription.of("Admin commands for NotQuests"),
            List.of("nquestsadmin", "nquestadmin", "notquestadmin", "qadmin", "questadmin", "qa", "qag", "notquestsadmin"));

    public static List<CommandInfo> commandInfos(final Collection<? extends NQCommandSchema.Node> roots) {
        final List<CommandInfo> commands = new ArrayList<>();
        for (final NQCommandSchema.Node root : roots) {
            collectCommandSchema(root, List.of(root), commands);
        }
        commands.sort(Comparator.comparing(CommandInfo::syntax));
        return List.copyOf(commands);
    }

    public static String displayToken(final NQCommandSchema.Node node) {
        return switch (node.kindName()) {
            case "LITERAL" -> node.name();
            case "REQUIRED" -> "<" + node.name() + ">";
            case "OPTIONAL" -> "[<" + node.name() + ">]";
            default -> node.name();
        };
    }

    public static String pathSyntax(final List<? extends NQCommandSchema.Node> path) {
        final StringBuilder syntax = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                syntax.append(' ');
            }
            syntax.append(displayToken(path.get(i)));
        }
        return syntax.toString();
    }

    public static String flagsSyntax(final NQCommandSchema.Node node) {
        if (node.flags().isEmpty()) {
            return "";
        }
        final StringBuilder syntax = new StringBuilder();
        for (final NQCommandSchema.Flag flag : node.flags()) {
            syntax.append('[').append("--").append(flag.name());
            if (!flag.isPresence()) {
                syntax.append(" <value>");
            }
            syntax.append("] ");
        }
        return " " + syntax.toString().trim();
    }

    public static String descriptionText(final NQDescription description) {
        return description == null ? "" : description.textDescription();
    }

    private static void collectCommandSchema(
            final NQCommandSchema.Node node,
            final List<NQCommandSchema.Node> path,
            final List<CommandInfo> commands) {
        if (node.executable()) {
            commands.add(commandInfo(path, node));
        }
        for (final NQCommandSchema.Node child : node.children()) {
            collectCommandSchema(child, appendPath(path, child), commands);
        }
    }

    private static CommandInfo commandInfo(final List<NQCommandSchema.Node> path, final NQCommandSchema.Node executable) {
        final List<SegmentInfo> segments = new ArrayList<>();
        for (final NQCommandSchema.Node node : path) {
            final NQCommandSchema.Argument argument = node.argumentInfo();
            segments.add(new SegmentInfo(
                    node.kindName().toLowerCase(Locale.ROOT),
                    node.name(),
                    displayToken(node),
                    descriptionText(node.description()),
                    argument == null ? null : argument.argumentTypeName(),
                    argument == null ? null : argument.valueTypeName(),
                    !"OPTIONAL".equals(node.kindName())));
        }

        final List<FlagInfo> flags = new ArrayList<>();
        for (final NQCommandSchema.Flag flag : executable.flags()) {
            final NQCommandSchema.Argument argument = flag.valueArgumentInfo();
            flags.add(new FlagInfo(
                    flag.name(),
                    flag.isPresence() ? "--" + flag.name() : "--" + flag.name() + " <value>",
                    descriptionText(flag.description()),
                    argument == null ? null : argument.argumentTypeName(),
                    argument == null ? null : argument.valueTypeName(),
                    flag.isPresence()));
        }

        final NQCommandSchema.Node root = path.get(0);
        final NQDescription description =
                executable.commandDescription().isEmpty() ? executable.description() : executable.commandDescription();
        return new CommandInfo(
                root.name(),
                List.copyOf(root.aliases()),
                "/" + pathSyntax(path) + flagsSyntax(executable),
                descriptionText(description),
                executable.permission(),
                executable.senderTypeName(),
                List.copyOf(segments),
                List.copyOf(flags));
    }

    private static List<NQCommandSchema.Node> appendPath(final List<NQCommandSchema.Node> path, final NQCommandSchema.Node child) {
        final List<NQCommandSchema.Node> childPath = new ArrayList<>(path.size() + 1);
        childPath.addAll(path);
        childPath.add(child);
        return List.copyOf(childPath);
    }

    public record CommandIndex(String pluginVersion, List<CommandInfo> commands) {
        public String toJson() {
            final StringBuilder json = new StringBuilder(64_000);
            json.append("{\n");
            appendField(json, 1, "pluginVersion", pluginVersion).append(",\n");
            indent(json, 1).append("\"commands\": [\n");
            for (int i = 0; i < commands.size(); i++) {
                commands.get(i).appendJson(json, 2);
                if (i + 1 < commands.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            indent(json, 1).append("]\n");
            json.append("}\n");
            return json.toString();
        }
    }

    public record CommandInfo(
            String root,
            List<String> rootAliases,
            String syntax,
            String description,
            String permission,
            String senderType,
            List<SegmentInfo> segments,
            List<FlagInfo> flags) {
        private void appendJson(final StringBuilder json, final int level) {
            indent(json, level).append("{\n");
            appendField(json, level + 1, "root", root).append(",\n");
            appendStringArrayField(json, level + 1, "rootAliases", rootAliases).append(",\n");
            appendField(json, level + 1, "syntax", syntax).append(",\n");
            appendField(json, level + 1, "description", description).append(",\n");
            appendField(json, level + 1, "permission", permission).append(",\n");
            appendField(json, level + 1, "senderType", senderType).append(",\n");
            indent(json, level + 1).append("\"segments\": [\n");
            for (int i = 0; i < segments.size(); i++) {
                segments.get(i).appendJson(json, level + 2);
                if (i + 1 < segments.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            indent(json, level + 1).append("],\n");
            indent(json, level + 1).append("\"flags\": [\n");
            for (int i = 0; i < flags.size(); i++) {
                flags.get(i).appendJson(json, level + 2);
                if (i + 1 < flags.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            indent(json, level + 1).append("]\n");
            indent(json, level).append('}');
        }
    }

    public record SegmentInfo(
            String kind,
            String name,
            String token,
            String description,
            String argumentType,
            String valueType,
            boolean required) {
        private void appendJson(final StringBuilder json, final int level) {
            indent(json, level).append("{\n");
            appendField(json, level + 1, "kind", kind).append(",\n");
            appendField(json, level + 1, "name", name).append(",\n");
            appendField(json, level + 1, "token", token).append(",\n");
            appendField(json, level + 1, "description", description).append(",\n");
            appendField(json, level + 1, "argumentType", argumentType).append(",\n");
            appendField(json, level + 1, "valueType", valueType).append(",\n");
            appendField(json, level + 1, "required", required).append('\n');
            indent(json, level).append('}');
        }
    }

    public record FlagInfo(
            String name,
            String token,
            String description,
            String argumentType,
            String valueType,
            boolean presenceOnly) {
        private void appendJson(final StringBuilder json, final int level) {
            indent(json, level).append("{\n");
            appendField(json, level + 1, "name", name).append(",\n");
            appendField(json, level + 1, "token", token).append(",\n");
            appendField(json, level + 1, "description", description).append(",\n");
            appendField(json, level + 1, "argumentType", argumentType).append(",\n");
            appendField(json, level + 1, "valueType", valueType).append(",\n");
            appendField(json, level + 1, "presenceOnly", presenceOnly).append('\n');
            indent(json, level).append('}');
        }
    }

    private static StringBuilder appendField(
            final StringBuilder json, final int level, final String name, final String value) {
        indent(json, level).append('"').append(escape(name)).append("\": ");
        if (value == null) {
            return json.append("null");
        }
        return json.append('"').append(escape(value)).append('"');
    }

    private static StringBuilder appendField(
            final StringBuilder json, final int level, final String name, final int value) {
        return indent(json, level).append('"').append(escape(name)).append("\": ").append(value);
    }

    private static StringBuilder appendField(
            final StringBuilder json, final int level, final String name, final boolean value) {
        return indent(json, level).append('"').append(escape(name)).append("\": ").append(value);
    }

    private static StringBuilder appendStringArrayField(
            final StringBuilder json, final int level, final String name, final List<String> values) {
        indent(json, level).append('"').append(escape(name)).append("\": [");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }
            json.append('"').append(escape(values.get(i))).append('"');
        }
        return json.append(']');
    }

    private static StringBuilder indent(final StringBuilder json, final int level) {
        return json.append("  ".repeat(Math.max(0, level)));
    }

    private static String escape(final String value) {
        final StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            final char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
