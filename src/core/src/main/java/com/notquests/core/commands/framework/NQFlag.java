package com.notquests.core.commands.framework;

import java.util.Objects;

public final class NQFlag<A extends NQCommandSchema.Argument, S> implements NQCommandSchema.Flag {
    private final String name;
    private final A valueArgument;
    private final NQDescription description;
    private final S valueSuggestions;

    private NQFlag(
            final String name,
            final A valueArgument,
            final NQDescription description,
            final S valueSuggestions) {
        this.name = Objects.requireNonNull(name, "name");
        this.valueArgument = valueArgument;
        this.description = description == null ? NQDescription.EMPTY : description;
        this.valueSuggestions = valueSuggestions;
    }

    public static <A extends NQCommandSchema.Argument, S> NQFlag<A, S> presence(
            final String name, final NQDescription description) {
        return new NQFlag<>(name, null, description, null);
    }

    public static <A extends NQCommandSchema.Argument, S> Builder<A, S> builder(
            final String name, final NQDescription description) {
        return new Builder<>(name, description);
    }

    @Override
    public String name() {
        return name;
    }

    public A valueArgument() {
        return valueArgument;
    }

    @Override
    public NQCommandSchema.Argument valueArgumentInfo() {
        return valueArgument;
    }

    @Override
    public boolean isPresence() {
        return valueArgument == null;
    }

    @Override
    public NQDescription description() {
        return description;
    }

    public S valueSuggestions() {
        return valueSuggestions;
    }

    public static final class Builder<A extends NQCommandSchema.Argument, S> {
        private final String name;
        private final NQDescription description;
        private A valueArgument;
        private S valueSuggestions;

        private Builder(final String name, final NQDescription description) {
            this.name = Objects.requireNonNull(name, "name");
            this.description = Objects.requireNonNull(description, "description");
        }

        public Builder<A, S> withArgument(final A valueArgument) {
            this.valueArgument = valueArgument;
            return this;
        }

        public Builder<A, S> withSuggestions(final S valueSuggestions) {
            this.valueSuggestions = valueSuggestions;
            return this;
        }

        public NQFlag<A, S> build() {
            return new NQFlag<>(name, valueArgument, description, valueSuggestions);
        }
    }
}
