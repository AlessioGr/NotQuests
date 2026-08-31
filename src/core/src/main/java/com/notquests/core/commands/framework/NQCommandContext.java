package com.notquests.core.commands.framework;

import com.notquests.core.platform.PlatformPlayer;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface NQCommandContext {
    String argument(String name);

    Object rawArgument(String name);

    boolean flagPresent(String name);

    String flag(String name);

    Object rawFlag(String name);

    PlatformPlayer questPlayer();

    Object platformSender();

    String rawInput();

    String platformVersion();

    static Flags flags(
            final String input,
            final List<NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>> flags) {
        final DoubleDashFlagParser.ParseResult parsed = DoubleDashFlagParser.parse(
                input,
                flags,
                NQFlag::name,
                NQFlag::isPresence,
                (flag, value) -> {
                    if (flag.valueArgument() == null) {
                        return value;
                    }
                    try {
                        return flag.valueArgument().convert(value);
                    } catch (final RuntimeException ignored) {
                        return value;
                    }
                });
        return new Flags(parsed.values(), parsed.present());
    }

    record Flags(Map<String, Object> values, Set<String> present) {
        public Flags {
            values = Map.copyOf(values == null ? Map.of() : values);
            present = Set.copyOf(present == null ? Set.of() : present);
        }

        public boolean contains(final String name) {
            return present.contains(name);
        }

        public String value(final String name) {
            final Object value = rawValue(name);
            return value == null ? "" : String.valueOf(value);
        }

        public Object rawValue(final String name) {
            return values.get(name);
        }
    }
}
