package com.notquests.core.metadata;

import com.notquests.core.commands.framework.NQCommandSchema.CommandIndex;
import com.notquests.core.commands.framework.NQCommandSchema;

import java.util.List;

/**
 * Runtime-generated metadata exported by NotQuests.
 *
 * <p>This is intentionally built from the enabled server's runtime managers, not from a docs-only
 * hand-written list. That keeps the plugin, docs, E2E sweep, and future editor integration on one
 * source of truth, including API-added types and integration-gated types.
 */
public final class NQMetadataSchema {
    private NQMetadataSchema() {}

    public record MetadataIndex(
            String pluginVersion,
            String minecraftVersion,
            CommandIndex commands,
            RegistryIndex registry) {
        public String toJson() {
            final StringBuilder json = new StringBuilder(96_000);
            json.append("{\n");
            appendField(json, 1, "pluginVersion", pluginVersion).append(",\n");
            appendField(json, 1, "minecraftVersion", minecraftVersion).append(",\n");
            indent(json, 1).append("\"commands\": ");
            json.append(commands.toJson().trim()).append(",\n");
            indent(json, 1).append("\"registry\": ");
            registry.appendJson(json, 1);
            json.append('\n').append("}\n");
            return json.toString();
        }
    }

    public record RegistryIndex(
            List<Type> objectives,
            List<Type> actions,
            List<Type> conditions,
            List<Type> triggers,
            List<Variable> variables) {
        void appendJson(final StringBuilder json, final int level) {
            json.append("{\n");
            appendTypeArray(json, level + 1, "objectives", objectives).append(",\n");
            appendTypeArray(json, level + 1, "actions", actions).append(",\n");
            appendTypeArray(json, level + 1, "conditions", conditions).append(",\n");
            appendTypeArray(json, level + 1, "triggers", triggers).append(",\n");
            appendVariableArray(json, level + 1, "variables", variables).append('\n');
            indent(json, level).append('}');
        }
    }

    public record Type(
            String id,
            String displayName,
            String className,
            String description,
            String source,
            boolean integrationOnly,
            List<Field> fields,
            List<Field> flags) {
        public Type(
                final String id,
                final String className,
                final String description,
                final String source,
                final boolean integrationOnly) {
            this(id, id, className, description, source, integrationOnly, List.of(), List.of());
        }

        void appendJson(final StringBuilder json, final int level) {
            indent(json, level).append("{\n");
            appendField(json, level + 1, "id", id).append(",\n");
            appendField(json, level + 1, "displayName", displayName).append(",\n");
            appendField(json, level + 1, "className", className).append(",\n");
            appendField(json, level + 1, "description", description).append(",\n");
            appendField(json, level + 1, "source", source).append(",\n");
            appendField(json, level + 1, "integrationOnly", integrationOnly).append(",\n");
            appendFieldArray(json, level + 1, "fields", fields).append(",\n");
            appendFieldArray(json, level + 1, "flags", flags).append('\n');
            indent(json, level).append('}');
        }
    }

    public record Field(
            String name,
            String description,
            String argumentType,
            String valueType,
            boolean required,
            boolean flag) {
        void appendJson(final StringBuilder json, final int level) {
            indent(json, level).append("{\n");
            appendField(json, level + 1, "name", name).append(",\n");
            appendField(json, level + 1, "description", description).append(",\n");
            appendField(json, level + 1, "argumentType", argumentType).append(",\n");
            appendField(json, level + 1, "valueType", valueType).append(",\n");
            appendField(json, level + 1, "required", required).append(",\n");
            appendField(json, level + 1, "flag", flag).append('\n');
            indent(json, level).append('}');
        }
    }

    public record Variable(
            String id,
            String displayName,
            String className,
            String description,
            String source,
            boolean integrationOnly,
            String valueType,
            boolean settable,
            List<String> stringArguments,
            List<String> numberArguments,
            List<String> booleanArguments,
            List<String> booleanFlags) {
        void appendJson(final StringBuilder json, final int level) {
            indent(json, level).append("{\n");
            appendField(json, level + 1, "id", id).append(",\n");
            appendField(json, level + 1, "displayName", displayName).append(",\n");
            appendField(json, level + 1, "className", className).append(",\n");
            appendField(json, level + 1, "description", description).append(",\n");
            appendField(json, level + 1, "source", source).append(",\n");
            appendField(json, level + 1, "integrationOnly", integrationOnly).append(",\n");
            appendField(json, level + 1, "valueType", valueType).append(",\n");
            appendField(json, level + 1, "settable", settable).append(",\n");
            appendStringArrayField(json, level + 1, "stringArguments", stringArguments).append(",\n");
            appendStringArrayField(json, level + 1, "numberArguments", numberArguments).append(",\n");
            appendStringArrayField(json, level + 1, "booleanArguments", booleanArguments).append(",\n");
            appendStringArrayField(json, level + 1, "booleanFlags", booleanFlags).append('\n');
            indent(json, level).append('}');
        }
    }

    private static StringBuilder appendTypeArray(
            final StringBuilder json, final int level, final String name, final List<Type> values) {
        indent(json, level).append('"').append(escape(name)).append("\": [\n");
        for (int i = 0; i < values.size(); i++) {
            values.get(i).appendJson(json, level + 1);
            if (i + 1 < values.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        return indent(json, level).append(']');
    }

    private static StringBuilder appendVariableArray(
            final StringBuilder json, final int level, final String name, final List<Variable> values) {
        indent(json, level).append('"').append(escape(name)).append("\": [\n");
        for (int i = 0; i < values.size(); i++) {
            values.get(i).appendJson(json, level + 1);
            if (i + 1 < values.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        return indent(json, level).append(']');
    }

    private static StringBuilder appendFieldArray(
            final StringBuilder json, final int level, final String name, final List<Field> values) {
        indent(json, level).append('"').append(escape(name)).append("\": [\n");
        for (int i = 0; i < values.size(); i++) {
            values.get(i).appendJson(json, level + 1);
            if (i + 1 < values.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        return indent(json, level).append(']');
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
