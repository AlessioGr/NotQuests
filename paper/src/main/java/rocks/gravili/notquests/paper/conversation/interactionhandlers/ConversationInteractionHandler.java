package rocks.gravili.notquests.paper.conversation.interactionhandlers;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.conversation.ConversationLine;
import rocks.gravili.notquests.paper.conversation.ConversationPlayer;
import rocks.gravili.notquests.paper.conversation.Speaker;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public interface ConversationInteractionHandler {

  void sendText(final String text, final Speaker speaker, final Player player, final QuestPlayer questPlayer, final
  Conversation conversation, final ConversationLine conversationLine, final boolean deletePrevious, final ConversationPlayer conversationPlayer);

  void sendOption(final String optionMessage, final Speaker speaker, final Player player, final QuestPlayer questPlayer, final
  Conversation conversation, final ConversationLine conversationLine, final ConversationPlayer conversationPlayer);
}
