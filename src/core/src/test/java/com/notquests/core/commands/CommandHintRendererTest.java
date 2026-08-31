package com.notquests.core.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;

import com.notquests.core.commands.framework.*;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.test.TestPlatformPlayer;

class CommandHintRendererTest {
    @Test
    void stripsLeadingSlashAndShowsNextArgumentAfterTrailingSpace() {
        assertEquals(
                Component.text("/qa edit ", NamedTextColor.GRAY)
                        .append(Component.text("[quest]", TextColor.color(0x00FFFB), TextDecoration.BOLD)),
                CommandHintRenderer.render("/qa edit ", "[quest]", 8));
    }

    @Test
    void echoesTheTokenCurrentlyBeingTyped() {
        assertEquals(
                Component.text("/qa ", NamedTextColor.GRAY)
                        .append(Component.text("ed", TextColor.color(0x00FFFB), TextDecoration.BOLD)),
                CommandHintRenderer.render("/qa ed", "[subcommand]", 8));
    }

    @Test
    void truncatesOldArgumentsWithEllipsis() {
        assertEquals(
                Component.text("\u2026 c d ", NamedTextColor.GRAY)
                        .append(Component.text("[next]", TextColor.color(0x00FFFB), TextDecoration.BOLD)),
                CommandHintRenderer.render("/a b c d ", "[next]", 2));
    }

    @Test
    void miniMessageRenderingMatchesComponentRendering() {
        final String miniMessage = CommandHintRenderer.renderMiniMessage("/qa edit ", "[Quest to edit.]", 2);

        assertEquals(
                CommandHintRenderer.render("/qa edit ", "[Quest to edit.]", 2),
                MiniMessage.miniMessage().deserialize(miniMessage));
    }

    @Test
    void sendsActionBarThroughQuestPlayerLeaf() {
        final CapturingPlayer player = new CapturingPlayer();

        assertTrue(CommandHintRenderer.sendActionBarHint(player, "/qa edit ", "[Quest to edit.]", 2));

        assertEquals(
                CommandHintRenderer.render("/qa edit ", "[Quest to edit.]", 2),
                MiniMessage.miniMessage().deserialize(player.actionBar));
    }

    @Test
    void labelsCommandNodesFromDescriptions() {
        final NQCommandTree<TestArgument, NQFlag<TestArgument, Object>, Object, Runnable> tree =
                new NQCommandTree<>();
        tree.register(NQCommandBuilder
                .<TestArgument, NQFlag<TestArgument, Object>, Object, Runnable>root(
                        "qa", NQDescription.of("Admin commands."))
                .literal("edit", NQDescription.of("Edits a quest."))
                .required("quest", new TestArgument(), NQDescription.of("Quest to edit."))
                .handler(() -> {})
                .registration());

        final NQCommandTree.Node<TestArgument, NQFlag<TestArgument, Object>, Object, Runnable> questNode =
                tree.root("qa").childNodes().stream()
                        .filter(node -> node.name().equals("edit"))
                        .findFirst()
                        .orElseThrow()
                        .childNodes().stream()
                        .filter(node -> node.name().equals("quest"))
                        .findFirst()
                        .orElseThrow();

        assertEquals("[Quest to edit.]", CommandHintRenderer.hintLabel(questNode));
    }

    private static final class CapturingPlayer implements TestPlatformPlayer {
        private String actionBar = "";

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return "test-player";
        }

        @Override
        public long currentWorldTimeTicks() {
            return 0;
        }

        @Override
        public void sendMessage(final String miniMessage) {}

        @Override
        public void sendActionBar(final String miniMessage) {
            actionBar = miniMessage;
        }

        @Override
        public void showProgressBossBar(final String miniMessage, final double progress) {}

        @Override
        public void hideProgressBossBar() {}

        @Override
        public void showTitle(
                final String title,
                final String subtitle,
                final java.time.Duration fadeIn,
                final java.time.Duration stay,
                final java.time.Duration fadeOut) {}
        @Override
        public void chat(final String message) {}

        @Override
        public void performCommand(final String command) {}

        @Override
        public void closeInventory() {}

        @Override
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }
    }

    private record TestArgument() implements NQCommandSchema.Argument {
        @Override
        public String argumentTypeName() {
            return "StringArgument";
        }

        @Override
        public String valueTypeName() {
            return "text";
        }
    }
}
