package com.notquests.core.config;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.api.lowlevel.Present;
import org.snakeyaml.engine.v2.api.lowlevel.Serialize;
import org.snakeyaml.engine.v2.comments.CommentLine;
import org.snakeyaml.engine.v2.comments.CommentType;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.representer.StandardRepresenter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Mutable SnakeYAML Engine-backed NotQuests configuration. */
public final class YamlConfig {
    private final Map<String, Object> root;
    private final Map<String, Comments> comments;
    private final List<CommentLine> leadingComments;

    /** Reads a numeric YAML scalar, including the string scalars produced by older YAML writers. */
    public static Number number(final Object value, final Number fallback) {
        if (value instanceof Number number) {
            return number;
        }
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string);
            } catch (final NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private YamlConfig(final Map<String, Object> root) {
        this(root, Map.of(), List.of());
    }

    private YamlConfig(
            final Map<String, Object> root,
            final Map<String, Comments> comments,
            final List<CommentLine> leadingComments) {
        this.root = root;
        this.comments = new LinkedHashMap<>(comments);
        this.leadingComments = List.copyOf(leadingComments);
    }

    public static YamlConfig empty() {
        return new YamlConfig(new LinkedHashMap<>());
    }

    public static YamlConfig fromMap(final Map<?, ?> map) {
        return new YamlConfig(toMutableMap(map));
    }

    public static YamlConfig parse(final String yaml) {
        final LoadSettings settings = LoadSettings.builder().setParseComments(true).build();
        final Load load = new Load(settings);
        final Object loaded = yaml == null || yaml.isBlank() ? null : load.loadFromString(yaml);
        if (loaded == null) {
            return empty();
        }
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("NotQuests YAML root must be a map/object.");
        }
        final Map<String, Comments> comments = new LinkedHashMap<>();
        final Optional<Node> node = new Compose(settings).composeString(yaml);
        node.ifPresent(rootNode -> collectComments(rootNode, "$", valueNodeId("$"), comments));
        final List<CommentLine> leadingComments = leadingComments(yaml);
        node.ifPresent(rootNode -> removeLeadingCommentsFromFirstKey(rootNode, comments, leadingComments));
        return new YamlConfig(toMutableMap(map), comments, leadingComments);
    }

    public static YamlConfig load(final Path path) throws IOException {
        if (!Files.exists(path) || Files.size(path) == 0) {
            return empty();
        }
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    public static YamlConfig load(final Path path, final ValueCodec codec) throws IOException {
        final YamlConfig raw = load(path);
        return new YamlConfig(
                decodeMap(raw.asMap(), codec == null ? ValueCodec.PASSTHROUGH : codec),
                raw.comments,
                raw.leadingComments);
    }

    public static void save(final YamlConfig configuration, final Path path) throws IOException {
        save(configuration, path, ValueCodec.PASSTHROUGH);
    }

    public static void save(
            final YamlConfig configuration,
            final Path path,
            final ValueCodec codec) throws IOException {
        final Map<String, Object> encoded = encodeMap(
                configuration == null ? Map.of() : configuration.asMap(),
                codec == null ? ValueCodec.PASSTHROUGH : codec);
        if (configuration == null) {
            fromMap(encoded).save(path);
        } else {
            new YamlConfig(encoded, configuration.comments, configuration.leadingComments).save(path);
        }
    }

    public void save(final Path path) throws IOException {
        final Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, render(), StandardCharsets.UTF_8);
    }

    public Map<String, Object> asMap() {
        return toMutableMap(root);
    }

    /** Replaces the data tree while retaining comments loaded by this configuration. */
    public void replaceContents(final Map<?, ?> contents) {
        root.clear();
        if (contents != null) {
            root.putAll(toMutableMap(contents));
        }
    }

    public String render() {
        if (root.isEmpty()) {
            return "";
        }
        final DumpSettings settings = DumpSettings.builder()
                .setDefaultFlowStyle(FlowStyle.BLOCK)
                .setIndent(2)
                .setDumpComments(true)
                .build();
        final Node node = new StandardRepresenter(settings).represent(root);
        applyComments(node, "$", valueNodeId("$"), comments);
        if (!leadingComments.isEmpty()) {
            node.setBlockComments(merge(leadingComments, node.getBlockComments()));
        }
        return new Present(settings)
                .emitToString(new Serialize(settings).serializeOne(node).iterator());
    }

    public Object get(final String path) {
        return valueAt(path);
    }

    public Object get(final String path, final Object fallback) {
        final Object value = get(path);
        return value == null ? fallback : value;
    }

    public <T> T get(final String path, final Class<T> type, final T fallback) {
        final Object value = valueAt(path);
        return type.isInstance(value) ? type.cast(value) : fallback;
    }

    public String getString(final String path) {
        final Object value = valueAt(path);
        return value == null ? null : value.toString();
    }

    public String getString(final String path, final String fallback) {
        final String value = getString(path);
        return value == null ? fallback : value;
    }

    public int getInt(final String path) {
        return getInt(path, 0);
    }

    public int getInt(final String path, final int fallback) {
        final Object value = valueAt(path);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    public double getDouble(final String path) {
        return getDouble(path, 0);
    }

    public double getDouble(final String path, final double fallback) {
        final Object value = valueAt(path);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    public long getLong(final String path, final long fallback) {
        final Object value = valueAt(path);
        return value instanceof Number number ? number.longValue() : fallback;
    }

    public boolean getBoolean(final String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(final String path, final boolean fallback) {
        final Object value = valueAt(path);
        return value instanceof Boolean bool ? bool : fallback;
    }

    public List<String> getStringList(final String path) {
        final Object value = valueAt(path);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(Object::toString).toList();
    }

    public List<Integer> getIntegerList(final String path) {
        final Object value = get(path);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::intValue)
                .toList();
    }

    public boolean contains(final String path) {
        return valueAt(path) != null;
    }

    public Section section(final String path) {
        if (path == null || path.isBlank()) {
            return new Section(root);
        }
        final Object value = valueAt(path);
        return value instanceof Map<?, ?> map ? new Section(castMap(map)) : null;
    }

    public Section getConfigurationSection(final String path) {
        return section(path);
    }

    public boolean isBoolean(final String path) {
        return get(path) instanceof Boolean;
    }

    public boolean isInt(final String path) {
        return get(path) instanceof Number;
    }

    public boolean isDouble(final String path) {
        return get(path) instanceof Number;
    }

    public boolean isString(final String path) {
        return get(path) instanceof String;
    }

    public boolean isList(final String path) {
        return get(path) instanceof List<?>;
    }

    public void setComments(final String path, final List<String> comments) {
        final String nodeId = keyNodeId(configPath(path));
        if (comments == null || comments.isEmpty()) {
            final Comments existing = this.comments.get(nodeId);
            if (existing == null) {
                return;
            }
            final Comments withoutBlock = existing.withBlock(List.of());
            if (withoutBlock.empty()) {
                this.comments.remove(nodeId);
            } else {
                this.comments.put(nodeId, withoutBlock);
            }
            return;
        }
        final List<CommentLine> block = comments.stream()
                .filter(Objects::nonNull)
                .map(comment -> new CommentLine(
                        Optional.empty(),
                        Optional.empty(),
                        comment.isEmpty() || comment.startsWith(" ") ? comment : " " + comment,
                        CommentType.BLOCK))
                .toList();
        final Comments existing = this.comments.getOrDefault(nodeId, Comments.EMPTY);
        this.comments.put(nodeId, existing.withBlock(block));
    }

    public void set(final String path, final Object value) {
        final String[] parts = splitPath(path);
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            final Object child = current.get(parts[i]);
            if (child instanceof Map<?, ?> map) {
                current = castMap(map);
            } else {
                final Map<String, Object> newChild = new LinkedHashMap<>();
                current.put(parts[i], newChild);
                current = newChild;
            }
        }
        if (value == null) {
            current.remove(parts[parts.length - 1]);
            removeComments(configPath(parts));
        } else {
            current.put(parts[parts.length - 1], normalize(value));
        }
    }

    private void removeComments(final String path) {
        comments.keySet().removeIf(nodeId -> {
            final int separator = nodeId.indexOf(':');
            final String commentPath = separator < 0 ? nodeId : nodeId.substring(separator + 1);
            return commentPath.equals(path) || commentPath.startsWith(path + "/");
        });
    }

    private Object valueAt(final String path) {
        Object current = root;
        for (final String part : splitPath(path)) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = castMap(map).get(part);
        }
        return current;
    }

    private static String[] splitPath(final String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("configuration path is required");
        }
        return path.split("\\.");
    }

    private static String configPath(final String path) {
        return configPath(splitPath(path));
    }

    private static String configPath(final String[] parts) {
        String path = "$";
        for (final String part : parts) {
            path = childPath(path, part);
        }
        return path;
    }

    private static String childPath(final String parent, final String key) {
        return parent + "/k" + key.length() + ":" + key;
    }

    private static String itemPath(final String parent, final int index) {
        return parent + "/i" + index;
    }

    private static String keyNodeId(final String path) {
        return "key:" + path;
    }

    private static String valueNodeId(final String path) {
        return "value:" + path;
    }

    private static String itemNodeId(final String path) {
        return "item:" + path;
    }

    private static void collectComments(
            final Node node,
            final String path,
            final String nodeId,
            final Map<String, Comments> comments) {
        final Comments nodeComments = Comments.from(node);
        if (!nodeComments.empty()) {
            comments.put(nodeId, nodeComments);
        }
        if (node instanceof MappingNode mapping) {
            for (final NodeTuple tuple : mapping.getValue()) {
                if (!(tuple.getKeyNode() instanceof ScalarNode key)) {
                    continue;
                }
                final String childPath = childPath(path, key.getValue());
                collectComments(tuple.getKeyNode(), childPath, keyNodeId(childPath), comments);
                collectComments(tuple.getValueNode(), childPath, valueNodeId(childPath), comments);
            }
        } else if (node instanceof SequenceNode sequence) {
            for (int index = 0; index < sequence.getValue().size(); index++) {
                final String itemPath = itemPath(path, index);
                collectComments(sequence.getValue().get(index), itemPath, itemNodeId(itemPath), comments);
            }
        }
    }

    private static void applyComments(
            final Node node,
            final String path,
            final String nodeId,
            final Map<String, Comments> comments) {
        comments.getOrDefault(nodeId, Comments.EMPTY).applyTo(node);
        if (node instanceof MappingNode mapping) {
            for (final NodeTuple tuple : mapping.getValue()) {
                if (!(tuple.getKeyNode() instanceof ScalarNode key)) {
                    continue;
                }
                final String childPath = childPath(path, key.getValue());
                applyComments(tuple.getKeyNode(), childPath, keyNodeId(childPath), comments);
                applyComments(tuple.getValueNode(), childPath, valueNodeId(childPath), comments);
            }
        } else if (node instanceof SequenceNode sequence) {
            for (int index = 0; index < sequence.getValue().size(); index++) {
                final String itemPath = itemPath(path, index);
                applyComments(sequence.getValue().get(index), itemPath, itemNodeId(itemPath), comments);
            }
        }
    }

    private static List<CommentLine> leadingComments(final String yaml) {
        if (yaml == null || yaml.isEmpty()) {
            return List.of();
        }
        final ArrayList<CommentLine> comments = new ArrayList<>();
        boolean foundComment = false;
        for (final String line : yaml.split("\\R", -1)) {
            final String trimmed = line.stripLeading();
            if (trimmed.startsWith("#")) {
                comments.add(new CommentLine(
                        Optional.empty(),
                        Optional.empty(),
                        trimmed.substring(1),
                        CommentType.BLOCK));
                foundComment = true;
            } else if (trimmed.isEmpty() && foundComment) {
                comments.add(new CommentLine(
                        Optional.empty(), Optional.empty(), "\n", CommentType.BLANK_LINE));
            } else if (!trimmed.isEmpty()) {
                break;
            }
        }
        while (!comments.isEmpty()
                && comments.getLast().getCommentType() == CommentType.BLANK_LINE) {
            comments.removeLast();
        }
        return List.copyOf(comments);
    }

    private static void removeLeadingCommentsFromFirstKey(
            final Node root,
            final Map<String, Comments> comments,
            final List<CommentLine> leadingComments) {
        if (leadingComments.isEmpty()
                || !(root instanceof MappingNode mapping)
                || mapping.getValue().isEmpty()
                || !(mapping.getValue().getFirst().getKeyNode() instanceof ScalarNode firstKey)) {
            return;
        }
        final String nodeId = keyNodeId(childPath("$", firstKey.getValue()));
        final Comments existing = comments.get(nodeId);
        if (existing == null || !startsWith(existing.block(), leadingComments)) {
            return;
        }
        final Comments remaining = existing.withBlock(
                existing.block().subList(leadingComments.size(), existing.block().size()));
        if (remaining.empty()) {
            comments.remove(nodeId);
        } else {
            comments.put(nodeId, remaining);
        }
    }

    private static boolean startsWith(
            final List<CommentLine> comments,
            final List<CommentLine> prefix) {
        if (comments.size() < prefix.size()) {
            return false;
        }
        for (int index = 0; index < prefix.size(); index++) {
            final CommentLine actual = comments.get(index);
            final CommentLine expected = prefix.get(index);
            if (actual.getCommentType() != expected.getCommentType()
                    || !actual.getValue().equals(expected.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static List<CommentLine> merge(
            final List<CommentLine> first,
            final List<CommentLine> second) {
        if (second == null || second.isEmpty()) {
            return first;
        }
        final ArrayList<CommentLine> merged = new ArrayList<>(first);
        merged.addAll(second);
        return List.copyOf(merged);
    }

    private record Comments(
            List<CommentLine> block,
            List<CommentLine> inline,
            List<CommentLine> end) {
        private static final Comments EMPTY = new Comments(List.of(), List.of(), List.of());

        private Comments {
            block = copyComments(block);
            inline = copyComments(inline);
            end = copyComments(end);
        }

        private static Comments from(final Node node) {
            return new Comments(
                    node.getBlockComments(),
                    node.getInLineComments(),
                    node.getEndComments());
        }

        private Comments withBlock(final List<CommentLine> block) {
            return new Comments(block, inline, end);
        }

        private boolean empty() {
            return block.isEmpty() && inline.isEmpty() && end.isEmpty();
        }

        private void applyTo(final Node node) {
            if (!block.isEmpty()) {
                node.setBlockComments(block);
            }
            if (!inline.isEmpty()) {
                node.setInLineComments(inline);
            }
            if (!end.isEmpty()) {
                node.setEndComments(end);
            }
        }

        private static List<CommentLine> copyComments(final List<CommentLine> comments) {
            return comments == null || comments.isEmpty() ? List.of() : List.copyOf(comments);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(final Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private static Object normalize(final Object value) {
        if (value instanceof Section section) {
            return normalize(section.asMap());
        }
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> normalized = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
            }
            return normalized;
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list.stream().map(YamlConfig::normalize).toList());
        }
        return value;
    }

    private static Map<String, Object> toMutableMap(final Map<?, ?> source) {
        final Map<String, Object> copy = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
        }
        return copy;
    }

    private static Map<String, Object> encodeMap(
            final Map<String, Object> source,
            final ValueCodec codec) {
        final Map<String, Object> encoded = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            encoded.put(entry.getKey(), encodeValue(entry.getValue(), codec));
        }
        return encoded;
    }

    private static Object encodeValue(final Object value, final ValueCodec codec) {
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> encoded = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                encoded.put(String.valueOf(entry.getKey()), encodeValue(entry.getValue(), codec));
            }
            return encoded;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(entry -> encodeValue(entry, codec)).toList();
        }
        return codec.toYamlValue(value);
    }

    private static Map<String, Object> decodeMap(
            final Map<String, Object> source,
            final ValueCodec codec) {
        final Map<String, Object> decoded = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            decoded.put(entry.getKey(), decodeValue(entry.getValue(), codec));
        }
        return decoded;
    }

    private static Object decodeValue(final Object value, final ValueCodec codec) {
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> decoded = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                decoded.put(String.valueOf(entry.getKey()), decodeValue(entry.getValue(), codec));
            }
            return codec.fromYamlValue(decoded);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(entry -> decodeValue(entry, codec)).toList();
        }
        return codec.fromYamlValue(value);
    }

    public interface ValueCodec {
        ValueCodec PASSTHROUGH = new ValueCodec() {
            @Override
            public Object toYamlValue(final Object value) {
                return value;
            }

            @Override
            public Object fromYamlValue(final Object value) {
                return value;
            }
        };

        Object toYamlValue(Object value);

        Object fromYamlValue(Object value);
    }

    public record Section(Map<String, Object> map) {
        public Map<String, Object> asMap() {
            return toMutableMap(map);
        }

        public Set<String> keys(final boolean deep) {
            if (!deep) {
                return map.keySet();
            }
            final LinkedHashSet<String> keys = new LinkedHashSet<>();
            collectKeys("", map, keys);
            return keys;
        }

        public Set<String> getKeys(final boolean deep) {
            return keys(deep);
        }

        public Object get(final String path) {
            return new YamlConfig(map).get(path);
        }

        public Object get(final String path, final Object fallback) {
            final Object value = get(path);
            return value == null ? fallback : value;
        }

        public <T> T get(final String path, final Class<T> type, final T fallback) {
            return new YamlConfig(map).get(path, type, fallback);
        }

        public String getString(final String path) {
            return new YamlConfig(map).getString(path);
        }

        public String getString(final String path, final String fallback) {
            final String value = getString(path);
            return value == null ? fallback : value;
        }

        public int getInt(final String path) {
            return getInt(path, 0);
        }

        public int getInt(final String path, final int fallback) {
            final Object value = get(path);
            return value instanceof Number number ? number.intValue() : fallback;
        }

        public double getDouble(final String path, final double fallback) {
            final Object value = get(path);
            return value instanceof Number number ? number.doubleValue() : fallback;
        }

        public boolean getBoolean(final String path, final boolean fallback) {
            final Object value = get(path);
            return value instanceof Boolean bool ? bool : fallback;
        }

        public List<String> getStringList(final String path) {
            final Object value = get(path);
            if (!(value instanceof List<?> list)) {
                return List.of();
            }
            return list.stream().map(Object::toString).toList();
        }

        public boolean contains(final String path) {
            return get(path) != null;
        }

        public Section section(final String path) {
            final Object value = get(path);
            return value instanceof Map<?, ?> map ? new Section(castMap(map)) : null;
        }

        public Section getConfigurationSection(final String path) {
            return section(path);
        }

        public boolean isConfigurationSection(final String path) {
            return get(path) instanceof Map<?, ?>;
        }

        private static void collectKeys(
                final String prefix,
                final Map<String, Object> current,
                final Set<String> keys) {
            for (final Map.Entry<String, Object> entry : current.entrySet()) {
                final String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                keys.add(key);
                if (entry.getValue() instanceof Map<?, ?> child) {
                    collectKeys(key, castMap(child), keys);
                }
            }
        }
    }
}
