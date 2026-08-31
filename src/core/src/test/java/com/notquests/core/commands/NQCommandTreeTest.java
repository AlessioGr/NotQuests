package com.notquests.core.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.notquests.core.commands.framework.*;

import java.util.List;

final class NQCommandTreeTest {
    @Test
    void mergesRegistryEntriesAndExportsExecutableCommandInfo() {
        final NQCommandTree<TestArgument, NQFlag<TestArgument, Object>, Object, Runnable> tree =
                new NQCommandTree<>();
        final TestArgument quest = new TestArgument("QuestArgument", "quest name");
        final TestArgument displayName = new TestArgument("StringArgument", "text");

        tree.register(NQCommandBuilder
                .<TestArgument, NQFlag<TestArgument, Object>, Object, Runnable>root(
                        "qa", NQDescription.of("Admin commands."), "notquestsadmin")
                .literal("edit", NQDescription.of("Edits a quest."))
                .required("quest", quest, NQDescription.of("Quest to edit."))
                .literal("displayName", NQDescription.of("Quest display name."))
                .literal("set", NQDescription.of("Sets the display name."))
                .required("display-name", displayName, NQDescription.of("New display name."))
                .flag(NQFlag.presence("silent", NQDescription.of("Suppresses chat output.")))
                .commandDescription(NQDescription.of("Sets a quest display name."))
                .permission("notquests.admin")
                .senderType(TestSender.class)
                .handler(() -> {})
                .registration());

        assertNotNull(tree.root("qa"));
        assertFalse(tree.commandInfos().stream().anyMatch(info -> info.syntax().equals("/qa edit <quest>")));

        final NQCommandSchema.CommandInfo command = tree.commandInfos().stream()
                .filter(info -> info.syntax().equals("/qa edit <quest> displayName set <display-name> [--silent]"))
                .findFirst()
                .orElseThrow();
        assertEquals("/qa edit <quest> displayName set <display-name> [--silent]", command.syntax());
        assertEquals("Sets a quest display name.", command.description());
        assertEquals(List.of("notquestsadmin"), command.rootAliases());
        assertEquals("notquests.admin", command.permission());
        assertEquals("TestSender", command.senderType());
        assertEquals("quest", command.segments().get(2).name());
        assertEquals("QuestArgument", command.segments().get(2).argumentType());
        assertEquals("quest name", command.segments().get(2).valueType());
        assertEquals("silent", command.flags().getFirst().name());

        final var root = tree.root("qa");
        final var edit = root.childNodes().stream().filter(node -> node.name().equals("edit")).findFirst().orElseThrow();
        assertEquals(List.of("notquestsadmin"), root.aliases());
        assertEquals(List.of(), edit.aliases());
        assertEquals(List.of(root, edit), edit.appendTo(List.of(root)));
    }

    private record TestArgument(String argumentTypeName, String valueTypeName) implements NQCommandSchema.Argument {}

    private static final class TestSender {}
}
