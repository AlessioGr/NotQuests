package com.notquests.core.commands.framework;

import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface NQCommandHandler {
    List<CommandMessage> execute(NQCommandContext context);

    static NQCommandHandler message(final Function<NQCommandContext, String> message) {
        return context -> List.of(CommandMessage.success(message.apply(context)));
    }

    static NQCommandHandler commandMessage(
            final Function<NQCommandContext, CommandMessage> commandMessage) {
        return context -> List.of(commandMessage.apply(context));
    }
}
