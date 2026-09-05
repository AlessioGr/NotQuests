package com.notquests.core.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.notquests.core.NotQuestsPlugin;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ConversationHistoryTest {
    @Test
    void leavingClearsConversationAndChatHistoryBeforeThePlayerRejoins() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final String playerId = UUID.randomUUID().toString();
        plugin.rememberNonConversationDisplayMessage(playerId, Component.text("before leaving"));
        plugin.conversationManager().rememberConversationMessage(playerId, Component.text("conversation"));

        plugin.playerLeft(playerId, null, "world", null, ignored -> {});

        assertNull(plugin.conversationOptionReplay(playerId));
        plugin.rememberNonConversationDisplayMessage(playerId, Component.text("after joining"));
        plugin.conversationManager().rememberConversationMessage(playerId, Component.text("new conversation"));
        final Component replay = plugin.conversationOptionReplay(playerId);
        assertNotNull(replay);
        assertEquals(Component.text("\n".repeat(100)).append(Component.text("")
                .append(Component.text("after joining"))
                .append(Component.newline())), replay);
    }

    @Test
    @DisplayName("conversation messages are not recorded as normal chat history")
    void skipsConversationMessagesWhenRecordingNormalChat() {
        final ConversationManager store = new ConversationManager();
        final String playerId = UUID.randomUUID().toString();
        final Component conversationLine = Component.text("Choose an answer");

        store.rememberConversationMessage(playerId, conversationLine);
        store.rememberNonConversationMessage(playerId, conversationLine, 20);

        assertNull(store.removeConversationMessages(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("normal chat history is trimmed to the configured replay size")
    void trimsNormalChatHistory() {
        final ConversationManager store = new ConversationManager();
        final String playerId = UUID.randomUUID().toString();
        final Component first = Component.text("first");
        final Component second = Component.text("second");
        final Component third = Component.text("third");

        store.rememberNonConversationMessage(playerId, first, 2);
        store.rememberNonConversationMessage(playerId, second, 2);
        store.rememberNonConversationMessage(playerId, third, 2);

        store.rememberConversationMessage(playerId, Component.text("trigger"));
        final Component replay = store.removeConversationMessages(playerId);
        assertEquals(Component.text("\n".repeat(100)).append(Component.text("")
                .append(second).append(Component.newline())
                .append(third).append(Component.newline())), replay);
    }

    @Test
    void removeConversationMessagesReturnsNullWhenNoHistoryExists() {
        final ConversationManager store = new ConversationManager();
        assertNull(store.removeConversationMessages(UUID.randomUUID().toString()));
    }
}
