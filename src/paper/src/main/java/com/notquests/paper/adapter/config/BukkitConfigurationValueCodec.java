package com.notquests.paper.adapter.config;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts Paper/Bukkit runtime values to and from SnakeYAML-safe Java values. */
public final class BukkitConfigurationValueCodec {
    private static final String BUKKIT_TYPE_KEY = "==";
    private static final String NOTQUESTS_TYPE_KEY = "__notquestsType";
    private static final String ITEM_STACK_TYPE = "ItemStack";
    private static final String ITEM_STACK_DATA_KEY = "serialized";

    private BukkitConfigurationValueCodec() {}

    public static Object toYamlValue(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof ItemStack itemStack) {
            return itemStackToYamlValue(itemStack);
        }
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> converted = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                converted.put(String.valueOf(entry.getKey()), toYamlValue(entry.getValue()));
            }
            return converted;
        }
        if (value instanceof List<?> list) {
            final List<Object> converted = new ArrayList<>();
            for (final Object entry : list) {
                converted.add(toYamlValue(entry));
            }
            return converted;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        throw new IllegalArgumentException(
                "Unsupported Bukkit configuration value type: " + value.getClass().getName());
    }

    public static Object fromYamlValue(final Object value) {
        if (value instanceof List<?> list) {
            final List<Object> converted = new ArrayList<>();
            for (final Object entry : list) {
                converted.add(fromYamlValue(entry));
            }
            return converted;
        }
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> source = stringMap(map);
            if (ITEM_STACK_TYPE.equals(source.get(NOTQUESTS_TYPE_KEY))) {
                return itemStackFromYamlValue(source);
            }
            final Map<String, Object> converted = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : source.entrySet()) {
                converted.put(String.valueOf(entry.getKey()), fromYamlValue(entry.getValue()));
            }
            return converted;
        }
        return value;
    }

    private static Map<String, Object> itemStackToYamlValue(final ItemStack itemStack) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put(NOTQUESTS_TYPE_KEY, ITEM_STACK_TYPE);
        map.put(ITEM_STACK_DATA_KEY, serializedValue(itemStack.serialize()));
        return map;
    }

    private static ItemStack itemStackFromYamlValue(final Map<String, Object> map) {
        final Object raw = map.get(ITEM_STACK_DATA_KEY);
        final Object decoded = serializedValueFromYaml(raw);
        if (!(decoded instanceof Map<?, ?> serialized)) {
            throw new IllegalArgumentException("ItemStack YAML value is missing serialized data.");
        }
        return ItemStack.deserialize(stringMap(serialized));
    }

    private static Object serializedValue(final Object value) {
        if (value instanceof ConfigurationSerializable serializable) {
            final Map<String, Object> serialized = new LinkedHashMap<>();
            serialized.put(BUKKIT_TYPE_KEY, ConfigurationSerialization.getAlias(serializable.getClass()));
            for (final Map.Entry<String, Object> entry : serializable.serialize().entrySet()) {
                serialized.put(entry.getKey(), serializedValue(entry.getValue()));
            }
            return serialized;
        }
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> serialized = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                serialized.put(String.valueOf(entry.getKey()), serializedValue(entry.getValue()));
            }
            return serialized;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(BukkitConfigurationValueCodec::serializedValue).toList();
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return value;
    }

    private static Object serializedValueFromYaml(final Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(BukkitConfigurationValueCodec::serializedValueFromYaml).toList();
        }
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> decoded = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                decoded.put(String.valueOf(entry.getKey()), serializedValueFromYaml(entry.getValue()));
            }
            if (decoded.containsKey(BUKKIT_TYPE_KEY)) {
                return ConfigurationSerialization.deserializeObject(decoded);
            }
            return decoded;
        }
        return value;
    }

    private static Map<String, Object> stringMap(final Map<?, ?> source) {
        final Map<String, Object> converted = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : source.entrySet()) {
            converted.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return converted;
    }

}
