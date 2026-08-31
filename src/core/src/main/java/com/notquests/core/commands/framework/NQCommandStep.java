package com.notquests.core.commands.framework;

import java.util.List;
import java.util.Objects;

public record NQCommandStep<A extends NQCommandSchema.Argument, S>(
        NQCommandKind kind,
        String name,
        List<String> aliases,
        A argument,
        NQDescription description,
        S suggestionOverride) {

    public NQCommandStep {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
        aliases = List.copyOf(aliases == null ? List.of() : aliases);
        description = Objects.requireNonNull(description, "description");
    }
}
