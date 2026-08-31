package com.notquests.core.registry.fields;

import java.util.List;
import java.util.function.Supplier;

/**
 * Portable field type used by built-in registry entries.
 *
 * <p>The platform implementation decides how this maps to commands, config serialization, and
 * metadata. Methods return {@code this} so built-ins can keep the existing concise registry style.
 */
public interface RegistryField<T> {
    RegistryField<T> config(String configPath);

    RegistryField<T> progressNeeded();

    RegistryField<T> invertedBooleanConfig(String configPath);

    /** Metadata shared by the five registered type families for commands, YAML and generated docs. */
    record Definition(
            String name,
            String description,
            String valueType,
            String configPath,
            boolean progressNeeded,
            boolean flag,
            boolean presenceFlag,
            boolean invertedBooleanConfig,
            Supplier<List<String>> suggestions) {}
}
