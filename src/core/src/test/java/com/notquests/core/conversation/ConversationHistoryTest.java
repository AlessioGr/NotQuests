package com.notquests.core.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ConversationHistoryTest {
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
