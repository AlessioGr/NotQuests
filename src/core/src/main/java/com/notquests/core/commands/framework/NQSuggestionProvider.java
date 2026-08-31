package com.notquests.core.commands.framework;

import java.util.List;

@FunctionalInterface
public interface NQSuggestionProvider<C> {
    List<String> suggest(C context, String input);
}
