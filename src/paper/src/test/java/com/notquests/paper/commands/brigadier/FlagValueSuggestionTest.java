package com.notquests.paper.commands.brigadier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.NotQuestsCommands;
import com.notquests.core.commands.framework.NQArgumentType;
import com.notquests.core.commands.framework.NQCommandBuilder;
import com.notquests.core.commands.framework.NQCommandContext;
import com.notquests.core.commands.framework.NQCommandHandler;
import com.notquests.core.commands.framework.NQCommandTree;
import com.notquests.core.commands.framework.NQDescription;
import com.notquests.core.commands.framework.NQFlag;
import com.notquests.core.commands.framework.NQSuggestionProvider;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.paper.NotQuests;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

class FlagValueSuggestionTest {
    private PaperCoreCommandCompiler compiler;
    private NQCommandTree<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            tree;

    @BeforeEach
    void setUp() {
        final NotQuests main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsCommands commands = NotQuestsCommands.create(
                plugin,
                registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                () -> "test",
                () -> "test",
                () -> Path.of("."));
        compiler = new PaperCoreCommandCompiler(main, commands);
        tree = new NQCommandTree<>();
        tree.register(NQCommandBuilder.<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        root("root", NQDescription.of("root"))
                .literal("action", NQDescription.of("action"))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                "delay", NQDescription.of("Delay before running the action."))
                        .withArgument(NQArgumentType.duration())
                        .build())
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                "player", NQDescription.of("Player used as the action target."))
                        .withArgument(NQArgumentType.word("player"))
                        .build())
                .handler(context -> List.of())
                .registration());
        tree.register(NQCommandBuilder.<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        root("root", NQDescription.of("root"))
                .literal("npc", NQDescription.of("npc"))
                .required("selector", NQArgumentType.npcSelector(), NQDescription.of("NPC selector"))
                .handler(context -> List.of())
                .registration());
        tree.register(NQCommandBuilder.<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        root("root", NQDescription.of("root"))
                .literal("boolean", NQDescription.of("boolean"))
                .required("value", NQArgumentType.bool("true or false"), NQDescription.of("value"))
                .handler(context -> List.of())
                .registration());
        tree.register(NQCommandBuilder.<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        root("root", NQDescription.of("root"))
                .literal("number", NQDescription.of("number"))
                .required("value", NQArgumentType.number("number"), NQDescription.of("value"))
                .handler(context -> List.of())
                .registration());
        tree.register(NQCommandBuilder.<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        root("root", NQDescription.of("root"))
                .literal("profiles", NQDescription.of("profiles"))
                .literal("show", NQDescription.of("show"))
                .handler(context -> List.of())
                .registration());
        tree.register(NQCommandBuilder.<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        root("root", NQDescription.of("root"))
                .literal("optional", NQDescription.of("optional"))
                .optional("player", NQArgumentType.word("player"), NQDescription.of("optional player"))
                .handler(context -> List.of())
                .registration());
    }

    @SuppressWarnings("unchecked")
    private LiteralCommandNode<CommandSourceStack> compiledRoot() throws Exception {
        final Object root = tree.root("root");

        final Method compile = PaperCoreCommandCompiler.class.getDeclaredMethod("literal", NQCommandTree.Node.class, List.class);
        compile.setAccessible(true);
        return ((com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack>)
                        compile.invoke(compiler, root, List.of(root)))
                .build();
    }

    private List<String> completionsFor(final String input) throws Exception {
        final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(compiledRoot());
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, null))
                .get()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .toList();
    }

    @Test
    void delayFlagSuggestsDurationsInsteadOfFlagNamesWhenAwaitingValue() throws Exception {
        final List<String> suggestions = completionsFor("root action --delay ");
        assertTrue(suggestions.contains("500ms"), "delay value suggestions should include millisecond examples: " + suggestions);
        assertTrue(suggestions.contains("1s"), "delay value suggestions should include duration examples: " + suggestions);
        assertFalse(suggestions.contains("--delay"), "delay value position must not re-suggest flags: " + suggestions);
        assertFalse(suggestions.contains("--player"), "delay value position must not re-suggest flags: " + suggestions);
    }

    @Test
    void valueFlagWithNoSuggestionsDoesNotFallBackToFlagNamesWhenAwaitingValue() throws Exception {
        final List<String> suggestions = completionsFor("root action --player ");
        assertFalse(suggestions.contains("--delay"), "player value position must not re-suggest flags: " + suggestions);
        assertFalse(suggestions.contains("--player"), "player value position must not re-suggest flags: " + suggestions);
    }

    @Test
    void nestedLiteralsHaveNoAliasNodes() throws Exception {
        final var profiles = compiledRoot().getChild("profiles");

        assertNotNull(profiles.getChild("show"), "primary child literal should be registered");
        assertNull(profiles.getChild("view"), "child literal aliases must not be registered");
        assertNull(profiles.getChild("list"), "child literal aliases must not be registered");
    }

    @Test
    void onlyCanonicalNestedLiteralsAreSuggested() throws Exception {
        assertEquals(List.of("show"), completionsFor("root profiles "));
    }

    @Test
    void optionalArgumentsCompileAsOmittedAndExplicitForms() throws Exception {
        final var optional = compiledRoot().getChild("optional");

        assertNotNull(optional.getCommand(), "optional argument command should execute without the optional value");
        assertNotNull(optional.getChild("player"), "optional argument node should still be registered");
        assertNotNull(optional.getChild("player").getCommand(), "optional argument command should execute with the value");
    }

    @Test
    void portableBooleanAndNumberSuggestionsReachPaperBrigadier() throws Exception {
        final List<String> booleans = completionsFor("root boolean ");
        assertTrue(booleans.contains("true"));
        assertTrue(booleans.contains("false"));

        final List<String> numbers = completionsFor("root number ");
        assertTrue(numbers.contains("1"));
        assertTrue(numbers.contains("10"));
        assertTrue(numbers.contains("100"));
    }

    @Test
    void npcSelectorsAcceptCitizensIdsAndFancyNpcsUuids() throws Exception {
        assertParses("root npc citizens:0");
        assertParses("root npc fancynpcs:b7ffc743-0e12-4415-bf43-feb69a73f649");
    }

    private void assertParses(final String input) throws Exception {
        final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(compiledRoot());
        final var parsed = dispatcher.parse(input, null);

        assertTrue(parsed.getExceptions().isEmpty(), () -> "Could not parse " + input + ": " + parsed.getExceptions());
        assertFalse(parsed.getReader().canRead(), () -> "Parser left trailing data for " + input);
    }
}
