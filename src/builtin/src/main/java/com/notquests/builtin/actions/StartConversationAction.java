package com.notquests.builtin.actions;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Locale;

public final class StartConversationAction {
    private static final String CONVERSATION = "conversation";
    private static final String END_PREVIOUS = "endPrevious";

    private StartConversationAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("StartConversation")
                .displayName("Start Conversation")
                .description("Starts a NotQuests conversation for the target player.")
                .field(
                        CONVERSATION,
                        adapter.fields().text(plugin::conversationNames).config("specifics.conversation"),
                        "Conversation that should start for the target player.")
                .flag(
                        END_PREVIOUS,
                        adapter.fields().presenceFlag().config("specifics.endPrevious"),
                        "Ends the player's currently open conversation before starting this one.")
                .singleLine((action, arguments) -> {
                    action.setValue(CONVERSATION, arguments.get(0));
                    action.setValue(END_PREVIOUS, String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--endprevious"));
                })
                .execute((action, questPlayer, objects) ->
                        plugin.startConversation(questPlayer, action.text(CONVERSATION), action.flag(END_PREVIOUS), adapter::warn))
                .actionDescription((action, questPlayer, objects) -> "Starts conversation: " + action.text(CONVERSATION))
                .register();
    }
}
