package com.notquests.core.commands.framework;

import java.util.List;
import java.util.Objects;

public record NQCommandRegistration<A extends NQCommandSchema.Argument, F extends NQCommandSchema.Flag, S, H>(
        List<NQCommandStep<A, S>> steps,
        List<F> flags,
        NQDescription commandDescription,
        String permission,
        Class<?> senderType,
        H handler) {

    public NQCommandRegistration {
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        flags = List.copyOf(flags == null ? List.of() : flags);
        commandDescription = commandDescription == null ? NQDescription.EMPTY : commandDescription;
    }
}
